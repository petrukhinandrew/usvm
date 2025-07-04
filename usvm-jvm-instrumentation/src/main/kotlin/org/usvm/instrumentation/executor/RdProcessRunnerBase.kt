package org.usvm.instrumentation.executor

import com.jetbrains.rd.framework.IRdTask
import com.jetbrains.rd.framework.IdKind
import com.jetbrains.rd.framework.Identities
import com.jetbrains.rd.framework.Protocol
import com.jetbrains.rd.framework.RdTaskResult
import com.jetbrains.rd.framework.Serializers
import com.jetbrains.rd.framework.SocketWire
import com.jetbrains.rd.framework.base.RdExtBase
import com.jetbrains.rd.framework.impl.RdCall
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SingleThreadScheduler
import com.jetbrains.rd.util.threading.SynchronousScheduler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import org.usvm.instrumentation.generated.models.syncProtocolModel
import org.usvm.instrumentation.rd.CHILD_PROCESS_NAME
import org.usvm.instrumentation.rd.MAIN_PROCESS_NAME
import org.usvm.instrumentation.rd.RdServerProcess
import org.usvm.instrumentation.rd.adviseForConditionAsync
import org.usvm.instrumentation.rd.pumpAsync

open class RdProcessRunnerBase(
    protected val protocolName: String,
    protected val process: Process,
    protected val checkProcessAliveDelay: Duration = 1.seconds,
    protected val rdPort: Int,
    protected val lifetime: LifetimeDefinition
) {
    protected val scheduler = SingleThreadScheduler(lifetime, "usvm-executor-scheduler")
    protected val coroutineScope = UsvmRdCoroutineScope(lifetime, scheduler)
    lateinit var rdProcess: RdServerProcess


    init {
        lifetime.onTermination { process.destroyForcibly() }
    }

    suspend fun init() {
        rdProcess = initRdServerProcess()
    }

    protected open fun initModels(protocol: Protocol): RdExtBase {
        return protocol.syncProtocolModel
    }

    protected open fun initSerializers(): Serializers {
        return Serializers()
    }

    protected suspend fun initRdServerProcess(): RdServerProcess {
        val serializers = initSerializers()
        val protocol = Protocol(
            protocolName,
            serializers,
            Identities(IdKind.Server),
            scheduler,
            SocketWire.Server(lifetime, scheduler, rdPort, "usvm-executor-socket"),
            lifetime
        )

        println("VSE ZBS, YA NACHAL")


        protocol.wire.connected.adviseForConditionAsync(lifetime).await()

        coroutineScope.launch(lifetime) {
            while (process.isAlive) {
                delay(checkProcessAliveDelay)
            }
            lifetime.terminate()
        }

        val model = protocol.scheduler.pumpAsync(lifetime) {
            initModels(protocol)
        }.await()


        protocol.syncProtocolModel.synchronizationSignal.let { sync ->
            val messageFromChild = sync.adviseForConditionAsync(lifetime) {
                it == CHILD_PROCESS_NAME
            }

            while (messageFromChild.isActive) {
                sync.fire(MAIN_PROCESS_NAME)
                delay(20.milliseconds)
            }
        }

        println("VSE ZBS, YA GOTOV")

        return RdServerProcess(process, lifetime, protocol, model)
    }

    protected fun <TReq, Tres> RdCall<TReq, Tres>.fastSync(
        lifetime: Lifetime, request: TReq, timeout: Duration
    ): Tres {
        val task = start(lifetime, request, SynchronousScheduler)
        return task.wait(timeout.inWholeMilliseconds).unwrap()
    }

    protected fun <T> IRdTask<T>.wait(timeoutMs: Long): RdTaskResult<T> {
        val future = CompletableFuture<RdTaskResult<T>>()
        result.advise(lifetime) {
            future.complete(it)
        }
        return future.get(timeoutMs, TimeUnit.MILLISECONDS)
    }

    protected suspend fun <T, R> RdCall<T, R>.execute(request: T): R =
        run {
            startSuspending(lifetime, request)
        }

    protected fun <T, R> RdCall<T, R>.executeSync(request: T, timeout: Duration): R =
        run {
            fastSync(lifetime, request, timeout)
        }

    fun destroy() {
        lifetime.terminate()
    }
}