package org.usvm.instrumentation.rd

import org.jacodb.api.jvm.JcClasspath
import org.usvm.instrumentation.classloader.WorkerClassLoader
import org.usvm.instrumentation.collector.trace.MockCollector
import org.usvm.instrumentation.collector.trace.TraceCollector
import org.usvm.instrumentation.testcase.api.UTestExecutionResult
import org.usvm.instrumentation.util.URLClassPathLoader
import org.usvm.test.api.UTest

abstract class UTestExecutor(
    protected val jcClasspath: JcClasspath,
    protected val ucp: URLClassPathLoader
) {
    abstract fun executeUTest(uTest: UTest): UTestExecutionResult
    protected fun createWorkerClassLoader() =
        WorkerClassLoader(
            urlClassPath = ucp,
            traceCollectorClassLoader = this::class.java.classLoader,
            traceCollectorClassName = TraceCollector::class.java.name,
            mockCollectorClassName = MockCollector::class.java.name,
            jcClasspath = jcClasspath
        )
}