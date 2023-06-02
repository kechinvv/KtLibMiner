package me.valer.ktlibminer.analysis

import com.google.gson.GsonBuilder
import heros.InterproceduralCFG
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.config.TraceNode
import me.valer.ktlibminer.storage.DatabaseController
import sootup.analysis.interprocedural.icfg.JimpleBasedInterproceduralCFG

import sootup.callgraph.ClassHierarchyAnalysisAlgorithm
import sootup.core.jimple.common.expr.AbstractInvokeExpr
import sootup.core.jimple.common.stmt.Stmt
import sootup.core.model.SootMethod
import sootup.core.types.ClassType
import sootup.core.types.VoidType
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation
import sootup.java.core.JavaIdentifierFactory
import sootup.java.core.JavaProject
import sootup.java.core.language.JavaLanguage
import sootup.java.core.views.JavaView
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.jvm.optionals.getOrNull


class SceneExtractorUp {

    class SceneExtractor(var lib: String) {

        lateinit var icfg: JimpleBasedInterproceduralCFG

        // lateinit var analysis: PointsTo
        private var counter = 0
        private var stop = false
        lateinit var project: JavaProject
        lateinit var view: JavaView
        lateinit var mainMethods: MutableList<SootMethod>

        fun runAnalyze(classpath: String): Boolean {
            try {
                stop = false
                counter = 0
                val inputLocation = JavaClassPathAnalysisInputLocation(classpath)

                val language = JavaLanguage(20)

                project = JavaProject.builder(language)
                    .addInputLocation(inputLocation)
                    .addInputLocation(
                        JavaClassPathAnalysisInputLocation(
                            System.getProperty("java.home") + "/lib/rt.jar"
                        )
                    )
                    .build()

                view = project.createFullView()

                view.classes.forEach { klass ->
                    klass.methods.forEach { method ->
                        if (method.isStatic && method.name == "main") mainMethods.add(method)
                    }
                }
                mainMethods.forEach { method ->
                    icfg = JimpleBasedInterproceduralCFG(view, method.signature, false, true)
                    val startPoints = icfg.getStartPointsOf(method)
                    startPoints.forEach { graphTraverseLib(it) }
                }

                //Waiting spark release...
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }


        @OptIn(ExperimentalStdlibApi::class)
        fun graphTraverseLib(
            startPoint: Stmt,
            ttl: Int = Configurations.traversJumps,
            isMethod: Boolean = false,
            extracted: HashMap<String, MutableList<MutableList<AbstractInvokeExpr>>> = HashMap(),
            continueStack: ArrayDeque<Pair<Stmt, Boolean>> = ArrayDeque(),
            depth: Int = Configurations.traversDepth
        ) {
            val currentSuccessors = icfg.getSuccsOf(startPoint)
            if (currentSuccessors.size == 0 || ttl <= 0 || depth == 0) {
                if (ttl <= 0 || !isMethod) {
                    counter++
                    if (counter % 50000 == 0) println("Traces already analyzed... = $counter")
                    if (counter == Configurations.traceLimit) stop = true
                    save(extracted)
                } else {
                    val succInfo = continueStack.removeLast()
                    graphTraverseLib(succInfo.first, ttl - 1, succInfo.second, extracted, continueStack, depth + 1)
                    continueStack.add(succInfo)
                }
            } else {
                for (succ in currentSuccessors) {
                    if (stop) return
                    var continueAdded = false
                    var klass: String? = null
                    var addedIndex: Int? = null
                    val method = view.getMethod(succ.invokeExpr.methodSignature).getOrNull()
                    try {
                        if (foundLib(succ.invokeExpr.methodSignature.declClassType.fullyQualifiedName)) {
                            saveMethod(succ.invokeExpr)
                            val sign = succ.invokeExpr.methodSignature
                            klass = if (sign.toString()
                                    .contains("static", true)
                            ) "${sign.declClassType.fullyQualifiedName}__s" else sign.declClassType.fullyQualifiedName
                            if (extracted[klass] == null) extracted[klass!!] = mutableListOf()
                            //addedIndex = fillExtracted(succ.invokeExpr, extracted[klass]!!)
                        }

                    } catch (_: Exception) {
                    }
                    if (method != null) {
                        continueStack.add(Pair(succ, isMethod))
                        continueAdded = true
                        icfg.getStartPointsOf(method).forEach { methodStart ->
                            graphTraverseLib(methodStart, ttl - 1, true, extracted, continueStack, depth - 1)
                        }
                    } else graphTraverseLib(succ, ttl - 1, isMethod, extracted, continueStack, depth)

                    if (addedIndex != null) confiscate(addedIndex, extracted[klass]!!)
                    if (continueAdded) continueStack.removeLast()
                }
            }
        }


//        private fun fillExtracted(
//            invoke: AbstractInvokeExpr,
//            extractedKlass: MutableList<MutableList<InvokeExpr>>
//        ): Int {
//            return if (invoke.method.isStatic) {
//                if (extractedKlass.size != 0) extractedKlass[0].add(invoke)
//                else extractedKlass.add(mutableListOf(invoke))
//                0
//            } else {
//                defaultExtracting(invoke, extractedKlass)
//            }
//        }

        private fun confiscate(index: Int, extractedKlass: MutableList<MutableList<AbstractInvokeExpr>>) {
            extractedKlass[index].removeLast()
            if (extractedKlass[index].isEmpty()) extractedKlass.removeAt(index)
        }

        private fun save(extracted: HashMap<String, MutableList<MutableList<AbstractInvokeExpr>>>) {
            extracted.forEach { (key, value) ->
                value.forEach inner@{
                    if (it.size < 2) return@inner
                    val jsonData = GsonBuilder().disableHtmlEscaping().create().toJson(it.map { invoke ->
                        if (Configurations.traceNode == TraceNode.NAME) invoke.methodSignature.name
                        else invoke.methodSignature.toString().replace(' ', '+')
                    })
                    DatabaseController.addTrace(jsonData!!, key)
                }
            }
        }


//        private fun defaultExtracting(invoke: InvokeExpr, extractedKlass: MutableList<MutableList<InvokeExpr>>): Int {
//            val obj1PT = getPointsToSet(invoke)
//
//            extractedKlass.forEachIndexed { index, it ->
//                if (it.isEmpty()) return@forEachIndexed
//                val obj2PT = getPointsToSet(it.last())
//                if (obj1PT.hasNonEmptyIntersection(obj2PT)) {
//                    it.add(invoke)
//                    return index
//                }
//            }
//            extractedKlass.add(mutableListOf(invoke))
//            return extractedKlass.lastIndex
//
//        }

        private fun saveMethod(invoke: AbstractInvokeExpr) {
            val name = if (Configurations.traceNode == TraceNode.NAME) invoke.methodSignature.name
            else invoke.methodSignature.toString().replace(' ', '+')
            val klass =
                if (invoke.methodSignature.toString()
                        .contains("static", true)
                ) "${invoke.methodSignature.declClassType.fullyQualifiedName}__s"
                else invoke.methodSignature.declClassType.fullyQualifiedName
            DatabaseController.addMethod(
                name,
                klass
            )
        }


//        private fun getPointsToSet(inv: InvokeExpr): PointsToSet {
//            return analysis.reachingObjects(inv.useBoxes[0].value as Local)
//        }

        private fun foundLib(method: String): Boolean {
            return method.startsWith("$lib.", true) ||
                    method.lowercase() == lib.lowercase()
        }

    }
}