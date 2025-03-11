package org.usvm.jvm.rendering.testRenderer

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.printer.DefaultPrettyPrinter
import org.usvm.jvm.rendering.testRenderer.testTransformers.JcTestTransformer
import org.usvm.test.api.UTest
import java.io.PrintWriter
import java.net.URI
import java.nio.file.Paths

class JcTestsRenderer {
    val testFilePath = Paths.get(URI("usvm-jvm/src/test/kotlin/org/usvm/generated")).toAbsolutePath()
    val transformers: List<JcTestTransformer> = TODO()
    fun renderTests(tests: List<Pair<UTest, JcTestInfo>>) {
        val testClasses =
            tests.groupBy { (_, info) -> info.method.enclosingClass }

        for ((declType, testsToRender) in testClasses) {
            val testClassName = declType.simpleName + "Tests"
            val testClassRenderer = JcTestClassRenderer(testClassName)

            for ((test, testInfo) in testsToRender) {
                val testRenderer = testClassRenderer.addTest(test, "test")
                testRenderer.render()
            }

            val renderedTestClass = testClassRenderer.render()
            val imports = testClassRenderer.importManager.render()
            val packageDecl = PackageDeclaration(StaticJavaParser.parseName("org.usvm.generated"))
            val cu = CompilationUnit(packageDecl, imports, NodeList(renderedTestClass), null)
            val writer = PrintWriter(testFilePath.toString())
            writer.print(DefaultPrettyPrinter().print(cu))
            writer.close()
        }
    }
}
