package plans

import context.ResourceData
import expressionmakers.move
import patterns.*
import rules.*
import steps.metadata.PlanExplanation
import steps.metadata.Skill


val simplifyArithmeticExpression = plan {

    pattern = AnyPattern() /* TODO add condition that it is constant in all variables */
    explanation(PlanExplanation.SimplifyArithmeticExpression)

    whilePossible {
        deeply(deepFirst = true) {
            firstOf {
                option(removeBracketAroundUnsignedInteger)
                option(removeBracketAroundSignedIntegerInSum)
                option(simplifyDoubleNeg)
                option(evaluateSignedIntegerPower)
                option {
                    explanation(PlanExplanation.SimplifyIntegerProduct)
                    whilePossible(evaluateSignedIntegerProduct)
                }
                option {
                    explanation(PlanExplanation.SimplifyIntegerSum)
                    whilePossible(evaluateSignedIntegerAddition)
                }
            }
        }
    }
}

val replaceAllInvisibleBrackets = plan {

    explanation(PlanExplanation.ReplaceAllInvisibleBrackets)

    whilePossible {
        deeply(replaceInvisibleBrackets)
    }
}

val convertMixedNumberToImproperFraction = plan {
    pattern = mixedNumberOf()

    pipeline {
        step(splitMixedNumber)
        step(convertIntegerToFraction)
        step {
            whilePossible {
                deeply(evaluateIntegerProduct)
            }
        }
        step(addLikeFractions)
        step {
            whilePossible {
                deeply(evaluateSignedIntegerAddition)
            }
        }
    }
}

val addUnlikeFractions = plan {
    val f1 = fractionOf(UnsignedIntegerPattern(), UnsignedIntegerPattern())
    val f2 = fractionOf(UnsignedIntegerPattern(), UnsignedIntegerPattern())

    overridePattern = sumContaining(f1, f2)

    explanation(PlanExplanation.AddFractions, move(f1), move(f2))

    skill(Skill.AddFractions, move(f1), move(f2))

    pipeline {
        step(commonDenominator)
        step {
            whilePossible {
                deeply(evaluateIntegerProduct)
            }
        }
        step(addLikeFractions)
        step {
            whilePossible {
                deeply(evaluateSignedIntegerAddition)
            }
        }
    }
}

val addMixedNumbersByConverting = plan {
    pattern = sumOf(mixedNumberOf(), mixedNumberOf())

    pipeline {
        step(ApplyToChildrenInStep(convertMixedNumberToImproperFraction))
        step(addUnlikeFractions)
        step(fractionToMixedNumber)
    }
}

val addMixedNumbersUsingCommutativity = plan {
    pattern = sumOf(mixedNumberOf(), mixedNumberOf())

    pipeline {
        step {
            whilePossible {
                deeply(splitMixedNumber)
            }
        }
        step {
            whilePossible(removeBracketsSum)
        }
        step(evaluateSignedIntegerAddition)
        step(addUnlikeFractions)
        step(convertIntegerToFraction)
        step {
            whilePossible {
                deeply(evaluateIntegerProduct)
            }
        }
        step(addLikeFractions)
        step {
            deeply(evaluateSignedIntegerAddition)
        }
        step(fractionToMixedNumber)
    }
}


val addMixedNumbers = ContextSensitivePlanSelector(
    alternatives = listOf(
        AnnotatedPlan(addMixedNumbersByConverting, ResourceData("EU")),
        AnnotatedPlan(addMixedNumbersUsingCommutativity, ResourceData("US")),
    ),
    default = addMixedNumbersByConverting,
    pattern = sumOf(mixedNumberOf(), mixedNumberOf()),
)