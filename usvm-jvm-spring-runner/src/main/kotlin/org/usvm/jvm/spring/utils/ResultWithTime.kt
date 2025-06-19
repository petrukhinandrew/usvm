package org.usvm.jvm.spring.utils

import kotlin.time.Duration
import kotlin.time.measureTime
import kotlinx.coroutines.runBlocking

data class ResultWithTime<T>(val result: T?, val elapsedTime: Duration, val error: Throwable?) {
    companion object {
        fun <T> calculate(block: suspend () -> T): ResultWithTime<T> {
            var res: T? = null
            var err: Throwable? = null
            val elapsed = measureTime {
                try {
                    res = runBlocking { block() }
                } catch (e: Throwable) {
                    err = e
                }
            }
            return ResultWithTime(res, elapsed, err)
        }
    }

    fun <R> chain(block: suspend (T) -> R): ResultWithTime<R> {
        if (error != null) return ResultWithTime(null, elapsedTime, error)
        var res: R? = null
        var err: Throwable? = null
        val elapsed = measureTime {
            try {
                res = runBlocking { block(result!!) }
            } catch (e: Throwable) {
                err = e
            }
        }
        return ResultWithTime(res, elapsed + elapsedTime, err)
    }
}
