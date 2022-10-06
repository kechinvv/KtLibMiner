package me.valer.ktlibminer.repository

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

}