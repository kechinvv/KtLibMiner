package me.valer.ktlibminer.storage

import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt

class Invoke() {
    lateinit var methodName: String
    lateinit var signature: String
    lateinit var declClass: String
    lateinit var returnedClass: String

    constructor(methodName: String, signature: String, declClass: String, returnedClass: String) : this() {
        this.methodName = methodName
        this.signature = signature
        this.declClass = declClass
        this.returnedClass = returnedClass
    }

    constructor(invoke: Unit) : this() {
        if (invoke is JAssignStmt) {
            methodName = invoke.invokeExpr.method.name
            signature = invoke.invokeExpr.method.signature
            declClass = invoke.invokeExpr.method.declaringClass.toString()
            returnedClass = invoke.invokeExpr.method.returnType.toString()
        } else if (invoke is JInvokeStmt) {
            methodName = invoke.invokeExpr.method.name
            signature = invoke.invokeExpr.method.signature
            declClass = invoke.invokeExpr.method.declaringClass.toString()
            returnedClass = invoke.invokeExpr.method.returnType.toString()
        }
    }
}