package org.usvm.jvm.spring.loader

import features.JcClinitFeature
import features.JcEncodingFeature
import features.JcGeneratedTypesFeature
import features.JcInitFeature
import java.io.File
import machine.JcBuildDirsConcreteMachineOptionsImpl
import machine.JcJarConcreteMachineOptions
import machine.interpreter.transformers.springjpa.JcDataclassTransformer
import machine.interpreter.transformers.springjpa.JcRepositoryCrudTransformer
import machine.interpreter.transformers.springjpa.JcRepositoryQueryTransformer
import machine.interpreter.transformers.springjpa.JcRepositoryTransformer
import org.jacodb.api.jvm.JcDatabase
import org.jacodb.approximation.Approximations
import org.jacodb.impl.JcRamErsSettings
import org.jacodb.impl.features.InMemoryHierarchy
import org.jacodb.impl.features.Usages
import org.jacodb.impl.features.classpaths.UnknownClasses
import org.jacodb.impl.jacodb
import org.usvm.jmv.spring.models.ClasspathSource
import org.usvm.jvm.spring.BenchCp
import org.usvm.machine.interpreter.transformers.JcStringConcatTransformer
import org.usvm.test.api.spring.JcSpringTestKind
import org.usvm.util.classpathWithApproximations
import util.database.JcTableInfoCollector


suspend fun loadBenchDatabase(cpSource: ClasspathSource, classes: List<File>, dependencies: List<File>): JcDatabase {
    if (cpSource == ClasspathSource.JAR) {
        val bootJar = classes.single()
        check(bootJar.exists()) {
            "no boot jar file"
        }

        check(bootJar.extension == "jar") {
            "not a jar found"
        }
    }

    val cpFiles = classes + dependencies + concreteApiFile()

    val db = jacodb {
        useProcessJavaRuntime()

        persistenceImpl(JcRamErsSettings)

        installFeatures(InMemoryHierarchy, Usages, Approximations)

        loadByteCode(cpFiles)
    }

    db.awaitBackgroundJobs()

    return db
}

fun concreteApiFile(): File {
    val usvmConcreteApiJar = File(System.getenv("usvm.jvm.concrete.api.jar.path"))
    check(usvmConcreteApiJar.exists()) { "Concrete API jar does not exist" }
    return usvmConcreteApiJar
}

suspend fun loadBenchClasspath(
    db: JcDatabase,
    cpSource: ClasspathSource,
    allCpFiles: List<File>,
    classes: List<File>,
    dependencies: List<File>,
    isPureClasspath: Boolean = true,
    tablesInfo: JcTableInfoCollector? = null,
    testKind: JcSpringTestKind? = null
): BenchCp {
    val features = mutableListOf(
        UnknownClasses,
        JcStringConcatTransformer,
        JcClinitFeature,
        JcInitFeature,
        JcEncodingFeature,
        JcGeneratedTypesFeature
    )

//    if (!isPureClasspath) {
//        val dbFeatures = listOf(
//            JcRepositoryCrudTransformer,
//            JcRepositoryQueryTransformer,
//            JcRepositoryTransformer,
//            JcDataclassTransformer(tablesInfo!!)
//        )
//        features.addAll(dbFeatures)
//    }

    val cp = db.classpathWithApproximations(allCpFiles, features)

    return BenchCp(
        cp,
        db,
        when (cpSource) {
            ClasspathSource.JAR -> {
                JcJarConcreteMachineOptions(classes.single())
            }

            ClasspathSource.BUILD_DIRS -> {
                val classLocations = cp.locations.filter { it.jarOrFolder in classes }
                val depsLocations = cp.locations.filter { it.jarOrFolder in dependencies }

                JcBuildDirsConcreteMachineOptionsImpl(classLocations, depsLocations)
            }
        },
        allCpFiles,
        classes,
        dependencies,
        testKind
    )
}