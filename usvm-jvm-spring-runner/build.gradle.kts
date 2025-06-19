import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jetbrains.rd.generator.gradle.RdGenExtension
import com.jetbrains.rd.generator.gradle.RdGenTask
import org.gradle.kotlin.dsl.register

plugins {
    id("usvm.kotlin-conventions")
    id(Plugins.Shadow)
    id(Plugins.RdGen)
    kotlin("plugin.serialization") version "2.1.21"
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(project(":usvm-jvm"))
    implementation(project(":usvm-jvm-instrumentation"))
    implementation(project(":usvm-jvm-concrete"))
    implementation(project(":usvm-jvm-spring"))
    implementation(project(":usvm-jvm-spring:usvm-jvm-spring-test-api"))
    implementation(project(":usvm-jvm-rendering"))
    implementation(project(":usvm-core"))

    implementation(project(":usvm-jvm-concrete:usvm-jvm-concrete-api"))
    implementation(project(":usvm-jvm:usvm-jvm-test-api"))
    implementation(project(":usvm-jvm:usvm-jvm-util"))
    implementation(project(":usvm-jvm:usvm-jvm-api"))

    implementation(Libs.rd_framework)
    implementation(Libs.rd_core)
    compileOnly(Libs.rd_gen)

    implementation(Libs.jacodb_api_jvm)
    implementation(Libs.jacodb_core)
    implementation(Libs.jacodb_approximations)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("commons-cli:commons-cli:1.5.0")
    implementation(Libs.logback)
    implementation(Libs.kotlinx_coroutines_core)
}

val sourcesBaseDir = projectDir.resolve("src/main/kotlin")

val generatedPackage = "org.usvm.jvm.spring.models"
val generatedModelsPackage = "org.usvm.jmv.spring.models"
val generatedModelsSourceDir = sourcesBaseDir.resolve(generatedPackage.replace('.', '/'))

val generateModels = tasks.register<RdGenTask>("generateAnalysisProtocolModels") {
//    dependsOn.addAll(listOf("compileKotlin"))
    val rdParams = extensions.getByName("params") as RdGenExtension
    val sourcesDir = projectDir.resolve("src/main/kotlin").resolve("org/usvm/jvm/spring/models")

    group = "rdgen"
    rdParams.verbose = true
    rdParams.sources(sourcesDir)
    rdParams.hashFolder = layout.buildDirectory.file("rdgen/hashes").get().asFile.absolutePath
    // where to search roots
    rdParams.packages = "org.usvm.jvm.spring.models"

    rdParams.generator {
        language = "kotlin"
        transform = "symmetric"
        root = "org.usvm.jvm.spring.models.definitions.AnalysisProcessRoot"

        directory = generatedModelsSourceDir.absolutePath
        namespace = generatedModelsPackage
    }
}


val usvmApiJarConfiguration by configurations.creating
dependencies {
    usvmApiJarConfiguration(project(":usvm-jvm:usvm-jvm-api"))
}

val usvmConcreteApiJarConfiguration by configurations.creating
dependencies {
    usvmConcreteApiJarConfiguration(project(":usvm-jvm-concrete:usvm-jvm-concrete-api"))
}

val approximations by configurations.creating
val approximationsRepo = "org.usvm.approximations.java.stdlib"
val approximationsVersion = "0.0.0"

dependencies {
    approximations(approximationsRepo, "approximations", approximationsVersion)
}

val agentJarConfiguration by configurations.creating
dependencies {
    agentJarConfiguration(project(":usvm-jvm-concrete:agent"))
}

// TODO: make versions flexible (JHipster needs 2.7.3, petclinic needs 3.2.0)
val springVersion = "3.5.0"
val springSecurityVersion = "6.5.0"
val junitVersion = "5.3.1"

//dependencies {
//    implementation("org.springframework.boot:spring-boot-starter-web:$springVersion")
//    implementation("org.springframework.boot:spring-boot-starter-test:$springVersion")
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springVersion")
//    implementation("org.apache.xmlbeans:xmlbeans:5.2.1")
//    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:$springVersion")
//}

val springTestDeps by configurations.creating

dependencies {
    springTestDeps("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    springTestDeps("org.springframework.boot:spring-boot-starter-test:$springVersion")
    springTestDeps("org.springframework.security:spring-security-test:$springSecurityVersion")
}

fun createOrClear(file: File) {
    if (file.exists()) {
        file.listFiles()?.forEach { it.deleteRecursively() }
    } else {
        file.mkdirs()
    }
}

java {
    withSourcesJar()
}

fun buildAddOpens(): List<String> {
    val javaBasePackages = listOf (
        "jdk.internal.misc",
        "java.lang",
        "java.lang.reflect",
        "sun.security.provider",
        "jdk.internal.event",
        "jdk.internal.jimage",
        "jdk.internal.jimage.decompressor",
        "jdk.internal.jmod",
        "jdk.internal.jtrfs",
        "jdk.internal.loader",
        "jdk.internal.logger",
        "jdk.internal.math",
        "jdk.internal.misc",
        "jdk.internal.module",
        "jdk.internal.org.objectweb.asm.commons",
        "jdk.internal.org.objectweb.asm.signature",
        "jdk.internal.org.objectweb.asm.tree",
        "jdk.internal.org.objectweb.asm.tree.analysis",
        "jdk.internal.org.objectweb.asm.util",
        "jdk.internal.org.xml.sax",
        "jdk.internal.org.xml.sax.helpers",
        "jdk.internal.perf",
        "jdk.internal.platform",
        "jdk.internal.ref",
        "jdk.internal.reflect",
        "jdk.internal.util",
        "jdk.internal.util.jar",
        "jdk.internal.util.xml",
        "jdk.internal.util.xml.impl",
        "jdk.internal.vm",
        "jdk.internal.vm.annotation",
        "java.util.concurrent.atomic",
        "java.io",
        "java.util.zip",
        "java.util.concurrent",
        "sun.security.util",
        "java.lang.invoke",
        "java.lang.ref",
        "java.lang.constant",
        "java.util",
        "java.util.concurrent.locks",
        "java.nio.charset",
        "java.util.regex",
        "java.net",
        "sun.util.locale",
        "java.util.stream",
        "java.security",
        "java.time",
        "jdk.internal.access",
        "sun.reflect.annotation",
        "sun.reflect.generics.reflectiveObjects",
        "sun.reflect.generics.factory",
        "sun.reflect.generics.tree",
        "sun.reflect.generics.scope",
        "sun.invoke.util",
        "sun.nio.cs",
        "sun.nio.fs",
        "java.nio",
        "java.time.format",
        "java.time.zone",
        "java.time.temporal",
        "java.text",
        "sun.util.calendar",
        "sun.net.www.protocol.jar",
        "java.util.jar",
        "java.nio.file.attribute",
        "java.util.function",
        "java.math",
        "java.nio.file",
        "java.nio.channels",
        "javax.net.ssl",
        "java.lang.annotation",
        "java.lang.runtime",
        "javax.crypto",
        "java.nio.file.spi",
        "jdk.internal.jrtfs",
        "sun.nio.ch",
        "sun.net.util",
    )

    val javaBaseAddOpens = javaBasePackages.map {
        packageEntry("java.base", it)
    }

    val misc = listOf(
        packageEntry("java.management", "javax.management"),
        packageEntry("java.logging", "java.util.logging"),
        packageEntry("java.desktop", "java.beans"),
        packageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl.xs"),
        packageEntry("jdk.zipfs", "jdk.nio.zipfs"),
        packageEntry("java.instrument", "sun.instrument"),
        packageEntry("java.xml", "com.sun.xml.internal.stream"),
        packageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl"),
        packageEntry("java.xml", "com.sun.org.apache.xerces.internal.utils"),
        packageEntry("java.sql", "java.sql"),
    )


    return javaBaseAddOpens + misc
}

fun buildAddExports(): List<String> {
    return listOf(
        packageEntry("java.base", "jdk.internal.access.foreign"),
        packageEntry("java.base", "sun.security.action"),
        packageEntry("java.base", "sun.util.locale"),
        packageEntry("java.base", "jdk.internal.misc"),
        packageEntry("java.base", "jdk.internal.reflect"),
        packageEntry("java.base", "sun.nio.cs"),
        packageEntry("java.xml", "com.sun.org.apache.xerces.internal.impl.xs.util"),
        packageEntry("java.base", "jdk.internal.loader")
    )
}

fun openPackageEntry(module: String, pkg: String): String = "$module/$pkg=ALL-UNNAMED"

fun packageEntry(module: String, pkg: String): String = "$module/$pkg"

val addOpensPool: List<String> = buildAddOpens()
val addExportsPool: List<String> = buildAddExports()

val springRunnerJar = tasks.register<ShadowJar>("springJar") {
    group = "jar"
    version = "1.2.10"
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
    archiveBaseName.set("usvm-jvm-spring-runner")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "bench.WebBenchKt",
                "Premain-Class" to "org.usvm.jvm.concrete.agent.Agent",
                "Can-Retransform-Classes" to "true",
                "Can-Redefine-Classes" to "true",
//                "Enable-Native-Access" to "ALL-UNNAMED",
//                "Add-Opens" to addOpensPool.joinToString(" "),
//                "Add-Exports" to addExportsPool.joinToString(" ")
            )
        )
    }

    configurations = listOf(project.configurations.runtimeClasspath.get())

    mergeServiceFiles()
    with(tasks.jar.get() as CopySpec)
    println(this.outputs.files.joinToString(" ") { it.absolutePath })
}


tasks.register<JavaExec>("runWebBench") {
    mainClass.set("bench.WebBenchKt")
    classpath = sourceSets.test.get().runtimeClasspath

    systemProperty("jdk.util.jar.enableMultiRelease", false)

    val absolutePaths = springTestDeps.resolvedConfiguration.files.joinToString(";") { it.absolutePath }
    environment("usvm.jvm.springTestDeps.paths", absolutePaths)

    val currentDir = File(System.getProperty("user.dir"))
    val generatedDir = currentDir.resolve("generated")
    createOrClear(generatedDir)

    val lambdaDir = generatedDir.resolve("lambdas")
    print("Lambda dir here " + lambdaDir)
    createOrClear(lambdaDir)
    environment("lambdaDir", lambdaDir.absolutePath)
    val springDir = generatedDir.resolve("spring")
    createOrClear(springDir)
    environment("springDir", springDir.absolutePath)

    val usvmApiJarPath = usvmApiJarConfiguration.resolvedConfiguration.files.single()
    environment("usvm.jvm.api.jar.path", usvmApiJarPath.absolutePath)

    val usvmApproximationJarPath = approximations.resolvedConfiguration.files.single()
    environment("usvm.jvm.approximations.jar.path", usvmApproximationJarPath.absolutePath)

    val usvmConcreteApiJarPath = usvmConcreteApiJarConfiguration.resolvedConfiguration.files.single()
    environment("usvm.jvm.concrete.api.jar.path", usvmConcreteApiJarPath)

    environment(
        "usvm-jvm-instrumentation-jar",
        project(":usvm-jvm-instrumentation")
            .layout
            .buildDirectory
            .file("libs/usvm-jvm-instrumentation-runner.jar")
            .get().asFile.absolutePath
    )

    environment(
        "usvm-jvm-collectors-jar",
        project(":usvm-jvm-instrumentation")
            .layout
            .buildDirectory
            .file("libs/usvm-jvm-instrumentation-collectors.jar")
            .get().asFile.absolutePath
    )

    val agentJarPath = agentJarConfiguration.resolvedConfiguration.files.single()

    jvmArgs = listOf("-Xmx12g") + mutableListOf<String>().apply {
        add("-Djava.security.manager -Djava.security.policy=webExplorationPolicy.policy")
        add("-Djdk.internal.lambda.dumpProxyClasses=${lambdaDir.absolutePath}")
        add("-javaagent:${agentJarPath.absolutePath}")
        openPackage("java.base", "jdk.internal.misc")
        openPackage("java.base", "java.lang")
        openPackage("java.base", "java.lang.reflect")
        openPackage("java.base", "sun.security.provider")
        openPackage("java.base", "jdk.internal.event")
        openPackage("java.base", "jdk.internal.jimage")
        openPackage("java.base", "jdk.internal.jimage.decompressor")
        openPackage("java.base", "jdk.internal.jmod")
        openPackage("java.base", "jdk.internal.jtrfs")
        openPackage("java.base", "jdk.internal.loader")
        openPackage("java.base", "jdk.internal.logger")
        openPackage("java.base", "jdk.internal.math")
        openPackage("java.base", "jdk.internal.misc")
        openPackage("java.base", "jdk.internal.module")
        openPackage("java.base", "jdk.internal.org.objectweb.asm.commons")
        openPackage("java.base", "jdk.internal.org.objectweb.asm.signature")
        openPackage("java.base", "jdk.internal.org.objectweb.asm.tree")
        openPackage("java.base", "jdk.internal.org.objectweb.asm.tree.analysis")
        openPackage("java.base", "jdk.internal.org.objectweb.asm.util")
        openPackage("java.base", "jdk.internal.org.xml.sax")
        openPackage("java.base", "jdk.internal.org.xml.sax.helpers")
        openPackage("java.base", "jdk.internal.perf")
        openPackage("java.base", "jdk.internal.platform")
        openPackage("java.base", "jdk.internal.ref")
        openPackage("java.base", "jdk.internal.reflect")
        openPackage("java.base", "jdk.internal.util")
        openPackage("java.base", "jdk.internal.util.jar")
        openPackage("java.base", "jdk.internal.util.xml")
        openPackage("java.base", "jdk.internal.util.xml.impl")
        openPackage("java.base", "jdk.internal.vm")
        openPackage("java.base", "jdk.internal.vm.annotation")
        openPackage("java.base", "java.util.concurrent.atomic")
        openPackage("java.base", "java.io")
        openPackage("java.base", "java.util.zip")
        openPackage("java.base", "java.util.concurrent")
        openPackage("java.base", "sun.security.util")
        openPackage("java.base", "java.lang.invoke")
        openPackage("java.base", "java.lang.ref")
        openPackage("java.base", "java.lang.constant")
        openPackage("java.base", "java.util")
        openPackage("java.base", "java.util.concurrent.locks")
        openPackage("java.management", "javax.management")
        openPackage("java.base", "java.nio.charset")
        openPackage("java.base", "java.util.regex")
        openPackage("java.base", "java.net")
        openPackage("java.base", "sun.util.locale")
        openPackage("java.base", "java.util.stream")
        openPackage("java.base", "java.security")
        openPackage("java.base", "java.time")
        openPackage("java.base", "jdk.internal.access")
        openPackage("java.base", "sun.reflect.annotation")
        openPackage("java.base", "sun.reflect.generics.reflectiveObjects")
        openPackage("java.base", "sun.reflect.generics.factory")
        openPackage("java.base", "sun.reflect.generics.tree")
        openPackage("java.base", "sun.reflect.generics.scope")
        openPackage("java.base", "sun.invoke.util")
        openPackage("java.base", "sun.nio.cs")
        openPackage("java.base", "sun.nio.fs")
        openPackage("java.base", "java.nio")
        openPackage("java.logging", "java.util.logging")
        openPackage("java.base", "java.time.format")
        openPackage("java.base", "java.time.zone")
        openPackage("java.base", "java.time.temporal")
        openPackage("java.base", "java.text")
        openPackage("java.base", "sun.util.calendar")
        openPackage("java.base", "sun.net.www.protocol.jar")
        openPackage("java.base", "java.util.jar")
        openPackage("java.base", "java.nio.file.attribute")
        openPackage("java.base", "java.util.function")
        openPackage("java.desktop", "java.beans")
        openPackage("java.xml", "com.sun.org.apache.xerces.internal.impl.xs")
        openPackage("java.base", "java.math")
        openPackage("java.base", "java.nio.file")
        openPackage("java.base", "java.nio.channels")
        openPackage("java.base", "javax.net.ssl")
        openPackage("java.base", "java.lang.annotation")
        openPackage("java.base", "java.lang.runtime")
        openPackage("java.base", "javax.crypto")
        openPackage("java.base", "java.nio.file.spi")
        openPackage("java.base", "jdk.internal.jrtfs")
        openPackage("java.base", "sun.nio.ch")
        openPackage("java.base", "sun.net.util")
        openPackage("java.management", "javax.management")
        openPackage("java.logging", "java.util.logging")
        openPackage("java.desktop", "java.beans")
        openPackage("java.xml", "com.sun.org.apache.xerces.internal.impl.xs")
        openPackage("jdk.zipfs", "jdk.nio.zipfs")
        openPackage("java.instrument", "sun.instrument")
        openPackage("java.xml", "com.sun.xml.internal.stream")
        openPackage("java.xml", "com.sun.org.apache.xerces.internal.impl")
        openPackage("java.xml", "com.sun.org.apache.xerces.internal.utils")
        openPackage("java.sql", "java.sql")
        exportPackage("java.base", "jdk.internal.access.foreign")
        exportPackage("java.base", "sun.security.action")
        exportPackage("java.base", "sun.util.locale")
        exportPackage("java.base", "jdk.internal.misc")
        exportPackage("java.base", "jdk.internal.reflect")
        exportPackage("java.base", "sun.nio.cs")
        exportPackage("java.xml", "com.sun.org.apache.xerces.internal.impl.xs.util")
        exportPackage("java.base", "jdk.internal.loader")
        add("--illegal-access=warn")
        add("-XX:+UseParallelGC")
        addModule("jdk.incubator.foreign")
    }
}

fun MutableList<String>.openPackage(module: String, pakage: String) {
    add("--add-opens")
    add("$module/$pakage=ALL-UNNAMED")
}

fun MutableList<String>.exportPackage(module: String, pakage: String) {
    add("--add-exports")
    add("$module/$pakage=ALL-UNNAMED")
}

fun MutableList<String>.addModule(module: String) {
    add("--add-modules")
    add(module)
}

fun JavaExec.addEnvIfExists(envName: String, path: String) {
    val file = File(path)
    if (!file.exists()) {
        println("Not found $envName at $path")
        return
    }

    environment(envName, file.absolutePath)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
