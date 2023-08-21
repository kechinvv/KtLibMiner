package me.valer.ktlibminer.entities

import kotlinx.serialization.Serializable
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.config.TraceNode
import soot.SootMethod

val wrappers = mapOf(
    "java.lang.Integer" to "int",
    "int" to "int",
    "java.lang.Boolean" to "bool",
    "boolean" to "bool",
    "java.lang.Byte" to "byte",
    "byte" to "byte",
    "java.lang.Character" to "char",
    "char" to "char",
    "java.lang.Short" to "short",
    "short" to "short",
    "java.lang.Long" to "long",
    "long" to "long",
    "java.lang.Float" to "float",
    "float" to "float",
    "java.lang.Double" to "double",
    "double" to "double"
)

class MethodParser(method: SootMethod) {
    var methodData: MethodData

    init {
        val name = if (method.name == "<init>") "constructor" else method.name
        val args = mutableListOf<String>()
        if (Configurations.traceNode == TraceNode.SIGNATURE) method.parameterTypes.forEach { type ->
            val finType = type.defaultFinalType.toString()
            when {
                finType.endsWith("[]") -> args.add("array")
                finType in wrappers.keys -> args.add(wrappers[finType]!!)
                else -> args.add(finType)
            }
        }
        methodData = MethodData(name, args)
    }
}

@Serializable
data class MethodData(val name: String, val args: List<String>)