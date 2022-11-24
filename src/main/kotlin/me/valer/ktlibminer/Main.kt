package me.valer.ktlibminer

import com.google.gson.Gson
import java.io.FileReader
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val cfg = Gson().fromJson(
        FileReader("C:\\Users\\valer\\IdeaProjects\\KtLibMiner\\src\\main\\resources\\token.txt"),
        HashMap::class.java
    )
    val seq = ProjectsSequence("khttp", cfg["token"] as String)
    val list = seq.take(200).distinctBy { it.name }.toList()
    println(list)
    list.forEach {
        val prj =
            it.cloneTo(Path("C:/Users/valer/IdeaProjects/KtLibMiner/src/test/resources/" + it.name.replace('/', '_')))
        prj.getKfg()
    }
}
