package methods.equations

import engine.methods.testMethodInX
import methods.polynomials.PolynomialsExplanation
import org.junit.jupiter.api.Test

class EquationsPlansTest {
    @Test
    fun testSimplifyByFactoringNegativeSignOfLeadingCoefficient() = testMethodInX {
        method = EquationsPlans.SimplifyByFactoringNegativeSignOfLeadingCoefficient
        inputExpr = "-2[x^2] + 4x - 2 = 0"

        check {
            fromExpr = "-2 [x ^ 2] + 4 x - 2 = 0"
            toExpr = "2 [x ^ 2] - 4 x + 2 = 0"
            explanation {
                key = EquationsExplanation.SimplifyByFactoringNegativeSignOfLeadingCoefficient
            }

            step {
                fromExpr = "-2 [x ^ 2] + 4 x - 2 = 0"
                toExpr = "(-1) (2 [x ^ 2] - 4 x + 2) = 0"
                explanation {
                    key = EquationsExplanation.FactorNegativeSignOfLeadingCoefficient
                }
            }

            step {
                fromExpr = "(-1) (2 [x ^ 2] - 4 x + 2) = 0"
                toExpr = "2 [x ^ 2] - 4 x + 2 = 0"
                explanation {
                    key = EquationsExplanation.EliminateConstantFactorOfLhsWithZeroRhs
                }
            }
        }
    }

    @Test
    fun testSimplifyByDividingByGcfOfCoefficients() = testMethodInX {
        method = EquationsPlans.SimplifyByDividingByGcfOfCoefficients
        inputExpr = "2 [x ^ 2] - 4 x + 2 = 0"

        check {
            fromExpr = "2 [x ^ 2] - 4 x + 2 = 0"
            toExpr = "[x ^ 2] - 2 x + 1 = 0"
            explanation {
                key = EquationsExplanation.SimplifyByDividingByGcfOfCoefficients
            }

            step {
                fromExpr = "2 [x ^ 2] - 4 x + 2 = 0"
                toExpr = "2 ([x ^ 2] - 2 x + 1) = 0"
                explanation {
                    key = PolynomialsExplanation.FactorGreatestCommonFactor
                }
            }

            step {
                fromExpr = "2 ([x ^ 2] - 2 x + 1) = 0"
                toExpr = "[x ^ 2] - 2 x + 1 = 0"
                explanation {
                    key = EquationsExplanation.EliminateConstantFactorOfLhsWithZeroRhs
                }
            }
        }
    }
}
