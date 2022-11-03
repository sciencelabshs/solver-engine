package engine.patterns

import engine.context.Context
import engine.expressions.Subexpression
import engine.operators.VariableOperator

abstract class VariablePattern : BasePattern() {
    fun getBoundSymbol(m: Match): String {
        return when (val operator = m.getBoundExpr(this)!!.operator) {
            is VariableOperator -> operator.name
            else -> throw InvalidMatch("Variable matched to $operator")
        }
    }
}

/**
 * A pattern to match with any variable (i.e. symbol)
 */
class ArbitraryVariablePattern : VariablePattern() {
    override fun doFindMatches(context: Context, match: Match, subexpression: Subexpression): Sequence<Match> {
        return when (subexpression.expr.operator) {
            is VariableOperator -> sequenceOf(match.newChild(this, subexpression))
            else -> emptySequence()
        }
    }
}

class SolutionVariablePattern : VariablePattern() {
    override fun doFindMatches(context: Context, match: Match, subexpression: Subexpression): Sequence<Match> {
        val operator = subexpression.expr.operator

        return if (operator is VariableOperator && operator.name == context.solutionVariable) {
            sequenceOf(match.newChild(this, subexpression))
        } else {
            emptySequence()
        }
    }
}
