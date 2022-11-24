package me.valer.ktlibminer

import me.valer.ktlibminer.repository.LocalRepository
import org.apache.maven.shared.verifier.Verifier
import org.gradle.tooling.GradleConnector
import java.io.File
import java.nio.file.Path
/***
 * With maven_path use installed maven, without maven_path use default path to maven
 * With gradle_path use installed gradle, without gradle_path gradle.tooling auto download version from
 * gradle_version or necessary for project
 ***/
class PrjBuilder(var maven_path: Path?, var gradle_path: Path? = null, var gradle_version: String? = null) {

    constructor(maven_path: Path) : this(maven_path, null, null)
    constructor(maven_path: Path, gradle_path: Path) : this(maven_path, gradle_path, null)

    constructor(maven_path: Path, gradle_version: String) : this(maven_path, null, gradle_version)

    constructor(gradle_version: String) : this(null, null, gradle_version)

    fun build(prj: LocalRepository) {
        val files = File(prj.path.toString()).listFiles().filter { it.isFile }.map { it.name }
        if (files.contains("pom.xml")) buildMaven(prj)
        else if (files.contains("build.gradle.kts") || files.contains("build.gradle")) buildGradle(prj)
    }

    fun buildGradle(prj: LocalRepository) {
        val connector = GradleConnector.newConnector()
        if (gradle_path != null) connector.useInstallation(gradle_path!!.toFile())
        else if (gradle_version != null) connector.useGradleVersion(gradle_version)
        connector.forProjectDirectory(File(prj.path.toString()))
        connector.connect().use {
            val build = it.newBuild()
            build.forTasks("build")
            build.run()
        }
    }

    fun buildMaven(prj: LocalRepository) {
        val baseDir = prj.path.toString()
        try {
            val v = Verifier(baseDir)
            v.setEnvironmentVariable("maven.multiModuleProjectDirectory", baseDir)
            if (maven_path != null) v.setLocalRepo(maven_path.toString())
            v.addCliArguments("package")
            v.execute()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    /*
        fun buildMaven(prj: LocalRepository) {
            val connector = GradleConnector.newConnector()
            if (gradle_path != null) connector.useInstallation(gradle_path!!.toFile())
            else if (gradle_version != null) connector.useGradleVersion(gradle_version)
            connector.forProjectDirectory(File(prj.path.toString()))
            connector.connect().use {
                val build = it.newBuild()
                build.forTasks("init")
                build.run()
                build.forTasks("classes")
                build.run()
            }
        }

    */
}

