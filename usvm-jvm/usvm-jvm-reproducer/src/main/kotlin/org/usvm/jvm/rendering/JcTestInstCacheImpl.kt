package org.usvm.jvm.rendering

import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.Statement
import java.util.*
import kotlin.math.max
import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.ext.void
import org.usvm.test.api.UTest
import org.usvm.test.api.UTestAllocateMemoryCall
import org.usvm.test.api.UTestConstExpression
import org.usvm.test.api.UTestExpression
import org.usvm.test.api.UTestInst
import org.usvm.test.api.UTestSetFieldStatement

class JcTestInstCacheImpl(
    override val renderer: JcTestRenderer,
) :
    JcTestInstCache {
    private val cache: IdentityHashMap<UTestExpression, Expression> = IdentityHashMap()
    private val instToRequiredDeclarations: IdentityHashMap<UTestInst, List<Statement>> = IdentityHashMap()

    private val varNameManager = renderer.varNameManager

    override fun initialize(test: UTest): UTest {

        val wrappersCollector: IdentityHashMap<UTestExpression, MutableSet<UTestExpression>> = IdentityHashMap()
        val filteredInitStatements = test.initStatements.filter { inst ->
            if (inst !is UTestSetFieldStatement) return@filter true
            val lhs = inst.instance
            if (lhs is UTestAllocateMemoryCall && lhs.clazz.isPrimitiveWrapper()) {
                wrappersCollector.getOrPut(lhs) { mutableSetOf() }.add(inst.value)
                return@filter false
            }
            true
        }
        val wrappersMapping =
            wrappersCollector.mapNotNull { (k, v) ->
                if (v.size == 1) k to renderer.renderConstExpression(v.single() as UTestConstExpression<*>) else null
            }.toMap()

        val exprCounter: IdentityHashMap<UTestExpression, Int> = IdentityHashMap()
        val declCollector: IdentityHashMap<UTestInst, IdentityHashMap<UTestExpression, Int>> = IdentityHashMap()
        (filteredInitStatements + test.callMethodExpression).forEach { inst ->
            declCollector[inst] = IdentityHashMap<UTestExpression, Int>()
            UTestInstTraverser.traverseInst(inst) { i, depth ->
                if (i is UTestExpression) {
                    exprCounter.compute(i) { _, v -> (v ?: 0) + 1 }
                    declCollector[inst]!!.compute(i) { _, u -> max(u ?: 0, depth) }
                }
            }
        }

        val exprMapping =
            exprCounter.filter { (k, v) -> (v > 1 || renderer.requireDeclarationOf(k)) && !wrappersMapping.contains(k) }.keys.associateWith { expr ->
                varNameManager.chooseNameFor(expr)
            }.toMutableMap()

        val declared = mutableSetOf<UTestExpression>()
        val declMapping = filteredInitStatements.associateWith { inst ->
            val requireDecl = declCollector[inst]!!.entries.filter { (k, _) ->
                exprMapping.contains(k) && !declared.contains(k)
            }.sortedByDescending { e -> e.value }
            declared.addAll(requireDecl.map { entry -> entry.key })
            requireDecl.mapNotNull { entry ->
                if (entry.key.type == null || entry.key.type == entry.key.type!!.classpath.void) return@mapNotNull null
                renderer.renderVarDeclaration(
                    entry.key.type!!,
                    exprMapping.getOrPut(entry.key) {
                        varNameManager.chooseNameFor(
                            entry.key
                        )
                    }, entry.key
                )
            }
        }

        cache.clear()
        cache.putAll(wrappersMapping + exprMapping.mapValues { (_, name) -> NameExpr(name) })

        instToRequiredDeclarations.clear()
        instToRequiredDeclarations.putAll(declMapping)

        return UTest(filteredInitStatements, test.callMethodExpression)
    }

    override fun getRequiredDeclarations(inst: UTestInst): List<Statement> =
        instToRequiredDeclarations[inst] ?: emptyList()

    private fun JcClassOrInterface.isPrimitiveWrapper(): Boolean =
        listOf(
            "java.lang.Void",
            "java.lang.Object",
            "java.lang.Boolean",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Byte",
            "java.lang.Character"
        ).contains(name)

    override fun put(expr: UTestExpression): Expression =
        cache.computeIfAbsent(expr) { e -> NameExpr(varNameManager.chooseNameFor(e)) }

    override fun getOrElse(
        expr: UTestExpression,
        block: () -> Expression
    ): Expression = cache.getOrElse(expr) {
        block()
    }
}
