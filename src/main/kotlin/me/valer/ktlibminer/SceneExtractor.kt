package me.valer.ktlibminer

import heros.InterproceduralCFG
import me.valer.ktlibminer.storage.DatabaseController
import me.valer.ktlibminer.storage.Jsonator
import soot.*
import soot.Unit
import soot.jimple.internal.AbstractStmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.spark.pag.PAG
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG


class SceneExtractor(var lib: String) {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    var allFullTraces = mutableListOf<MutableList<AbstractStmt>>(mutableListOf())
    var extractedTraces = HashSet<List<AbstractStmt>>()

    var mainMethod: SootMethod? = null


    lateinit var analysis: PAG

    init {
        if (!PackManager.v().hasPack("wjtp.ifds")) PackManager.v().getPack("wjtp")
            .add(Transform("wjtp.ifds", object : SceneTransformer() {
                override fun internalTransform(phaseName: String?, options: MutableMap<String, String>?) {
                    if (Scene.v().hasMainClass()) {
                        println(Scene.v().hasMainClass())
                        val mainClass = Scene.v().mainClass
                        println("All methods inside are: ")
                        println(mainClass.methods.toString())
                        for (methods: SootMethod in mainClass.methods) {
                            if (methods.name == "main") {
                                mainMethod = methods
                                println("Main method found!. Terminating Search!")
                                println(mainMethod)
                                break
                            }
                        }

                        Scene.v().entryPoints = arrayListOf(mainMethod)
                        icfg = JimpleBasedInterproceduralCFG()

                        println("Entry Points are: ")
                        println(icfg!!.getStartPointsOf(mainMethod))
                        icfg!!.getStartPointsOf(mainMethod).forEach { startPoint ->
                            println("START POINT SET")
                            println(icfg!!.getSuccsOf(startPoint))

                            allFullTraces = mutableListOf(mutableListOf())
                            graphTraverseLib(startPoint)
                            allFullTraces = allFullTraces.distinct() as MutableList<MutableList<AbstractStmt>>
                            allFullTraces.forEach {
                                //println(it)
                                it.forEach { invoke ->
                                    DatabaseController.addMethod(
                                        invoke.invokeExpr.method.name,
                                        invoke.invokeExpr.method.declaringClass.toString(),
                                    )
                                }
                            }

                            analysis = Scene.v().pointsToAnalysis as PAG
                            extractedTraces = sequenceExtracting(allFullTraces).filter { it.size > 1 }.toHashSet()
                            extractedTraces.forEach {
                                //println(it)
                                val indicator = it.first()
                                var inpClass = indicator.invokeExpr.method.declaringClass.toString().replace(".", "+")
                                if (indicator.invokeExpr.method.isStatic) inpClass += "__s"
                                val jsonData = Jsonator.traceToJson(it)
                                //println(jsonData)
                                DatabaseController.addTrace(jsonData!!, inpClass)
                            }
                        }
                    } else println("Not a malware with main method")
                }
            }))
    }

    fun sequenceExtracting(allTraces: List<List<AbstractStmt>>): HashSet<List<AbstractStmt>> {
        val extractedTracesRet = HashSet<List<AbstractStmt>>(mutableListOf())
        for (trace in allTraces) {
            if (trace.size < 2) continue
            val (traceStatic, traceCopy) = trace.partition { it.invokeExpr.method.isStatic }

            val staticExtracted = staticExtracting(traceStatic.toMutableList())
            extractedTracesRet.addAll(staticExtracted)
            val invokeExtracted = invokeExtracting(traceCopy.toMutableList())
            extractedTracesRet.addAll(invokeExtracted)
        }
        return extractedTracesRet
    }

    private fun staticExtracting(traceStatic: MutableList<AbstractStmt>): HashSet<List<AbstractStmt>> {
        val extractedTracesRet = HashSet<List<AbstractStmt>>(mutableListOf())
        while (traceStatic.isNotEmpty()) {
            val collector = mutableListOf<AbstractStmt>()
            val invokeClass = traceStatic.first().invokeExpr.method.declaringClass
            for (statInvoke in traceStatic) {
                if (statInvoke.invokeExpr.method.declaringClass == invokeClass) collector.add(statInvoke)
            }
            extractedTracesRet.add(collector)
            traceStatic.removeAll(collector)
        }
        return extractedTracesRet
    }

    private fun invokeExtracting(traceCopy: MutableList<AbstractStmt>): HashSet<List<AbstractStmt>> {
        val extractedTracesRet = HashSet<List<AbstractStmt>>(mutableListOf())
        while (traceCopy.size > 1) {
            var collector = mutableListOf<AbstractStmt>()
            val firstCallInd = 0
            var secondCallInd = 1
            var obj1 = traceCopy[firstCallInd]
            var obj2 = traceCopy[secondCallInd]
            while (traceCopy.size > 1) {

                val obj1PT = getPointsTo(obj1)
                val obj2PT = getPointsTo(obj2)

                val resAlias = obj1PT.hasNonEmptyIntersection(obj2PT)
                val sameClass = obj1.invokeExpr.method.declaringClass == obj2.invokeExpr.method.declaringClass

                // println("$obj1 to $obj2 = $resAlias")

                if (resAlias && sameClass) {
                    collector.add(obj1)
                    traceCopy.remove(obj1)
                    obj1 = obj2
                    secondCallInd--
                }
                if (secondCallInd >= traceCopy.size - 1) {
                    collector.add(obj1)
                    traceCopy.remove(obj1)

                    extractedTracesRet.add(collector)
                    collector = mutableListOf()
                    if (traceCopy.size > 1) {
                        obj1 = traceCopy[firstCallInd]
                        secondCallInd = firstCallInd + 1
                        obj2 = traceCopy[secondCallInd]
                    }
                } else {
                    secondCallInd++
                    obj2 = traceCopy[secondCallInd]
                }
            }
        }
        return extractedTracesRet
    }


    fun graphTraverseLib(
        startPoint: Unit,
        ttl: Int = 100,
    ) {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0) {
            return
            // println("Traversal complete")
        } else {
            //println("List of sucs: $currentSuccessors       len: ${currentSuccessors.size}")
            val traceOrig = allFullTraces.last().toMutableList()
            for (succ in currentSuccessors) {
                //println("Succesor: $succ        Class: ${succ.javaClass}")
                var method: SootMethod? = null
                try {
                    if (succ is JInvokeStmt || succ is JAssignStmt) {
                        succ as AbstractStmt
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) allFullTraces.last()
                            .add(succ)
                    }
                    if (method != null) {
                        val methodStart = icfg!!.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, ttl - 1)
                    }

                } catch (_: Exception) {
                }
                graphTraverseLib(succ, ttl)
                if (currentSuccessors.indexOf(succ) != currentSuccessors.size - 1) allFullTraces.add(traceOrig)
            }
        }
    }


    fun runAnalyze(classpath: String): Boolean {
        try {
            val args = arrayOf(
                "-w",
                "-pp",
                "-allow-phantom-refs",
                "-process-dir",
                classpath,
                "-p",
                "cg.spark",
                "enabled:true,verbose:true"
            )
            Main.main(args)
            G.reset()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getPointsTo(stmt: AbstractStmt): PointsToSet {
        return analysis.reachingObjects(stmt.invokeExpr.useBoxes[0].value as Local)
    }
}