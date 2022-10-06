import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "me.valer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter { url = uri("https://jcenter.bintray.com/") }
    google()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("khttp:khttp:0.1.0")
    implementation("com.google.code.gson:gson:2.8.6")

}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}