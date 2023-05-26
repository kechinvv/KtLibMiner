package me.valer.ktlibminer.repository

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.valer.ktlibminer.Configurations
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.jgit.api.Git
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.notExists


class RemoteRepository(var url: String, var name: String, val client: OkHttpClient) {

    constructor(repoJSON: JsonObject, client: OkHttpClient) : this(
        repoJSON.get("html_url").toString(),
        repoJSON.get("full_name").toString(),
        client
    )

    init {
        url = url.replace("\"", "")
        name = name.replace("\"", "")
    }

    fun getAssets(token: String): String? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$name/releases/latest")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .build()
        val response = client.newCall(request).execute().body?.string()
        val json = JsonParser.parseString(response).asJsonObject
        if (json.has("assets")) {
            val jsonAssets = json.getAsJsonArray("assets")
            for (asset in jsonAssets) {
                if ((asset as JsonObject).has("browser_download_url")) {
                    val downloadLink = asset.get("browser_download_url").toString().drop(1).dropLast(1)
                    if (downloadLink.endsWith(".jar")) return downloadLink
                }
            }
        }
        return if (json.has("zipball_url")) {
            json.get("zipball_url").toString().drop(1).dropLast(1)
        } else null
    }

    override fun toString(): String {
        return "$name $url"
    }


    @Throws(InterruptedException::class, IOException::class)
    fun cloneTo(path: Path): LocalRepository {
        var jarName: String? = null
        val downloadURL = getAssets(Configurations.ghToken!!)
        if (downloadURL != null) {
            println("this!!!!1")
            Files.createDirectories(path)
            val fileBytes = URL(downloadURL).readBytes()
            var remoteName = downloadURL.split('/').last()
            if (!downloadURL.endsWith(".jar")) remoteName += ".zip"
            val fileName = "$path/$remoteName"
            val file = File(fileName)
            file.createNewFile()
            file.writeBytes(fileBytes)
            if (!downloadURL.endsWith(".jar")) {
                unzip(fileName, path.toString())
                Files.delete(Path(fileName))
            } else jarName = fileName
        } else {
            println("this")

            if (path.notExists()) Git.cloneRepository()
                .setDepth(1)
                .setCloneSubmodules(true)
                .setURI(url)
                .setDirectory(path.toFile())
                .call().close()
        }
        return LocalRepository(path, jarName)
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
        File(destFilePath).outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }


}

