package methods.approximation

import engine.context.Context
import methods.decimals.DecimalsExplanation
import methods.plans.testMethod
import org.junit.jupiter.api.Test

class ApproximationPlansTest {

    @Test
    fun testRoundingTerminatingDecimals() = testMethod {
        method = approximateExpression
        context = Context(precision = 2)
        inputExpr = "1.2345 + 2.324"

        check {
            toExpr = "3.55"
            explanation {
                key = ApproximationExplanation.ApproximateExpression
            }

            step {
                toExpr = "1.23 + 2.324"
                explanation {
                    key = ApproximationExplanation.RoundTerminatingDecimal
                }
            }

            step {
                toExpr = "1.23 + 2.32"
                explanation {
                    key = ApproximationExplanation.RoundTerminatingDecimal
                }
            }

            step {
                toExpr = "3.55"
                explanation {
                    key = DecimalsExplanation.EvaluateDecimalAddition
                }
            }
        }
    }

    @Test
    fun testRoundingRecurringDecimals() = testMethod {
        method = approximateExpression
        context = Context(precision = 5)
        inputExpr = "3.14[15] * 2.7[182]"

        check {
            toExpr = "8.53934"
            explanation {
                key = ApproximationExplanation.ApproximateExpression
            }

            step {
                toExpr = "3.14152 * 2.7[182]"

                step {
                    fromExpr = "3.14[15]"
                    toExpr = "3.1415[15]"
                    explanation {
                        key = ApproximationExplanation.ExpandRecurringDecimal
                    }
                }

                step {
                    fromExpr = "3.1415[15]"
                    toExpr = "3.14152"
                    explanation {
                        key = ApproximationExplanation.RoundRecurringDecimal
                    }
                }
            }

            step {
                toExpr = "3.14152 * 2.71822"

                step {
                    fromExpr = "2.7[182]"
                    toExpr = "2.7182[182]"
                    explanation {
                        key = ApproximationExplanation.ExpandRecurringDecimal
                    }
                }

                step {
                    fromExpr = "2.7182[182]"
                    toExpr = "2.71822"
                    explanation {
                        key = ApproximationExplanation.RoundRecurringDecimal
                    }
                }
            }

            step {
                toExpr = "8.53934"
                explanation {
                    key = ApproximationExplanation.ApproximateDecimalProduct
                }
            }
        }
    }

    @Test
    fun testDecimalNumberInPower() = testMethod {
        method = approximateExpression
        inputExpr = "[3.14[15]^1.2]"

        check {
            noTransformation()
        }
    }
}