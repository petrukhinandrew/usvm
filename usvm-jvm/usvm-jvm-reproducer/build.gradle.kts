import org.gradle.tooling.model.java.JavaRuntime

plugins {
    id("usvm.kotlin-conventions")
}

group = "org.usvm"
version = "unspecified"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":usvm-core"))
    implementation(project(":usvm-jvm"))
    implementation(project(":usvm-jvm-instrumentation"))
    implementation(project(":usvm-util"))
    implementation(project(":usvm-jvm:usvm-jvm-test-api"))
    implementation(project(":usvm-jvm:usvm-jvm-util"))
    implementation(Libs.jacodb_api_jvm)
    implementation(Libs.jacodb_core)
    implementation(Libs.jacodb_approximations)
    implementation(Libs.slf4j_simple)
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.3")

    testImplementation(kotlin("test"))
}
tasks.register<JavaExec>("runRenderer") {
    mainClass.set("org.usvm.jvm.rendering.JcTestRenderRunner")
    classpath = sourceSets.main.get().runtimeClasspath
    val instrumentationTask = project(":usvm-jvm-instrumentation").tasks.getByName("instrumentationJar")
    val collectorTask = project(":usvm-jvm-instrumentation").tasks.getByName("collectorsJar")
    println(instrumentationTask.outputs.files.single())
    println(collectorTask.outputs.files.single())
    environment(
        "usvm-jvm-instrumentation-jar",
        instrumentationTask.outputs.files.single()
    )
    environment(
        "usvm-jvm-collectors-jar",
        collectorTask.outputs.files.single()
    )
}
tasks.test {
    useJUnitPlatform()
}


