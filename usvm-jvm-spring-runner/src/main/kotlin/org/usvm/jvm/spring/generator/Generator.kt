package org.usvm.jvm.spring.generator

import java.io.File
import kotlinx.coroutines.runBlocking
import machine.JcSpringAnalysisMode
import machine.JcSpringConfigProvider
import org.jacodb.api.jvm.cfg.JcRawClassConstant
import org.jacodb.api.jvm.cfg.JcRawReturnInst
import org.jacodb.api.jvm.ext.findClass
import org.jacodb.api.jvm.ext.jvmName
import org.jacodb.api.jvm.ext.packageName
import org.jacodb.impl.cfg.JcInstListImpl
import org.jacodb.impl.cfg.MethodNodeBuilder
import org.jacodb.impl.features.hierarchyExt
import org.jacodb.impl.types.TypeNameImpl
import org.objectweb.asm.Type
import org.usvm.jmv.spring.models.ClasspathSource
import org.usvm.jvm.spring.BenchCp
import org.usvm.jvm.spring.loader.loadBenchClasspath
import org.usvm.jvm.spring.utils.allByAnnotation
import org.usvm.jvm.spring.utils.jvmDescriptor
import org.usvm.jvm.spring.utils.replaceTypeInClassNode
import org.usvm.jvm.util.isSameSignature
import org.usvm.jvm.util.replace
import org.usvm.jvm.util.write
import org.usvm.test.api.spring.JcSpringTestKind
import org.usvm.test.api.spring.SpringBootTest


@Suppress("SameParameterValue")
fun generateTestClass(benchmark: BenchCp, cpSource: ClasspathSource, springAnalysisMode: JcSpringAnalysisMode, springBootApp: String?): BenchCp {
    val cp = benchmark.cp

    val springDirFile = File(System.getenv("springDir"))
    check(springDirFile.exists()) { "Generated directory ${springDirFile.absolutePath} does not exist" }
    val nonAbstractClasses = benchmark.nonAbstractUserClasses()

    val repositoryType = cp.findClassOrNull("org.springframework.data.repository.Repository") ?: error("cannot find Repository class")
    val repositories = runBlocking { cp.hierarchyExt() }
        .findSubClasses(repositoryType, entireHierarchy = true, includeOwn = false)
        .filter { benchmark.concreteMachineOptions.isUserClass(it) }
        .toList() + allByAnnotation(nonAbstractClasses, "org.springframework.stereotype.Repository")
    val entityManagerType = cp.findClassOrNull("jakarta.persistence.EntityManager")
    val hasJpa = false // repositories.isNotEmpty() || entityManagerType != null && entityManagerType !is JcUnknownClass

    val testClassTemplateName =
        if (hasJpa) "generated.org.springframework.boot.testClasses.SpringBootJpaTestClass"
        else "generated.org.springframework.boot.testClasses.SpringBootTestClass"

    val applicationClass =
        if (springBootApp == null) {
            val applicationClasses = allByAnnotation(
                nonAbstractClasses,
                "org.springframework.boot.autoconfigure.SpringBootApplication"
            ).toList()

            applicationClasses.singleOrNull() ?: error("No entry classes found (with SpringBootApplication annotation)")
        } else {
            cp.findClassOrNull(springBootApp) ?: error("Not SpringBootApplication annotated class found for name $springBootApp")
        }

    val entryPackagePath = applicationClass.packageName.replace('.', '/')
    val testClassName = "NewSpringBootTestClass"
    val newTestClassSlashName = "$entryPackagePath/$testClassName"
    val newTestClassName = newTestClassSlashName.replace('/', '.')

    var testKind: JcSpringTestKind? = null
    val testClassTemplate = cp.findClassOrNull(testClassTemplateName) ?: error("testClassTemplate not found")
    testClassTemplate.withAsmNode { classNode ->
        classNode.name = newTestClassSlashName

        when (springAnalysisMode) {
            JcSpringAnalysisMode.SpringBootTest -> {
                testKind = SpringBootTest(applicationClass)
                val sprintBootTestAnnotation = classNode.visibleAnnotations.find {
                    it.desc == "org.springframework.boot.test.context.SpringBootTest".jvmName()
                } ?: error("SpringBootTest annotation not found")
                sprintBootTestAnnotation.values = listOf(
                    "classes", listOf(Type.getType(applicationClass.jvmDescriptor))
                )
            }
            JcSpringAnalysisMode.SpringJpaTest -> TODO("not supported yet")
        }

        replaceTypeInClassNode(classNode, testClassTemplateName, newTestClassName)
        classNode.write(cp, springDirFile.resolve("$newTestClassSlashName.class").toPath(), checkClass = true)
    }

    System.setProperty("generatedTestClass", newTestClassName)

    val tablesInfo = null //DatabaseGenerator(cp, springDirFile, repositories)
//        .generateJPADatabase(springAnalysisMode == JcSpringAnalysisMode.SpringBootTest)

    val startSpringTemplateName = "generated.org.springframework.boot.StartSpring"
    val newStartSpringName = "NewStartSpring"
    val startSpringClass = cp.findClassOrNull(startSpringTemplateName) ?: error("cannot find StartSpring class")
    startSpringClass.withAsmNode { startSpringAsmNode ->
        val chooseTestClassMethod = startSpringClass.declaredMethods.find { it.name == "chooseTestClass" } ?: error("chooseTestClass method does not exist")
        chooseTestClassMethod.withAsmNode { chooseTestClassMethodAsmNode ->
            val classConstant = JcRawClassConstant(
                TypeNameImpl.fromTypeName(newTestClassName),
                TypeNameImpl.fromTypeName("java.lang.Class")
            )
            val returnStmt = JcRawReturnInst(chooseTestClassMethod, classConstant)
            val newNode = MethodNodeBuilder(
                chooseTestClassMethod,
                JcInstListImpl(listOf(returnStmt))
            ).build()
            val asmMethods = startSpringAsmNode.methods
            val asmMethod = asmMethods.find { chooseTestClassMethodAsmNode.isSameSignature(it) }
            check(asmMethods.replace(asmMethod, newNode))
        }
        startSpringAsmNode.name = newStartSpringName
        replaceTypeInClassNode(startSpringAsmNode, startSpringTemplateName, newStartSpringName)
        startSpringAsmNode.write(cp, springDirFile.resolve("$newStartSpringName.class").toPath(), checkClass = true)
    }
    runBlocking {
        benchmark.db.load(springDirFile)
        benchmark.db.awaitBackgroundJobs()
    }

    val newCpFiles = benchmark.cpFiles + springDirFile
    val newClasses = benchmark.classes + springDirFile

    val updatedBench = runBlocking {
        loadBenchClasspath(
            benchmark.db,
            cpSource,
            newCpFiles,
            newClasses,
            benchmark.dependencies,
            false,
            tablesInfo,
            testKind,
        )
    }

    JcSpringConfigProvider.bindTestClassStub(updatedBench.cp.findClass(newTestClassName))
    return updatedBench
}
