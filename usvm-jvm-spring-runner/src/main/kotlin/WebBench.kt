package bench

import BenchCp
import SpringTestRenderer
import SpringTestReproducer
import createOrClear
import generateTestClass
import loadWebAppBenchCp
import logTime
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import machine.JcConcreteMachineOptions
import machine.JcSpringTestGenerationMode
import machine.JcSpringAnalysisMode
import machine.JcSpringMachine
import machine.JcSpringMachineOptions
import machine.JcSpringTestObserver
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.ext.toType
import org.usvm.CoverageZone
import org.usvm.PathSelectionStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.jvm.rendering.spring.webMvcTestRenderer.JcSpringMvcTestInfo
import org.usvm.jvm.rendering.testRenderer.JcTestInfo
import org.usvm.jvm.util.nonAbstractClasses
import org.usvm.logger
import org.usvm.machine.JcMachineOptions
import org.usvm.test.api.UTest
import testGeneration.SpringTestInfo
import util.SpringApproximationPaths
import java.io.PrintStream
import machine.JcBuildDirsConcreteMachineOptionsImpl
import machine.JcSpringAnyAnalysisConfig

private fun loadBenchFromEnv(): BenchCp {
    val benchDir = Path(System.getenv("usvm.benchmark"))
    return loadWebAppBenchCp(benchDir / "classes", benchDir / "lib")
}

fun main() {
    val benchCp = logTime("Init jacodb") {
        loadBenchFromEnv()
    }

    logTime("Analysis ALL") {
        benchCp.use { runWebBench(it) }
//        benchCp.use { analyzeBench(it, 2.minutes) }
    }

    exitProcess(0)
}

private fun runWebBench(benchmark: BenchCp) {
    // using file instead of console
    val fileStream = PrintStream(System.getenv("usvm.log") ?: "springLog.ansi")
    System.setOut(fileStream)

    val options = UMachineOptions(
        useSoftConstraints = false,
        pathSelectionStrategies = listOf(PathSelectionStrategy.BFS),
        coverageZone = CoverageZone.METHOD,
        exceptionsPropagation = true,
        timeout = 2.minutes,
        solverType = SolverType.YICES,
        loopIterationLimit = 2,
        solverTimeout = Duration.INFINITE, // we do not need the timeout for a solver in tests
        typeOperationsTimeout = Duration.INFINITE, // we do not need the timeout for type operations in tests
    )

    analyzeBench(benchmark, options)
}

fun analyzeBench(benchmark: BenchCp, options: UMachineOptions, runnerTimeout: Duration = Duration.INFINITE, springBootApp: String? = null, testObserver: JcSpringTestObserver = JcSpringTestObserver()): List<SpringTestInfo> {
    val springAnalysisMode = JcSpringTestGenerationMode.SpringBootTest
    val springApproximationPaths = SpringApproximationPaths()
    val newBench = generateTestClass(benchmark, springAnalysisMode, springApproximationPaths)

    val jcConcreteMachineOptions = JcBuildDirsConcreteMachineOptionsImpl(
        projectLocations = newBench.classLocations,
        dependenciesLocations = newBench.depsLocations,
    )
    newBench.bindMachineOptions(jcConcreteMachineOptions)

    val jcSpringMachineOptions = JcSpringMachineOptions(
        springTestGenerationMode = springAnalysisMode,
        springApproximationPaths = springApproximationPaths,
        sessionConfig = JcSpringAnyAnalysisConfig()
    )

    val cp = newBench.cp
    val nonAbstractClasses = cp.nonAbstractClasses(newBench.classLocations)
    val startClass = nonAbstractClasses.find { it.simpleName == "NewStartSpring" }!!.toType()
    val method = startClass.declaredMethods.find { it.name == "startSpring" }!!

    val jcMachineOptions = JcMachineOptions(
        forkOnImplicitExceptions = true,
        arrayMaxSize = 10_000,
    )

    val machine = JcSpringMachine(
        cp,
        options,
        jcMachineOptions,
        jcConcreteMachineOptions,
        jcSpringMachineOptions,
        testObserver
    )

    try {
        machine.analyze(method.method)
    } catch (e: Throwable) {
        logger.error(e) { "Machine failed" }
    }

    reproduceTests(testObserver.generatedTests, jcConcreteMachineOptions, cp)

    return testObserver.generatedTests
}

fun SpringTestInfo.toRenderInfo(): Pair<UTest, JcSpringMvcTestInfo> {
    return this.test to JcSpringMvcTestInfo(this.method, this.isExceptional)
}

fun renderTests(testRenderer: SpringTestRenderer, tests: List<Pair<UTest, JcTestInfo>>, dir: File) {
    val rendered = testRenderer.render(tests)
    for ((testClassInfo, result) in rendered) {
        val testFile = dir.resolve("${testClassInfo.testClassName}.java")
        testFile.writeText(result)
    }
}

fun reproduceTests(
    tests: List<SpringTestInfo>,
    jcConcreteMachineOptions: JcConcreteMachineOptions,
    cp: JcClasspath
) {
    val testReproducer by lazy { SpringTestReproducer(jcConcreteMachineOptions, cp) }
    val testRenderer by lazy { SpringTestRenderer(cp) }

    val reproducedTests = mutableListOf<Pair<UTest, JcSpringMvcTestInfo>>()
    val notReproducedTests = mutableListOf<Pair<UTest, JcSpringMvcTestInfo>>()
    for (testInfo in tests) {
        val reproduced = testReproducer.reproduce(testInfo.test)
        if (reproduced)
            reproducedTests.add(testInfo.toRenderInfo())
        else notReproducedTests.add(testInfo.toRenderInfo())
    }
    testReproducer.kill()

    val currentDir = File(System.getProperty("user.dir"))
    val generatedTestsDir = currentDir.resolve("generatedTests")
    createOrClear(generatedTestsDir)
    val reproducedDir = generatedTestsDir.resolve("reproduced")
    createOrClear(reproducedDir)
    val notReproducedDir = generatedTestsDir.resolve("notReproduced")
    createOrClear(notReproducedDir)

    renderTests(testRenderer, reproducedTests, reproducedDir)
    renderTests(testRenderer, notReproducedTests, notReproducedDir)

    println("Reproduced ${reproducedTests.size} of ${tests.size} tests")
}
