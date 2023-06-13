package me.valer.ktlibminer.scenario

import me.valer.ktlibminer.analysis.SceneExtractor
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.storage.DatabaseController
import kotlin.concurrent.thread
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.walk

class PrependScenario(val path: String) : Scenario {
    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        if (Configurations.saveDb) DatabaseController.openConnection()
        else DatabaseController.initDB()
        try {
            val extractor = SceneExtractor(Configurations.libName)

            val jars = Path(path).walk().filter {
                it.name.endsWith(".jar")
            }.distinctBy { it.name }.filterNot { it.name == "gradle-wrapper.jar" }
            jars.forEach { jar ->
                val t = thread { extractor.runAnalyze(jar.toString()) }
                t.join()
            }
            DatabaseController.clearError()
            FSMInference(Configurations.workdir).inferenceAll()
        } catch (e: Exception) {
            println(e)
        } finally {
            DatabaseController.closeConnection()
        }
    }
}