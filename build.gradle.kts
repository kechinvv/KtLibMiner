import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "me.valer"
version = "1.0-SNAPSHOT"
val kfgVersion = "0.3.5"
val toolingApiVersion = "7.5.1"
val walaVersion = "1.5.9"

repositories {
    mavenCentral()
    maven("https://maven.vorpal-research.science")
    maven { url = uri("https://jcenter.bintray.com/") }
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    google()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.20")
    implementation("khttp:khttp:0.1.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.gradle:gradle-tooling-api:$toolingApiVersion")
    implementation("org.apache.maven.shared:maven-verifier:2.0.0-M1")
    implementation("org.soot-oss:soot:4.4.1")

    implementation("org.xerial:sqlite-jdbc:3.41.2.1")

    testImplementation(kotlin("test"))

    runtimeOnly("org.slf4j:slf4j-simple:2.0.5")

    implementation(files("libs/mint-core-1.0.0-jar-with-dependencies.jar"))
    implementation(files("libs/mint-inference-1.2.0-jar-with-dependencies.jar"))
    implementation(files("libs/mint-testgen-1.1.0-jar-with-dependencies.jar"))

    implementation("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0")
    implementation("guru.nidi:graphviz-java-all-j2v8:0.18.1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.5.0.202303070854-r")
}


tasks.test {
    jvmArgs = listOf("-Xss512m")
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}