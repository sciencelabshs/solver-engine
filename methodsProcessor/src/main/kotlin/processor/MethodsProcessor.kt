package processor

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSDefaultVisitor
import java.io.OutputStream

/**
 * This scans the code for enum entries annotated with @PublicMethod and creates a [methodRegistry] object in the
 * [mehods] package which registers all annotated entries as publicly available methods.
 */
class MethodsProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {

    private lateinit var file: OutputStream
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) {
            return emptyList()
        }

        val symbols = resolver.getSymbolsWithAnnotation("engine.methods.PublicMethod").toList()
        val ret = symbols.filter { !it.validate() }.toList()

        file = codeGenerator.createNewFile(
            Dependencies(true, *symbols.map { it.containingFile!! }.toTypedArray()),
            "methods",
            "PublicMethods",
            "kt"
        )

        val writer = file.writer()
        with(writer) {
            appendLine("package methods\n")
            appendLine("import engine.methods.MethodRegistry")
            appendLine("import engine.methods.SimpleMethodId\n")
            appendLine("val methodRegistry = run {")
            appendLine("    val registry = MethodRegistry()")
            for (item in symbols.filter { it.validate() }.map { it.accept(PublicMethodVisitor(), Unit) }) {
                appendLine("    registry.registerEntry(")
                appendLine("        MethodRegistry.EntryData(")
                appendLine("            SimpleMethodId(\"${item.category}\", \"${item.name}\"),")
                appendLine("            true,")
                appendLine("            \"\"\"${item.description.trim()}\"\"\",")
                appendLine("            ${item.implementationName}")
                appendLine("        )")
                appendLine("    )")
            }
            appendLine("    registry")
            appendLine("}")
        }
        writer.close()

        invoked = true
        return ret
    }

    private inner class PublicMethodVisitor : KSDefaultVisitor<Unit, Entry>() {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit): Entry {
            val parentDeclaration = classDeclaration.parentDeclaration
            if (classDeclaration.qualifiedName == null || parentDeclaration == null || parentDeclaration.qualifiedName == null) {
                throw invalidNodeError(classDeclaration)
            }
            val category = classDeclaration.parentDeclaration!!.qualifiedName!!.getShortName().removeSuffix("Plans")
            val qname = classDeclaration.qualifiedName!!
            return Entry(
                category = category,
                name = qname.getShortName(),
                implementationName = qname.asString(),
                description = classDeclaration.docString ?: ""
            )
        }

        override fun defaultHandler(node: KSNode, data: Unit): Entry {
            throw invalidNodeError(node)
        }

        private fun invalidNodeError(node: KSNode) = InvalidPublicMethodException(
            "The object at ${node.location} is not a valid target for @PublicMethod. " +
                "Annotated object must be an enum entry."
        )
    }
}

private data class Entry(
    val category: String,
    val name: String,
    val implementationName: String,
    val description: String
)

class InvalidPublicMethodException(msg: String) : Exception(msg)

class MethodsProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return MethodsProcessor(environment.codeGenerator)
    }
}
