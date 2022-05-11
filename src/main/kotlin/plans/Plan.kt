package plans

import expressions.Subexpression
import patterns.*
import steps.Skill
import steps.Transformation
import transformations.*

interface Plan {

    val pattern: Pattern
    fun execute(match: Match, sub: Subexpression): Transformation?

    fun tryExecute(sub: Subexpression): Transformation? {
        for (match in pattern.findMatches(sub, RootMatch)) {
            return execute(match, sub)
        }
        return null
    }

    // fun getExplanation(match: Match): Explanation
    fun getSkills(match: Match): Sequence<Skill> = emptySequence()
}

data class Deeply(val plan: Plan) : Plan {

    override val pattern = FindPattern(plan.pattern)
    override fun execute(match: Match, sub: Subexpression): Transformation? {
        val step = plan.execute(match, match.getLastBinding(plan.pattern)!!) ?: return null
        return Transformation(sub.path, sub.expr, sub.subst(step.toSubexpr).expr, emptyList(), listOf(step))
    }
}

data class WhilePossible(val plan: Plan) : Plan {

    override val pattern = plan.pattern

    override fun execute(match: Match, sub: Subexpression): Transformation? {
        var lastStep: Transformation? = plan.execute(match, sub)
        var lastSub = sub
        val steps: MutableList<Transformation> = mutableListOf()
        while (lastStep != null) {
            steps.add(lastStep)
            lastSub = lastSub.subst(lastStep.toSubexpr)
            lastStep = plan.tryExecute(lastSub)
        }
        return Transformation(sub.path, sub.expr, lastSub.expr, emptyList(), steps)
    }
}

interface PlanPipeline : Plan {

    val plans : List<Plan>

    override fun execute(match: Match, sub: Subexpression): Transformation? {
        val steps = mutableListOf<Transformation>()
        var lastSub = sub
        for (plan in plans) {
            val step = plan.tryExecute(lastSub)
            if (step != null) {
                lastSub = lastSub.subst(step.toSubexpr)
                steps.add(step)
            }
        }

        return Transformation(sub.path, sub.expr, lastSub.expr, emptyList(), steps)
    }
}

object ConvertMixedNumberToImproperFraction : PlanPipeline {
    override val pattern = MixedNumberPattern()

    override val plans = listOf(
        SplitMixedNumber,
        AddIntegerToFraction,
        WhilePossible(Deeply(EvaluateIntegerProduct)),
        AddLikeFractions,
        WhilePossible(Deeply(EvaluateIntegerSum)),
    )
}

object AddUnlikeFractions : PlanPipeline {
    private val f1 = fractionOf(IntegerPattern(), IntegerPattern())
    private val f2 = fractionOf(IntegerPattern(), IntegerPattern())

    override val pattern = sumContaining(f1, f2)

    override val plans = listOf(
        CommonDenominator,
        WhilePossible(Deeply(EvaluateIntegerProduct)),
        AddLikeFractions,
        WhilePossible(Deeply(EvaluateIntegerSum)),
    )
}

object AddMixedNumbers : PlanPipeline {
    override val pattern = sumOf(MixedNumberPattern(), MixedNumberPattern())

    override val plans = listOf(
        WhilePossible(Deeply(ConvertMixedNumberToImproperFraction)),
        AddUnlikeFractions,
        FractionToMixedNumber,
    )
}