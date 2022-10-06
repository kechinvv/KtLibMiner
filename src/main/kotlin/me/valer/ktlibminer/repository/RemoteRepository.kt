package me.valer.ktlibminer.repository

import java.io.IOException
import java.nio.file.Path


class RemoteRepository(var url: String?, var name: String?) {

    @Throws(InterruptedException::class, IOException::class)
    fun cloneTo(path: Path): LocalRepository {
        val proc = ProcessBuilder("git", "clone", "--depth=1", "--recurse-submodules", url, path.toString()).start()
        proc.waitFor()
        proc.destroy()
        return LocalRepository(path)
    }

}