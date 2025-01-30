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
//    implementation(task("usvm-api-jar").outputs.files.single())
    // TODO ask valya
    implementation("org.usvm:usvm-jvm-api:unspecified")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}