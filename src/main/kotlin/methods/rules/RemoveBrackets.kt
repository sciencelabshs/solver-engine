package methods.rules

import engine.expressionmakers.makeBracketOf
import engine.expressionmakers.move
import engine.expressionmakers.substituteIn
import engine.expressions.UnaryOperator
import engine.patterns.*
import engine.rules.Rule
import engine.steps.metadata.Explanation
import engine.steps.metadata.makeMetadata

val replaceInvisibleBrackets = run {
    val innerExpr = AnyPattern()
    val pattern = OperatorPattern(UnaryOperator.InvisibleBracket, listOf(innerExpr))

    Rule(
        pattern = pattern,
        resultMaker = makeBracketOf(move(innerExpr)),
        explanationMaker = makeMetadata(Explanation.ReplaceInvisibleBrackets),
    )
}

val removeBracketsSum = run {
    val innerSum = sumContaining()
    val bracket = bracketOf(innerSum)
    val pattern = sumContaining(bracket)

    Rule(
        pattern = pattern,
        resultMaker = substituteIn(pattern, move(innerSum)),
        explanationMaker = makeMetadata(Explanation.RemoveBracketSumInSum)
    )
}

val removeBracketAroundSignedIntegerInSum = run {
    val number = SignedIntegerPattern()
    val bracket = bracketOf(number)
    val pattern = sumContaining(bracket)

    Rule(
        pattern = pattern,
        resultMaker = substituteIn(pattern, move(number)),
        explanationMaker = makeMetadata(Explanation.RemoveBracketSignedIntegerInSum)
    )
}


val removeBracketAroundUnsignedInteger = run {
    val number = UnsignedIntegerPattern()
    val pattern = bracketOf(number)

    Rule(
        pattern = pattern,
        resultMaker = move(number),
        explanationMaker = makeMetadata(Explanation.RemoveBracketUnsignedInteger)
    )
}