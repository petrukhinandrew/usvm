package org.usvm.jvm.spring.runner

import SpringTestReproducer
import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import machine.JcSpringAnalysisMode
import machine.JcSpringAnalysisSessionConfig
import machine.JcSpringControllerAnalysisConfig
import machine.JcSpringHandlerAnalysisConfig
import machine.JcSpringMachineOptions
import machine.JcSpringPathAnalysisConfig
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.ext.findClass
import org.usvm.instrumentation.generated.models.syncProtocolModel
import org.usvm.instrumentation.rd.CHILD_PROCESS_NAME
import org.usvm.instrumentation.rd.MAIN_PROCESS_NAME
import org.usvm.instrumentation.rd.adviseForConditionAsync
import org.usvm.instrumentation.rd.pumpAsync
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisRequest
import org.usvm.jmv.spring.models.ClasspathSource
import org.usvm.jmv.spring.models.PrepareDbRequest
import org.usvm.jmv.spring.models.ProcCpReady
import org.usvm.jmv.spring.models.ProcDbReady
import org.usvm.jmv.spring.models.ProcStarted
import org.usvm.jmv.spring.models.analysisProcessModel
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.jvm.spring.BenchCp
import org.usvm.jvm.spring.generator.generateTestClass
import org.usvm.jvm.spring.loader.concreteApiFile
import org.usvm.jvm.spring.loader.loadBenchClasspath
import org.usvm.jvm.spring.loader.loadBenchDatabase
import org.usvm.jvm.spring.models.JcSpringTestRdObserver
import org.usvm.jvm.spring.utils.ResultWithTime
import org.usvm.jvm.spring.utils.awaitTermination
import org.usvm.jvm.spring.utils.terminateOnException
import org.usvm.jvm.spring.utils.toProcError
import org.usvm.jvm.util.stringType
import org.usvm.test.api.UTest
import org.usvm.test.api.UTestStringExpression
import org.usvm.test.api.spring.SpringTestExecBuilder

class AnalysisProcess private constructor() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val proc = AnalysisProcess()
            proc.start(args)
        }
    }

    lateinit var rawBenchCp: BenchCp

    lateinit var cpSource: ClasspathSource

    lateinit var analysisModel: AnalysisProcessModel

    fun start(args: Array<String>) = runBlocking {
        val opts = Options()
            .addOption("t", true, "timeout in seconds")
            .addOption("p", true, "port")

        val cmd = DefaultParser().parse(opts, args)
        val timeout = cmd.getOptionValue("t")
            .toIntOrNull()?.toDuration(DurationUnit.SECONDS) ?: 120.toDuration(DurationUnit.SECONDS)
        val port = cmd.getOptionValue("p")
            .toIntOrNull() ?: error("Specify rd port number")

        val def = LifetimeDefinition()

        terminateOnException(def) {
            initiate(def, port, timeout)
            awaitTermination(def)
        }
    }

    private suspend fun initiate(lifetime: Lifetime, port: Int, timeout: Duration) {
        val scheduler = SingleThreadScheduler(lifetime, "usvm-analysis-worker-scheduler")
        val protocol = Protocol(
            "usvm-analysis-worker",
            Serializers(),
            Identities(IdKind.Client),
            scheduler,
            SocketWire.Client(lifetime, scheduler, port),
            lifetime
        )

        analysisModel = protocol.scheduler.pumpAsync(lifetime) {
            protocol.syncProtocolModel
            protocol.analysisProcessModel
        }.await()

        analysisModel.setup(timeout, lifetime, true)

        val syncSignal = protocol.syncProtocolModel.synchronizationSignal
        val answerFromMainProcess = syncSignal.adviseForConditionAsync(lifetime) {
            if (it == MAIN_PROCESS_NAME) {
                syncSignal.fire(CHILD_PROCESS_NAME)
                true
            } else {
                false
            }
        }
        answerFromMainProcess.await()
    }

    val mockController by lazy {
        AnalysisProcessMockController()
    }
    private fun AnalysisProcessModel.setup(runnerTimeout: Duration, lifetime: Lifetime, mockAnalysis: Boolean) {

        prepareDb.advise(lifetime) { request ->
            if (mockAnalysis)
                mockController.prepareDbHandlerMock(this, request)
            else
                prepareDbHandler(request)
        }

        runAnalysis.advise(lifetime) { request ->
            if (mockAnalysis)
                mockController.runAnalysisHandlerMock(this, request, runnerTimeout)
            else
                runAnalysisHandler(request, runnerTimeout) }

        // TODO: looks like hack
        serverReady.advise(lifetime) { processSignal.fire(ProcStarted()) }
    }

    private fun AnalysisProcessModel.runAnalysisHandler(
        request: AnalysisRequest,
        runnerTimeout: Duration
    ) {
        check(this@AnalysisProcess::cpSource.isInitialized && this@AnalysisProcess::rawBenchCp.isInitialized) {
            "cp is not initialized"
        }

        val analysisMode = JcSpringAnalysisMode.SpringBootTest
        val updatedBenchResult = ResultWithTime.calculate {
            generateTestClass(
                rawBenchCp,
                cpSource,
                analysisMode,
                request.analysisBootApp
            )
        }
        when {
            updatedBenchResult.error != null -> {
                println("${updatedBenchResult.error} occurred")
                processSignal.fire(updatedBenchResult.error.toProcError("cp preparation error"))
            }

            else -> {
                processSignal.fire(ProcCpReady())
                val (bench, testClass) = updatedBenchResult.result!!
                val springMachineOptions = JcSpringMachineOptions(
                    analysisMode,
                    request.toSessionConfig(testClass)
                )
                runConcreteAnalysis(springMachineOptions, bench, runnerTimeout)
            }
        }
    }

    private fun AnalysisProcessModel.prepareDbHandler(request: PrepareDbRequest) {
        val userClasses = request.userClassPath.map { File(it) }
        val libsClasses = request.libsClassPath.map { File(it) }

        val benchResult = ResultWithTime.calculate {
            loadBenchDatabase(
                request.classpathSource,
                userClasses,
                libsClasses
            )
        }.chain { db ->
            loadBenchClasspath(
                db,
                request.classpathSource,
                userClasses + libsClasses + concreteApiFile(),
                userClasses,
                libsClasses
            )
        }

        when {
            benchResult.error != null -> {
                processSignal.fire(benchResult.error.toProcError("db load error"))
            }

            else -> {
                cpSource = request.classpathSource
                rawBenchCp = benchResult.result!!
                processSignal.fire(ProcDbReady(benchResult.elapsedTime.inWholeSeconds.toInt()))
            }
        }
    }

    private fun runConcreteAnalysis(springOptions: JcSpringMachineOptions, updatedBench: BenchCp, runnerTimeout: Duration) {
        val reproducer = SpringTestReproducer(updatedBench.concreteMachineOptions, updatedBench.cp)

        val observer = JcSpringTestRdObserver(springOptions, analysisModel, reproducer)

        updatedBench.use { bench ->
            analyzeBenchMock(bench.cp, observer)
//            analyzeBench(
//                bench,
//                analysisMode,
//                runnerTimeout,
//                JcSpringConfigProvider.bootApp,
//                observer
//            )
        }
    }

    private fun analyzeBenchMock(cp: JcClasspath, observer: JcSpringTestRdObserver) {
        val gtcName = System.getProperty("generatedTestClass")
        val gtc = cp.findClass(gtcName)
        val ctl = cp.findClass("io.aiven.klaw.controller.ResourceClientController")
        val handler = ctl.declaredMethods.first { it.name == "getFoos" }
        try {
            val builder = SpringTestExecBuilder.initTestCtx(cp, gtc).addPerformCall(UTestStringExpression("/resources", cp.stringType))
            repeat(10) {
                if (it == 5) throw IllegalStateException()
                val uTest = UTest(
                    builder.getInitDSL(),
                    builder.getExecDSL()
                )
                val testInfo = JcSpringMvcTestInfo(handler, false, null, "io.aiven.klaw.controller", "ResourceClientControllerTest", "${handler.name}Test$it")
                observer.mockOnStateTerminated(uTest, testInfo)
                runBlocking { delay(3000L) }
            }
        }
        catch (e: Throwable) {
            analysisModel.processSignal.fire(e.toProcError("irl pider"))
        }
    }

    private fun AnalysisRequest.toSessionConfig(testClass: JcClassOrInterface): JcSpringAnalysisSessionConfig =
        when {
            analysisController != null -> JcSpringControllerAnalysisConfig(analysisController, testClass)
            analysisHandle != null -> JcSpringHandlerAnalysisConfig(analysisHandle, testClass)
            analysisPath.isNotEmpty() -> JcSpringPathAnalysisConfig(analysisPath.toSet(), testClass)
            else -> error("bad analysis request provided")
        }
}
