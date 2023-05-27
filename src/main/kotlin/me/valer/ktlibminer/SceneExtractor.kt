package me.valer.ktlibminer

import com.google.gson.GsonBuilder
import heros.InterproceduralCFG
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.config.TraceNode
import me.valer.ktlibminer.storage.DatabaseController
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
    var allFullTraces = mutableListOf<MutableList<InvokeExpr>>(mutableListOf())

    lateinit var icfg: InterproceduralCFG<Unit, SootMethod>
    lateinit var analysis: PAG

    fun runAnalyze(classpath: String): Boolean {
        try {
            init()
            Options.v().set_prepend_classpath(true)
            Options.v().set_whole_program(true)
            Options.v().set_allow_phantom_refs(true)
            Options.v().set_src_prec(2)
            Options.v().set_process_dir(listOf(classpath))
            Options.v().set_output_format(Options.output_format_jimple);
            Options.v().setPhaseOption("cg.spark", "enabled:true")
            Options.v().setPhaseOption("cg.spark", "verbose:true")
            Scene.v().loadBasicClasses()
            Scene.v().loadNecessaryClasses()
            PackManager.v().runPacks().runCatching { }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun init() {
        G.reset()
        if (!PackManager.v().hasPack("wjtp.ifds")) PackManager.v().getPack("wjtp")
            .add(Transform("wjtp.ifds", object : SceneTransformer() {
                override fun internalTransform(phaseName: String?, options: MutableMap<String, String>?) {
                    if (Scene.v().hasMainClass()) {
                        println(Scene.v().hasMainClass())
                        val mainClass = Scene.v().mainClass
                        val mainMethod = mainClass.getMethodByName("main")

                        Scene.v().entryPoints = arrayListOf(mainMethod)
                        icfg = JimpleBasedInterproceduralCFG()
                        analysis = Scene.v().pointsToAnalysis as PAG

                        val startPoints = icfg.getStartPointsOf(mainMethod)
                        println("Entry Points are: ")
                        println(startPoints)

                        startPoints.forEach { startPoint ->
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

    fun graphTraverseLib(
        startPoint: Unit,
        ttl: Int = Configurations.traversJumps,
        isMethod: Boolean = false
    ) {
        val currentSuccessors = icfg.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0) {
            if (!isMethod) saveTraces()
            return
        } else {
            val traceOrig = allFullTraces.last().toMutableList()
            for (succ in currentSuccessors) {
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
                        val methodStart = icfg.getStartPointsOf(method).first()
                        graphTraverseLib(methodStart, ttl - 1, true)
                    }
                } catch (_: Exception) {
                }
                graphTraverseLib(succ, ttl - 1, isMethod)
                if (currentSuccessors.indexOf(succ) != currentSuccessors.size - 1) allFullTraces.add(traceOrig)
            }
        }
    }

    fun sequenceExtracting(trace: List<InvokeExpr>): HashSet<List<InvokeExpr>> {
        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
        if (trace.size < 2) return extractedTracesRet

        val (traceStatic, traceDef) = trace.partition { it.method.isStatic }

        val staticExtracted = staticExtracting(traceStatic.toMutableList())
        extractedTracesRet.addAll(staticExtracted)
        val invokeExtracted = invokeExtracting(traceDef.toMutableList())
        extractedTracesRet.addAll(invokeExtracted)

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

    private fun invokeExtracting(traceDef: MutableList<InvokeExpr>): HashSet<List<InvokeExpr>> {
        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
        while (traceDef.size > 1) {
            var collector = mutableListOf<InvokeExpr>()
            val firstCallInd = 0
            var secondCallInd = 1
            var obj1 = traceDef[firstCallInd]
            var obj2 = traceDef[secondCallInd]
            while (traceDef.size > 1) {

                val obj1PT = getPointsToSet(obj1)
                val obj2PT = getPointsToSet(obj2)

                val resAlias = obj1PT.hasNonEmptyIntersection(obj2PT)
                val sameClass = obj1.method.declaringClass == obj2.method.declaringClass

                if (resAlias && sameClass) {
                    collector.add(obj1)
                    traceDef.remove(obj1)
                    obj1 = obj2
                    secondCallInd--
                }
                if (secondCallInd >= traceDef.size - 1) {
                    collector.add(obj1)
                    traceDef.remove(obj1)

                    extractedTracesRet.add(collector)
                    collector = mutableListOf()
                    if (traceDef.size > 1) {
                        obj1 = traceDef[firstCallInd]
                        secondCallInd = firstCallInd + 1
                        obj2 = traceDef[secondCallInd]
                    }
                } else {
                    secondCallInd++
                    obj2 = traceDef[secondCallInd]
                }
            }
        }
        return extractedTracesRet
    }

    private fun getPointsToSet(inv: InvokeExpr): PointsToSet {
        return analysis.reachingObjects(inv.useBoxes[0].value as Local)
    }

    fun saveTraces() {
        val traceIterator = allFullTraces.iterator()
        while (traceIterator.hasNext()) {
            val trace = traceIterator.next()
            if (trace.isNotEmpty()) {
                extractAndSave(trace)
                traceIterator.remove()
            }
        }
    }


    fun extractAndSave(trace: List<InvokeExpr>) {
        trace.forEach { invoke ->
            val name = if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
            else invoke.method.signature.replace(' ', '+')
            DatabaseController.addMethod(
                name,
                invoke.method.declaringClass.toString().replace(".", "+"),
            )
        }

        val extractedTraces = sequenceExtracting(trace).filter { it.size > 1 }.toHashSet()
        extractedTraces.forEach {
            val indicator = it.first()
            var inpClass = indicator.method.declaringClass.toString()
            if (indicator.method.isStatic) inpClass += "__s"
            val jsonData = GsonBuilder().disableHtmlEscaping().create().toJson(it.map { invoke ->
                if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
                else invoke.method.signature.replace(' ', '+')
            })
            DatabaseController.addTrace(jsonData!!, inpClass)
        }

    }


}