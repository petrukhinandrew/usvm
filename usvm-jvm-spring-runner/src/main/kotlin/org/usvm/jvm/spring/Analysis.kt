package org.usvm.jvm.spring

import kotlin.time.Duration
import machine.JcSpringAnalysisMode
import machine.JcSpringMachine
import machine.JcSpringMachineOptions
import machine.JcSpringTestObserver
import org.jacodb.api.jvm.ext.toType
import org.usvm.CoverageZone
import org.usvm.PathSelectionStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.logger
import org.usvm.machine.JcMachineOptions
import testGeneration.SpringTestInfo


fun analyzeBench(
    bench: BenchCp,
    springAnalysisMode: JcSpringAnalysisMode,
    runnerTimeout: Duration,
    springBootApp: String? = null,
    testObserver: JcSpringTestObserver = JcSpringTestObserver()
): List<SpringTestInfo> {
    val cp = bench.cp

    val jcSpringMachineOptions = JcSpringMachineOptions(
        springAnalysisMode = springAnalysisMode
    )

    val options = UMachineOptions(
        useSoftConstraints = false,
        pathSelectionStrategies = listOf(PathSelectionStrategy.BFS),
        coverageZone = CoverageZone.METHOD,
        exceptionsPropagation = false,
        timeout = runnerTimeout,
        solverType = SolverType.YICES,
        loopIterationLimit = 2,
        solverTimeout = Duration.INFINITE, // we do not need the timeout for a solver in tests
        typeOperationsTimeout = Duration.INFINITE, // we do not need the timeout for type operations in tests
    )
    val jcMachineOptions = JcMachineOptions(
        forkOnImplicitExceptions = true,
        arrayMaxSize = 10_000,
    )

    val machine = JcSpringMachine(
        cp,
        options,
        jcMachineOptions,
        bench.concreteMachineOptions,
        jcSpringMachineOptions,
        testObserver
    )

    val nonAbstractClasses = bench.nonAbstractUserClasses()
    val startClass = nonAbstractClasses.find { it.simpleName == "NewStartSpring" }?.toType() ?: error("NewStartSpring not found")
    val method = startClass.declaredMethods.find { it.name == "startSpring" } ?: error("no startSpring method found")

    try {
        machine.analyze(method.method)
    } catch (e: Throwable) {
        logger.error(e) { "Machine failed" }
    }

    return testObserver.generatedTests
}
