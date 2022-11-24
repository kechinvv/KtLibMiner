package me.valer.ktlibminer.repository

import org.vorpal.research.kfg.ClassManager
import org.vorpal.research.kfg.KfgConfig
import org.vorpal.research.kfg.container.DirectoryContainer
import org.vorpal.research.kfg.util.Flags
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream


class LocalRepository(val path: Path) {

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

    @get:Throws(IOException::class)
    val ktStream: Stream<String>
        get() = Files.walk(path, FileVisitOption.FOLLOW_LINKS)
            .map { obj: Path -> obj.toFile() }
            .filter { f: File -> f.isFile && f.name.endsWith(".kt") }.map { obj: File -> obj.absolutePath }


    fun getKfg(){
        val cm = ClassManager(KfgConfig(Flags.readAll, failOnError = true))
        val prj = DirectoryContainer(this.path.toFile())
        cm.initialize(prj)
        for (klass in cm.concreteClasses) {
            for (method in klass.allMethods) {
                // view each method as graph
                method.view("C:/Program Files/Graphviz/bin/dot.exe", "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe")
            }
        }
    }

}