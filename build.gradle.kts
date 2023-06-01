fun properties(key: String) = project.findProperty(key).toString()

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "me.valer"
version = "1.0"

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
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.gradle:gradle-tooling-api:7.5.1")
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

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("org.soot-oss:sootup.core:1.0.0")
    implementation("org.soot-oss:sootup.java.core:1.0.0")
    implementation("org.soot-oss:sootup.java.sourcecode:1.0.0")
    implementation("org.soot-oss:sootup.java.bytecode:1.0.0")
    implementation("org.soot-oss:sootup.jimple.parser:1.0.0")
    implementation("org.soot-oss:sootup.callgraph:1.0.0")
    implementation("org.soot-oss:sootup.analysis:1.0.0")
}


tasks.test {
    jvmArgs = listOf("-Xss512m", "-Xmx2g")
    useJUnitPlatform()
}


tasks.create("MyFatJar", Jar::class) {
    group = "build"
    description = "Creates a self-contained fat JAR of the application that can be run."
    manifest.attributes["Main-Class"] = "me.valer.ktlibminer.MainKt"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/INDEX.LIST")
    from(dependencies)
    with(tasks.jar.get())
    archiveBaseName.set("${project.name}-fat")
}
