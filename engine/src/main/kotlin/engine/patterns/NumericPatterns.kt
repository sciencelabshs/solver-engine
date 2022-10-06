package engine.patterns

import engine.expressions.Expression
import engine.expressions.Path
import engine.expressions.Subexpression
import engine.expressions.xp
import engine.operators.DecimalOperator
import engine.operators.IntegerOperator
import engine.operators.RecurringDecimalOperator
import engine.utility.RecurringDecimal
import java.math.BigDecimal
import java.math.BigInteger

class InvalidMatch(message: String) : Exception(message)

interface NumberProvider : PathProvider {
    fun getBoundNumber(m: Match): BigDecimal
}

interface IntegerProvider : NumberProvider {
    fun getBoundInt(m: Match): BigInteger

    override fun getBoundNumber(m: Match) = getBoundInt(m).toBigDecimal()
}

interface NumberPattern : Pattern, NumberProvider

interface IntegerPattern : Pattern, IntegerProvider

class UnsignedIntegerPattern : IntegerPattern {

    override fun getBoundInt(m: Match): BigInteger {
        return when (val operator = m.getBoundExpr(this)!!.operator) {
            is IntegerOperator -> operator.value
            else -> throw InvalidMatch("Unsigned integer matched to $operator")
        }
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

class UnsignedDecimalPattern : NumberPattern {

    override fun getBoundNumber(m: Match): BigDecimal {
        return when (val operator = m.getBoundExpr(this)!!.operator) {
            is DecimalOperator -> operator.value
            is IntegerOperator -> operator.value.toBigDecimal()
            else -> throw InvalidMatch("Unsigned decimal matched $operator")
        }
    }

    override fun findMatches(subexpression: Subexpression, match: Match): Sequence<Match> {
        if (!checkPreviousMatch(subexpression.expr, match)) {
            return emptySequence()
        }
        return when (subexpression.expr.operator) {
            is IntegerOperator, is DecimalOperator -> sequenceOf(match.newChild(this, subexpression))
            else -> emptySequence()
        }
    }
}

class RecurringDecimalPattern : Pattern {

    fun getBoundRecurringDecimal(m: Match): RecurringDecimal {
        return when (val operator = m.getBoundExpr(this)!!.operator) {
            is RecurringDecimalOperator -> operator.value
            else -> throw InvalidMatch("Recurring decimal matched to $operator")
        }
    }

    override fun findMatches(subexpression: Subexpression, match: Match): Sequence<Match> {
        if (!checkPreviousMatch(subexpression.expr, match)) {
            return emptySequence()
        }
        return when (subexpression.expr.operator) {
            is RecurringDecimalOperator -> sequenceOf(match.newChild(this, subexpression))
            else -> emptySequence()
        }
    }
}

class SignedIntegerPattern : OptionalNegPatternBase<UnsignedIntegerPattern>(UnsignedIntegerPattern()), IntegerPattern {
    override fun getBoundInt(m: Match): BigInteger {
        val value = unsignedPattern.getBoundInt(m)
        return if (isNeg(m)) -value else value
    }
}

class SignedNumberPattern : OptionalNegPatternBase<UnsignedDecimalPattern>(UnsignedDecimalPattern()), NumberPattern {
    override fun getBoundNumber(m: Match): BigDecimal {
        val value = unsignedPattern.getBoundNumber(m)
        return if (isNeg(m)) -value else value
    }
}

/**
 * This wraps a PathProvider so that it is given a default value if did not match.
 */
class IntegerProviderWithDefault(
    private val integerProvider: IntegerProvider,
    private val default: BigInteger
) : IntegerProvider {

    override fun getBoundInt(m: Match): BigInteger {
        return if (integerProvider.getBoundExpr(m) != null) {
            integerProvider.getBoundInt(m)
        } else {
            default
        }
    }

    override fun getBoundPaths(m: Match): List<Path> {
        return integerProvider.getBoundPaths(m)
    }

    override fun getBoundExpr(m: Match): Expression {
        return integerProvider.getBoundExpr(m) ?: xp(default)
    }
}