package me.valer.ktlibminer.analysis

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
                        println("Total traces analyzed = $counter")
                    } else {
                        println("Not a malware with main method")
                    }
                }
            }))
    }

    fun graphTraverseLib(
        startPoint: Unit,
        ttl: Int = Configurations.traversJumps,
        isMethod: Boolean = false,
        extracted: HashMap<String, MutableList<MutableList<InvokeExpr>>> = HashMap(),
        continueStack: ArrayDeque<Pair<Unit, Boolean>> = ArrayDeque(),
        depth: Int = Configurations.traversDepth
    ) {
        val currentSuccessors = icfg.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0 || ttl <= 0 || depth == 0) {
            if (ttl <= 0 || !isMethod) {
                counter++
                if (counter % 50000 == 0) println("Traces already analyzed... = $counter")
                save(extracted)
            } else {
                val succInfo = continueStack.removeLast()
                graphTraverseLib(succInfo.first, ttl - 1, succInfo.second, extracted, continueStack, depth + 1)
                continueStack.add(succInfo)
            }
        } else {
            for (succ in currentSuccessors) {
                var method: SootMethod? = null
                var continueAdded = false
                var klass: String? = null
                var addedIndex: Int? = null
                try {
                    if (succ is JInvokeStmt || succ is JAssignStmt) {
                        succ as AbstractStmt
                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
                            method = succ.invokeExpr.method
                        if (foundLib(succ.invokeExpr.method)) {
                            saveMethod(succ.invokeExpr)
                            val decl = succ.invokeExpr.method.declaringClass
                            klass = if (decl.isStatic) "${decl}__s" else decl.toString()
                            if (extracted[klass] == null) extracted[klass] = mutableListOf()
                            addedIndex = fillExtracted(succ.invokeExpr, extracted[klass]!!)
                        }
                    }
                } catch (_: Exception) {
                }
                if (method != null && method.declaringClass in Scene.v().applicationClasses) {
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


    private fun fillExtracted(invoke: InvokeExpr, extractedKlass: MutableList<MutableList<InvokeExpr>>): Int {
        return if (invoke.method.isStatic) {
            if (extractedKlass.size != 0) extractedKlass[0].add(invoke)
            else extractedKlass.add(mutableListOf(invoke))
            0
        } else {
            defaultExtracting(invoke, extractedKlass)
        }
    }

    private fun confiscate(index: Int, extractedKlass: MutableList<MutableList<InvokeExpr>>) {
        extractedKlass[index].removeLast()
        if (extractedKlass[index].isEmpty()) extractedKlass.removeAt(index)
    }

    private fun save(extracted: HashMap<String, MutableList<MutableList<InvokeExpr>>>) {
        extracted.forEach { (key, value) ->
            value.forEach inner@{
                if (it.size < 2) return@inner
                val jsonData = GsonBuilder().disableHtmlEscaping().create().toJson(it.map { invoke ->
                    if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
                    else invoke.method.signature.replace(' ', '+')
                })
                DatabaseController.addTrace(jsonData!!, key)
            }
        }
    }


    private fun defaultExtracting(invoke: InvokeExpr, extractedKlass: MutableList<MutableList<InvokeExpr>>): Int {
        val obj1PT = getPointsToSet(invoke)

        extractedKlass.forEachIndexed { index, it ->
            if (it.isEmpty()) return@forEachIndexed
            val obj2PT = getPointsToSet(it.last())
            if (obj1PT.hasNonEmptyIntersection(obj2PT)) {
                it.add(invoke)
                return index
            }
        }
        extractedKlass.add(mutableListOf(invoke))
        return extractedKlass.lastIndex

    }

    private fun saveMethod(invoke: InvokeExpr) {
        val name = if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
        else invoke.method.signature.replace(' ', '+')
        val klass =
            if (invoke.method.isStatic) "${invoke.method.declaringClass}__s" else invoke.method.declaringClass.toString()
        DatabaseController.addMethod(
            name,
            klass
        )
    }


    private fun getPointsToSet(inv: InvokeExpr): PointsToSet {
        return analysis.reachingObjects(inv.useBoxes[0].value as Local)
    }

    private fun foundLib(method: SootMethod): Boolean {
        return method.declaringClass.toString().startsWith("$lib.", true) ||
                method.declaringClass.toString().lowercase() == lib.lowercase()
    }

}
//    fun graphTraverseLib(
//        startPoint: Unit,
//        ttl: Int = Configurations.traversJumps,
//        isMethod: Boolean = false,
//        trace: ArrayDeque<InvokeExpr> = ArrayDeque(),
//        continueStack: ArrayDeque<Pair<Unit, Boolean>> = ArrayDeque()
//    ) {
//        val currentSuccessors = icfg.getSuccsOf(startPoint)
//        if (currentSuccessors.size == 0 || ttl <= 0) {
//            if (ttl <= 0 || !isMethod) {
//                counter++
//                if (counter % 50000 == 0) println(counter)
//                //println("TTL = $ttl ; fullSize = ${trace.size}  ; counter = $counter")
//                extractAndSave(trace)
//            } else {
//                val succInfo = continueStack.removeLast()
//                graphTraverseLib(succInfo.first, ttl - 1, succInfo.second, trace, continueStack)
//                continueStack.add(succInfo)
//            }
//        } else {
//            for (succ in currentSuccessors) {
//                var method: SootMethod? = null
//                var invAdded = false
//                var continueAdded = false
//                try {
//                    if (succ is JInvokeStmt || succ is JAssignStmt) {
//                        succ as AbstractStmt
//                        if (succ.invokeExpr.method.declaringClass in Scene.v().applicationClasses)
//                            method = succ.invokeExpr.method
//                        if (foundLib(succ.invokeExpr.method)) {
//                            saveMethod(succ.invokeExpr)
//                            trace.add(succ.invokeExpr)
//                            invAdded = true
//                        }
//                    }
//                } catch (_: Exception) {
//                }
//                if (method != null && method.declaringClass in Scene.v().applicationClasses) {
//                    continueStack.add(Pair(succ, isMethod))
//                    continueAdded = true
//                    icfg.getStartPointsOf(method).forEach { methodStart ->
//                        graphTraverseLib(methodStart, ttl - 1, true, trace, continueStack)
//                    }
//                } else graphTraverseLib(succ, ttl - 1, isMethod, trace, continueStack)
//                if (invAdded) trace.removeLast()
//                if (continueAdded) continueStack.removeLast()
//            }
//        }
//    }


//    private fun extractAndSave(trace: List<InvokeExpr>) {
//        val extractedTraces = sequenceExtracting(trace).filter { it.size > 1 }
//        save(extractedTraces)
//    }
//
//    private fun save(extracted: List<List<InvokeExpr>>) {
//        extracted.forEach {
//            if (it.size < 2) return@forEach
//            val indicator = it.first().method
//            var klass = indicator.declaringClass.toString()
//            if (indicator.isStatic) klass += "__s"
//            val jsonData = GsonBuilder().disableHtmlEscaping().create().toJson(it.map { invoke ->
//                if (Configurations.traceNode == TraceNode.NAME) invoke.method.name
//                else invoke.method.signature.replace(' ', '+')
//            })
//            DatabaseController.addTrace(jsonData!!, klass)
//        }
//    }


//    fun sequenceExtracting(trace: List<InvokeExpr>): HashSet<List<InvokeExpr>> {
//        val extractedTracesRet = HashSet<List<InvokeExpr>>(mutableListOf())
//        if (trace.size < 2) return extractedTracesRet
//
//        val (traceStatic, traceDef) = trace.partition { it.method.isStatic }
//
//        val staticExtracted = staticExtracting(traceStatic.toMutableList())
//        extractedTracesRet.addAll(staticExtracted)
//        val invokeExtracted = defaultExtracting(traceDef.toMutableList())
//        extractedTracesRet.addAll(invokeExtracted)
//
//        return extractedTracesRet
//    }


//    private fun staticExtracting(traceStatic: MutableList<InvokeExpr>): HashSet<MutableList<InvokeExpr>> {
//        val extractedTracesRet = HashSet<MutableList<InvokeExpr>>()
//        traceStatic.forEach { inv ->
//            val invClass = inv.method.declaringClass
//            var inserted = false
//            extractedTracesRet.forEach {
//                val sameClass = invClass == it.last().method.declaringClass
//                if (sameClass) {
//                    it.add(inv)
//                    inserted = true
//                }
//            }
//            if (!inserted) extractedTracesRet.add(mutableListOf(inv))
//        }
//        return extractedTracesRet
//    }
//
//    private fun defaultExtracting(traceDef: MutableList<InvokeExpr>): HashSet<MutableList<InvokeExpr>> {
//        val extractedTracesRet = HashSet<MutableList<InvokeExpr>>()
//        traceDef.forEach { inv ->
//            val obj1PT = getPointsToSet(inv)
//            val invClass = inv.method.declaringClass
//            var inserted = false
//            extractedTracesRet.forEach {
//                val obj2PT = getPointsToSet(it.last())
//                val sameClass = invClass == it.last().method.declaringClass
//                if (obj1PT.hasNonEmptyIntersection(obj2PT) && sameClass) {
//                    it.add(inv)
//                    inserted = true
//                }
//            }
//            if (!inserted) extractedTracesRet.add(mutableListOf(inv))
//        }
//        return extractedTracesRet
//    }

