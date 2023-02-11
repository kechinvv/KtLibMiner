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
    maven{ url = uri("https://oss.sonatype.org/content/repositories/snapshots")}
    google()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.7.20")
    implementation("khttp:khttp:0.1.0")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.vorpal.research:kfg:$kfgVersion")
    implementation("org.gradle:gradle-tooling-api:$toolingApiVersion")
    implementation("org.apache.maven.shared:maven-verifier:2.0.0-M1")
    implementation("org.soot-oss:soot:4.4.0-SNAPSHOT")
    implementation("de.upb.cs.swt:heros:1.2.3")
    implementation("org.graphstream:gs-core:1.3")
    implementation("org.graphstream:gs-ui:1.3")
    // implementation("in.ac.iitb.cse:vasco:1.1-SNAPSHOT")



//    implementation("com.ibm.wala:com.ibm.wala.shrike:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.util:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.core:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.js:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.js.rhino:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.js:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.js.rhino:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.java:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.cast.java.ecj:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.dalvik:${walaVersion}")
//    implementation("com.ibm.wala:com.ibm.wala.scandroid:${walaVersion}")
    testImplementation(kotlin("test"))

    runtimeOnly("org.slf4j:slf4j-simple:2.0.4")
}


tasks.test {
    //minHeapSize = "512m"
    //maxHeapSize = "1024m"
    jvmArgs = listOf("-Xss512m")
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}