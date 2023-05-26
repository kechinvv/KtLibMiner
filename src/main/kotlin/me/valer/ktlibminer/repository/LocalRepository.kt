package me.valer.ktlibminer.repository

import me.valer.ktlibminer.Configurations
import org.apache.maven.shared.verifier.Verifier
import org.gradle.tooling.GradleConnector
import soot.*
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.*


class LocalRepository(val path: Path, val jar: String?) {

    fun build(): List<Path> {
        var jarPaths = findJar(path).toList()
        if (jarPaths.isEmpty()) {
            var successBuild = buildDir(path)
            if (!successBuild) successBuild = scanAndBuild(path)
            if (successBuild) jarPaths = findJar(path).toList()
        }
        return jarPaths
    }

    @Throws(IOException::class)
    fun delete() {
        File(path.toString()).deleteRecursively()
    }

    private fun buildDir(dir: Path): Boolean {
        return if (Files.exists(Paths.get("$dir/pom.xml"))) buildMaven(dir)
        else if (Files.exists(Paths.get("$dir/build.gradle"))) buildGradle(dir)
//        else if (Files.exists(Paths.get("$dir/build.gradle.kts")) || Files.exists(Paths.get("$dir/build.gradle"))) buildGradle(
//            dir
//        )
        else false
    }

    /*
    * Find pom or gradle file on 1 level below the root
    * */
    private fun scanAndBuild(path: Path): Boolean {
        var res = false
        Files.walk(path).filter { Files.isDirectory(it) }.forEach {
            val built = buildDir(it)
            if (built) res = true
        }
        return res
    }

    private fun buildGradle(dir: Path): Boolean {
        val connector = GradleConnector.newConnector()
        if (Configurations.gradlePath != null) connector.useInstallation(File(Configurations.gradlePath))
        else if (Configurations.gradleVersion != null) connector.useGradleVersion(Configurations.gradleVersion)
        connector.forProjectDirectory(File(dir.toString()))
        return try {
            connector.connect().use {
                val build = it.newBuild()
                build.forTasks("clean")
                build.run()
                build.forTasks("build").withArguments("-x", "test")
                build.run()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildMaven(dir: Path): Boolean {
        val baseDir = dir.toString()
        return try {
            val v = Verifier(baseDir)
            v.setEnvironmentVariable("maven.multiModuleProjectDirectory", baseDir)
            if (Configurations.mavenPath != null) v.setLocalRepo(Configurations.mavenPath)
            v.addCliArguments("clean")
            v.addCliArguments("package")
            v.addCliArguments("-DskipTests")
            v.addCliArguments("-Dmaven.javadoc.skip=true")
            v.addCliArguments("-Dadditionalparam=-Xdoclint:none")
            v.addCliArguments("-DadditionalJOption=-Xdoclint:none")
            v.addCliArguments("-DadditionalOption=-Xdoclint:none")
            v.addCliArguments("-Ddoclint=none")
            v.execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    @OptIn(ExperimentalPathApi::class)
    fun findJar(dir: Path): List<Path> {
        return dir.walk().filter {
            it.name.endsWith(".jar") &&
                    (it.absolutePathString().contains(BuilderType.GRADLE.path.joinToString("/")) ||
                            it.absolutePathString().contains(BuilderType.MAVEN.path.joinToString("/")))
        }.toList().distinctBy { it.name }.filterNot { it.name == "gradle-wrapper.jar" }
    }


}


