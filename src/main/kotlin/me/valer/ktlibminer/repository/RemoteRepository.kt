package me.valer.ktlibminer.repository

import com.google.gson.JsonObject
import java.io.IOException
import java.nio.file.Path


class RemoteRepository() {
    lateinit var url: String
    lateinit var name: String

    constructor(url: String, name: String) : this() {
        this.url = url.replace("\"", "")
        this.name = name.replace("\"", "")
    }

    constructor(repoJSON: JsonObject) : this() {
        this.url = repoJSON.get("html_url").toString().replace("\"", "")
        this.name = repoJSON.get("full_name").toString().replace("\"", "")
    }

    override fun toString(): String {
        return "$name $url"
    }

    @Throws(InterruptedException::class, IOException::class)
    fun cloneTo(path: Path): LocalRepository {
        val proc = ProcessBuilder("git", "clone", "--depth=1", "--recurse-submodules", url, path.toString()).start()
        proc.waitFor()
        proc.destroy()
        return LocalRepository(path)
    }

}