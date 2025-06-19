package org.usvm.jvm.spring.utils

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import kotlinx.coroutines.CompletableDeferred
import org.usvm.jmv.spring.models.ProcError

@Suppress("TooGenericExceptionCaught")
inline fun <T> terminateOnException(lifetimeDef: LifetimeDefinition, block: (Lifetime) -> T): T {
    try {
        return block(lifetimeDef)
    } catch (e: Throwable) {
        lifetimeDef.terminate()
        println((e.message ?: "") + "terminateOnException thrown")
        println(e.stackTraceToString())
        throw e
    }
}

suspend fun awaitTermination(lifetime: Lifetime) {
    val deferred = CompletableDeferred<Unit>()
    lifetime.onTermination { deferred.complete(Unit) }
    deferred.await()
}

internal fun Throwable.toProcError(msg: String): ProcError {
    return ProcError(message ?: msg, stackTrace.map { it.toString() })
}
