package transformations

import patterns.*
import steps.NumericLCM
import steps.Skill

object AddLikeFractions : Rule {
    private val num1 = IntegerPattern()
    private val num2 = IntegerPattern()
    private val denom = IntegerPattern()
    private val f1 = fractionOf(num1, denom)
    private val f2 = fractionOf(num2, denom)
    private val sum = sumContaining(f1, f2)

    override val pattern = sum

//    override fun getTransformation(match: Match): Transformation {
//        class AddLikeFractionsTransformation(
//            val first: Expression,
//            val second: Expression,
//        ) : Transformation("AddLikeFractions", sequenceOf(first, second))
//
//        return AddLikeFractionsTransformation(
//            f1.getExpressionBinding(match),
//            f2.getExpressionBinding(match),
//        )
//    }

    override fun getSkills(match: Match): Sequence<Skill> {
        return sequenceOf(NumericLCM(num1.getIntBinding(match), num2.getIntBinding(match)))
    }

    override fun apply(match: Match) =
        makeFractionOf(makeSumOf(move(num1), move(num2)), factor(denom)).makeExpression(match)
}
