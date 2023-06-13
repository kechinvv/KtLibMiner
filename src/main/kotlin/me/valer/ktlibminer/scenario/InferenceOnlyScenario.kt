package me.valer.ktlibminer.scenario

import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.storage.DatabaseController

class InferenceOnlyScenario : Scenario {
    override fun run() {
        DatabaseController.openConnection()
        FSMInference(Configurations.workdir).inferenceAll()
        DatabaseController.closeConnection()
    }
}