package me.valer.ktlibminer.scenario

import me.valer.ktlibminer.analysis.SceneExtractor
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.repository.ProjectsSequence
import me.valer.ktlibminer.storage.DatabaseController
import okhttp3.OkHttpClient
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.io.path.Path

class DefaultScenario : Scenario {
    override fun run() {
        if (Configurations.saveDb) DatabaseController.openConnection()
        else DatabaseController.initDB()
        try {
            // Configurations.gradleVersion = "7.5.1"
            val client = OkHttpClient()
            val repsPath = Path(Configurations.workdir, "reps")

            val analyzedPrjStorage = HashSet<String>()
            val extractor = SceneExtractor(Configurations.libName)
            val seq = ProjectsSequence(Configurations.libName, client)

            Files.createDirectories(repsPath)
            seq.filter { !analyzedPrjStorage.contains(it.name) && (it.hasJar() || Configurations.allProj) }.map {
                analyzedPrjStorage.add(it.name)
                println(it.name)

                val localPrj = it.cloneTo(Path(repsPath.toString(), it.name.replace('/', '_')))
                if (localPrj.jar != null) {
                    val t = thread { extractor.runAnalyze(localPrj.jar) }
                    t.join()
                } else {
                    val jars = localPrj.build()
                    jars.forEach { jar ->
                        val t = thread { extractor.runAnalyze(jar.toString()) }
                        t.join()
                    }
                }
                //localPrj.delete()
            }.take(Configurations.goal).last()
            DatabaseController.clearError()
            FSMInference(Configurations.workdir).inferenceAll()
        } catch (e: Exception) {
            println(e)
        } finally {
            DatabaseController.closeConnection()
        }
    }
}