package me.valer.ktlibminer

import heros.InterproceduralCFG
import soot.*
import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.spark.pag.PAG
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG


object SceneExtractor {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    var lib = "java.io.File"
    var allFullTraces = mutableListOf<MutableList<Unit>>(mutableListOf())
    var extractedTraces: List<List<Unit>> = mutableListOf(mutableListOf())

    lateinit var startPoint: Unit
    var mainMethod: SootMethod? = null


    lateinit var analysis: PAG

    init {

        allFullTraces = mutableListOf(mutableListOf())

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
                            }
                        }

                        Scene.v().entryPoints = arrayListOf(mainMethod)
                        println("Entry Points are: ")
                        icfg = JimpleBasedInterproceduralCFG()

                        println(icfg!!.getStartPointsOf(mainMethod))
                        startPoint = icfg!!.getStartPointsOf(mainMethod).first()
                        println("START POINT SET")
                        println(icfg!!.getSuccsOf(startPoint))

                        graphTraverseLib(startPoint, icfg!!)
                        allFullTraces = allFullTraces.distinct() as MutableList<MutableList<Unit>>
                        allFullTraces.forEach { println(it) }

                        analysis = Scene.v().pointsToAnalysis as PAG
                        extractedTraces = sequenceExtracting(allFullTraces).filter { it.size > 1 }
                        extractedTraces.forEach { println(it) }

                    } else println("Not a malware with main method")
                }
            }))
    }

    fun sequenceExtracting(allTraces: List<List<Unit>>): MutableList<MutableList<Unit>> {
        val extractedTracesRet = mutableListOf<MutableList<Unit>>(mutableListOf())
        for (f_ind in allTraces.indices) {
            if (allTraces[f_ind].size < 2) continue
            val traceCopy = allTraces[f_ind].toMutableList()
            while (traceCopy.size > 1) {
                var collector = mutableListOf<Unit>()
                val firstCallInd = 0
                var secondCallInd = 1
                var obj1 = traceCopy[firstCallInd]
                var obj2 = traceCopy[secondCallInd]
                while (traceCopy.size > 1) {

                    val obj1PT =
                        if (obj1 is JInvokeStmt) analysis.reachingObjects(obj1.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj1 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)
                    val obj2PT =
                        if (obj2 is JInvokeStmt) analysis.reachingObjects(obj2.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj2 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)

                    val pointsTo = obj1PT.hasNonEmptyIntersection(obj2PT)

                    // println("$obj1 to $obj2 = $pointsTo")

                    if (pointsTo) {
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

        }

        return extractedTracesRet
    }


    fun graphTraverseLib(
        startPoint: Unit,
        icfg: InterproceduralCFG<Unit, SootMethod>?,
        ttl: Int = 220,
    ) {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0) {
            return
            // println("Traversal complete")
        } else {
            //println("List of sucs: $currentSuccessors       len: ${currentSuccessors.size}")
            val traceOrig = allFullTraces.last().toMutableList()
            for (succ in currentSuccessors) {
                // println("Succesor: $succ        Class: ${succ.javaClass}")
                var method: SootMethod? = null
                try {
                    if (succ is JInvokeStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) allFullTraces.last()
                            .add(succ)
                    } else if (succ is JAssignStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) allFullTraces.last()
                            .add(succ)
                    }
                    if (method != null) {
                        val methodStart = icfg.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, icfg, ttl - 1)
                    }

                } catch (_: Exception) {
                }
                graphTraverseLib(succ, icfg, ttl)
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

    fun sequenceSelectingTest() {
        for (f_ind in 0 until allFullTraces.size) {
            for (t_ind in 0 until allFullTraces[f_ind].size - 1) {
                for (tp_ind in t_ind + 1 until allFullTraces[f_ind].size) {

                    val obj1 = allFullTraces[f_ind][t_ind]
                    val obj2 = allFullTraces[f_ind][tp_ind]

                    val obj1PT =
                        if (obj1 is JInvokeStmt) analysis.reachingObjects(obj1.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj1 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)
                    val obj2PT =
                        if (obj2 is JInvokeStmt) analysis.reachingObjects(obj2.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj2 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)
                    println(obj1.toString() + " to " + obj2 + " = " + obj1PT.hasNonEmptyIntersection(obj2PT))
                    if (obj1 is JInvokeStmt) {
                        println(obj1.invokeExpr.method.declaringClass)
                        println(obj1.invokeExpr.method.returnType)
                        println(obj1.invokeExpr.method.signature)
                    } else {
                        println((obj1 as JAssignStmt).invokeExpr.method.declaringClass)
                        println(obj1.invokeExpr.method.returnType)
                        println(obj1.invokeExpr.method.signature)
                    }
                }
            }
        }
    }
}