package me.valer.ktlibminer.repository

enum class BuilderType(val path: List<String>) {
    GRADLE(listOf("build", "libs")), MAVEN(listOf("target"))
}