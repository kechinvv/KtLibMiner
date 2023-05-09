package me.valer.ktlibminer

import heros.InterproceduralCFG
import soot.*
import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.internal.JimpleLocalBox
import soot.jimple.spark.SparkTransformer
import soot.jimple.spark.geom.geomPA.GeomPointsTo
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.callgraph.Targets
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.util.dot.DotGraph
import java.io.File


object CreatorICFG {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    val lib = "java.io.File"
    var fulTrace = mutableListOf<MutableList<Unit>>(mutableListOf())

    /**
     * For stable work need to set path to jt.jar manually
     **/
    var javaPaths = ""
    lateinit var startPoint: Unit
    lateinit var callGraph: CallGraph
    var mainMethod: SootMethod? = null


    var visited = ArrayList<Unit>()
    var dotIcfg: DotGraph? = null
    lateinit var analysis: PointsToAnalysis
    lateinit var geomAnal: GeomPointsTo

    init {
        val sep = File.separator
        val pathSep = File.pathSeparator
        javaPaths = System.getProperty("java.home") + sep + "jre" + sep + "lib" + sep + "rt.jar" + pathSep
        fulTrace = mutableListOf(mutableListOf())

        if (!PackManager.v().hasPack("wjtp.ifds")) PackManager.v().getPack("wjtp")
            .add(Transform("wjtp.ifds", object : SceneTransformer() {
                override fun internalTransform(phaseName: String?, options: MutableMap<String, String>?) {
                    if (Scene.v().hasMainClass()) {
                        println(Scene.v().hasMainClass())
                        var mainClass = Scene.v().mainClass
                        var mainMethodFlag = false
                        for (sc: SootClass in Scene.v().classes) {
                            if (sc.name == mainClass.toString()) {
                                mainClass = sc
                                println("All methods inside are: ")
                                println(sc.methods.toString())
                                for (methods: SootMethod in sc.methods) {
                                    if (methods.name == "main") {
                                        mainMethod = methods
                                        println("Main method found!. Terminating Search!")
                                        mainMethodFlag = true
                                        break
                                    }
                                }
                                if (mainMethodFlag) break
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
                        fulTrace = fulTrace.distinct() as MutableList<MutableList<Unit>>
                        fulTrace.forEach { println(it) }
                        //sceneCollector.forEach { println(it) }
                        val opt: MutableMap<String, String> = HashMap()
//                        opt["verbose"] = "true"
//                        opt["propagator"] = "worklist"
//                        opt["simple-edges-bidirectional"] = "false"
//                        opt["on-fly-cg"] = "true"
//                        opt["set-impl"] = "double"
//                        opt["double-set-old"] = "hybrid"
//                        opt["double-set-new"] = "hybrid"
//                        SparkTransformer.v().transform("", opt)
                        analysis = Scene.v().pointsToAnalysis
                        //geomAnal = analysis as GeomPointsTo
                        sequenceSelecting()

                    } else println("Not a malware with main method")
                }
            }))
    }


    fun sequenceSelecting() {
        for (f_ind in 0 until fulTrace.size) {
            for (t_ind in 0 until fulTrace[f_ind].size - 1) {
                for (tp_ind in t_ind until fulTrace[f_ind].size) {
//                    val obj1 =
//                        if (fulTrace[f_ind][t_ind] is JInvokeStmt) (fulTrace[f_ind][t_ind] as JInvokeStmt).invokeExpr.useBoxes[0].value
//                        else (fulTrace[f_ind][t_ind] as JAssignStmt).invokeExpr.useBoxes[0].value
//                    val obj2 =
//                        if (fulTrace[f_ind][tp_ind] is JInvokeStmt) (fulTrace[f_ind][tp_ind] as JInvokeStmt).invokeExpr.useBoxes[0].value
//                        else (fulTrace[f_ind][tp_ind] as JAssignStmt).invokeExpr.useBoxes[0].value
                    val obj1 = fulTrace[f_ind][t_ind]
                    val obj2 = fulTrace[f_ind][tp_ind]
                    val obj1PT =
                        if (obj1 is JInvokeStmt) analysis.reachingObjects(obj1.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj1 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)
                    val obj2PT =
                        if (obj2 is JInvokeStmt) analysis.reachingObjects(obj2.invokeExpr.useBoxes[0].value as Local)
                        else analysis.reachingObjects((obj2 as JAssignStmt).invokeExpr.useBoxes[0].value as Local)
                    println(obj1.toString() + " to " + obj2 + " = " + obj1PT.hasNonEmptyIntersection(obj2PT))
                }
            }
        }
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
            val traceOrig = fulTrace.last().toMutableList()
            for (succ in currentSuccessors) {
                // println("Succesor: $succ        Class: ${succ.javaClass}")
                var method: SootMethod? = null
                try {
                    if (succ is JInvokeStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) fulTrace.last().add(succ)
                    } else if (succ is JAssignStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) fulTrace.last().add(succ)
                    }
                    if (method != null) {
                        val methodStart = icfg.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, icfg, ttl - 1)
                    }

                } catch (_: Exception) {
                }
                graphTraverseLib(succ, icfg, ttl)
                if (currentSuccessors.indexOf(succ) != currentSuccessors.size - 1) fulTrace.add(traceOrig)
            }
        }
    }


    fun getICFG(classpath: String): InterproceduralCFG<Unit, SootMethod>? {
        try {
            val args = arrayOf(
                "-w",
                "-pp",
                "-allow-phantom-refs",
                "-process-dir",
                classpath,
                "-p",
                "cg.spark",
                "enabled,verbose:true"
                //"verbose:true,geom-pta:true"
            )
            if (javaPaths.last().toString() != File.pathSeparator) javaPaths += File.pathSeparator
            //Scene.v().sootClassPath = javaPaths + classpath

            Main.main(args)
            G.reset()
            return icfg
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}