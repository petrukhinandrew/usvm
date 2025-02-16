package org.usvm.org.usvm.rendering

import org.usvm.test.api.UTestArithmeticExpression
import org.usvm.test.api.UTestArrayGetExpression
import org.usvm.test.api.UTestArrayLengthExpression
import org.usvm.test.api.UTestArraySetStatement
import org.usvm.test.api.UTestBinaryConditionExpression
import org.usvm.test.api.UTestBinaryConditionStatement
import org.usvm.test.api.UTestCastExpression
import org.usvm.test.api.UTestConstructorCall
import org.usvm.test.api.UTestCreateArrayExpression
import org.usvm.test.api.UTestGetFieldExpression
import org.usvm.test.api.UTestInst
import org.usvm.test.api.UTestMethodCall
import org.usvm.test.api.UTestSetFieldStatement
import org.usvm.test.api.UTestSetStaticFieldStatement
import org.usvm.test.api.UTestStaticMethodCall

object UTestInstTraverser {

    fun traverseExpr(expr: UTestInst, block: (UTestInst) -> Unit): Unit = block(expr).also {
        when (expr) {
            is UTestArithmeticExpression -> {
                traverseExpr(expr.lhv, block)
                traverseExpr(expr.rhv, block)
            }

            is UTestArrayGetExpression -> {
                traverseExpr(expr.arrayInstance, block)
                traverseExpr(expr.index, block)
            }

            is UTestArrayLengthExpression -> traverseExpr(expr.arrayInstance, block)
            is UTestBinaryConditionExpression -> {
                traverseExpr(expr.lhv, block)
                traverseExpr(expr.rhv, block)
                traverseExpr(expr.trueBranch, block)
                traverseExpr(expr.elseBranch, block)
            }

            is UTestConstructorCall, is UTestStaticMethodCall -> {
                expr.args.forEach { arg -> traverseExpr(arg, block) }
            }

            is UTestMethodCall -> {
                traverseExpr(expr.instance, block)
                expr.args.forEach { arg -> traverseExpr(arg, block) }
            }

            is UTestCastExpression -> traverseExpr(expr.expr, block)
            is UTestCreateArrayExpression -> {
                traverseExpr(expr.size, block)
            }

            is UTestGetFieldExpression -> {
                traverseExpr(expr.instance, block)
            }

            is UTestArraySetStatement -> {
                traverseExpr(expr.arrayInstance, block)
                traverseExpr(expr.index, block)
                traverseExpr(expr.setValueExpression, block)
            }

            is UTestBinaryConditionStatement -> {
                traverseExpr(expr.lhv, block)
                traverseExpr(expr.rhv, block)
//                    traverseExpr(expr.trueBranch, block)
//                    traverseExpr(expr.elseBranch, block)
            }

            is UTestSetFieldStatement -> {
                traverseExpr(expr.instance, block)
                traverseExpr(expr.value, block)

            }

            is UTestSetStaticFieldStatement -> {
                traverseExpr(expr.value, block)
            }

            else -> return@also
        }
    }
}