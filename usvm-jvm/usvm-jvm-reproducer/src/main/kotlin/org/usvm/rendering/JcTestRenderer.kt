@file:Suppress("DEPRECATION")

package org.usvm.org.usvm.rendering

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ArrayAccessExpr
import com.github.javaparser.ast.expr.ArrayCreationExpr
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.CharLiteralExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.ConditionalExpr
import com.github.javaparser.ast.expr.DoubleLiteralExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.TypeExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.Statement
import com.github.javaparser.ast.type.ClassOrInterfaceType
import kotlin.concurrent.timerTask
import org.jacodb.api.jvm.JcArrayType
import org.jacodb.api.jvm.JcField
import org.jacodb.api.jvm.JcType
import org.jacodb.api.jvm.ext.toType
import org.usvm.test.api.ArithmeticOperationType
import org.usvm.test.api.ConditionType
import org.usvm.test.api.UTest
import org.usvm.test.api.UTestAllocateMemoryCall
import org.usvm.test.api.UTestArithmeticExpression
import org.usvm.test.api.UTestArrayGetExpression
import org.usvm.test.api.UTestArrayLengthExpression
import org.usvm.test.api.UTestArraySetStatement
import org.usvm.test.api.UTestBinaryConditionExpression
import org.usvm.test.api.UTestBinaryConditionStatement
import org.usvm.test.api.UTestBooleanExpression
import org.usvm.test.api.UTestByteExpression
import org.usvm.test.api.UTestCastExpression
import org.usvm.test.api.UTestCharExpression
import org.usvm.test.api.UTestClassExpression
import org.usvm.test.api.UTestConstructorCall
import org.usvm.test.api.UTestCreateArrayExpression
import org.usvm.test.api.UTestDoubleExpression
import org.usvm.test.api.UTestExpression
import org.usvm.test.api.UTestFloatExpression
import org.usvm.test.api.UTestGetFieldExpression
import org.usvm.test.api.UTestGetStaticFieldExpression
import org.usvm.test.api.UTestGlobalMock
import org.usvm.test.api.UTestIntExpression
import org.usvm.test.api.UTestLongExpression
import org.usvm.test.api.UTestMethodCall
import org.usvm.test.api.UTestMockObject
import org.usvm.test.api.UTestNullExpression
import org.usvm.test.api.UTestSetFieldStatement
import org.usvm.test.api.UTestSetStaticFieldStatement
import org.usvm.test.api.UTestShortExpression
import org.usvm.test.api.UTestStatement
import org.usvm.test.api.UTestStaticMethodCall
import org.usvm.test.api.UTestStringExpression


class JcTestMethodBodyConverter(
    private val importManager: JcImportManager,
    private val typeTranslator: JcTypeTranslator
) {
    val throwPool = mutableListOf<ClassOrInterfaceType>()
    var needUnsafe = false
    fun convert(test: UTest): BlockStmt {
        val translator = UTestMethodTranslator(importManager, typeTranslator)
        val res = BlockStmt(NodeList(translator.translate(test)))
        throwPool.addAll(translator.throwPool)
        needUnsafe = translator.needUnsafe
        return res
    }
}

class UTestMethodTranslator(private val importManager: JcImportManager, private val typeTranslator: JcTypeTranslator) {
    val throwPool = mutableListOf<ClassOrInterfaceType>()
    var needUnsafe = false
    fun translate(test: UTest): List<Statement> {
        val initStmts: List<Statement> = test.initStatements.fold(listOf()) { acc, it ->
            if (it is UTestBinaryConditionStatement)
                acc + it.translated()
            else
                acc + ExpressionStmt(
                    when (it) {
                        is UTestStatement -> it.translatedIntoExpr()
                        is UTestExpression -> it.translated()
                    }
                )
        }

        val observedCall = ExpressionStmt(test.callMethodExpression.translated())
        return initStmts + observedCall
    }

    private fun JcField.isAccessible() = this.isPublic && !this.isFinal


    private fun getArrayElemType(type: JcType): JcType = when (type) {
        is JcArrayType -> getArrayElemType(type.elementType)
        else -> type
    }

    private fun UTestExpression.translated(): Expression = when (this) {
        is UTestArithmeticExpression -> BinaryExpr(
            this.lhv.translated(), this.rhv.translated(), when (this.operationType) {
                ArithmeticOperationType.AND -> BinaryExpr.Operator.AND
                ArithmeticOperationType.PLUS -> BinaryExpr.Operator.PLUS
                ArithmeticOperationType.SUB -> BinaryExpr.Operator.MINUS
                ArithmeticOperationType.MUL -> BinaryExpr.Operator.MULTIPLY
                ArithmeticOperationType.DIV -> BinaryExpr.Operator.DIVIDE
                ArithmeticOperationType.REM -> BinaryExpr.Operator.REMAINDER
                ArithmeticOperationType.EQ -> BinaryExpr.Operator.EQUALS
                ArithmeticOperationType.NEQ -> BinaryExpr.Operator.NOT_EQUALS
                ArithmeticOperationType.GT -> BinaryExpr.Operator.GREATER
                ArithmeticOperationType.GEQ -> BinaryExpr.Operator.GREATER_EQUALS
                ArithmeticOperationType.LT -> BinaryExpr.Operator.LESS
                ArithmeticOperationType.LEQ -> BinaryExpr.Operator.LESS_EQUALS
                ArithmeticOperationType.OR -> BinaryExpr.Operator.OR
                ArithmeticOperationType.XOR -> BinaryExpr.Operator.XOR
            }
        )

        is UTestArrayGetExpression -> ArrayAccessExpr(this.arrayInstance.translated(), this.index.translated())
        is UTestBinaryConditionExpression -> ConditionalExpr(
            BinaryExpr(
                this.lhv.translated(), this.rhv.translated(), translateConditionType(this.conditionType)
            ),
            this.trueBranch.translated(),
            this.elseBranch.translated()
        )


        is UTestConstructorCall -> ObjectCreationExpr(
            null,
            typeTranslator.typeReprOf(
                this.type
            ) as ClassOrInterfaceType,
            NodeList(this.args.map { it.translated() })
        )

        is UTestMethodCall -> MethodCallExpr(
            this.instance.translated(),
            this.method.name,
            NodeList(this.args.map { it.translated() })
        ).also {
            this.method.exceptions.forEach { exc ->
                importManager.tryAdd(exc.typeName)
                StaticJavaParser.parseClassOrInterfaceType(exc.typeName)
            }
        }

        is UTestStaticMethodCall -> MethodCallExpr(
            TypeExpr(typeTranslator.typeReprOf(this.method.enclosingClass.toType())),
            this.method.name,
            NodeList(this.args.map { it.translated() })
        ).also {
            this.method.exceptions.forEach { exc ->
                importManager.tryAdd(exc.typeName)
                throwPool.add(StaticJavaParser.parseClassOrInterfaceType(exc.typeName))
            }
        }

        is UTestAllocateMemoryCall -> EnclosedExpr(
            CastExpr(
                typeTranslator.typeReprOf(this.clazz.toType()), MethodCallExpr(
                    NameExpr("UNSAFE"),
                    "allocateInstance",
                    NodeList(listOf(ClassExpr(typeTranslator.typeReprOf(this.clazz.toType()))))
                )
            ).also {
                needUnsafe = true
                importManager.tryAdd("sun.misc.Unsafe", "Unsafe")
                importManager.tryAdd("java.lang.InstantiationException", "InstantiationException")
                throwPool.add(ClassOrInterfaceType(null, "java.lang.InstantiationException"))
            }
        )

        is UTestCastExpression -> CastExpr(
            typeTranslator.typeReprOf(this.type),
            this.expr.translated()
        )

        is UTestClassExpression -> ClassExpr(typeTranslator.typeReprOf(this.type))

        is UTestBooleanExpression -> BooleanLiteralExpr(this.value)
        is UTestByteExpression -> IntegerLiteralExpr(this.value.toString())
        is UTestCharExpression -> CharLiteralExpr(this.value)
        is UTestDoubleExpression -> DoubleLiteralExpr(this.value)
        is UTestFloatExpression -> DoubleLiteralExpr(this.value.toDouble())
        is UTestIntExpression -> IntegerLiteralExpr(this.value.toString())
        is UTestLongExpression -> LongLiteralExpr(this.value.toString())
        is UTestNullExpression -> NullLiteralExpr()
        is UTestShortExpression -> IntegerLiteralExpr(this.value.toString())
        is UTestStringExpression -> StringLiteralExpr(this.value)
        is UTestCreateArrayExpression -> ArrayCreationExpr(typeTranslator.typeReprOf(this.elementType))
        is UTestGetFieldExpression -> FieldAccessExpr(
            this.instance.translated(),
            this.field.name
        ).also { if (!field.isAccessible()) println("inaccessible field get") }

        is UTestGetStaticFieldExpression -> FieldAccessExpr(
            TypeExpr(typeTranslator.typeReprOf(this.field.enclosingClass.toType())),
            this.field.name
        ).also { if (!field.isAccessible()) println("inaccessible field get") }

        is UTestArrayLengthExpression -> NameExpr("arrLen")
        is UTestGlobalMock -> NameExpr("mockGlobal")
        is UTestMockObject -> NameExpr("mockObj")
    }

    private fun UTestStatement.translatedIntoExpr(): Expression = when (this) {
        is UTestArraySetStatement -> AssignExpr(
            ArrayAccessExpr(this.arrayInstance.translated(), this.index.translated()),
            this.setValueExpression.translated(),
            AssignExpr.Operator.ASSIGN
        )

        is UTestSetFieldStatement -> AssignExpr(
            FieldAccessExpr(this.instance.translated(), this.field.name),
            this.value.translated(),
            AssignExpr.Operator.ASSIGN
        ).also { if (!field.isAccessible()) println("inaccessible field set") }

        is UTestSetStaticFieldStatement -> AssignExpr(
            FieldAccessExpr(
                TypeExpr(typeTranslator.typeReprOf(this.field.enclosingClass.toType())),
                this.field.name
            ),
            this.value.translated(),
            AssignExpr.Operator.ASSIGN
        ).also { if (!field.isAccessible()) println("inaccessible field set") }

        is UTestBinaryConditionStatement -> error("should not be reachable")
    }

    private fun translateConditionType(ct: ConditionType) = when (ct) {
        ConditionType.EQ -> BinaryExpr.Operator.EQUALS
        ConditionType.NEQ -> BinaryExpr.Operator.NOT_EQUALS
        ConditionType.GEQ -> BinaryExpr.Operator.GREATER_EQUALS
        ConditionType.GT -> BinaryExpr.Operator.GREATER
    }

    private fun UTestBinaryConditionStatement.translated(): Statement =
        IfStmt(
            BinaryExpr(lhv.translated(), rhv.translated(), translateConditionType(this.conditionType)),
            BlockStmt(NodeList(trueBranch.map {
                when (it) {
                    is UTestBinaryConditionStatement -> it.translated()
                    else -> ExpressionStmt(it.translatedIntoExpr())
                }
            })),
            BlockStmt(NodeList(elseBranch.map {
                when (it) {
                    is UTestBinaryConditionStatement -> it.translated()
                    else -> ExpressionStmt(it.translatedIntoExpr())
                }
            }))
        )
}