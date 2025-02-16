package org.usvm.org.usvm.rendering

class JcImportManager(importList: List<String>) {
    constructor() : this(listOf())


    fun tryAdd(fullName: String, simpleName: String) {
        simpleToFull.putIfAbsent(simpleName, fullName)
    }

    fun tryAdd(fullName: String) = tryAdd(fullName, simpleNameFor(fullName))
    private fun simpleNameFor(v: String) = v.split(".").last()
    private val simpleToFull: MutableMap<String, String> = importList.associateTo(mutableMapOf()) {
        it.split(".").last() to it
    }

    fun fullToSimple() = simpleToFull.entries.associate { e -> e.value to e.key }
}
