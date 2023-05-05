package engine.operators

const val ARITY_NULL = 0
const val ARITY_ONE = 1
const val ARITY_TWO = 2
const val ARITY_THREE = 3
const val ARITY_VARIABLE = -1
const val ARITY_VARIABLE_FROM_ZERO = -2

const val MAX_CHILD_COUNT = 1000

data class RenderContext(val align: Boolean = false) {
    companion object {
        val Default = RenderContext()
    }
}

interface LatexRenderable {
    fun toLatexString(ctx: RenderContext = RenderContext.Default): String

    /** Generates the LaTeX of this, prepended with a plus sign, if it would need a plus sign in front of it in the
     * situation where it was the second term in a sum/difference. */
    fun toLatexStringAsSecondTermInASum(ctx: RenderContext): String

    /** Generates a string representation of this, prepended with a plus sign, if it would need a plus sign in front
     * of it in the situation where it was the second term in a sum/difference. */
    fun toReadableStringAsSecondTermInASum(): String

    /** If this is true, then when this term is rendered/printed in a product, a ÷ sign should be used instead of a
     * multiplication sign in front of this term */
    fun isInlineDivideByTerm(): Boolean
}

enum class OperatorKind {
    EXPRESSION,
    SET,
    STATEMENT,
    SET_ELEMENT,
    INNER,
}

interface Operator {
    val name: String

    val precedence: Int
    val arity: Int
    val kind: OperatorKind

    fun nthChildAllowed(n: Int, op: Operator): Boolean

    fun childrenAllowed(ops: Iterable<Operator>): Boolean {
        return ops.withIndex().all { (i, op) -> nthChildAllowed(i, op) }
    }

    fun minChildCount(): Int = when (arity) {
        ARITY_VARIABLE -> 2
        ARITY_VARIABLE_FROM_ZERO -> 0
        else -> arity
    }

    fun maxChildCount(): Int = if (arity <= ARITY_VARIABLE) MAX_CHILD_COUNT else arity

    fun <T> readableString(children: List<T>): String {
        return "${toString()}(${children.joinToString(", ")})"
    }

    fun latexString(ctx: RenderContext, children: List<LatexRenderable>): String
}

interface UnaryOperator : Operator {

    override val arity get() = ARITY_ONE
    fun childAllowed(op: Operator) = op.precedence > this.precedence

    override fun nthChildAllowed(n: Int, op: Operator): Boolean {
        require(n == 0)
        return childAllowed(op)
    }

    fun <T> readableString(child: T): String {
        return "$this($child)"
    }

    override fun <T> readableString(children: List<T>): String {
        require(children.size == arity)
        return readableString(children[0])
    }

    fun latexString(ctx: RenderContext, child: LatexRenderable): String

    override fun latexString(ctx: RenderContext, children: List<LatexRenderable>): String {
        require(children.size == arity)
        return latexString(ctx, children[0])
    }
}

interface BinaryOperator : Operator {

    override val arity get() = ARITY_TWO

    fun leftChildAllowed(op: Operator) = op.precedence > this.precedence
    fun rightChildAllowed(op: Operator) = op.precedence > this.precedence

    override fun nthChildAllowed(n: Int, op: Operator) = when (n) {
        0 -> leftChildAllowed(op)
        1 -> rightChildAllowed(op)
        else -> throw IllegalArgumentException(
            "Binary operator ${this::class.simpleName} should have exactly two children. " +
                "Child $op is invalid at position $n.",
        )
    }

    fun <T> readableString(left: T, right: T): String {
        return "$this($left, $right)"
    }

    override fun <T> readableString(children: List<T>): String {
        require(children.size == arity)
        return readableString(children[0], children[1])
    }

    fun latexString(ctx: RenderContext, left: LatexRenderable, right: LatexRenderable): String

    override fun latexString(ctx: RenderContext, children: List<LatexRenderable>): String {
        require(children.size == arity)
        return latexString(ctx, children[0], children[1])
    }
}
