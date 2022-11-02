import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "me.valer"
version = "1.0-SNAPSHOT"
val kfgVersion = "0.3.5"

repositories {
    mavenCentral()
    maven("https://maven.vorpal-research.science")
    maven{ url = uri("https://jcenter.bintray.com/") }
    google()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.20")
    implementation("khttp:khttp:0.1.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.vorpal.research:kfg:$kfgVersion")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}