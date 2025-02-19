package org.usvm.jvm.rendering

import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.JcDatabase
import org.jacodb.api.jvm.JcSettings
import org.jacodb.api.jvm.ext.findClass
import org.jacodb.api.jvm.ext.toType
import org.jacodb.approximation.Approximations
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.jacodb
import org.usvm.UMachineOptions
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.executor.UTestExecutionOptions
import org.usvm.instrumentation.instrumentation.JcRuntimeTraceInstrumenterFactory
import org.usvm.instrumentation.rd.InstrumentedProcess
import org.usvm.instrumentation.testcase.api.UTestExecutionExceptionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionInitFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.util.InstrumentationModuleConstants
import org.usvm.jvm.reproducer.loadBenchClassesOnly
import org.usvm.machine.JcMachine
import org.usvm.machine.JcMachineOptions
import org.usvm.machine.interpreter.transformers.JcMultiDimArrayAllocationTransformer
import org.usvm.machine.interpreter.transformers.JcStringConcatTransformer
import org.usvm.test.api.UTest
import org.usvm.util.classpathWithApproximations

private class JacoDBContainer(
    key: Any?,
    classpath: List<File>,
    builder: JcSettings.() -> Unit,
) {
    val db: JcDatabase
    val cp: JcClasspath

    init {
        val (db, cp) = runBlocking {
            val db = jacodb {
                builder()

                if (samplesWithApproximationsKey == key) {
                    installFeatures(Approximations)
                }

                loadByteCode(classpath)
            }

            val features = listOf(
                JcMultiDimArrayAllocationTransformer,
                JcStringConcatTransformer,
            )

            val cp = if (samplesWithApproximationsKey == key) {
                db.classpathWithApproximations(classpath, features)
            } else {
                db.classpath(classpath, features)
            }
            db to cp
        }
        this.db = db
        this.cp = cp
        runBlocking {
            db.awaitBackgroundJobs()
        }
    }

    companion object {
        private val keyToJacoDBContainer = HashMap<Any?, JacoDBContainer>()

        operator fun invoke(
            key: Any?,
            classpath: List<File>,
            builder: JcSettings.() -> Unit = defaultBuilder,
        ): JacoDBContainer =
            keyToJacoDBContainer.getOrPut(key) { JacoDBContainer(key, classpath, builder) }

        private val defaultBuilder: JcSettings.() -> Unit = {
            useProcessJavaRuntime()
            installFeatures(InMemoryHierarchy)
        }
    }
}

const val samplesKey = "tests"
const val samplesWithApproximationsKey = "samplesWithApproximations"
fun loadClasspathFromEnv(envKey: String): List<File> {
    val classpath = System.getProperty(envKey) ?: error("Environment $envKey required")
    return parseClasspath(classpath)
}

fun parseClasspath(classpath: String): List<File> =
    classpath
        .split(File.pathSeparatorChar)
        .map { File(it) }

open class JcTestRenderRunner {
    companion object EntryPoint {

        protected val jacodbCpKey: String
            get() = "rndr"

        private val classpath: List<File>
            get() = listOf(File("/Users/petrukhinandrew/IdeaProjects/sandbox/build/classes/java/main"))

        protected val cp by lazy {
            JacoDBContainer(jacodbCpKey, classpath).cp
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val bench =
                loadBenchClassesOnly(
                    listOf(
                        File("/Users/petrukhinandrew/IdeaProjects/sandbox/build/classes/java/main"),
                        File("/Users/petrukhinandrew/IdeaProjects/sandbox/build/classes/kotlin/main"),
                        File("/Users/petrukhinandrew/IdeaProjects/sandbox/build/classes/resources/main")
                    )
                )

            val cp = bench.cp
            val className = "a.b.c.SampleA"
            val methodName = "genericUsage"
            val method = cp.findClass(className)
                .toType().declaredMethods.first { it.name == methodName }
            val utests = JcMachine(cp, UMachineOptions(), JcMachineOptions()).use { machine ->
                val states = machine.analyze(method.method)
                val utests = states.map {
                    UTest.fromSnapshot(method, it)
                }
                utests
            }

            val utestsChecked = checkByExecution(
                utests,
                cp.locations.map { it.path }, /*JacoDBContainer(jacodbCpKey, loadClasspathFromEnv("java.class.path")).*/
                cp
            )
            println("created ${utests.size} / reproduced ${utestsChecked.size}")
            val utestsWrapped = utestsChecked.map {
                UTestRenderWrapper(
                    it, JcSpringTestMeta(
                        "/Users/petrukhinandrew/IdeaProjects/sandbox/src/test/java",
                        method.method, JcSpringTestKind.None
                    )
                )
            }
            val manager = JcSpringTestRenderManager()
            manager.render(cp, utestsWrapped)

        }


        fun checkByExecution(testPool: List<UTest>, locs: List<String>, cp: JcClasspath): List<UTest> {
            val passing = mutableListOf<UTest>()
            val exec = UTestConcreteExecutor(
                JcRuntimeTraceInstrumenterFactory::class,
                locs.joinToString(File.pathSeparator),
                cp,
                InstrumentationModuleConstants.testExecutionTimeout,
                UTestExecutionOptions(execMode = InstrumentedProcess.UTestExecMode.RESULT_ONLY)
            )
            runBlocking {
                exec.ensureRunnerAlive()

                testPool.forEach { t ->
                    launch {
                        val execRes = exec.executeAsync(t)
                        when (execRes) {
                            is UTestExecutionSuccessResult, is UTestExecutionExceptionResult -> passing.add(t)
                            is UTestExecutionInitFailedResult -> {
                                println(execRes.cause.raisedByUserCode)
                                println(execRes.trace?.joinToString(";"))
                            }

                            is UTestExecutionFailedResult ->
                                println(execRes.cause)

                            else -> println(execRes::class)
                        }
                    }
                }
            }
            exec.close()
            return passing

        }
    }
}