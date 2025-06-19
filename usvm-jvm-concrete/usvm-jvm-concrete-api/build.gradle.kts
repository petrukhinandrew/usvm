plugins {
    java
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

repositories {
    mavenLocal()
}

dependencies {
    compileOnly(Libs.jacodb_api_jvm)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:all")
        options.compilerArgs.add("-Xlint:-options")
        options.compilerArgs.add("-Werror")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.usvm"
            artifactId = "usvm-jvm-concrete-api"
            version = "1.2.10"
            from(components["java"])
        }
    }
}
