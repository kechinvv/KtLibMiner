package me.valer.ktlibminer


fun main(args: Array<String>) {
    val seq = ProjectsSequence("java.io.File")
    val list = seq.take(200).toList()
    println(list)
    println(list.size)
}
