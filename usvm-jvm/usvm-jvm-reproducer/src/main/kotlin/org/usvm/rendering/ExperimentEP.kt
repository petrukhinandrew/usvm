package org.usvm.org.usvm.rendering

import bench.loadBenchClassesOnly
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.ClassExpr
import com.github.javaparser.ast.expr.MarkerAnnotationExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.TryStmt
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.type.ReferenceType
import com.github.javaparser.printer.DefaultPrettyPrinter
import java.io.File
import java.lang.Exception
import kotlin.io.path.Path
import kotlin.use
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.JcMethod
import org.jacodb.api.jvm.ext.findClass
import org.jacodb.api.jvm.ext.packageName
import org.jacodb.api.jvm.ext.toType
import org.jooq.Block
import org.objectweb.asm.tree.TryCatchBlockNode
import org.usvm.UMachineOptions
import org.usvm.machine.JcMachine
import org.usvm.machine.JcMachineOptions
import org.usvm.test.api.UTest

class JcTestRenderRunner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val bench =
                loadBenchClassesOnly(
                    listOf(File("/Users/petrukhinandrew/IdeaProjects/sandbox/build/classes/java/main"))
                )

            val cp = bench.cp
            val className = "a.b.c.SampleA"
            val methodName = "SomeMethod"
            val method = cp.findClass(className)
                .toType().declaredMethods.first { it.name == methodName }
            JcMachine(cp, UMachineOptions(), JcMachineOptions()).use { machine ->
                val states = machine.analyze(method.method).map {
                    UTest.fromSnapshot(method, it)
                }.map {
                    UTestWrapper(
                        it, JcSpringTestMeta(
                            "/Users/petrukhinandrew/IdeaProjects/sandbox/src/test/java",
                            method.method, JcSpringTestKind.None
                        )
                    )
                }
                val manager = JcSpringTestRenderManager()
                manager.render(cp, states)
            }
        }
    }
}

data class UTestWrapper<TestMeta>(val test: UTest, val meta: TestMeta)

enum class JcSpringTestKind {
    WebMVC,
    Other,
    None
}

data class JcSpringTestMeta(
    val filePath: String,
    val targetMethod: JcMethod,
    val testKind: JcSpringTestKind
)

interface UTestRenderManager<TestMeta> {
    fun render(cp: JcClasspath, tests: List<UTestWrapper<TestMeta>>)
}

class JcSpringTestRenderManager : UTestRenderManager<JcSpringTestMeta> {
    override fun render(cp: JcClasspath, tests: List<UTestWrapper<JcSpringTestMeta>>) {
        val classes = tests.groupBy { wrapper -> wrapper.meta }
        classes.forEach { cls ->
            val renderer = JcSpringTestClassRenderer.loadFileOrCreateFor(cls.key)
            renderer.renderToFile(cp, cls.value)

        }
    }
}

class JcSpringTestClassRenderer(
    private val cu: CompilationUnit,
    private val importManager: JcImportManager,
    private val testFile: File
) {
    companion object Builder {
        private val CLASS_NAME_SUFFIX = "Test"
        private val METHOD_NAME_PREFIX = "TestCase"
        private val JUNIT5_ANNOTATION = "org.junit.jupiter.api.Test"

        private fun simpleNameFromString(value: String): String = value.split(".").last()

        fun loadFileOrCreateFor(meta: JcSpringTestMeta): JcSpringTestClassRenderer {
            val testFile = File(
                (listOf(meta.filePath) + meta.targetMethod.enclosingClass.packageName.split(".") + listOf(meta.targetMethod.enclosingClass.simpleName + CLASS_NAME_SUFFIX + ".java")).joinToString(
                    File.separator
                )
            )
            return if (testFile.exists()) loadFromAndCheck(testFile, meta) else createAndInit(testFile, meta)
        }

        private fun loadFromAndCheck(file: File, meta: JcSpringTestMeta): JcSpringTestClassRenderer {

            val cu = StaticJavaParser.parse(file)
            return if (cu.types.any { declaration -> declaration.name == SimpleName(meta.targetMethod.enclosingClass.simpleName + CLASS_NAME_SUFFIX) })
                JcSpringTestClassRenderer(
                    cu,
                    JcImportManager(cu.imports.map { it.name.asString() }),
                    file
                ) else createAndInit(file, meta)
        }

        private fun createAndInit(file: File, meta: JcSpringTestMeta): JcSpringTestClassRenderer {
            file.createNewFile()
            val cu = CompilationUnit()
            val testClass = ClassOrInterfaceDeclaration()
            cu.setPackageDeclaration(meta.targetMethod.enclosingClass.packageName)
            testClass.name = SimpleName(meta.targetMethod.enclosingClass.simpleName + CLASS_NAME_SUFFIX)
            testClass.isPublic = true
            cu.addType(testClass)
            // TODO insert headers for tests of proper kind
            when (meta.testKind) {
                JcSpringTestKind.WebMVC -> {}
                JcSpringTestKind.Other -> {}
                JcSpringTestKind.None -> {}
            }
            return JcSpringTestClassRenderer(cu, JcImportManager(), file)
        }
    }

    fun renderToFile(cp: JcClasspath, tests: List<UTestWrapper<JcSpringTestMeta>>) {
        val freshTestsPool = mutableListOf<MethodDeclaration>()
        var staticInit: BlockStmt? = null
        tests.forEach { t ->
            val testClass = cu.types.single {
                it.name == SimpleName(t.meta.targetMethod.enclosingClass.simpleName + CLASS_NAME_SUFFIX)
            }

            if (testClass.methods.isNotEmpty()) {

                testClass.methods.forEach { methodDeclaration ->
                    if (methodDeclaration.associatedWith(t))
                        testClass.remove(methodDeclaration)
                }
            }
            val testMethod = testClass.injectTestBy(t.meta)
            val bodyConverter = JcTestMethodBodyConverter(importManager, JcTypeTranslator(importManager))
            val testBody = bodyConverter.convert(t.test)
            testMethod.setBody(testBody)
            bodyConverter.throwPool.forEach { exc ->
                testMethod.addThrownException(exc)
            }
            freshTestsPool.add(testMethod)
            if (bodyConverter.needUnsafe && !testClass.fields.any { declaration ->
                    declaration.variables.any { v ->
                        v.name == SimpleName(
                            "UNSAFE"
                        )
                    }
                }) {
                testClass.addField(
                    ClassOrInterfaceType(null, "sun.misc.Unsafe"),
                    "UNSAFE",
                    Modifier.Keyword.STATIC,
                    Modifier.Keyword.PRIVATE
                )

                importManager.tryAdd("java.lang.reflect.Field")
                importManager.tryAdd("java.lang.Exception")
                staticInit = testClass.addStaticInitializer()
                val tryStmt = TryStmt()
                tryStmt.setTryBlock(
                    BlockStmt(
                        NodeList(
                            listOf(
                                ExpressionStmt(
                                    AssignExpr(
                                        VariableDeclarationExpr(
                                            ClassOrInterfaceType(null, "java.lang.reflect.Field"), "uns"
                                        ),
                                        MethodCallExpr(
                                            ClassExpr(ClassOrInterfaceType(null, "sun.misc.Unsafe")),
                                            "getDeclaredField",
                                            NodeList.nodeList(StringLiteralExpr("theUnsafe"))
                                        ), AssignExpr.Operator.ASSIGN
                                    )
                                ),
                                ExpressionStmt(
                                    MethodCallExpr(
                                        NameExpr("uns"), "setAccessible", NodeList.nodeList(
                                            BooleanLiteralExpr(true)
                                        )
                                    )
                                ),
                                ExpressionStmt(
                                    AssignExpr(
                                        NameExpr("UNSAFE"), CastExpr(
                                            ClassOrInterfaceType(null, "sun.misc.Unsafe"),
                                            MethodCallExpr(NameExpr("uns"), "get", NodeList.nodeList(NullLiteralExpr()))
                                        ), AssignExpr.Operator.ASSIGN
                                    )
                                )

                            )
                        )
                    )
                )
                tryStmt.setCatchClauses(
                    NodeList.nodeList(
                        CatchClause(
                            Parameter(ClassOrInterfaceType(null, "java.lang.Exception"), "ignored"),
                            BlockStmt()
                        )
                    )
                )
                staticInit.addStatement(tryStmt)
            }
        }
        val fullNameToSimple = importManager.fullToSimple()
        fullNameToSimple.keys.forEach { fq -> cu.addImport(fq) }

        freshTestsPool.forEach { case -> case.accept(FullNameToSimpleVisitor(fullNameToSimple), Unit) }
        staticInit?.accept(FullNameToSimpleVisitor(fullNameToSimple), Unit)

        val printer = DefaultPrettyPrinter()

        testFile.writeText(printer.print(cu))
    }

    private fun TypeDeclaration<*>.injectTestBy(meta: JcSpringTestMeta): MethodDeclaration {
        val test = this.addMethod(meta.targetMethod.name + METHOD_NAME_PREFIX + methods.size, Modifier.Keyword.PUBLIC)
        // TODO does not scale now
        test.addAnnotation(MarkerAnnotationExpr(Name(JUNIT5_ANNOTATION)))
        importManager.tryAdd(JUNIT5_ANNOTATION, simpleNameFromString(JUNIT5_ANNOTATION))
        // TODO notify import manager
        return test
    }

    // TODO
    private fun MethodDeclaration.associatedWith(test: UTestWrapper<JcSpringTestMeta>): Boolean = false

    //        this.name.asString() == test.meta.targetMethodName
}
