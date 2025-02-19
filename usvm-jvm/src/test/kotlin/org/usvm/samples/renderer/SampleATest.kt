package org.usvm.samples.renderer

import org.junit.jupiter.api.Test
import org.usvm.samples.JavaMethodTestRunner
import org.usvm.samples.algorithms.BinarySearch
import org.usvm.test.util.checkers.ignoreNumberOfAnalysisResults
import org.usvm.util.JcTestResolverType

class SampleATest : JavaMethodTestRunner() {

    override val resolverType get() = JcTestResolverType.CONCRETE_EXECUTOR
//    @Test
//    fun nuf() {
//        checkDiscoveredPropertiesWithExceptions(
//            SampleA::genericUsage,
//            ignoreNumberOfAnalysisResults
//
//        )
//    }

    @Test
    fun kk() {
        checkDiscoveredPropertiesWithExceptions(
            SampleA::SomeMethod,
            ignoreNumberOfAnalysisResults
        )
    }
}