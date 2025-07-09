package org.usvm.jvm.spring.models

import machine.JcSpringTestObserver
import machine.state.JcSpringState
import org.usvm.jmv.spring.models.ErrorDescriptor
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestRenderer
import testGeneration.SpringTestInfo
import testGeneration.canGenerateTest
import testGeneration.generateTest

class JcSpringTestRdObserver(private val onNewTest: (String) -> Unit, private val onError: (ErrorDescriptor) -> Unit) : JcSpringTestObserver() {
    override fun onStateTerminated(state: JcSpringState, stateReachable: Boolean) {
        if (!stateReachable || !state.canGenerateTest()) return
        try {
            println("DBG: GENERATING TEST")
            val newTest = state.generateTest()
            tests.add(newTest)
            onNewTest(renderSingleTest(newTest))
        } catch (e: Throwable) {
            println("generation failed with $e on state terminated")
            onError(e.toDescriptor())
        }
    }

    private fun renderSingleTest(testInfo: SpringTestInfo): String {
        JcSpringMvcTestRenderer
    }
}