package me.valer.ktlibminer.config

object Configurations {
    var libName: String = ""
    var ghToken: String? = null

    var mavenPath: String? = null
    var gradlePath: String? = null
    var gradleVersion: String? = null

    var workdir = "./"

    var kAlg = 1
    var goal = 100

    var allProj = false

    var traversJumps = 200
    var traceNode = TraceNode.NAME
    var unionEnd = true
    var toJson = true
}