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
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.setAttribute(file, "dos:readonly", false)
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun postVisitDirectory(dir: Path, exc: IOException): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
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
    private fun scanAndBuild(path: Path): Boolean {
        var res = false
        Files.list(path).filter { Files.isDirectory(it) }.forEach {
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
            v.execute()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun findJar(dir: Path): List<Path> {
        return if (Files.exists(Paths.get("$dir/pom.xml"))) findJarWalk(dir, BuilderType.MAVEN)
        else if (Files.exists(Paths.get("$dir/build.gradle.kts")) || Files.exists(Paths.get("$dir/build.gradle"))) findJarWalk(
            dir, BuilderType.GRADLE
        ) else findJarWalk(dir, BuilderType.UNDEFINED)
    }


    fun findJarWalk(dir: Path, buildSys: BuilderType): List<Path> {
        val jars = mutableListOf<Path>()
        jars.addAll(findJarByType(dir, buildSys))
        Files.list(dir).filter { Files.isDirectory(it) }.forEach {
            BuilderType.values().forEach { type ->
                jars.addAll(findJarByType(it, type))
            }
        }
        return jars.distinctBy { it.name }
    }

    @OptIn(ExperimentalPathApi::class)
    fun findJarByType(dir: Path, buildSys: BuilderType): List<Path> {
        val jarDirDef = Path(dir.toString(), *buildSys.path.toTypedArray())
        return if (jarDirDef.exists()) jarDirDef.walk().filter { it.name.endsWith(".jar") }.toList()
        else listOf()
    }


}


