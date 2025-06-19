plugins {
    java
    `java-library`
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(Libs.jacodb_api_jvm)
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
