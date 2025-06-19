package org.usvm.jvm.spring

import java.io.File
import machine.JcConcreteMachineOptions
import machine.interpreter.transformers.springjpa.JcRepositoryTransformer
import org.jacodb.api.jvm.JcClassOrInterface
import org.jacodb.api.jvm.JcClasspath
import org.jacodb.api.jvm.JcDatabase
import org.usvm.test.api.spring.JcSpringTestKind

class BenchCp(
    val cp: JcClasspath,
    val db: JcDatabase,
    val concreteMachineOptions: JcConcreteMachineOptions,
    val cpFiles: List<File>,
    val classes: List<File>,
    val dependencies: List<File>,
    val testKind: JcSpringTestKind?
) : AutoCloseable {
    override fun close() {
        cp.close()
        db.close()
    }

    init {
        bindMachineOptions()
    }

    private fun bindMachineOptions() {
        (cp.features?.find { it is JcRepositoryTransformer } as? JcRepositoryTransformer)?.bindMachineOptions(concreteMachineOptions)
    }

    fun nonAbstractUserClasses(): Sequence<JcClassOrInterface> {
        return concreteMachineOptions.userClassesIn(cp).filterNot { it.isAbstract || it.isInterface || it.isAnonymous }
    }
}
