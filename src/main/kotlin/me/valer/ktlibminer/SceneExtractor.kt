package me.valer.ktlibminer

import heros.InterproceduralCFG
import me.valer.ktlibminer.storage.DatabaseController
import me.valer.ktlibminer.storage.Jsonator
import soot.*
import soot.Unit
import soot.jimple.InvokeExpr
import soot.jimple.internal.AbstractStmt
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JInvokeStmt
import soot.jimple.spark.pag.PAG
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.options.Options


class SceneExtractor(var lib: String) {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    var allFullTraces = mutableListOf<MutableList<InvokeExpr>>(mutableListOf())

    var mainMethod: SootMethod? = null


    lateinit var analysis: PAG

    init {
        G.reset()
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
                        analysis = Scene.v().pointsToAnalysis as PAG

                        println("Entry Points are: ")
                        println(icfg!!.getStartPointsOf(mainMethod))

                        icfg!!.getStartPointsOf(mainMethod).forEach { startPoint ->
                            println("START POINT SET")
                            println(icfg!!.getSuccsOf(startPoint))
                            allFullTraces = mutableListOf(mutableListOf())

                            graphTraverseLib(startPoint)
                        }
                    } else {
                        println("App classes = " + Scene.v().applicationClasses.size)
                        println("Not a malware with main method")
                    }
                }
            }))
    }

    fun sequenceExtracting(allTraces: List<List<InvokeExpr>>): HashSet<List<InvokeExpr>> {
        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
        for (trace in allTraces) {
            if (trace.size < 2) continue
            val (traceStatic, traceCopy) = trace.partition { it.method.isStatic }

            val staticExtracted = staticExtracting(traceStatic.toMutableList())
            extractedTracesRet.addAll(staticExtracted)
            val invokeExtracted = invokeExtracting(traceCopy.toMutableList())
            extractedTracesRet.addAll(invokeExtracted)
        }
        return extractedTracesRet
    }

    private fun staticExtracting(traceStatic: MutableList<InvokeExpr>): HashSet<List<InvokeExpr>> {
        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
        while (traceStatic.isNotEmpty()) {
            val collector = mutableListOf<InvokeExpr>()
            val invokeClass = traceStatic.first().method.declaringClass
            for (statInvoke in traceStatic) {
                if (statInvoke.method.declaringClass == invokeClass) collector.add(statInvoke)
            }
            extractedTracesRet.add(collector)
            traceStatic.removeAll(collector)
        }
        return extractedTracesRet
    }

    private fun invokeExtracting(traceCopy: MutableList<InvokeExpr>): HashSet<List<InvokeExpr>> {
        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
        while (traceCopy.size > 1) {
            var collector = mutableListOf<InvokeExpr>()
            val firstCallInd = 0
            var secondCallInd = 1
            var obj1 = traceCopy[firstCallInd]
            var obj2 = traceCopy[secondCallInd]
            while (traceCopy.size > 1) {

                val obj1PT = getPointsTo(obj1)
                val obj2PT = getPointsTo(obj2)

                val resAlias = obj1PT.hasNonEmptyIntersection(obj2PT)
                val sameClass = obj1.method.declaringClass == obj2.method.declaringClass

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
        ttl: Int = 1000,
        isMethod: Boolean = false
    ) {
        //println(icfg?.getPredsOf(startPoint))
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        //val  method = icfg!!.getMethodOf(startPoint)
        //println("PARENT = $startPoint NAME = ${method.signature} METHOD = $isMethod SUCCS SIZE = ${currentSuccessors.size}")
        if (currentSuccessors.size == 0 || ttl <= 0) {
            if (!isMethod) {
                val traceIterator = allFullTraces.iterator()
                while (traceIterator.hasNext()) {
                    val trace = traceIterator.next()
                    if (trace.isNotEmpty()) {
                        println(trace)
                        extractSoloWay(trace)
                        traceIterator.remove()
                    }
                }
            }
            return
        } else {
            //println("List of sucs: $currentSuccessors       len: ${currentSuccessors.size}")
            val traceOrig = allFullTraces.last().toMutableList()
            for (succ in currentSuccessors) {
                //println("Succesor: $succ        Class: ${icfg!!.getMethodOf(succ).signature}")
                var method: SootMethod? = null
                try {
                    if (succ is JInvokeStmt || succ is JAssignStmt) {
                        succ as AbstractStmt
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (succ.invokeExpr.method.declaringClass.toString().startsWith(lib)) allFullTraces.last()
                            .add(succ.invokeExpr)
                    }
                    if (method != null && method.declaringClass in Scene.v().applicationClasses) {
                        val methodStart = icfg!!.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, ttl - 1, true)
                    }

                } catch (_: Exception) {
                }
                graphTraverseLib(succ, ttl-1, isMethod)
                if (currentSuccessors.indexOf(succ) != currentSuccessors.size - 1) allFullTraces.add(traceOrig)
            }
        }
    }


    fun extractSoloWay(trace: List<InvokeExpr>) {
        trace.forEach { invoke ->
            DatabaseController.addMethod(
                invoke.method.name,
                invoke.method.declaringClass.toString().replace(".", "+"),
            )
        }

        val extractedTraces = sequenceExtracting(listOf(trace)).filter { it.size > 1 }.toHashSet()
        extractedTraces.forEach {
            // println(it)
            val indicator = it.first()
            var inpClass = indicator.method.declaringClass.toString().replace(".", "+")
            if (indicator.method.isStatic) inpClass += "__s"
            val jsonData = Jsonator.traceToJson(it)
            //println(jsonData)
            DatabaseController.addTrace(jsonData!!, inpClass)
        }

    }

    fun runAnalyze(classpath: String): Boolean {
        try {
            Options.v().set_prepend_classpath(true)
            Options.v().set_whole_program(true)
            Options.v().set_allow_phantom_refs(true)
            Options.v().set_src_prec(2)
            //Options.v().set_soot_classpath(listOf(classpath).joinToString(";"))
            Options.v().set_process_dir(listOf(classpath))
            //Options.v().set_output_format(Options.output_format_jimple);
            Options.v().setPhaseOption("cg.spark", "enabled:true")
            Options.v().setPhaseOption("cg.spark", "verbose:true")
            Scene.v().loadBasicClasses()
            Scene.v().loadNecessaryClasses()
            PackManager.v().runPacks().runCatching { }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } catch (e: RuntimeException) {
            e.printStackTrace()
            return false
        }
    }

    private fun getPointsTo(inv: InvokeExpr): PointsToSet {
        return analysis.reachingObjects(inv.useBoxes[0].value as Local)
    }
}