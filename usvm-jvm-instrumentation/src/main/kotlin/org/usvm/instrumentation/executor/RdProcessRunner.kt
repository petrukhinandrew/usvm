package org.usvm.instrumentation.executor

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.cfg.JcInst
import org.usvm.instrumentation.generated.models.*
import org.usvm.jvm.util.findFieldByFullNameOrNull
import org.usvm.instrumentation.serializer.SerializationContext
import org.usvm.instrumentation.serializer.UTestInstSerializer.Companion.registerUTestInstSerializer
import org.usvm.instrumentation.serializer.UTestValueDescriptorSerializer.Companion.registerUTestValueDescriptorSerializer
import org.usvm.test.api.UTest
import org.usvm.instrumentation.testcase.api.*
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RdProcessRunner(
    process: Process,
    checkProcessAliveDelay: Duration = 1.seconds,
    rdPort: Int,
    private val jcClasspath: JcClasspath,
    lifetime: LifetimeDefinition
): RdProcessRunnerBase("usvm-executor", process, checkProcessAliveDelay, rdPort, lifetime) {

    private val serializationContext = SerializationContext(jcClasspath)

    private val traceDeserializer = TraceDeserializer(jcClasspath)

    private val model get() = rdProcess.model as InstrumentedProcessModel

    override fun initModels(protocol: Protocol): RdExtBase {
        super.initModels(protocol)
        return protocol.instrumentedProcessModel
    }

    override fun initSerializers(): Serializers {
        val serializers = Serializers()
        serializers.registerUTestInstSerializer(serializationContext)
        serializers.registerUTestValueDescriptorSerializer(serializationContext)
        return serializers
    }

    fun callUTestSync(uTest: UTest, timeout: Duration): UTestExecutionResult = try {
        val serializedUTest = SerializedUTest(uTest.initStatements, uTest.callMethodExpression)
        val serializedExecutionResult = model.callUTest.executeSync(serializedUTest, timeout)
        deserializeExecutionResult(serializedExecutionResult)
    } finally {
        serializationContext.reset()
    }

    suspend fun callUTestAsync(uTest: UTest): UTestExecutionResult =
        try {
            val serializedUTest = SerializedUTest(uTest.initStatements, uTest.callMethodExpression)
            val serializedExecutionResult = model.callUTest.execute(serializedUTest)
            deserializeExecutionResult(serializedExecutionResult)
        } finally {
            serializationContext.reset()
        }

    private fun deserializeExecutionResult(executionResult: ExecutionResult): UTestExecutionResult {
        val coveredClasses = executionResult.classes ?: listOf()
        return when (executionResult.type) {
            ExecutionResultType.UTestExecutionInitFailedResult -> UTestExecutionInitFailedResult(
                cause = executionResult.cause as? UTestExceptionDescriptor ?: error("deserialization failed"),
                trace = executionResult.trace?.let { deserializeTrace(it, coveredClasses) }
            )

            ExecutionResultType.UTestExecutionSuccessResult -> UTestExecutionSuccessResult(
                trace = executionResult.trace?.let { deserializeTrace(it, coveredClasses) },
                result = executionResult.result,
                initialState = executionResult.initialState?.let { deserializeExecutionState(it) }
                    ?: error("deserialization failed"),
                resultState = executionResult.resultState?.let { deserializeExecutionState(it) }
                    ?: error("deserialization failed"),
            )

            ExecutionResultType.UTestExecutionExceptionResult -> UTestExecutionExceptionResult(
                cause = executionResult.cause as? UTestExceptionDescriptor ?: error("deserialization failed"),
                trace = executionResult.trace?.let {
                    deserializeTrace(it, coveredClasses)
                },
                initialState = executionResult.initialState?.let { deserializeExecutionState(it) }
                    ?: error("deserialization failed"),
                resultState = executionResult.resultState?.let { deserializeExecutionState(it) }
                    ?: error("deserialization failed"),
            )

            ExecutionResultType.UTestExecutionFailedResult -> UTestExecutionFailedResult(
                cause = executionResult.cause as? UTestExceptionDescriptor ?: error("deserialization failed")
            )

            ExecutionResultType.UTestExecutionTimedOutResult -> UTestExecutionTimedOutResult(
                cause = executionResult.cause as? UTestExceptionDescriptor ?: error("deserialization failed")
            )
        }
    }

    private fun deserializeExecutionState(state: ExecutionStateSerialized): UTestExecutionState {
        val statics = state.statics?.associate {
            val jcField = jcClasspath.findFieldByFullNameOrNull(it.fieldName) ?: error("deserialization failed")
            val jcFieldDescriptor = it.fieldDescriptor
            jcField to jcFieldDescriptor
        } ?: mapOf()
        return UTestExecutionState(state.instanceDescriptor, state.argsDescriptors, statics.toMutableMap())
    }

    private fun deserializeTrace(trace: List<Long>, coveredClasses: List<ClassToId>): List<JcInst> =
        traceDeserializer.deserializeTrace(trace, coveredClasses)
}
