package org.usvm.jvm.spring

import SpringTestRenderer
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.jvm.rendering.testRenderer.JcTestInfo
import org.usvm.test.api.UTest
import testGeneration.SpringTestInfo

fun SpringTestInfo.toRenderInfo(): Pair<UTest, JcSpringMvcTestInfo> {
    return this.test to JcSpringMvcTestInfo(this.handler, this.isExceptional)
}

fun renderTests(testRenderer: SpringTestRenderer, tests: List<Pair<UTest, JcTestInfo>>, dir: File) {
    val rendered = testRenderer.render(tests)
    for ((testClassInfo, result) in rendered) {
        val testFile = dir.resolve("${testClassInfo.testClassName}.java")
        testFile.writeText(result)
    }
}
