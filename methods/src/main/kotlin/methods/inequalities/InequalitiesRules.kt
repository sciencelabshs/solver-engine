package methods.inequalities

import engine.conditions.Sign
import engine.conditions.signOf
import engine.expressions.Constants
import engine.expressions.fractionOf
import engine.expressions.inverse
import engine.expressions.isFraction
import engine.expressions.isNeg
import engine.expressions.productOf
import engine.expressions.simplifiedNegOf
import engine.expressions.solutionOf
import engine.expressions.xp
import engine.methods.Rule
import engine.methods.RunnerMethod
import engine.methods.rule
import engine.methods.ruleResult
import engine.patterns.AnyPattern
import engine.patterns.ConstantInSolutionVariablePattern
import engine.patterns.SolutionVariablePattern
import engine.patterns.UnsignedNumberPattern
import engine.patterns.inequalityOf
import engine.patterns.negOf
import engine.patterns.withOptionalConstantCoefficient
import engine.steps.metadata.metadata

enum class InequalitiesRules(override val runner: Rule) : RunnerMethod {

    ExtractSolutionFromConstantInequality(
        rule {
            val lhs = UnsignedNumberPattern()
            val rhs = UnsignedNumberPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                if (inequality.holdsFor(getValue(lhs), getValue(rhs))) {
                    ruleResult(
                        toExpr = solutionOf(xp(context.solutionVariable!!), Constants.Reals),
                        explanation = metadata(Explanation.ExtractSolutionFromTrueInequality),
                    )
                } else {
                    ruleResult(
                        toExpr = solutionOf(xp(context.solutionVariable!!), Constants.EmptySet),
                        explanation = metadata(Explanation.ExtractSolutionFromFalseInequality),
                    )
                }
            }
        },
    ),

    ExtractSolutionFromConstantInequalityBasedOnSign(
        rule {
            val lhs = AnyPattern()
            val rhs = AnyPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                val lhsSign = get(lhs).signOf()
                val rhsSign = get(rhs).signOf()

                if (lhsSign.isKnown() && rhsSign.isKnown()) {
                    if (inequality.holdsFor(lhsSign.signum.toBigDecimal(), rhsSign.signum.toBigDecimal())) {
                        ruleResult(
                            toExpr = solutionOf(xp(context.solutionVariable!!), Constants.Reals),
                            explanation = metadata(Explanation.ExtractSolutionFromTrueInequality),
                        )
                    } else {
                        ruleResult(
                            toExpr = solutionOf(xp(context.solutionVariable!!), Constants.EmptySet),
                            explanation = metadata(Explanation.ExtractSolutionFromFalseInequality),
                        )
                    }
                } else {
                    null
                }
            }
        },
    ),

    ExtractSolutionFromInequalityInSolvedForm(
        rule {
            val lhs = SolutionVariablePattern()
            val rhs = ConstantInSolutionVariablePattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                ruleResult(
                    toExpr = solutionOf(move(lhs), inequality.toInterval(get(rhs))),
                    explanation = metadata(Explanation.ExtractSolutionFromInequalityInSolvedForm),
                )
            }
        },
    ),

    FlipInequality(
        rule {
            val lhs = AnyPattern()
            val rhs = AnyPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                ruleResult(
                    toExpr = inequality.dualInequality(move(rhs), move(lhs)),
                    explanation = metadata(Explanation.FlipInequality),
                )
            }
        },
    ),

    NegateBothSides(
        rule {
            val variable = SolutionVariablePattern()
            val lhs = negOf(variable)
            val rhs = AnyPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                ruleResult(
                    toExpr = inequality.dualInequality(
                        move(variable),
                        simplifiedNegOf(move(rhs)),
                    ),
                    explanation = metadata(Explanation.NegateBothSidesAndFlipTheSign),
                )
            }
        },
    ),

    MultiplyByInverseCoefficientOfVariable(
        rule {
            val lhs = withOptionalConstantCoefficient(SolutionVariablePattern())
            val rhs = AnyPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                val coefficient = get(lhs::coefficient)!!

                if (coefficient.isFraction() || (coefficient.isNeg() && coefficient.firstChild.isFraction())) {
                    val inverse = coefficient.inverse()

                    when (inverse.signOf()) {
                        Sign.POSITIVE -> ruleResult(
                            toExpr = inequality.sameInequality(
                                productOf(move(lhs), inverse),
                                productOf(move(rhs), inverse),
                            ),
                            explanation = metadata(Explanation.MultiplyByInverseCoefficientOfVariable),
                        )
                        Sign.NEGATIVE -> ruleResult(
                            toExpr = inequality.dualInequality(
                                productOf(move(lhs), inverse),
                                productOf(move(rhs), inverse),
                            ),
                            explanation = metadata(Explanation.MultiplyByInverseCoefficientOfVariableAndFlipTheSign),
                        )
                        else -> null
                    }
                } else {
                    null
                }
            }
        },
    ),

    DivideByCoefficientOfVariable(
        rule {
            val lhs = withOptionalConstantCoefficient(SolutionVariablePattern())
            val rhs = AnyPattern()

            val inequality = inequalityOf(lhs, rhs)

            onPattern(inequality) {
                val coefficient = get(lhs::coefficient)!!
                if (coefficient == Constants.One) return@onPattern null

                when (coefficient.signOf()) {
                    Sign.POSITIVE -> ruleResult(
                        toExpr = inequality.sameInequality(
                            fractionOf(move(lhs), coefficient),
                            fractionOf(move(rhs), coefficient),
                        ),
                        explanation = metadata(Explanation.DivideByCoefficientOfVariable),
                    )
                    Sign.NEGATIVE -> ruleResult(
                        toExpr = inequality.dualInequality(
                            fractionOf(move(lhs), coefficient),
                            fractionOf(move(rhs), coefficient),
                        ),
                        explanation = metadata(Explanation.DivideByCoefficientOfVariableAndFlipTheSign),
                    )
                    else -> null
                }
            }
        },
    ),
}
