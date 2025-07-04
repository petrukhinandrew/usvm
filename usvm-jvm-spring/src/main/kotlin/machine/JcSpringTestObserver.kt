package machine

import machine.state.JcSpringState
import org.usvm.statistics.UMachineObserver
import testGeneration.SpringTestInfo
import testGeneration.canGenerateTest
import testGeneration.generateTest

open class JcSpringTestObserver : UMachineObserver<JcSpringState> {

    protected val tests = mutableListOf<SpringTestInfo>()

    override fun onStateTerminated(state: JcSpringState, stateReachable: Boolean) {
        if (!stateReachable || !state.canGenerateTest()) return
        try {
            println("DBG: GENERATING TEST")
            tests.add(state.generateTest())
        } catch (e: Throwable) {
            println("generation failed with $e on state terminated")
        }
    }

    val generatedTests: List<SpringTestInfo> get() = tests
}
 