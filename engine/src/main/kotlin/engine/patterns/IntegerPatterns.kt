package engine.patterns

import engine.expressions.IntegerOperator
import engine.expressions.Subexpression
import java.math.BigInteger

interface IntegerProvider : PathProvider {

    fun getBoundInt(m: Match): BigInteger
}

class UnsignedIntegerPattern : Pattern, IntegerProvider {

    override fun getBoundInt(m: Match): BigInteger {
        return (m.getBoundExpr(this)!!.operator as IntegerOperator).value
    }

    override fun findMatches(subexpression: Subexpression, match: Match): Sequence<Match> {
        if (!checkPreviousMatch(subexpression.expr, match)) {
            return emptySequence()
        }
        return when (subexpression.expr.operator) {
            is IntegerOperator -> sequenceOf(match.newChild(this, subexpression))
            else -> emptySequence()
        }
    }
}

class SignedIntegerPattern : OptionalNegPatternBase<UnsignedIntegerPattern>(UnsignedIntegerPattern()), IntegerProvider {
    override fun getBoundInt(m: Match): BigInteger {
        val value = pattern.getBoundInt(m)
        return if (isNeg(m)) -value else value
    }
}
