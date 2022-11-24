import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    kotlin("jvm") version "1.7.20"

}

group = "me.valer"
version = "1.0-SNAPSHOT"
val kfgVersion = "0.3.5"
val toolingApiVersion = "7.5.1"

repositories {
    mavenCentral()
    maven("https://maven.vorpal-research.science")
    maven { url = uri("https://jcenter.bintray.com/") }
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    google()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.20")
    implementation("khttp:khttp:0.1.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.vorpal.research:kfg:$kfgVersion")
    implementation("org.gradle:gradle-tooling-api:$toolingApiVersion")
    implementation("org.apache.maven.shared:maven-verifier:2.0.0-M1")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.4")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}