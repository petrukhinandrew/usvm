package org.usvm.instrumentation.executor

data class UTestExecutorOptions(
    val mode: UTestExecutorMode = UTestExecutorMode.STATE,
    val instrumentedClassNames: List<String> = emptyList()
)

enum class UTestExecutorMode(val id: String) {
    RESULT("result"), STATE("state")
}