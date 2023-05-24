package me.valer.ktlibminer.builder

enum class BuilderType(val path: List<String>) {
    GRADLE(listOf("build","libs")), MAVEN(listOf("target")), UNDEFINED(listOf())
}