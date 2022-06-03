package rules

import java.util.stream.Stream

object CancelInAFractionTest : RuleTest() {
    @JvmStatic
    fun testCaseProvider(): Stream<RuleTestCase> = Stream.of(
        RuleTestCase("[x*y*z/a*y*c]", cancelInAFraction, "[x*z/a*c]"),
    )
}