package me.valer.ktlibminer

import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.storage.DatabaseController
import okhttp3.OkHttpClient
import kotlin.io.path.Path

fun main(args: Array<String>) {
    DatabaseController.initDB()
    try {
        Configurations.ghToken = System.getenv("token") ?: "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"
        Configurations.gradleVersion = "7.5.1"
        val client = OkHttpClient()

        val analyzedPrjStorage = HashSet<String>()
        val extractor = SceneExtractor("java.util.zip.ZipOutputStream")
        val seq = ProjectsSequence("java.util.zip.ZipOutputStream", client)

        seq.filter { !analyzedPrjStorage.contains(it.name) }.map {
            analyzedPrjStorage.add(it.name)
            println(it.name)
            val localPrj = it.cloneTo(Path("D:/ktlibminertest/reps/" + it.name.replace('/', '_')))
            println(localPrj.path)
            if (localPrj.jar != null) {
                println("JAR!")
                extractor.runAnalyze(localPrj.jar)
            } else {
                val jars = localPrj.build()
                println(jars)
                jars.forEach { jar ->
                    extractor.runAnalyze(jar.toString())
                }
            }
            //localPrj.delete()
        }.take(100).last()

        FSMInference("D:/ktlibminertest/").inferenceAll()
    } catch (e: Exception) {
        throw e
    } finally {
        DatabaseController.closeConnection()
    }
}




