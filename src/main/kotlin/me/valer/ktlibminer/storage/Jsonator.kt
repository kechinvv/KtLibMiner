package me.valer.ktlibminer.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import soot.Unit
import soot.jimple.internal.AbstractStmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt

object Jsonator {

    fun traceToJson(trace: List<AbstractStmt>): String? {
        val invokeTrace: MutableList<Invoke> = mutableListOf()
        for (item in trace) {
            invokeTrace.add(Invoke(item))
        }
        val json = GsonBuilder().disableHtmlEscaping().create().toJson(invokeTrace)
        return json
    }

    fun jsonToTrace(jsonTrace: String): List<Invoke> {
        return Gson().fromJson(jsonTrace, Array<Invoke>::class.java).toList()
    }

}