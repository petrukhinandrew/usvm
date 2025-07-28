package org.usvm.jvm.spring.models

import SpringTestReproducer
import bench.toRenderInfo
import machine.JcSpringConfigProvider
import machine.JcSpringTestObserver
import machine.state.JcSpringState
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jvm.rendering.JcTestsRenderer
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.jvm.spring.runner.toProcError
import org.usvm.test.api.UTest
import testGeneration.canGenerateTest
import testGeneration.generateTest

class JcSpringTestRdObserver(
    private val analysisModel: AnalysisProcessModel,
    private val reproducer: SpringTestReproducer
) : JcSpringTestObserver() {

    fun mockOnStateTerminated(uTest: UTest, testInfo: JcSpringMvcTestInfo) {
//        val reproduced = uTest.reproducesIn(reproducer)
//        if (!reproduced) return

        val render = JcTestsRenderer().renderSingleTestInClass(
            JcSpringConfigProvider.getTestClassStub(),
            Pair(uTest, testInfo)
        )
        analysisModel.generatedTests.add(render)
    }

    override fun onStateTerminated(state: JcSpringState, stateReachable: Boolean) {
        if (!stateReachable || !state.canGenerateTest()) return
        try {
            val newTest = state.generateTest()
            val reproduced = newTest.test.reproducesIn(reproducer)
            if (!reproduced) return

            tests.add(newTest)

            val render = JcTestsRenderer().renderSingleTestInClass(
                JcSpringConfigProvider.getTestClassStub(),
                newTest.toRenderInfo()
            )
            analysisModel.generatedTests.add(render)
        } catch (e: Throwable) {
            analysisModel.processSignal.fire(e.toProcError("state processing failed"))
        }
    }

    private fun UTest.reproducesIn(reproducer: SpringTestReproducer): Boolean {
        return reproducer.reproduce(this) == "success"
    }
}
