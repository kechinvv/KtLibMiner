package me.valer.ktlibminer

import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.storage.DatabaseController
import kotlin.io.path.Path

fun main(args: Array<String>) {
    DatabaseController.initDB()
    try {
        Configurations.ghToken = System.getenv("token")
        Configurations.gradleVersion = "7.5.1"

        val analyzedPrjStorage = HashSet<String>()
        val extractor = SceneExtractor("java.io.File")
        val seq = ProjectsSequence("java.io.FileReader")

        seq.filter { !analyzedPrjStorage.contains(it.name) }.map {
            analyzedPrjStorage.add(it.name)
            println(it.name)
            val localPrj = it.cloneTo(Path("D:/ktlibminer/reps/" + it.name.replace('/', '_')))
            if (localPrj.jar != null) extractor.runAnalyze(localPrj.jar)
            else {
                val jars = localPrj.build()
                jars.forEach { jar ->
                    extractor.runAnalyze(jar.toString())
                }
            }
            localPrj.delete()
        }.take(100).last()

        FSMInference("D:/ktlibminer/").inferenceAll()
    } catch (e: Exception) {
        println(e)
    } finally {
        DatabaseController.closeConnection()
    }
}




