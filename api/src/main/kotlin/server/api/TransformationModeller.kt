package server.api

import engine.expressions.RootPath
import server.models.Format
import server.models.MappedExpression
import server.models.Metadata
import server.models.PathMapping
import server.models.Transformation

data class TransformationModeller(val format: Format) {

    fun modelTransformation(trans: engine.steps.Transformation): Transformation {
        return Transformation(
            path = trans.fromExpr.origin.path.toString(),
            fromExpr = modelExpression(trans.fromExpr),
            toExpr = modelExpression(trans.toExpr.removeBrackets()),
            pathMappings = modelPathMappings(trans.toExpr.pathMappings(trans.fromExpr.origin.path!!)),
            explanation = trans.explanation?.let { modelMetadata(it) },
            skills = trans.skills.map { modelMetadata(it) },
            steps = trans.steps?.let { step -> step.map { modelTransformation(it) } }
        )
    }

    private fun modelPathMappings(mappings: Sequence<engine.expressions.PathMapping>): List<PathMapping> {
        return mappings.map { modelPathMapping(it) }.toList()
    }

    private fun modelPathMapping(mapping: engine.expressions.PathMapping): PathMapping {
        return PathMapping(
            type = mapping.type.toString(),
            fromPaths = mapping.fromPaths.map { it.toString() },
            toPaths = mapping.toPaths.map { it.toString() }
        )
    }

    private fun modelMetadata(metadata: engine.steps.metadata.Metadata): Metadata {
        return Metadata(
            key = metadata.key.keyName,
            params = metadata.mappedParams.map {
                MappedExpression(
                    expression = modelExpression(it),
                    pathMappings = modelPathMappings(it.pathMappings(RootPath))
                )
            }
        )
    }

    private fun modelExpression(expr: engine.expressions.Expression): Any {
        return when (format) {
            Format.Latex -> expr.toLatexString()
            Format.Solver -> expr.toString()
            Format.Json -> expr.toJson()
        }
    }
}
