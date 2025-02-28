package org.usvm.jvm.rendering

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.Statement
import org.jacodb.api.jvm.ext.toType
import org.usvm.test.api.UTest
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

class ImportFeature(private val cu: CompilationUnit) : JcTestRenderer.Feature {
    private val importedFullNames: MutableSet<String>
    private val importedPackages: MutableSet<String>
    private val fullToSimple: MutableMap<String, String>

    init {
        val (asterisk, nonAsterisk) = cu.imports.partition { import -> import.isAsterisk }
        importedPackages = asterisk.map { decl -> decl.nameAsString }.toMutableSet()
        importedFullNames = nonAsterisk.map { decl -> decl.nameAsString }.toMutableSet()
        fullToSimple = importedFullNames.associateByTo(mutableMapOf()) { it.split(".").last() }
    }

    override fun prepare(test: UTest) {
        cu.addImport(ImportDeclaration("java.lang", false, true))
    }

    private fun tryAdd(type: String) {
        val clazz = StaticJavaParser.parseClassOrInterfaceType(type).removeTypeArguments()
        if (!fullToSimple.values.contains(clazz.nameAsString)) {
            fullToSimple.putIfAbsent(clazz.nameWithScope, clazz.nameAsString)
            clazz.scope.ifPresent { scope -> if (!importedPackages.contains(scope.nameWithScope))
                cu.addImport(clazz.nameWithScope)
            }
        }
    }

    override fun applyTo(expr: UTestExpression): UTestExpression {
        when (expr) {
            is UTestMethodCall -> {
                tryAdd(expr.method.enclosingClass.name)
            }

            is UTestStaticMethodCall, is UTestConstructorCall -> {
                tryAdd(expr.method!!.enclosingClass.name)
            }

            is UTestAllocateMemoryCall -> {
                tryAdd("sun.misc.Unsafe")
            }

            is UTestCastExpression, is UTestClassExpression -> {
                if (expr.type != null) tryAdd(expr.type!!.typeName)
            }

            is UTestGetStaticFieldExpression -> {
                tryAdd(expr.field.enclosingClass.name)
            }

            else -> {}
        }

        return expr
    }

    override fun applyTo(stmt: UTestStatement): UTestStatement {
        if (stmt is UTestSetStaticFieldStatement) {
            tryAdd(stmt.field.enclosingClass.name)
        }
        return stmt
    }

    override fun applyToRendered(stmt: Statement): Statement {
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
