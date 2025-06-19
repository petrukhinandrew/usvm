package org.usvm.jvm.spring.runner

import bench.analyzeBench
import bench.loadBenchCp
import bench.toRenderInfo
import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.util.launch
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import java.io.File
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import machine.JcSpringConfigProvider
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import org.usvm.instrumentation.generated.models.syncProtocolModel
import org.usvm.instrumentation.rd.CHILD_PROCESS_NAME
import org.usvm.instrumentation.rd.MAIN_PROCESS_NAME
import org.usvm.instrumentation.rd.adviseForConditionAsync
import org.usvm.instrumentation.rd.pumpAsync
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisResponse
import org.usvm.jmv.spring.models.analysisProcessModel
import org.usvm.jvm.rendering.JcTestsRenderer
import org.usvm.logger

open class ExecutableProcess private constructor() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val proc = newProcessInstance()

        }
        fun newProcessInstance(): ExecutableProcess {
            return ExecutableProcess()
        }
    }
}

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
            def.launch {
                checkAliveLoop(def, 1.seconds)
            }

            initiate(def, port, timeout)

            def.awaitTermination()
        }
    }

    private enum class State {
        STARTED, ENDED
    }

    private val synchronizer = Channel<State>(capacity = 1)

    private suspend fun checkAliveLoop(lifetime: LifetimeDefinition, timeout: Duration) {
        var lastState = State.ENDED
        while (true) {
            val current = withTimeoutOrNull(timeout) {
                synchronizer.receive()
            }

            if (current == null) {
                if (lastState == State.ENDED) {
                    lifetime.terminate()
                    break
                }
            } else {
                lastState = current
            }
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

        val model = protocol.scheduler.pumpAsync(lifetime) {
            protocol.syncProtocolModel
            protocol.analysisProcessModel
        }.await()

        model.setup(timeout)
        println(timeout)

        protocol.syncProtocolModel.synchronizationSignal.let { sync ->
            val answerFromMainProcess = sync.adviseForConditionAsync(lifetime) {
                if (it == MAIN_PROCESS_NAME) {
                    measureExecutionForTermination {
                        sync.fire(CHILD_PROCESS_NAME)
                    }
                    true
                } else {
                    false
                }
            }
            answerFromMainProcess.await()
        }
    }

    private fun AnalysisProcessModel.setup(runnerTimeout: Duration) {
        runAnalysis.measureExecutionForTermination { request ->

            JcSpringConfigProvider.reset()
            request.analysisController?.let { JcSpringConfigProvider.analyzeController(it) }
            request.analysisHandle?.let { JcSpringConfigProvider.analyzeHandler(it) }
            request.analysisPath.forEach { JcSpringConfigProvider.addAnalyzePath(it) }
            JcSpringConfigProvider.analyzeBootApp(request.analysisBootApp)

            val benchCp = logTime("Init jacodb") {
                loadBenchCp(request.userClassPath.map { File(it) }, request.libsClassPath.map { File(it) })
            }

            println("bench loaded")

            logTime("Analysis ALL") {
                val tests = runCatching {
                    benchCp.use {
                        analyzeBench(it, runnerTimeout, JcSpringConfigProvider.bootApp)
                    }
                }.getOrElse { exception -> println("${exception.message}\n${exception.cause}\n${exception.stackTraceToString()}"); return@getOrElse listOf() }
                println("DBG: tests generated ${tests.size}")
                AnalysisResponse(JcTestsRenderer().renderTests(benchCp.cp, tests.map { it.toRenderInfo() }, true).values.firstOrNull())
            }

//            AnalysisResponse("""
//                package ${request.testClassPackage};
//
//                import jakarta.servlet.ServletException;
//                import org.junit.jupiter.api.Assertions;
//                import org.junit.jupiter.api.Test;
//                import org.junit.jupiter.api.extension.ExtendWith;
//                import org.springframework.beans.factory.annotation.Autowired;
//                import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//                import org.springframework.boot.test.context.SpringBootTest;
//                import org.springframework.samples.petclinic.PetClinicApplication;
//                import org.springframework.test.context.TestContextManager;
//                import org.springframework.test.context.TestPropertySource;
//                import org.springframework.test.context.aot.DisabledInAotMode;
//                import org.springframework.test.context.junit.jupiter.SpringExtension;
//                import org.springframework.test.web.servlet.MockMvc;
//                import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
//
//                import java.lang.reflect.Method;
//
//                @ExtendWith(value = {SpringExtension.class})
//                @SpringBootTest(classes = {PetClinicApplication.class})
//                @AutoConfigureMockMvc
//                @TestPropertySource(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.defer-datasource-initialization=true"})
//                @DisabledInAotMode
//                class ${request.testClassName} {
//
//                	@Test
//                	void initCreationForm() throws NoSuchMethodException, Exception, SecurityException {
//
//                		Assertions.assertThrows(ServletException.class, () -> {
//                			this.mockMvc.perform(MockMvcRequestBuilders.get("/owners/new"));
//                		});
//                	}
//
//                	@Autowired
//                	private MockMvc mockMvc;
//                }
//            """.trimIndent())
        }
    }

    @Suppress("TooGenericExceptionCaught")
    inline fun <T> LifetimeDefinition.terminateOnException(block: (Lifetime) -> T): T {
        try {
            return block(this)
        } catch (e: Throwable) {
            terminate()
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
