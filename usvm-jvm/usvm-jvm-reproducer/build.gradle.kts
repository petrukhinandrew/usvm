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
tasks.test {
    useJUnitPlatform()
}


