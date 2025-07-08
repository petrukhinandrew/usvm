package org.usvm.jvm.spring.runner

import bench.analyzeBench
import bench.generateTestClass
import bench.loadBenchCp
import bench.loadBenchCpFromJar
import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.reactive.IMutableViewableList
import com.jetbrains.rd.util.reactive.IScheduler
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import java.io.File
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import machine.JcSpringAnalysisMode
import machine.JcSpringConfigProvider
import mu.KLogging
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.usvm.instrumentation.generated.models.syncProtocolModel
import org.usvm.instrumentation.rd.CHILD_PROCESS_NAME
import org.usvm.instrumentation.rd.MAIN_PROCESS_NAME
import org.usvm.instrumentation.rd.adviseForConditionAsync
import org.usvm.instrumentation.rd.pumpAsync
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisRequest
import org.usvm.jmv.spring.models.analysisProcessModel
import org.usvm.jvm.spring.models.JcSpringTestRdObserver

val logger = object : KLogging() {}.logger

class AnalysisProcess private constructor() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val proc = AnalysisProcess()
            proc.start(args)
        }
    }

    fun start(args: Array<String>) = runBlocking {
        val opts = Options()
        with(opts) {
            addOption("t", true, "timeout in seconds")
            addOption("p", true, "port")
        }
        val argParser = DefaultParser()
        val cmd = argParser.parse(opts, args)
        val timeout = cmd.getOptionValue("t").toIntOrNull()?.toDuration(DurationUnit.SECONDS)
            ?: 120.toDuration(DurationUnit.SECONDS)
        val port = cmd.getOptionValue("p").toIntOrNull() ?: error("Specify rd port number")
        val def = LifetimeDefinition()

        def.terminateOnException {
            initiate(def, port, timeout)

            def.awaitTermination()
        }
    }

    private enum class State {
        STARTED, ENDED
    }

    private val synchronizer = Channel<State>(capacity = 1)

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

        val model = protocol.scheduler.pumpAsync(lifetime) {
            protocol.syncProtocolModel
            protocol.analysisProcessModel
        }.await()
        model.setup(timeout, lifetime, scheduler)
        protocol.syncProtocolModel.synchronizationSignal.let { sync ->
            val answerFromMainProcess = sync.adviseForConditionAsync(lifetime) {
                if (it == MAIN_PROCESS_NAME) {
                        sync.fire(CHILD_PROCESS_NAME)
                    true
                } else {
                    false
                }
            }
            answerFromMainProcess.await()
        }
    }

    private fun AnalysisProcessModel.setup(runnerTimeout: Duration, lifetime: Lifetime, scheduler: IScheduler) {
        val observer = JcSpringTestRdObserver(

            onNewTest = { render ->
                newTestGenerated.fire(render)
                generatedTests.add(render)
            },
            onError = { descr -> errorOccured.fire(descr) }
        )

        runAnalysis.advise(lifetime) { request ->
            runConcreteAnalysis(observer, request, runnerTimeout)
//            println("proc: signal received")
//            runAnalysisMock(request, generatedTests)
        }
    }

    private fun runConcreteAnalysis(observer: JcSpringTestRdObserver, request: AnalysisRequest, runnerTimeout: Duration) {
        bindRequest(request)
        val springAnalysisMode = JcSpringAnalysisMode.SpringBootTest
        println("running concrete analysis")

        val benchCp = logTime("Init jacodb") {
            loadBenchCpFromJar(request.userClassPath.first { it.endsWith(".jar")}, request.libsClassPath)
        }

        val newBench = generateTestClass(benchCp, springAnalysisMode, request.analysisBootApp)

        newBench.use { bench ->
            println("running analysis")
            analyzeBench(
                bench,
                springAnalysisMode,
                runnerTimeout,
                JcSpringConfigProvider.bootApp,
                observer
            )
        }
    }

    private fun runAnalysisMock(request: AnalysisRequest, tests: IMutableViewableList<String>) {
        try {
            repeat(10) {
                val msg = "$it'th test for requested ${request.testClassName}"
                println(msg)
                tests.add(msg)
                runBlocking { delay(3000L) }
            }
        }
        catch (e: Throwable) {
            println("ti pidor ${e.message ?: "irl"}\n ${e.stackTraceToString()}")
        }
    }

    private fun bindRequest(request: AnalysisRequest) {
        JcSpringConfigProvider.reset()
        request.analysisController?.let { JcSpringConfigProvider.analyzeController(it) }
        request.analysisHandle?.let { JcSpringConfigProvider.analyzeHandler(it) }
        request.analysisPath.forEach { JcSpringConfigProvider.addAnalyzePath(it) }
        JcSpringConfigProvider.analyzeBootApp(request.analysisBootApp)
    }

    @Suppress("TooGenericExceptionCaught")
    inline fun <T> LifetimeDefinition.terminateOnException(block: (Lifetime) -> T): T {
        try {
            return block(this)
        } catch (e: Throwable) {
            terminate()
            println((e.message ?: "") + "terminateOnException thrown")
            println(e.stackTraceToString())
            throw e
        }
    }

    suspend fun Lifetime.awaitTermination() {
        val deferred = CompletableDeferred<Unit>()
        onTermination { deferred.complete(Unit) }
        deferred.await()
    }


    private inline fun <T> measureExecutionForTermination(block: () -> T): T {
        try {
            synchronizer.trySendBlocking(State.STARTED).exceptionOrNull()
            return block()
        } finally {
            synchronizer.trySendBlocking(State.ENDED).exceptionOrNull()
        }
    }

    private fun <T, R> RdCall<T, R>.measureExecutionForTermination(block: (T) -> R) {
        set { request ->
            try {
                measureExecutionForTermination<R> {
                    block(request)
                }
            } finally {

            }
        }
    }

    private fun <T> logTime(message: String, body: () -> T): T {
        val result: T
        val time = measureNanoTime {
            result = body()
        }
        logger.info { "Time: $message | ${time.nanoseconds}" }
        return result
    }
}
