package org.usvm.instrumentation.rd

import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.JcField
import org.jacodb.api.jvm.ext.findClass
import org.jacodb.api.jvm.ext.toType
import org.usvm.instrumentation.collector.trace.MockCollector
import org.usvm.instrumentation.instrumentation.JcInstructionTracer
import org.usvm.instrumentation.mock.MockHelper
import org.usvm.instrumentation.testcase.api.*
import org.usvm.instrumentation.testcase.descriptor.StaticDescriptorsBuilder
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.testcase.descriptor.Value2DescriptorConverter
import org.usvm.instrumentation.testcase.executor.UTestExpressionExecutor
import org.usvm.instrumentation.util.InstrumentationModuleConstants
import org.usvm.instrumentation.util.URLClassPathLoader
import org.usvm.test.api.UTest

class UTestExecutorCollectingResultOnly(jcClasspath: JcClasspath, ucp: URLClassPathLoader) :
    UTestExecutor(jcClasspath, ucp) {

    private var workerClassLoader = createWorkerClassLoader()
    private var initStateDescriptorBuilder = Value2DescriptorConverter(
        workerClassLoader = workerClassLoader,
        previousState = null
    )
    private var staticDescriptorsBuilder = StaticDescriptorsBuilder(
        workerClassLoader = workerClassLoader,
        initialValue2DescriptorConverter = initStateDescriptorBuilder
    )
    private var mockHelper = MockHelper(
        jcClasspath = jcClasspath,
        classLoader = workerClassLoader
    )
    private val emptyExecState: UTestExecutionState = UTestExecutionState(null, emptyList(), mutableMapOf())

    init {
        workerClassLoader.setStaticDescriptorsBuilder(staticDescriptorsBuilder)
    }

    private fun reset() {
        initStateDescriptorBuilder = Value2DescriptorConverter(
            workerClassLoader = workerClassLoader,
            previousState = null
        )
        staticDescriptorsBuilder.setClassLoader(workerClassLoader)
        staticDescriptorsBuilder.setInitialValue2DescriptorConverter(initStateDescriptorBuilder)
        //In case of new worker classloader
        workerClassLoader.setStaticDescriptorsBuilder(staticDescriptorsBuilder)
        JcInstructionTracer.reset()
        MockCollector.mocks.clear()
    }

    override fun executeUTest(uTest: UTest): UTestExecutionResult {
        when (InstrumentationModuleConstants.testExecutorStaticsRollbackStrategy) {
            StaticsRollbackStrategy.HARD -> workerClassLoader = createWorkerClassLoader()
            else -> {}
        }
        reset()
        val accessedStatics = mutableSetOf<Pair<JcField, JcInstructionTracer.StaticFieldAccessType>>()
        val callMethodExpr = uTest.callMethodExpression

        val executor = UTestExpressionExecutor(workerClassLoader, accessedStatics, mockHelper)
        val initStmts = (uTest.initStatements + listOf(callMethodExpr.instance) + callMethodExpr.args).filterNotNull()
        executor.executeUTestInsts(initStmts)
            ?.onFailure {
                return UTestExecutionInitFailedResult(
                    cause = buildExceptionDescriptor(
                        builder = initStateDescriptorBuilder,
                        exception = it,
                        raisedByUserCode = false
                    ),
                    trace = JcInstructionTracer.getTrace().trace
                )
            }

        accessedStatics.addAll(JcInstructionTracer.getTrace().statics.toSet())

        val methodInvocationResult =
            executor.executeUTestInst(callMethodExpr)
        val resultStateDescriptorBuilder =
            Value2DescriptorConverter(workerClassLoader, initStateDescriptorBuilder)
        val unpackedInvocationResult =
            when {
                methodInvocationResult.isFailure -> methodInvocationResult.exceptionOrNull()
                else -> methodInvocationResult.getOrNull()
            }

        val trace = JcInstructionTracer.getTrace()
        accessedStatics.addAll(trace.statics.toSet())

        if (unpackedInvocationResult is Throwable) {
            return UTestExecutionExceptionResult(
                cause = buildExceptionDescriptor(
                    builder = resultStateDescriptorBuilder,
                    exception = unpackedInvocationResult,
                    raisedByUserCode = methodInvocationResult.isSuccess
                ),
                trace = JcInstructionTracer.getTrace().trace,
                initialState = emptyExecState,
                resultState = emptyExecState
            )
        }

        val methodInvocationResultDescriptor =
            resultStateDescriptorBuilder.buildDescriptorResultFromAny(unpackedInvocationResult, callMethodExpr.type)
                .getOrNull()

        if (InstrumentationModuleConstants.testExecutorStaticsRollbackStrategy == StaticsRollbackStrategy.ROLLBACK) {
            staticDescriptorsBuilder.rollBackStatics()
        } else if (InstrumentationModuleConstants.testExecutorStaticsRollbackStrategy == StaticsRollbackStrategy.REINIT) {
            val accessedStaticsFields = accessedStatics.map { it.first }
            workerClassLoader.reset(accessedStaticsFields)
        }


        return UTestExecutionSuccessResult(
            trace.trace, methodInvocationResultDescriptor, emptyExecState, emptyExecState
        )
    }

    private fun buildExceptionDescriptor(
        builder: Value2DescriptorConverter,
        exception: Throwable,
        raisedByUserCode: Boolean
    ): UTestExceptionDescriptor {
        val descriptor =
            builder.buildDescriptorResultFromAny(any = exception, type = null).getOrNull() as? UTestExceptionDescriptor
        return descriptor
            ?.also { it.raisedByUserCode = raisedByUserCode }
            ?: UTestExceptionDescriptor(
                type = jcClasspath.findClassOrNull(exception::class.java.name)?.toType()
                    ?: jcClasspath.findClass<Exception>().toType(),
                message = exception.message ?: "message_is_null",
                stackTrace = listOf(),
                raisedByUserCode = raisedByUserCode
            )
    }
}