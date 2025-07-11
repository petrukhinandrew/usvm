package org.usvm.jvm.spring.runner

import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.util.NetUtils
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.lifetime.isAlive
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.usvm.instrumentation.executor.RdProcessRunnerBase
import org.usvm.instrumentation.util.InstrumentationModuleConstants
import org.usvm.instrumentation.util.UTestExecutorInitException
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.AnalysisRequest
import org.usvm.jmv.spring.models.ErrorDescriptor
import org.usvm.jmv.spring.models.analysisProcessModel

// TODO: get coroutine context as a parameter?
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

    fun startAnalysis(request: AnalysisRequest) {
        println("FIIIIIIRRRRREEEEE")
        model.runAnalysis.fire(request)
    }

    fun bindOnNewTest(callback: (String) -> Unit) {
        model.newTestGenerated.advise(lifetime, callback)
    }

    fun bindOnError(callback: (ErrorDescriptor) -> Unit) {
        model.errorOccured.advise(lifetime, callback)
    }
}

@Suppress("unused")
class AnalysisProcessRunner: AutoCloseable {

    companion object {
        private const val localAgentPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-concrete/agent/build/libs/agent.jar"
        private const val localRunnerPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-spring-runner/build/libs/usvm-jvm-spring-runner-1.2.10.jar"
    }

    lateinit var rdProcessRunner: AnalysisRdProcessRunner

    val lifetime = LifetimeDefinition()

    val model: AnalysisProcessModel get() = rdProcessRunner.model

    override fun close() {
        lifetime.terminate()
    }

    suspend fun start(timeoutSeconds: Int, javaPath: String, allowDebugging: Boolean, onNewTest: (String) -> Unit, onError: (ErrorDescriptor) -> Unit): Process {
        val port = NetUtils.findFreePort(0)
        val process = runJar(
            javaPath,
            localRunnerPath,
            buildJvmArgs("org.usvm.jvm.spring.runner.AnalysisProcess", allowDebugging = allowDebugging) + listOf("-t", timeoutSeconds.toString(), "-p", port.toString())
        ) ?: error("cannot run jar")
        rdProcessRunner =
            AnalysisRdProcessRunner(process = process, checkProcessAliveDelay = 1.seconds, rdPort = port, lifetimeDefinition = lifetime)
        rdProcessRunner.init()
        rdProcessRunner.bindOnError(onError)
        rdProcessRunner.bindOnNewTest(onNewTest)

        return process
    }

    suspend fun ensureRunnerAlive() {
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
        return rdProcessRunner.startAnalysis(request)
    }

    private val workingDir = Files.createTempDirectory("springAnalysis").toFile()

    private val localLambdaDir get() = workingDir.resolve("lambda").createOrClear()

    private val localSpringDir get() = workingDir.resolve("spring").createOrClear()

    init {
        workingDir.deleteOnExit()
    }

    private val addOpens: List<String> get() {
        val javaBasePackages = listOf(
            "jdk.internal.misc",
            "java.lang",
            "java.lang.reflect",
            "sun.security.provider",
            "jdk.internal.event",
            "jdk.internal.jimage",
            "jdk.internal.jimage.decompressor",
            "jdk.internal.jmod",
            "jdk.internal.jtrfs",
            "jdk.internal.loader",
            "jdk.internal.logger",
            "jdk.internal.math",
            "jdk.internal.misc",
            "jdk.internal.module",
            "jdk.internal.org.objectweb.asm.commons",
            "jdk.internal.org.objectweb.asm.signature",
            "jdk.internal.org.objectweb.asm.tree",
            "jdk.internal.org.objectweb.asm.tree.analysis",
            "jdk.internal.org.objectweb.asm.util",
            "jdk.internal.org.xml.sax",
            "jdk.internal.org.xml.sax.helpers",
            "jdk.internal.perf",
            "jdk.internal.platform",
            "jdk.internal.ref",
            "jdk.internal.reflect",
            "jdk.internal.util",
            "jdk.internal.util.jar",
            "jdk.internal.util.xml",
            "jdk.internal.util.xml.impl",
            "jdk.internal.vm",
            "jdk.internal.vm.annotation",
            "java.util.concurrent.atomic",
            "java.io",
            "java.util.zip",
            "java.util.concurrent",
            "sun.security.util",
            "java.lang.invoke",
            "java.lang.ref",
            "java.lang.constant",
            "java.util",
            "java.util.concurrent.locks",
            "java.nio.charset",
            "java.util.regex",
            "java.net",
            "sun.util.locale",
            "java.util.stream",
            "java.security",
            "java.time",
            "jdk.internal.access",
            "sun.reflect.annotation",
            "sun.reflect.generics.reflectiveObjects",
            "sun.reflect.generics.factory",
            "sun.reflect.generics.tree",
            "sun.reflect.generics.scope",
            "sun.invoke.util",
            "sun.nio.cs",
            "sun.nio.fs",
            "java.nio",
            "java.time.format",
            "java.time.zone",
            "java.time.temporal",
            "java.text",
            "sun.util.calendar",
            "sun.net.www.protocol.jar",
            "java.util.jar",
            "java.nio.file.attribute",
            "java.util.function",
            "java.math",
            "java.nio.file",
            "java.nio.channels",
            "javax.net.ssl",
            "java.lang.annotation",
            "java.lang.runtime",
            "javax.crypto",
            "java.nio.file.spi",
            "jdk.internal.jrtfs",
            "sun.nio.ch",
            "sun.net.util",
        )

        val javaBaseAddOpens = javaBasePackages.flatMap {
            openPackageEntry("java.base", it)
        }

        val misc = listOf(
            openPackageEntry("java.management", "javax.management"),
            openPackageEntry("java.logging", "java.util.logging"),
            openPackageEntry("java.desktop", "java.beans"),
            openPackageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl.xs"),
            openPackageEntry("jdk.zipfs", "jdk.nio.zipfs"),
            openPackageEntry("java.instrument", "sun.instrument"),
            openPackageEntry("java.xml", "com.sun.xml.internal.stream"),
            openPackageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl"),
            openPackageEntry("java.xml", "com.sun.org.apache.xerces.internal.utils"),
            openPackageEntry("java.sql", "java.sql"),
        ).flatten()

        return javaBaseAddOpens + misc
    }

    private val addExports: List<String> get() {
        return listOf(
            exportPackageEntry("java.base", "jdk.internal.access.foreign"),
            exportPackageEntry("java.base", "sun.security.action"),
            exportPackageEntry("java.base", "sun.util.locale"),
            exportPackageEntry("java.base", "jdk.internal.misc"),
            exportPackageEntry("java.base", "jdk.internal.reflect"),
            exportPackageEntry("java.base", "sun.nio.cs"),
            exportPackageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl.xs.util"),
            exportPackageEntry("java.base", "jdk.internal.loader")
        ).flatten()
    }

    private fun openPackageEntry(module: String, pkg: String): List<String> =
        listOf("--add-opens", "$module/$pkg=ALL-UNNAMED")

    fun exportPackageEntry(module: String, pkg: String): List<String> =
        listOf("--add-exports", "$module/$pkg=ALL-UNNAMED")

    private fun buildJvmArgs(
        mainClazz: String,
        agentPath: String = localAgentPath,
        lambdaDirPath: String = localLambdaDir.absolutePath,
        allowDebugging: Boolean = false
    ): List<String> {
        return listOf(
            "-Xmx12g",
            "-Djava.security.manager",
            "-Djava.security.policy=/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-spring-runner/webExplorationPolicy.policy",
            "-Djdk.util.jar.enableMultiRelease=false",
            "-Djdk.util.jar.enableMultiRelease=false",
            "-javaagent:$agentPath",
            "-Djdk.internal.lambda.dumpProxyClasses=${lambdaDirPath}",
            "--illegal-access=warn",
            "-XX:+UseParallelGC"
        ) +
                (if (allowDebugging)
                    listOf(
                        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:${NetUtils.findFreePort(0)}"
                    )
                else
                    emptyList<String>()) +
                            addOpens +
                            addExports +
                            mainClazz
    }

    @Suppress("SameParameterValue")
    private fun runJar(javaPath: String, jarPath: String, arguments: List<String>): Process? {
        val collectorsJarPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-instrumentation/build/libs/usvm-jvm-instrumentation-collectors.jar"
        val instrumentationJarPath =
            "/Users/petrukhinandrew/IdeaProjects/usvm-renderilka/usvm-jvm-instrumentation/build/libs/usvm-jvm-instrumentation-runner.jar"
        val usvmJvmApi =
            "/Users/petrukhinandrew/.m2/repository/org/usvm/usvm-jvm-api/1.2.10/usvm-jvm-api-1.2.10.jar"
        val approximations =
            "/Users/petrukhinandrew/.m2/repository/org/usvm/approximations/java/stdlib/approximations/0.0.0/approximations-0.0.0.jar"
        val concreteApi =
            "/Users/petrukhinandrew/.m2/repository/org/usvm/usvm-jvm-concrete-api/1.2.10/usvm-jvm-concrete-api-1.2.10.jar"
        try {
            val startCommand = listOf(javaPath, "-cp", jarPath) + arguments
            val procBuilder = ProcessBuilder(startCommand).inheritIO()
            with(procBuilder.environment()) {
                put("springDir", localSpringDir.absolutePath)
                put("lambdaDir", localLambdaDir.absolutePath)
                put("usvm.jvm.api.jar.path", usvmJvmApi)
                put("usvm.jvm.approximations.jar.path", approximations)
                put("usvm-jvm-instrumentation-jar", instrumentationJarPath)
                put("usvm-jvm-collectors-jar", collectorsJarPath)
                put("usvm.jvm.concrete.api.jar.path", concreteApi)
            }
            return procBuilder.start()
        } catch (e: Exception) {
            println("DBG fail ${e.message}")
        }
        println("DBG exiting runJar")
        return null
    }

    fun File.createOrClear(): File = apply {
        if (exists()) {
            listFiles()?.forEach { it.deleteRecursively() }
        } else {
            mkdirs()
        }
    }
}