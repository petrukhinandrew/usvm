package org.usvm.jvm.spring

import SpringTestRenderer
import SpringTestReproducer
import java.io.File
import machine.JcConcreteMachineOptions
import org.jacodb.api.jvm.JcClasspath
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.test.api.UTest
import testGeneration.SpringTestInfo


private fun createOrClear(file: File) {
    if (file.exists()) {
        file.listFiles()?.forEach { it.deleteRecursively() }
    } else {
        file.mkdirs()
    }
}

fun reproduceTests(
    tests: List<SpringTestInfo>,
    jcConcreteMachineOptions: JcConcreteMachineOptions,
    cp: JcClasspath
) {
    val testReproducer by lazy { SpringTestReproducer(jcConcreteMachineOptions, cp) }
    val testRenderer by lazy { SpringTestRenderer(cp) }

    val reproducedTests = mutableListOf<Pair<UTest, JcSpringMvcTestInfo>>()
    val notReproducedTests = mutableListOf<Pair<UTest, JcSpringMvcTestInfo>>()
    for (testInfo in tests) {
        val reproduced = testReproducer.reproduce(testInfo.test)
        if (reproduced == "success")
            reproducedTests.add(testInfo.toRenderInfo())
        else {
            println(testInfo.stateId)
            println(reproduced)
            println(testRenderer.render(testInfo.test, testInfo.handler, testInfo.isExceptional))
            notReproducedTests.add(testInfo.toRenderInfo())
        }
    }
    testReproducer.kill()

    val currentDir = File(System.getProperty("user.dir"))
    val generatedTestsDir = currentDir.resolve("generatedTests")
    createOrClear(generatedTestsDir)
    val reproducedDir = generatedTestsDir.resolve("reproduced")
    createOrClear(reproducedDir)
    val notReproducedDir = generatedTestsDir.resolve("notReproduced")
    createOrClear(notReproducedDir)

    renderTests(testRenderer, reproducedTests + notReproducedTests, notReproducedDir)

    println("Reproduced ${reproducedTests.size} of ${tests.size} tests")
}
