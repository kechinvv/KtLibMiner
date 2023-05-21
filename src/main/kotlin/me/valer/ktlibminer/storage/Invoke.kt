package me.valer.ktlibminer.storage

import soot.jimple.internal.AbstractStmt

data class Invoke(
    val methodName: String,
    val signature: String,
    val declClass: String,
    val returnedClass: String
) {

    constructor(invoke: AbstractStmt) : this(
        invoke.invokeExpr.method.name,
        invoke.invokeExpr.method.signature,
        invoke.invokeExpr.method.declaringClass.toString(),
        invoke.invokeExpr.method.returnType.toString()
    )

}