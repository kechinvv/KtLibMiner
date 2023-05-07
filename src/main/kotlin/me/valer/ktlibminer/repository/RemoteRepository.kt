package me.valer.ktlibminer.repository

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.Path


class RemoteRepository() {
    lateinit var url: String
    lateinit var name: String

    private val defaultZipName = "project.zip"

    constructor(url: String, name: String) : this() {
        this.url = url.replace("\"", "")
        this.name = name.replace("\"", "")
    }

    constructor(repoJSON: JsonObject) : this() {
        this.url = repoJSON.get("html_url").toString().replace("\"", "")
        this.name = repoJSON.get("full_name").toString().replace("\"", "")
    }

    private fun getAssets(token: String): String? {
        val response = khttp.get(
            url = "https://api.github.com/repos/$name/releases/latest",
            headers = mapOf("Authorization" to "Bearer $token", "Accept" to "application/vnd.github+json")
        ).text
        val json = JsonParser.parseString(response).asJsonObject
        if (json.has("zipball_url")) {
            val zipUrl = json.get("zipball_url").toString().drop(1).dropLast(1)
            val zipBytes = URL(zipUrl).readBytes()
            val zipFile = File(defaultZipName)
            zipFile.createNewFile()
            zipFile.writeBytes(zipBytes)
            return zipUrl
        } else return null
    }

    override fun toString(): String {
        return "$name $url"
    }


    @Throws(InterruptedException::class, IOException::class)
    fun cloneTo(path: Path, token: String): LocalRepository {
        val zipUrl = getAssets(token)
        if (zipUrl != null) {
            Files.createDirectories(path)
            val zipBytes = URL(zipUrl).readBytes()
            val zipFileName = "$path/$defaultZipName"
            val zipFile = File(zipFileName)
            zipFile.createNewFile()
            zipFile.writeBytes(zipBytes)
            unzip(zipFileName, path.toString())
            Files.delete(Path(zipFileName))
        } else {
            val proc = ProcessBuilder("git", "clone", "--depth=1", "--recurse-submodules", url, path.toString()).start()
            proc.waitFor()
            proc.destroy()
        }
        return LocalRepository(path)
    }

    fun unzip(zipFileName: String, destDirectory: String) {
        File(destDirectory).run {
            if (!exists()) {
                mkdirs()
            }
        }

        ZipFile(zipFileName).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val dirLevels = entry.name.split('/').toMutableList()
                    dirLevels.removeAt(0)
                    val entryName = dirLevels.joinToString("/")
                    val filePath = destDirectory + File.separator + entryName
                    if (!entry.isDirectory) {
                        extractFile(input, filePath)
                    } else {
                        val dir = File(filePath)
                        dir.mkdir()
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        val bos = BufferedOutputStream(FileOutputStream(destFilePath))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (inputStream.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }

    private val BUFFER_SIZE = 4096


}