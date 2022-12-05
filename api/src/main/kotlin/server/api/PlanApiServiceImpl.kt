package server.api

import engine.context.Context
import engine.context.emptyContext
import engine.expressions.Root
import methods.methodRegistry
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.springframework.stereotype.Service
import parser.parseExpression
import server.models.SolveRequest
import server.models.Transformation

@Service
class PlanApiServiceImpl : PlansApiService {
    override fun applyPlan(planId: String, solveRequest: SolveRequest): Transformation {
        val plan =
            methodRegistry.getMethodByName(planId) ?: throw NotFoundException("plan not found")
        val expr = try {
            parseExpression(solveRequest.input)
        } catch (e: ParseCancellationException) {
            throw InvalidExpressionException(solveRequest.input, e)
        }
        val context = getContext(solveRequest.context)
        val trans = plan.tryExecute(context, expr.withOrigin(Root()))
            ?: throw PlanNotApplicableException(planId)
        val modeller = TransformationModeller(format = solveRequest.format)
        return modeller.modelTransformation(trans)
    }

    override fun getPlan(planId: String): Any {
        TODO()
    }

    override fun listPlans(): List<String> {
        return methodRegistry.publicEntries.map { it.methodId.toString() }.toList()
    }
}

fun getContext(apiCtx: server.models.Context?) = apiCtx?.let {
    Context(
        curriculum = apiCtx.curriculum,
        precision = apiCtx.precision?.toInt()
    )
} ?: emptyContext
