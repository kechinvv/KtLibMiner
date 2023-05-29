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

    lateinit var icfg: InterproceduralCFG<Unit, SootMethod>
    lateinit var analysis: PAG
    var counter = 0

    fun runAnalyze(classpath: String): Boolean {
        try {
            init()
            Options.v().set_prepend_classpath(true)
            Options.v().set_whole_program(true)
            Options.v().set_allow_phantom_refs(true)
            Options.v().set_src_prec(2)
            Options.v().set_process_dir(listOf(classpath))
            Options.v().set_output_format(Options.output_format_jimple)
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
                        val mainClass = Scene.v().mainClass
                        val mainMethod = mainClass.getMethodByName("main")

                        Scene.v().entryPoints = arrayListOf(mainMethod)
                        icfg = JimpleBasedInterproceduralCFG()
                        analysis = Scene.v().pointsToAnalysis as PAG

                        val startPoints = icfg.getStartPointsOf(mainMethod)
                        println("Entry Points are: ")
                        println(startPoints)

                        startPoints.forEach { startPoint ->
                            graphTraverseLib(startPoint)
                        }
                        println(counter)
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
        isMethod: Boolean = false,
        trace: ArrayDeque<InvokeExpr> = ArrayDeque(),
        continueStack: ArrayDeque<Pair<Unit, Boolean>> = ArrayDeque()
    ) {
        val currentSuccessors = icfg.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0) {
            if (ttl <= 0 || !isMethod) {
                counter++
                if (counter % 50000 == 0) println(counter)
                //println("TTL = $ttl ; fullSize = ${trace.size}  ; counter = $counter")
                extractAndSave(trace)
            } else {
                val succInfo = continueStack.removeLast()
                graphTraverseLib(succInfo.first, ttl - 1, succInfo.second, trace, continueStack)
                continueStack.add(succInfo)
            }
        } else {
            for (succ in currentSuccessors) {
                var method: SootMethod? = null
                var invAdded = false
                var continueAdded = false
                try {
                    if (succ is JInvokeStmt || succ is JAssignStmt) {
                        succ as AbstractStmt
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (foundLib(succ.invokeExpr.method)) {
                            trace.add(succ.invokeExpr)
                            invAdded = true
                        }
                    }
                } catch (_: Exception) {
                }
                if (method != null && method.declaringClass in Scene.v().applicationClasses) {
                    continueStack.add(Pair(succ, isMethod))
                    continueAdded = true
                    icfg.getStartPointsOf(method).forEach { methodStart ->
                        graphTraverseLib(methodStart, ttl - 1, true, trace, continueStack)
                    }
                } else graphTraverseLib(succ, ttl - 1, isMethod, trace, continueStack)
                if (invAdded) trace.removeLast()
                if (continueAdded) continueStack.removeLast()
            }
        }
    }


    private fun foundLib(method: SootMethod): Boolean {
        return method.declaringClass.toString().startsWith("$lib.") || method.declaringClass.toString() == lib
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

    private fun staticExtracting(traceStatic: MutableList<InvokeExpr>): HashSet<MutableList<InvokeExpr>> {
        val extractedTracesRet = HashSet<MutableList<InvokeExpr>>()
        traceStatic.forEach { inv ->
            val invClass = inv.method.declaringClass
            var inserted = false
            extractedTracesRet.forEach {
                val sameClass = invClass == it.last().method.declaringClass
                if (sameClass) {
                    it.add(inv)
                    inserted = true
                }
            }
            if (!inserted) extractedTracesRet.add(mutableListOf(inv))
        }
        return extractedTracesRet
    }

    private fun invokeExtracting(traceDef: MutableList<InvokeExpr>): HashSet<MutableList<InvokeExpr>> {
        val extractedTracesRet = HashSet<MutableList<InvokeExpr>>()
        traceDef.forEach { inv ->
            val obj1PT = getPointsToSet(inv)
            val invClass = inv.method.declaringClass
            var inserted = false
            extractedTracesRet.forEach {
                val obj2PT = getPointsToSet(it.last())
                val sameClass = invClass == it.last().method.declaringClass
                if (obj1PT.hasNonEmptyIntersection(obj2PT) && sameClass) {
                    it.add(inv)
                    inserted = true
                }
            }
            if (!inserted) extractedTracesRet.add(mutableListOf(inv))
        }
        return extractedTracesRet
    }

    private fun getPointsToSet(inv: InvokeExpr): PointsToSet {
        return analysis.reachingObjects(inv.useBoxes[0].value as Local)
    }


    private fun extractAndSave(trace: List<InvokeExpr>) {
        trace.forEach { invoke ->
            val name = if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
            else invoke.method.signature.replace(' ', '+')
            val klass =
                if (invoke.method.isStatic) "${invoke.method.declaringClass}__s" else invoke.method.declaringClass.toString()
            DatabaseController.addMethod(
                name,
                klass
            )
        }

        val extractedTraces = sequenceExtracting(trace).filter { it.size > 1 }.toHashSet()
        extractedTraces.forEach {
            val indicator = it.first()
            var klass = indicator.method.declaringClass.toString()
            if (indicator.method.isStatic) klass += "__s"
            val jsonData = GsonBuilder().disableHtmlEscaping().create().toJson(it.map { invoke ->
                if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
                else invoke.method.signature.replace(' ', '+')
            })
            DatabaseController.addTrace(jsonData!!, klass)
        }

    }


}