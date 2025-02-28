package org.usvm.jvm.rendering

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.MarkerAnnotationExpr
import com.github.javaparser.ast.expr.Name
import com.github.javaparser.ast.expr.SimpleName
import java.io.File
import org.jacodb.api.jvm.JcClassType
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.ext.packageName

enum class JcSpringTestKind {
    WebMVC,
    Other,
    None
}

data class JcSpringTestMeta(
    val classUnderTest: JcClassType,
    val pathUnderTest: String,
    val testKind: JcSpringTestKind
)
abstract class JcTestClassRenderer(protected val cu: CompilationUnit, protected val testFile: File) {
    companion object {
        val CLASS_NAME_SUFFIX = "Test"
        val METHOD_NAME_PREFIX = "TestCase"
        val JUNIT5_ANNOTATION = "org.junit.jupiter.api.Test"

        fun loadFileOrCreateFor(meta: JcSpringTestMeta): JcTestClassRenderer {
            val testFilePath = buildList {
                add(meta.classUnderTest.jcClass.declaration.location.path)
                addAll(meta.classUnderTest.jcClass.packageName.split("."))
                add(meta.classUnderTest.jcClass.simpleName + CLASS_NAME_SUFFIX + ".java")
            }.joinToString(File.separator)

            val testFile = File(testFilePath)
            return if (testFile.exists()) loadFileAndValidate(testFile, meta) else createAndInit(testFile, meta)
        }

        private fun loadFileAndValidate(file: File, meta: JcSpringTestMeta): JcTestClassRenderer {
            fun isValid(cu: CompilationUnit): Boolean = true
            val cu = StaticJavaParser.parse(file)
            return if (isValid(cu)) {
                JcSpringTestClassRenderer(cu, file)
            } else {
                createAndInit(file, meta)
            }
        }

        private fun createAndInit(file: File, meta: JcSpringTestMeta): JcTestClassRenderer {
            file.createNewFile()
            val cu = CompilationUnit()
            val testClass = ClassOrInterfaceDeclaration()
            cu.setPackageDeclaration(meta.classUnderTest.jcClass.packageName)
            testClass.name = SimpleName(meta.classUnderTest.jcClass.simpleName + CLASS_NAME_SUFFIX)
            testClass.isPublic = true
            cu.addType(testClass)
            // TODO insert headers for tests of proper kind
            when (meta.testKind) {
                JcSpringTestKind.WebMVC -> {}
                JcSpringTestKind.Other -> {}
                JcSpringTestKind.None -> {}
            }
            return JcSpringTestClassRenderer(cu, file)
        }
    }
}
class JcSpringTestClassRenderer(
    cu: CompilationUnit,
    testFile: File
): JcTestClassRenderer(cu, testFile) {


    fun renderToFile(cp: JcClasspath, tests: List<UTestRenderWrapper<JcSpringTestMeta>>) {
//        val freshTestsPool = mutableListOf<MethodDeclaration>()
//        var staticInit: BlockStmt? = null
//        tests.forEach { t ->
//            val testClass = cu.types.single {
//                it.name == SimpleName(t.meta.classUnderTest.jcClass.simpleName + CLASS_NAME_SUFFIX)
//            }
//
//            if (testClass.methods.isNotEmpty()) {
//
//                testClass.methods.forEach { methodDeclaration ->
//                    if (methodDeclaration.associatedWith(t))
//                        testClass.remove(methodDeclaration)
//                }
//            }
//            val testMethod = testClass.injectTestBy(t.meta)
//            val bodyConverter = JcTestRendererImpl(importManager, JcTypeTranslator(importManager))
//            val testBody = bodyConverter.render(t.test)
//            testMethod.setBody(testBody)
//            bodyConverter.throwPool.forEach { exc ->
//                testMethod.addThrownException(exc)
//            }
//            freshTestsPool.add(testMethod)
//
//        }
//        val fullNameToSimple = importManager.fullToSimple()
//        fullNameToSimple.keys.forEach { fq -> cu.addImport(fq) }
//
//        freshTestsPool.forEach { case -> case.accept(FullNameToSimpleVisitor(fullNameToSimple), Unit) }
//        staticInit?.accept(FullNameToSimpleVisitor(fullNameToSimple), Unit)
//
//        val printer = DefaultPrettyPrinter()
//
//        testFile.writeText(printer.print(cu))
    }

    private fun TypeDeclaration<*>.injectTestBy(meta: JcSpringTestMeta): MethodDeclaration {
        val test = this.addMethod(
            meta.pathUnderTest.replace("/", "") + METHOD_NAME_PREFIX + methods.size,
            Modifier.Keyword.PUBLIC
        )
        // TODO does not scale now
        test.addAnnotation(MarkerAnnotationExpr(Name(JUNIT5_ANNOTATION)))
//        importManager.tryAdd(JUNIT5_ANNOTATION, simpleNameFromString(JUNIT5_ANNOTATION))
        // TODO notify import manager
        return test
    }

    // TODO
    private fun MethodDeclaration.associatedWith(test: UTestRenderWrapper<JcSpringTestMeta>): Boolean = false
}

internal object Utils {
    fun simpleNameFromString(value: String): String = value.split(".").last()
}