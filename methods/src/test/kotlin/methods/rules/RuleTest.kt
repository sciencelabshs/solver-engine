package methods.rules

import engine.context.emptyContext
import engine.expressions.Subexpression
import engine.methods.Method
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import parser.parseExpression
import kotlin.test.assertEquals
import kotlin.test.assertNull

data class RuleTestCase(val inputExpr: String, val rule: Method, val outputExpr: String?) {

    fun assert() {
        val expression = parseExpression(inputExpr)
        val step = rule.tryExecute(emptyContext, Subexpression(expression))
        if (outputExpr == null) {
            assertNull(step)
        } else {
            assertEquals(parseExpression(outputExpr), step?.toExpr?.expr, inputExpr)
        }
    }
}

interface RuleTest {

    @ParameterizedTest
    @MethodSource("testCaseProvider")
    fun testRule(testCase: RuleTestCase) {
        testCase.assert()
    }
}

fun testRule(inputExpr: String, rule: Method, outputExpr: String?) {
    RuleTestCase(inputExpr, rule, outputExpr).assert()
}
