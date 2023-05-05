package engine.expressions

import engine.operators.BinaryExpressionOperator
import engine.operators.ProductOperator
import engine.operators.RenderContext
import engine.operators.UnaryExpressionOperator
import engine.operators.VariableOperator

class Product(
    factors: List<Expression>,
    val forcedSigns: List<Int> = emptyList(),
    meta: NodeMeta = BasicMeta(),
) : Expression(
    operator = ProductOperator(forcedSigns),
    operands = factors,
    meta,
) {
    private fun productSignRequiredForOperand(i: Int, op: Expression) = when {
        op.operator == UnaryExpressionOperator.DivideBy -> false
        forcedSigns.contains(i) -> true
        i > 0 && productSignRequired(operands[i - 1], op) -> true
        else -> false
    }

    override fun toString(): String {
        val str = operands.mapIndexed { i, op ->
            val sign = if (productSignRequiredForOperand(i, op)) "* " else ""
            "$sign$op"
        }.joinToString(separator = " ")
        return decorators.fold(str) { acc, dec -> dec.decorateString(acc) }
    }

    override fun toLatexString(ctx: RenderContext): String {
        val str = operands.mapIndexed { i, op ->
            val sign = if (productSignRequiredForOperand(i, op)) "\\times " else ""
            "$sign${op.toLatexString(ctx)}"
        }.joinToString(separator = " ")
        return decorators.fold(str) { acc, dec -> dec.decorateLatexString(acc) }
    }

    override fun toJson(): List<Any> {
        val serializedOperands = operands.map { it.toJson() }
        val initial = if (decorators.isEmpty()) {
            listOf("SmartProduct")
        } else {
            listOf(listOf("SmartProduct") + decorators.map { it.toString() })
        }
        return initial + serializedOperands.indices.map {
            listOf(productSignRequiredForOperand(it, operands[it]), serializedOperands[it])
        }
    }
}

private fun getBaseOfPower(expr: Expression): Expression = when (expr) {
    is Power -> getBaseOfPower(expr.base)
    else -> expr
}

private fun Expression.isNumbery(): Boolean = when {
    this is Power -> base.isNumbery()
    this is Fraction -> true
    this is IntegerExpression -> true
    this is DecimalExpression -> true
    this is RecurringDecimalExpression -> true
    operator == UnaryExpressionOperator.Minus || operator == UnaryExpressionOperator.Plus ||
        operator == UnaryExpressionOperator.PlusMinus -> {
        val op = operands[0]
        op is IntegerExpression || op is DecimalExpression || op is RecurringDecimalExpression
    }
    else -> false
}

// couldn't come up with a good way of splitting or simplifying this method
@Suppress("CyclomaticComplexMethod")
fun productSignRequired(left: Expression, right: Expression): Boolean = when {
    left.isPartialProduct() -> productSignRequired(left.children.last(), right)
    right.isPartialProduct() -> productSignRequired(left, right.children.first())
    left.operator == UnaryExpressionOperator.DivideBy || right.operator == UnaryExpressionOperator.DivideBy -> true
    right.isNumbery() -> true
    left.hasBracket() || right.hasBracket() -> false
    else -> {
        val rightOp = getBaseOfPower(right).operator
        val leftOp = getBaseOfPower(left).operator

        val leftIsVariable = leftOp is VariableOperator
        val rightIsRoot = rightOp == UnaryExpressionOperator.SquareRoot ||
            rightOp == BinaryExpressionOperator.Root
        val rightIsRootOrVariable = rightIsRoot || rightOp is VariableOperator
        val differentVariables = leftOp is VariableOperator && rightOp is VariableOperator &&
            leftOp.name != rightOp.name

        !(left.isNumbery() && rightIsRootOrVariable || leftIsVariable && rightIsRoot || differentVariables)
    }
}