package org.usvm.jvm.spring.runner

import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisRequest
import org.usvm.jmv.spring.models.PrepareDbRequest
import org.usvm.jmv.spring.models.ProcAnalysisFinished
import org.usvm.jmv.spring.models.ProcCpReady
import org.usvm.jmv.spring.models.ProcDbReady
import org.usvm.jmv.spring.models.ProcError

class AnalysisProcessMockController {

    val throwOnPrepareDb: Boolean get() = Random.nextDouble() > 0.9

    fun prepareDbHandlerMock(model: AnalysisProcessModel, request: PrepareDbRequest) = runBlocking {
        commonDelay()
        with(model.processSignal) {
            if (throwOnPrepareDb) {
                fire(ProcError("Error on bench database load", listOf()))
            } else {
                fire(ProcDbReady(2))
            }
        }
    }

    val throwOnCpCreation: Boolean get() = Random.nextDouble() > 0.8

    val throwOnAnalysisRun: Boolean get() = Random.nextDouble() > 0.7

    fun runAnalysisHandlerMock(model: AnalysisProcessModel, request: AnalysisRequest, runnerTimeout: Duration) = runBlocking {
        commonDelay()
        with(model.processSignal) {
            if (throwOnCpCreation) {
                fire(ProcError("Error on classpath update", listOf()))
            } else {
                fire(ProcCpReady())
            }
        }

        if (throwOnAnalysisRun) {
            model.processSignal.fire(ProcError("Error on analysis run", listOf()))
            return@runBlocking
        }

        (1..5).forEach {
            testGenDelay()
            model.generatedTests.add(testCase(request, it))
        }

        model.processSignal.fire(ProcAnalysisFinished())
    }

    private suspend fun commonDelay(): Unit = delay(1.seconds)

    private suspend fun testGenDelay(): Unit = delay(Random.nextInt(2,5).seconds)

    private fun testCase(request: AnalysisRequest, index: Int): String = """
        package ${request.testClassPackage};
        import org.junit.jupiter.api.Test;
        import org.junit.jupiter.api.extension.ExtendWith;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
        import org.springframework.boot.test.context.SpringBootTest;
        import org.springframework.test.context.junit.jupiter.SpringExtension;
        import org.springframework.test.web.servlet.MockMvc;

        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

        @SpringBootTest
        @AutoConfigureMockMvc
        @ExtendWith(SpringExtension.class)
        class ${request.testClassName} {
            @Autowired
            private MockMvc mockMvc;

            @Test
            void testCase$index() throws Exception {
                mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
            }
        }
    """.trimIndent()
}