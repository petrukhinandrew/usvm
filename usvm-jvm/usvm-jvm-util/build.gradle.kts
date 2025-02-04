plugins {
    id("usvm.kotlin-conventions")
}

group = "org.usvm"
version = "unspecified"


dependencies {
    implementation(Libs.jacodb_api_jvm) {
        // Unused dependencies
        exclude("javax.xml.bind", "jaxb-api")
        exclude("org.reactivestreams", "reactive-streams")
    }

    implementation(Libs.jacodb_core) {
        // Added above with exclusions
        exclude(Libs.jacodb_api_jvm)

        // Sqlite related dependencies. Unused because we use RAM persistence
        exclude("com.zaxxer", "HikariCP")
        exclude("org.xerial", "sqlite-jdbc")
    }
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}