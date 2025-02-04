package org.usvm.samples.renderer

import org.junit.jupiter.api.Test
import org.usvm.samples.JavaMethodTestRunner
import org.usvm.test.util.checkers.ignoreNumberOfAnalysisResults
import org.usvm.util.JcTestResolverType
import org.usvm.util.isException

class SimpleMethodTest : JavaMethodTestRunner() {

    override val resolverType: JcTestResolverType get() = JcTestResolverType.CONCRETE_EXECUTOR
    @Test
    fun simpleTest() {
        checkDiscoveredPropertiesWithExceptions(
            SimpleMethod::simpleLol,
            ignoreNumberOfAnalysisResults,
            { instance, x, r -> instance.yExplicitGet() > 0 && r.getOrNull() == instance.yExplicitGet() * x + 1 },
            { instance, _, r -> instance.yExplicitGet() < 0 && r.getOrNull() == 1 },
            { instance, _, r -> instance.yExplicitGet() == 0 && r.isException<ArithmeticException>() }
        )
    }

    @Test
    fun throwsIllegalStateTest() {
        checkDiscoveredPropertiesWithExceptions(
            SimpleMethod::throwsIllegalState,
            ignoreNumberOfAnalysisResults,
            {_, r -> r.exceptionOrNull() is IllegalStateException }
        )
    }
    @Test
    fun const10Test() {
        checkDiscoveredPropertiesWithExceptions(
            SimpleMethod::const10,
            ignoreNumberOfAnalysisResults,
            {_, r -> r.getOrNull() == 10}
        )
    }

}