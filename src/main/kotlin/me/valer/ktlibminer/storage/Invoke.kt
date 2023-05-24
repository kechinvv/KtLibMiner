package me.valer.ktlibminer.storage

import soot.SootMethod

data class Invoke(
    val methodName: String,
    val signature: String,
    val declClass: String,
    val returnedClass: String
) {

    constructor(invoke: SootMethod) : this(
        invoke.name,
        invoke.signature,
        invoke.declaringClass.toString(),
        invoke.returnType.toString()
    )

}