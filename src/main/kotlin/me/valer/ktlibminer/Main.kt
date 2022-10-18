package me.valer.ktlibminer


fun main(args: Array<String>) {
    val seq = ProjectsSequence("java.io.File", 5000)
    val list = seq.take(200).toList()
    println(list)
    println(list.size)
}
