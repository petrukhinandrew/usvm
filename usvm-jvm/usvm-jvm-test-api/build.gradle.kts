plugins {
    id("usvm.kotlin-conventions")
}

repositories {
    mavenLocal()
}

dependencies {
    implementation(Libs.jacodb_api_jvm)
    implementation(project(":usvm-core"))
    implementation(project(":usvm-jvm"))
    implementation("org.usvm:usvm-jvm-api:unspecified")


}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}