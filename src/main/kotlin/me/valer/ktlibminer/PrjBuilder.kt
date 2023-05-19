package me.valer.ktlibminer

import me.valer.ktlibminer.repository.LocalRepository
import org.apache.maven.shared.verifier.Verifier
import org.gradle.tooling.GradleConnector
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

/***
 * With maven_path use installed maven, without maven_path use default path to maven
 * With gradle_path use installed gradle, without gradle_path gradle.tooling auto download version from
 * gradle_version or necessary for project (last is recommended, but it requires memory)
 ***/
class PrjBuilder(var maven_path: Path?, var gradle_path: Path? = null, var gradle_version: String? = null) {

    constructor(maven_path: Path) : this(maven_path, null, null)
    constructor(maven_path: Path, gradle_path: Path) : this(maven_path, gradle_path, null)

    constructor(maven_path: Path, gradle_version: String) : this(maven_path, null, gradle_version)

    constructor(gradle_version: String) : this(null, null, gradle_version)

    fun build(prj: LocalRepository): List<Path> {
        var jarPaths = findJar(prj.path).toList()
        if (jarPaths.isEmpty()) {
            var successBuild = buildDir(prj.path)
            if (!successBuild) successBuild = scanAndBuild(prj)
            if (successBuild) jarPaths = findJar(prj.path).toList()
        }
        return jarPaths
    }

    private fun buildDir(dir: Path): Boolean {
        return if (Files.exists(Paths.get("$dir/pom.xml"))) buildMaven(dir)
        else if (Files.exists(Paths.get("$dir/build.gradle.kts")) || Files.exists(Paths.get("$dir/build.gradle"))) buildGradle(
            dir
        )
        else false
    }

    /*
    * Find pom or gradle file on 1 level below the root
    * */
    private fun scanAndBuild(prj: LocalRepository): Boolean {
        var res = false
        Files.list(prj.path).filter { Files.isDirectory(it) }.forEach {
            val built = buildDir(it)
            if (built) res = true
        }
        return res
    }

    private fun buildGradle(dir: Path): Boolean {
        val connector = GradleConnector.newConnector()
        if (gradle_path != null) connector.useInstallation(gradle_path!!.toFile())
        else if (gradle_version != null) connector.useGradleVersion(gradle_version)
        connector.forProjectDirectory(File(dir.toString()))
        return try {
            connector.connect().use {
                val build = it.newBuild()
                build.forTasks("clean")
                build.run()
                build.forTasks("jar")
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
            if (maven_path != null) v.setLocalRepo(maven_path.toString())
            v.addCliArguments("clean")
            v.addCliArguments("package")
            v.execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun findJar(dir: Path): Sequence<Path> {
        return dir.walk().filter {
            it.name.endsWith(".jar") && it.name.contains(dir.name)
        }
    }
}

