package org.usvm.jvm.spring

import kotlin.time.Duration
import machine.JcSpringAnalysisMode
import machine.JcSpringMachine
import machine.JcSpringMachineOptions
import machine.JcSpringTestObserver
import org.jacodb.api.jvm.JcMethod
import org.jacodb.api.jvm.ext.toType
import org.usvm.CoverageZone
import org.usvm.PathSelectionStrategy
import org.usvm.SolverType
import org.usvm.UMachineOptions
import org.usvm.jmv.spring.models.AnalysisProcessModel
import org.usvm.jmv.spring.models.ProcCtxReady
import org.usvm.logger
import org.usvm.machine.JcInterpreterObserver
import org.usvm.machine.JcMachineOptions
import org.usvm.machine.JcMethodCall
import org.usvm.machine.JcMethodCallBaseInst
import org.usvm.machine.JcMethodEntrypointInst
import org.usvm.machine.interpreter.JcExprResolver
import org.usvm.machine.interpreter.JcSimpleValueResolver
import org.usvm.machine.interpreter.JcStepScope
import testGeneration.SpringTestInfo


fun analyzeBench(
    bench: BenchCp,
    analysisProcessModel: AnalysisProcessModel,
    springOptions: JcSpringMachineOptions,
    springAnalysisMode: JcSpringAnalysisMode,
    runnerTimeout: Duration,
    springBootApp: String? = null,
    testObserver: JcSpringTestObserver = JcSpringTestObserver()
): List<SpringTestInfo> {
    val cp = bench.cp

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
        springOptions,
        testObserver,
        SpringTestContextPreparationObserver(analysisProcessModel)
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

class SpringTestContextPreparationObserver(private val analysisProcessModel: AnalysisProcessModel): JcInterpreterObserver {
    override fun onEntryPoint(
        simpleValueResolver: JcSimpleValueResolver,
        stmt: JcMethodEntrypointInst,
        stepScope: JcStepScope
    ) { }

    private var ctxPrepareStartTimestamp: Long = 0L

    override fun onMethodCallWithResolvedArguments(
        simpleValueResolver: JcSimpleValueResolver,
        stmt: JcMethodCallBaseInst,
        stepScope: JcStepScope
    ) {
        val method = stmt.method

        when {
            method.isPrepareTestInstance() -> {
                ctxPrepareStartTimestamp = System.currentTimeMillis()
                println("preparing test instance")
            }

            method.isPerformerPerform() -> {
                val elapsedMillis = System.currentTimeMillis() - ctxPrepareStartTimestamp
                analysisProcessModel.processSignal.fire(ProcCtxReady(elapsedMillis / 1000))
            }
        }
    }

    private fun JcMethod.isPrepareTestInstance(): Boolean {
        return enclosingClass.name == "org.springframework.test.context.TestContextManager" && name == "prepareTestInstance" && parameters.size == 1
    }

    private fun JcMethod.isPerformerPerform(): Boolean {
        return enclosingClass.name == "v3xx.generated.org.springframework.boot.SpringMvcPerformer" && name == "perform" && parameters.size == 1
    }
}
