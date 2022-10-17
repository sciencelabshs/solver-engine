package methods.integerrationalexponents

import engine.expressions.exponent
import engine.methods.plan
import engine.patterns.IntegerFractionPattern
import engine.patterns.UnsignedIntegerPattern
import engine.patterns.fractionOf
import engine.patterns.powerOf
import method.integerrationalexponents.Explanation
import methods.fractionarithmetic.convertImproperFractionToSumOfIntegerAndFraction
import methods.fractionarithmetic.multiplyAndSimplifyFractions
import methods.general.distributePowerOfProduct
import methods.general.distributeSumOfPowers
import methods.general.multiplyExponentsUsingPowerRule
import methods.general.removeBracketProductInProduct
import methods.integerarithmetic.simplifyIntegersInExpression

/**
 * [ ( [x^a] ) ^ b ] --> [x^ (ab)]
 * where `ab` is the simplified product of 'a' and 'b'
 */
val applyPowerRuleOfExponents = plan {
    explanation(Explanation.PowerRuleOfExponents)

    pipeline {
        optionalSteps(multiplyExponentsUsingPowerRule)
        optionalSteps {
            deeply(multiplyAndSimplifyFractions, deepFirst = true)
        }
    }
}

/**
 * [2 ^ [11/3]] --> [2 ^ [3 2/3]] --> [2 ^ 3 + [2 / 3]]
 * --> [2 ^ 3] * [2 ^ [2 / 3]]
 */
val splitRationalExponent = plan {
    pattern = powerOf(UnsignedIntegerPattern(), IntegerFractionPattern())
    explanation(Explanation.SplitRationalExponent)

    pipeline {
        steps { applyTo(convertImproperFractionToSumOfIntegerAndFraction) { it.exponent() } }
        steps(distributeSumOfPowers)
    }
}

val simplifyRationalExponentOfInteger = plan {
    pattern = powerOf(
        UnsignedIntegerPattern(),
        fractionOf(UnsignedIntegerPattern(), UnsignedIntegerPattern())
    )

    explanation(Explanation.SimplifyRationalExponentOfInteger)

    // input: 1350 ^ [2 / 5]
    pipeline {
        // [ ( 2 * 3^3 * 5^2 ) ^ [2 / 5] ]
        optionalSteps(factorizeIntegerUnderRationalExponent)
        // [2 ^ [2 / 5]] * [ (3^3) ^ [2 / 5]] * [ (5^2) ^ [2 / 5]]
        optionalSteps(distributePowerOfProduct)

        // [2 ^ [2 / 5] ] * [ 3 ^ [6 / 5] ] * [ 5 ^ [4 / 5] ]
        optionalSteps {
            whilePossible { deeply(applyPowerRuleOfExponents) }
        }

        // [2 ^ [2 / 5] ] * [ 3 * 3 ^ [1 / 5] ] * [ 5 ^ [4 / 5] ]
        optionalSteps {
            plan {
                explanation(Explanation.SplitProductOfExponentsWithImproperFractionPowers)
                pipeline {
                    optionalSteps {
                        whilePossible {
                            deeply(splitRationalExponent)
                        }
                    }
                    optionalSteps {
                        whilePossible {
                            deeply(removeBracketProductInProduct)
                        }
                    }
                }
            }
        }

        optionalSteps {
            plan {
                explanation(Explanation.NormalizeRationalExponentsAndIntegers)
                pipeline {
                    optionalSteps(normaliseProductWithRationalExponents)
                    optionalSteps {
                        whilePossible {
                            deeply(simplifyIntegersInExpression)
                        }
                    }
                }
            }
        }
    }
}