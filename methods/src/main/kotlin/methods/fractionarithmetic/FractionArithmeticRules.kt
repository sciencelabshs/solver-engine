package methods.fractionarithmetic

import engine.expressions.Constants
import engine.expressions.fractionOf
import engine.expressions.negOf
import engine.expressions.powerOf
import engine.expressions.productOf
import engine.expressions.simplifiedProductOf
import engine.expressions.sumOf
import engine.expressions.xp
import engine.methods.TransformationResult
import engine.methods.rule
import engine.operators.BinaryExpressionOperator
import engine.patterns.AnyPattern
import engine.patterns.ConditionPattern
import engine.patterns.FixedPattern
import engine.patterns.IntegerFractionPattern
import engine.patterns.SignedIntegerPattern
import engine.patterns.UnsignedIntegerPattern
import engine.patterns.commutativeSumOf
import engine.patterns.condition
import engine.patterns.fractionOf
import engine.patterns.integerCondition
import engine.patterns.negOf
import engine.patterns.numericCondition
import engine.patterns.oneOf
import engine.patterns.optionalNegOf
import engine.patterns.powerOf
import engine.patterns.productContaining
import engine.patterns.sumContaining
import engine.steps.metadata.Skill
import engine.steps.metadata.metadata
import engine.utility.divides
import engine.utility.isZero
import java.math.BigInteger

val convertIntegerToFraction = rule {
    val integer = UnsignedIntegerPattern()
    val fraction = IntegerFractionPattern()
    val sum = commutativeSumOf(integer, fraction)

    onPattern(sum) {
        TransformationResult(
            toExpr = sum.substitute(
                fractionOf(move(integer), introduce(Constants.One)),
                move(fraction),
            ),
            explanation = metadata(Explanation.ConvertIntegerToFraction, move(integer)),
        )
    }
}

val addLikeFractions = rule {
    val num1 = UnsignedIntegerPattern()
    val num2 = UnsignedIntegerPattern()
    val denom = UnsignedIntegerPattern()
    val f1 = fractionOf(num1, denom)
    val f2 = fractionOf(num2, denom)
    val nf1 = optionalNegOf(f1)
    val nf2 = optionalNegOf(f2)
    val sum = sumContaining(nf1, nf2)

    onPattern(sum) {
        TransformationResult(
            toExpr = sum.substitute(
                fractionOf(
                    sumOf(
                        copySign(nf1, move(num1)),
                        copySign(nf2, move(num2))
                    ),
                    factor(denom)
                )
            ),
            explanation = when {
                !nf1.isNeg() && nf2.isNeg() -> metadata(Explanation.SubtractLikeFractions, move(f1), move(f2))
                else -> metadata(Explanation.AddLikeFractions, move(nf1), move(nf2))
            }
        )
    }
}

val bringToCommonDenominator = rule {
    val f1 = IntegerFractionPattern()
    val f2 = IntegerFractionPattern()
    val nf1 = optionalNegOf(f1)
    val nf2 = optionalNegOf(f2)
    val sum = sumContaining(nf1, nf2)

    onPattern(ConditionPattern(sum, integerCondition(f1.denominator, f2.denominator) { n1, n2 -> n1 != n2 })) {
        val factor1 = integerOp(f1.denominator, f2.denominator) { n1, n2 -> n2 / n1.gcd(n2) }
        val factor2 = integerOp(f1.denominator, f2.denominator) { n1, n2 -> n1 / n1.gcd(n2) }

        TransformationResult(
            toExpr = sum.substitute(
                sumOf(
                    copySign(
                        nf1,
                        fractionOf(
                            productOf(move(f1.numerator), factor1),
                            productOf(move(f1.denominator), factor1)
                        )
                    ),
                    copySign(
                        nf2,
                        fractionOf(
                            productOf(move(f2.numerator), factor2),
                            productOf(move(f2.denominator), factor2)
                        )
                    ),
                )
            ),
            explanation = metadata(Explanation.BringToCommonDenominator, move(f1), move(f2)),
            skills = listOf(metadata(Skill.NumericLCM, move(f1.denominator), move(f2.denominator))),
        )
    }
}

val simplifyNegativeInDenominator = rule {
    val numerator = AnyPattern()
    val denominator = AnyPattern()

    val pattern = fractionOf(numerator, negOf(denominator))

    onPattern(pattern) {
        TransformationResult(
            toExpr = negOf(fractionOf(move(numerator), move(denominator))),
            explanation = metadata(Explanation.SimplifyNegativeInDenominator, move(pattern)),
        )
    }
}

val simplifyFractionToInteger = rule {
    val numerator = UnsignedIntegerPattern()
    val denominator = UnsignedIntegerPattern()

    val frac = fractionOf(numerator, denominator)

    onPattern(
        ConditionPattern(
            frac,
            integerCondition(numerator, denominator) { n, d -> d.divides(n) }
        )
    ) {
        TransformationResult(
            toExpr = integerOp(numerator, denominator) { n, d -> n / d },
            explanation = metadata(Explanation.SimplifyFractionToInteger),
        )
    }
}

val findCommonFactorInFraction = rule {
    val factorNumerator = UnsignedIntegerPattern()
    val factorDenominator = UnsignedIntegerPattern()

    val productNumerator = productContaining(factorNumerator)
    val productDenominator = productContaining(factorDenominator)

    val numerator = oneOf(factorNumerator, productNumerator)
    val denominator = oneOf(factorDenominator, productDenominator)

    val frac = fractionOf(numerator, denominator)

    onPattern(
        ConditionPattern(
            frac,
            integerCondition(factorNumerator, factorDenominator) { n, d -> n.gcd(d) != BigInteger.ONE }
        )
    ) {
        val gcd = integerOp(factorNumerator, factorDenominator) { n, d -> n.gcd(d) }
        val numeratorOverGcd = integerOp(factorNumerator, factorDenominator) { n, d -> n / n.gcd(d) }
        val denominatorOverGcd = integerOp(factorNumerator, factorDenominator) { n, d -> d / n.gcd(d) }

        TransformationResult(
            toExpr = fractionOf(
                if (isBound(productNumerator)) {
                    productNumerator.substitute(simplifiedProductOf(gcd, numeratorOverGcd))
                } else {
                    productOf(gcd, numeratorOverGcd)
                },
                if (isBound(productDenominator)) {
                    productDenominator.substitute(simplifiedProductOf(gcd, denominatorOverGcd))
                } else {
                    productOf(gcd, denominatorOverGcd)
                }
            ),
            explanation = metadata(Explanation.FindCommonFactorInFraction),
        )
    }
}

val simplifyNegativeInNumerator = rule {
    val numerator = AnyPattern()
    val denominator = AnyPattern()

    val pattern = fractionOf(negOf(numerator), denominator)

    onPattern(pattern) {
        TransformationResult(
            negOf(fractionOf(move(numerator), move(denominator))),
            explanation = metadata(Explanation.SimplifyNegativeInNumerator, move(pattern)),
        )
    }
}

val simplifyNegativeNumeratorAndDenominator = rule {
    val numerator = AnyPattern()
    val denominator = AnyPattern()

    val pattern = fractionOf(negOf(numerator), negOf(denominator))

    onPattern(pattern) {
        TransformationResult(
            fractionOf(move(numerator), move(denominator)),
            explanation = metadata(Explanation.SimplifyNegativeInNumeratorAndDenominator, move(pattern)),
        )
    }
}

val turnFactorIntoFractionInProduct = rule {
    val nonFractionFactor = condition(AnyPattern()) { it.operator != BinaryExpressionOperator.Fraction }
    val product = productContaining(nonFractionFactor)

    onPattern(
        condition(product) { expression ->
            expression.operands.any { it.operator == BinaryExpressionOperator.Fraction }
        }
    ) {
        TransformationResult(
            toExpr = product.substitute(
                fractionOf(move(nonFractionFactor), introduce(Constants.One))
            ),
            explanation = metadata(Explanation.TurnFactorIntoFractionInProduct, move(nonFractionFactor)),
        )
    }
}

val turnSumOfFractionAndIntegerToFractionSum = rule {
    val f = IntegerFractionPattern()
    val nf = optionalNegOf(f)
    val integerTerm = SignedIntegerPattern()

    val sum = commutativeSumOf(nf, integerTerm)

    onPattern(sum) {
        TransformationResult(
            sum.substitute(
                move(nf),
                copySign(
                    integerTerm,
                    fractionOf(
                        productOf(move(integerTerm.unsignedPattern), move(f.denominator)),
                        move(f.denominator)
                    )
                )
            ),
            explanation = metadata(Explanation.BringToCommonDenominator, move(f), move(integerTerm)),
        )
    }
}

val multiplyFractions = rule {
    val num1 = AnyPattern()
    val num2 = AnyPattern()
    val denom1 = AnyPattern()
    val denom2 = AnyPattern()
    val f1 = fractionOf(num1, denom1)
    val f2 = fractionOf(num2, denom2)
    val product = productContaining(f1, f2)

    onPattern(product) {
        TransformationResult(
            product.substitute(
                fractionOf(
                    productOf(move(num1), move(num2)),
                    productOf(move(denom1), move(denom2))
                ),
            ),
            explanation = metadata(Explanation.MultiplyFractions, move(f1), move(f2)),
        )
    }
}

val simplifyFractionWithFractionNumerator = rule {
    val numerator = fractionOf(AnyPattern(), AnyPattern())
    val denominator = AnyPattern()
    val f = fractionOf(numerator, denominator)

    onPattern(f) {
        TransformationResult(
            productOf(
                move(numerator),
                fractionOf(introduce(Constants.One), move(denominator))
            ),
            explanation = metadata(Explanation.SimplifyFractionWithFractionNumerator, move(f)),
        )
    }
}

val simplifyFractionWithFractionDenominator = rule {
    val numerator = UnsignedIntegerPattern()
    val denominator = UnsignedIntegerPattern()
    val innerFraction = fractionOf(numerator, denominator)
    val outerNumerator = AnyPattern()
    val outerFraction = fractionOf(outerNumerator, innerFraction)

    onPattern(outerFraction) {
        TransformationResult(
            productOf(
                move(outerNumerator),
                fractionOf(move(denominator), move(numerator))
            ),
            explanation = metadata(Explanation.SimplifyFractionWithFractionDenominator, move(outerFraction)),
        )
    }
}

val distributeFractionPositivePower = rule {
    val fraction = IntegerFractionPattern()
    val exponent = integerCondition(UnsignedIntegerPattern()) { it > BigInteger.ONE }
    val pattern = powerOf(fraction, exponent)

    onPattern(pattern) {
        TransformationResult(
            fractionOf(
                powerOf(move(fraction.numerator), move(exponent)),
                powerOf(move(fraction.denominator), move(exponent))
            ),
            explanation = metadata(Explanation.DistributeFractionPositivePower, move(fraction), move(exponent))
        )
    }
}

val simplifyFractionNegativePower = rule {
    val fraction = IntegerFractionPattern()
    val exponent = SignedIntegerPattern()
    val pattern = powerOf(fraction, integerCondition(exponent) { it < -BigInteger.ONE })

    onPattern(pattern) {
        TransformationResult(
            powerOf(
                fractionOf(move(fraction.denominator), move(fraction.numerator)),
                move(exponent.unsignedPattern)
            ),
            explanation = metadata(Explanation.SimplifyFractionNegativePower, move(fraction), move(exponent))
        )
    }
}

val simplifyFractionToMinusOne = rule {
    val fraction = IntegerFractionPattern()
    val pattern = powerOf(fraction, FixedPattern(xp(-1)))

    onPattern(pattern) {
        TransformationResult(
            fractionOf(move(fraction.denominator), move(fraction.numerator)),
            explanation = metadata(Explanation.SimplifyFractionToMinusOne, move(fraction)),
        )
    }
}

val turnIntegerToMinusOneToFraction = rule {
    val base = UnsignedIntegerPattern()
    val pattern = powerOf(base, FixedPattern(xp(-1)))

    onPattern(pattern) {
        TransformationResult(
            fractionOf(introduce(Constants.One), move(base)),
            explanation = metadata(Explanation.TurnIntegerToMinusOneToFraction, move(base))
        )
    }
}

val turnNegativePowerOfIntegerToFraction = rule {
    val base = integerCondition(UnsignedIntegerPattern()) { !it.isZero() }
    val exponent = SignedIntegerPattern()
    val pattern = powerOf(base, integerCondition(exponent) { it < -BigInteger.ONE })

    onPattern(pattern) {
        TransformationResult(
            toExpr = fractionOf(
                introduce(Constants.One),
                powerOf(move(base), move(exponent.unsignedPattern)),
            ),
            explanation = metadata(Explanation.TurnNegativePowerOfIntegerToFraction, move(exponent.unsignedPattern))
        )
    }
}

val turnNegativePowerOfZeroToPowerOfFraction = rule {
    val zero = FixedPattern(Constants.Zero)
    val unsignedExponent = AnyPattern()
    val power = powerOf(zero, negOf(unsignedExponent))

    onPattern(power) {
        TransformationResult(
            toExpr = powerOf(
                fractionOf(introduce(Constants.One), move(zero)),
                move(unsignedExponent)
            ),
            explanation = metadata(Explanation.TurnNegativePowerOfZeroToPowerOfFraction)
        )
    }
}

val convertImproperFractionToSumOfIntegerAndFraction = rule {
    val fraction = IntegerFractionPattern()
    val improperFractionCondition = numericCondition(fraction.numerator, fraction.denominator) { n1, n2 -> n1 > n2 }
    val improperFraction = ConditionPattern(fraction, improperFractionCondition)

    onPattern(improperFraction) {
        val quotient = integerOp(fraction.numerator, fraction.denominator) { n, d -> n / d }
        val remainder = integerOp(fraction.numerator, fraction.denominator) { n, d -> n % d }

        TransformationResult(
            toExpr = sumOf(quotient, fractionOf(remainder, move(fraction.denominator))),
            explanation = metadata(Explanation.ConvertImproperFractionToSumOfIntegerAndFraction),
            skills = listOf(
                metadata(Skill.DivisionWithRemainder, move(fraction.numerator), move(fraction.denominator))
            ),
        )
    }
}
