package me.valer.ktlibminer.entities

import kotlinx.serialization.Serializable
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.config.TraceNode
import soot.SootMethod



class MethodParser(method: SootMethod) {
    var methodData: MethodData

    init {
        val name = if (method.name == "<init>") "constructor" else method.name
        val args = mutableListOf<String>()
        if (Configurations.traceNode == TraceNode.SIGNATURE) method.parameterTypes.forEach { type ->
            val finType = type.defaultFinalType.toString()
            args.add(finType)
        }
        methodData = MethodData(name, args)
    }
}

@Serializable
data class MethodData(val name: String, val args: List<String>)