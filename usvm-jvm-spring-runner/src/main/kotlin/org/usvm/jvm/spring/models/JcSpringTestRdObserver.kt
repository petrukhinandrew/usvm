package org.usvm.jvm.spring.models

import SpringTestReproducer
import bench.toRenderInfo
import machine.JcSpringTestObserver
import machine.state.JcSpringState
import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.ext.findClass
import org.usvm.jmv.spring.models.ErrorDescriptor
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestClassRenderer
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.test.api.UTest
import testGeneration.SpringTestInfo
import testGeneration.canGenerateTest
import testGeneration.generateTest

class JcSpringTestRdObserver(
    private val onNewTest: (String) -> Unit,
    private val onError: (ErrorDescriptor) -> Unit
) : JcSpringTestObserver() {

    lateinit var reproducer: Reproducer

    fun bindReproducer(instance: Reproducer) {
        reproducer = instance
    }

    lateinit var renderer: SingleTestRenderer

    fun bindRenderer(instance: SingleTestRenderer) {
        renderer = instance
    }

    override fun onStateTerminated(state: JcSpringState, stateReachable: Boolean) {
        if (!stateReachable || !state.canGenerateTest()) return
        try {
            val newTest = state.generateTest()
            tests.add(newTest)
            val render = renderer.render(
                newTest.toRenderInfo()
            )
            onNewTest(render)
        } catch (e: Throwable) {
            onError(e.toDescriptor())
        }
    }
}

class Reproducer {
    fun reproduce(uTest: UTest): Boolean {
        TODO()
    }
}

class SingleTestRenderer {
    fun render(renderInfo: Pair<UTest, JcSpringMvcTestInfo>): String {
        val (uTest, testInfo) = renderInfo
        val controller = testInfo.controller
        val cp = controller.classpath
        TODO()
    }
}