package org.usvm.jvm.rendering

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.Statement
import org.jacodb.api.jvm.JcClassType
import org.jacodb.api.jvm.JcType
import org.jacodb.api.jvm.ext.packageName
import org.jacodb.api.jvm.ext.toType
import org.usvm.test.api.UTestAllocateMemoryCall
import org.usvm.test.api.UTestCastExpression
import org.usvm.test.api.UTestClassExpression
import org.usvm.test.api.UTestConstructorCall
import org.usvm.test.api.UTestExpression
import org.usvm.test.api.UTestGetStaticFieldExpression
import org.usvm.test.api.UTestMethodCall
import org.usvm.test.api.UTestSetStaticFieldStatement
import org.usvm.test.api.UTestStatement
import org.usvm.test.api.UTestStaticMethodCall

class ThrowCollectorFeature(private val cu: CompilationUnit) : JcTestRenderer.Feature {
    private val exceptions: MutableSet<String> = mutableSetOf()
    override fun applyTo(expr: UTestExpression): UTestExpression {
        when (expr) {
            is UTestMethodCall, is UTestStaticMethodCall, is UTestConstructorCall -> exceptions.addAll(expr.method!!.exceptions.map { it.typeName })
            is UTestAllocateMemoryCall -> exceptions.add("java.lang.InstantiationException")
            else -> {}
        }
        return expr
    }
}

class ImportFeature(private val cu: CompilationUnit) : JcTestRenderer.Feature {
    // TODO: asterisk unhandled
    private val imports = cu.imports.map { it.name.asString() }.toMutableSet()
    private val fullToSimple = imports.associateByTo(mutableMapOf()) {
        it.split(".").last()
    }

    private fun tryAdd(type: JcType?): String? =
        if (type == null) null else tryAdd(typeToString(type))

    private fun tryAdd(type: String): String? {
        cu.addImport(type)
        return fullToSimple.putIfAbsent(type, type.split(".").last())
    }

    private fun typeToString(type: JcType): String =
        StaticJavaParser.parseClassOrInterfaceType(type.typeName).removeTypeArguments().nameWithScope

    override fun applyTo(expr: UTestExpression): UTestExpression {
        when (expr) {
            is UTestMethodCall -> {
                tryAdd(expr.method.enclosingClass.toType())
            }

            is UTestStaticMethodCall, is UTestConstructorCall -> {
                tryAdd(expr.method!!.enclosingClass.toType())
            }

            is UTestAllocateMemoryCall -> {
                tryAdd("sun.misc.Unsafe")
            }

            is UTestCastExpression, is UTestClassExpression -> {
                tryAdd(expr.type)
            }

            is UTestGetStaticFieldExpression -> {
                tryAdd(expr.field.enclosingClass.toType())
            }

            else -> {}
        }

        return expr
    }

    override fun applyTo(stmt: UTestStatement): UTestStatement {
        if (stmt is UTestSetStaticFieldStatement) {
            tryAdd(stmt.field.enclosingClass.toType())
        }
        return stmt
    }

    override fun postProcess(stmt: Statement): Statement {
        val visitor = FullNameToSimpleVisitor(fullToSimple)
        return stmt.accept(visitor, Unit) as Statement
    }
}

class JcTestRendererImpl(override val features: List<Feature>) : JcTestRenderer() {
    override fun renderAllocateMemoryCall(expr: UTestAllocateMemoryCall): Expression {
        return MethodCallExpr(NameExpr("Unsafe"), "allocateInstance").apply {
            this.arguments = NodeList(renderExpression(UTestClassExpression(expr.clazz.toType())))
        }
    }

    override fun requireDeclarationOf(expr: UTestExpression): Boolean {
        return expr is UTestAllocateMemoryCall
    }
}
