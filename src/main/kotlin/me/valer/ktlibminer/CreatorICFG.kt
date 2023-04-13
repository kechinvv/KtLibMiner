package me.valer.ktlibminer

import heros.InterproceduralCFG
import soot.*
import soot.Unit
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.spark.SparkTransformer
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.callgraph.Targets
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.util.dot.DotGraph
import java.io.File


object CreatorICFG {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    val lib = "java.nio"
    val invSequnece = mutableListOf<Unit>()
    val sceneCollector: MutableList<MutableList<Unit>> = mutableListOf()

    /**
     * For stable work need to set path to jt.jar manually
     **/
    var javaPaths = ""
    lateinit var startPoint: Unit
    lateinit var callGraph: CallGraph
    var mainMethod: SootMethod? = null


    var visited = ArrayList<Unit>()
    var dotIcfg: DotGraph? = null

    init {
        val sep = File.separator
        val pathSep = File.pathSeparator
        javaPaths = System.getProperty("java.home") + sep + "jre" + sep + "lib" + sep + "rt.jar" + pathSep

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
                        // Scene.v().loadNecessaryClasses()
                        callGraph = Scene.v().callGraph
                        dotIcfg = DotGraph("")
                        // visit(callGraph, mainMethod!!)
                        // println(Scene.v().applicationClasses)
                        graphTraverseLib(startPoint, icfg!!)
                        sceneCollector.forEach { println(it) }
                        val opt: MutableMap<String, String> = HashMap()
                        opt["verbose"] = "true"
                        opt["propagator"] = "worklist"
                        opt["simple-edges-bidirectional"] = "false"
                        opt["on-fly-cg"] = "true"
                        opt["set-impl"] = "double"
                        opt["double-set-old"] = "hybrid"
                        opt["double-set-new"] = "hybrid"
                        SparkTransformer.v().transform("", opt)
                        val analysis = Scene.v().pointsToAnalysis
                        // println(analysis.reachingObjects())
                        //dotIcfg!!.plot("testg.dot")
                    } else println("Not a malware with main method")
                }
            }))
    }


    fun graphTraverseLib(
        startPoint: Unit,
        icfg: InterproceduralCFG<Unit, SootMethod>?,
        ttl: Int = 220,
        head: MutableList<Unit> = mutableListOf()
    ) {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0) {
            if (!sceneCollector.contains(head))
                sceneCollector.add(head)
            println("Traversal complete")
            return
        } else {
            println(currentSuccessors)
            for (succ in currentSuccessors) {
                println("Succesor: $succ")
                println(succ.javaClass)
                var method: SootMethod? = null
                try {
                    if (succ is JInvokeStmt) {
                        println("Decl:" + (succ.invokeExpr.method.declaringClass))
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) head.add(succ)
                    } else if (succ is JAssignStmt) {
                        println("??? " + succ.rightOp)
                        println("Decl:" + (succ.invokeExpr.method.declaringClass))
                        //if (succ.invokeExpr.method.isPhantom) {
                        println("start method:")
                        println(icfg.getStartPointsOf(succ.invokeExpr.method))
                        //}
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) head.add(succ)
                    }
                    if (method != null) {
                        val methodStart = icfg.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, icfg, ttl - 1, head)

                    }

                } catch (e: Exception) {
                    println(e.message)
                }
                graphTraverseLib(succ, icfg, ttl - 1, head)
            }
        }
    }

    fun graphTraverseSimple(startPoint: Unit, icfg: InterproceduralCFG<Unit, SootMethod>?) {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0) {
            println("Traversal complete")
            return
        } else {
            for (succ in currentSuccessors) {
                println("Succesor: $succ")
                if (!visited.contains(succ)) {
                    //dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                    visited.add(succ!!)
                    graphTraverseSimple(succ, icfg)
                } else {
                    //dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                }
            }
        }
    }

    val visited2 = HashMap<String, Boolean>()
    fun visit(cg: CallGraph, method: SootMethod) {
        val identifier = method.signature
        visited2[method.signature] = true
        dotIcfg!!.drawNode(identifier)

        val ctargets: Iterator<MethodOrMethodContext> = Targets(cg.edgesOutOf(method))
        while (ctargets.hasNext()) {
            val child = ctargets.next() as SootMethod
            if (child.declaringClass in Scene.v().applicationClasses) {
                dotIcfg!!.drawEdge(identifier, child.signature)
                println("$method may call $child")
                graphTraverseSimple(icfg!!.getStartPointsOf(child).first(), icfg)
                if (!visited2.containsKey(child.signature)) visit(cg, child)
            }

        }
    }

    fun graphTraverse(startPoint: Unit, icfg: InterproceduralCFG<Unit, SootMethod>?, end_node: Unit? = null): Unit {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0) {
            println("Traversal complete")
            return startPoint
        } else {
            println(currentSuccessors)
            for (succ in currentSuccessors) {
                println("Succesor: $succ")
                var method: SootMethod? = null
                var methodEnd: Unit? = null
                try {
                    if (succ is JInvokeStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                    } else if (succ is JAssignStmt) {
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                    }
                    if (method != null) {
                        val methodStart = icfg.getStartPointsOf(method).first()
                        dotIcfg!!.drawEdge(succ.toString(), methodStart.toString())
                        methodEnd = graphTraverse(methodStart, icfg)
                    }
                    //JGotoStmt
                    //JIfStmt
                } catch (e: Exception) {
                    println(e.message)
                }

                if (!visited.contains(succ)) {
                    if (end_node != null) dotIcfg!!.drawEdge(end_node.toString(), succ.toString())
                    else dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                    visited.add(succ!!)
                    if (methodEnd != null) graphTraverse(succ, icfg, methodEnd)
                    else graphTraverse(succ, icfg)
                } else {
                    dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                }
            }
        }
        return startPoint
    }

    fun getICFG(classpath: String): InterproceduralCFG<Unit, SootMethod>? {
        try {
            val args = arrayOf(
                "-w",
                "-pp",
                "-allow-phantom-refs",
                "-process-dir",
                classpath
            )
            if (javaPaths.last().toString() != File.pathSeparator) javaPaths += File.pathSeparator
            Scene.v().sootClassPath = javaPaths + classpath

            Main.main(args)
            G.reset()
            return icfg
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}