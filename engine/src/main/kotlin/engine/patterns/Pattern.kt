package engine.patterns

import engine.context.Context
import engine.expressions.Expression
import engine.expressions.Path
import engine.expressions.Subexpression

interface PathProvider {

    /**
     * Returns a list of `Path` objects from the root of
     * the tree to the where the pattern is present in the
     * match `m`.
     */
    fun getBoundPaths(m: Match): List<Path>

    /**
     * Returns the `Expression` value of the given pattern
     * with the provided `match` "m"
     */
    fun getBoundExpr(m: Match): Expression?
}

/**
 * Patterns are used to detect certain shapes in a [Subexpression].
 */
interface Pattern : PathProvider {
    /**
     * Gives a [Sequence] of all possible matches of [Pattern] object
     * in the [subexpression] building on the provided [match].
     */
    fun findMatches(context: Context, match: Match = RootMatch, subexpression: Subexpression): Sequence<Match>

    /**
     * Returns a list of [Path] objects from the root of
     * the tree to the where the pattern is present in the
     * match [m].
     */
    override fun getBoundPaths(m: Match) = m.getBoundPaths(this)

    override fun getBoundExpr(m: Match) = m.getBoundExpr(this)

    fun matches(context: Context, expression: Expression): Boolean {
        return findMatches(context, RootMatch, Subexpression(expression)).any()
    }

    val key: Pattern
}

/**
 * A type of pattern which defines a basic way of matching - its [key] is always equal to the instance itself.
 */
abstract class BasePattern : Pattern {

    internal abstract fun doFindMatches(context: Context, match: Match, subexpression: Subexpression): Sequence<Match>

    /**
     * Checks any potential existing match for this pattern is equivalent to [subexpression] and then use the
     * [doFindMatches] function to return all possible matches.
     */
    final override fun findMatches(context: Context, match: Match, subexpression: Subexpression): Sequence<Match> {
        if (!this.checkPreviousMatch(subexpression.expr, match)) {
            return emptySequence()
        }
        return doFindMatches(context, match, subexpression)
    }

    /**
     * Returns `true` when either the expression value
     * of match is `null` (i.e. root object) or is equivalent
     * to the passed `expr` else return `false`
     */
    private fun checkPreviousMatch(expr: Expression, match: Match): Boolean {
        val previous = getBoundExpr(match)
        return previous == null || previous.equiv(expr)
    }

    final override val key: Pattern get() = this
}

/**
 * A type of pattern whose matching is defined by the value of [key].  It can be subclassed for commonly used non-basic
 * patterns and if we want to add extra behaviour (e.g. see [FractionPattern] and [IntegerOrderRootPattern]).
 */
abstract class KeyedPattern : Pattern {
    final override fun findMatches(context: Context, match: Match, subexpression: Subexpression): Sequence<Match> {
        return key.findMatches(context, match, subexpression)
    }
}
