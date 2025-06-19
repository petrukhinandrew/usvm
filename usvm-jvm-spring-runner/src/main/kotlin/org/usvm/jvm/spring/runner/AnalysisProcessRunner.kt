package org.usvm.jvm.spring.runner

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.io.path.absolutePathString
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import mu.KLogging
import org.usvm.instrumentation.executor.RdProcessRunnerBase
import org.usvm.instrumentation.util.InstrumentationModuleConstants
import org.usvm.instrumentation.util.UTestExecutorInitException
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisRequest
import org.usvm.jmv.spring.models.PrepareDbRequest
import org.usvm.jmv.spring.models.ProcNotification
import org.usvm.jmv.spring.models.ProcTerminated
import org.usvm.jmv.spring.models.analysisProcessModel

val logger = object : KLogging() {}.logger

class AnalysisRdProcessRunner(
    process: Process,
    checkProcessAliveDelay: Duration,
    rdPort: Int,
    lifetimeDefinition: LifetimeDefinition
) : RdProcessRunnerBase("usvm-spring-analysis", process, checkProcessAliveDelay, rdPort, lifetimeDefinition) {
    val model get() = rdProcess.model as AnalysisProcessModel

    val isAlive get() = lifetime.isAlive

    override fun initModels(protocol: Protocol): RdExtBase {
        super.initModels(protocol)
        return protocol.analysisProcessModel
    }

    fun bindOnProcessExit(callback: () -> Unit) {
        process.onExit().thenRun { callback() }
    }
}

class NoProc: ProcNotification()

@Suppress("unused")
class AnalysisProcessRunner(val lifetime: LifetimeDefinition) {

    private val workingDir = Files.createTempDirectory("springAnalysis").toFile()

    private val localLambdaDir get() = workingDir.resolve("lambda").createOrClear()

    private val localSpringDir get() = workingDir.resolve("spring").createOrClear()

    private val tempPolicyPath: Path = generateTempPolicyFile(workingDir.toPath())

    init {
        workingDir.deleteOnExit() // TODO: check if it should be recursive?
        tempPolicyPath.toFile().deleteOnExit()
    }

    companion object {
        private const val localAgentPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-concrete/agent/build/libs/agent.jar"
        private const val localRunnerPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-spring-runner/build/libs/usvm-jvm-spring-runner-1.2.10.jar"

        private const val analysisProcessMainClass = "org.usvm.jvm.spring.runner.AnalysisProcess"
    }

    lateinit var rdProcessRunner: AnalysisRdProcessRunner

    val model: AnalysisProcessModel get() = rdProcessRunner.model

    val isAlive: Boolean get() = lifetime.isAlive

    private val runnerStateMutable = MutableStateFlow<ProcNotification>(NoProc())
    val runnerState: StateFlow<ProcNotification>
        get() = runnerStateMutable

    val generatedTests = ConcurrentLinkedDeque<String>()

    suspend fun start(timeoutSeconds: Int, javaPath: String, allowDebugging: Boolean) {
        val port = NetUtils.findFreePort(0)
        val process = runUsvmSpringJar(
            javaPath = javaPath,
            targetJarPath = localRunnerPath,
            springDir = localSpringDir,
            lambdaDir = localLambdaDir,
            arguments = buildJvmArgs(
                mainClazz = analysisProcessMainClass,
                agentPath = localAgentPath,
                lambdaDirPath = localLambdaDir.absolutePath,
                tempPolicyPath = tempPolicyPath.absolutePathString(),
                allowDebugging = allowDebugging
            ) + listOf("-t", timeoutSeconds.toString(), "-p", port.toString()),
        ) ?: error("cannot run jar")
        rdProcessRunner =
            AnalysisRdProcessRunner(process = process, checkProcessAliveDelay = 1.seconds, rdPort = port, lifetimeDefinition = lifetime)
        rdProcessRunner.init()

        rdProcessRunner.bindOnProcessExit {
            runnerStateMutable.value = ProcTerminated()
        }

        model.processSignal.advise(lifetime) {
            runnerStateMutable.value = it
        }

        model.generatedTests.advise(lifetime) {
            if (it.newValueOpt != null) {
                generatedTests.add(it.newValueOpt)
            }
        }
    }

    fun stop() {
        if (lifetime.isAlive) {
            rdProcessRunner.destroy()
            resetState()
        }
        else {
            logger.warn("AnalysisProcessRunner stop request after lifetime terminated")
        }
    }

    private suspend fun ensureRunnerAlive() {
        check(lifetime.isAlive) { "Executor already closed" }
        for (i in 0..InstrumentationModuleConstants.triesToRecreateExecutorRdProcess) {
            if (rdProcessRunner.isAlive) {
                return
            }
            try {
                rdProcessRunner.init()
            } catch (e: Throwable) {
                println("Cant init rdProcess $e")
            }
        }
        if (!rdProcessRunner.isAlive) {
            throw UTestExecutorInitException()
        }
    }

    suspend fun startAnalysis(request: AnalysisRequest) {
        ensureRunnerAlive()
        model.runAnalysis.fire(request)
    }

    suspend fun stopAnalysis() {
        ensureRunnerAlive()
        model.stopAnalysis.fire(Unit)
    }

    suspend fun prepareDb(dbRequest: PrepareDbRequest) {
        ensureRunnerAlive()
        model.prepareDb.fire(dbRequest)
    }

    suspend fun refreshContext() {
        ensureRunnerAlive()
        model.refreshContext.fire(Unit)
    }

    suspend fun notifyServerReady() {
        ensureRunnerAlive()
        model.serverReady.fire(Unit)
    }

    private fun resetState() {
        generatedTests.clear()
        runnerStateMutable.value = NoProc()
    }
}