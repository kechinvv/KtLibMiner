import heros.InterproceduralCFG
import me.valer.ktlibminer.CreatorICFG
import org.junit.jupiter.api.Test
import soot.*
import soot.Unit
import soot.jimple.internal.InvokeExprBox
import soot.jimple.internal.JInvokeStmt
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.callgraph.Sources
import soot.jimple.toolkits.callgraph.Targets
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.util.dot.DotGraph


class SootTests {

    var visited = ArrayList<Unit>()
    val visited2 = HashMap<String, Boolean>()
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    var argsList = ArrayList<String>()
    var dotIcfg: DotGraph? = null
    val endNodesStack: ArrayDeque<MutableList<Unit>>? = ArrayDeque()

    //@BeforeTest
    fun addPack() {
        PackManager.v().getPack("wjtp").add(Transform("wjtp.ifds", object : SceneTransformer() {
            override fun internalTransform(phaseName: String?, options: MutableMap<String, String>?) {
                if (Scene.v().hasMainClass()) {
                    println(Scene.v().hasMainClass())
                    var mainMethod: SootMethod? = null
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
                    val startPoint = icfg!!.getStartPointsOf(mainMethod).first()
                    println("START POINT SET")
                    println(icfg!!.getSuccsOf(startPoint))

                    // System.out.println(icfg.getSuccsOf(startPoint));
                    // G.v().out.println((mainClass.toString()))
                    //if (startPoint != null) graphTraverse(startPoint, icfg!!)
                    //println()
                    //visit(Scene.v().callGraph, main_method!!)
                } else println("Not a malware with main method")
            }
        }))
    }

//    @Test
//    fun testGraphCall() {
//        val classpath =
//            "C:\\Users\\valer\\IdeaProjects\\forkfgtest\\01\\kotlin\\main\\me\\valer\\ktlibminer"
//        val modulePath =
//            "C:\\Users\\valer\\IdeaProjects\\forkfgtest\\01\\kotlin\\main\\me\\valer\\ktlibminer"
//        val mainClass =
//            "C:\\Users\\valer\\IdeaProjects\\forkfgtest\\01\\kotlin\\main\\me\\valer\\ktlibminer\\MainKt.class"
//        val secClass =
//            "C:\\Users\\valer\\IdeaProjects\\forkfgtest\\01\\kotlin\\main\\me\\valer\\ktlibminer\\PrjBuilder.class"
//        val classpath2 = "C:\\Users\\valer\\IdeaProjects\\KtLibMiner\\build\\libs\\KtLibMiner-1.0-SNAPSHOT.jar"
//
//        // Options.v().set_soot_modulepath(modulePath)
//
//
//        // load classes from modules into Soot
//        // Here, getClassUnderModulePath() expects the module path to be set using the Options class as seen above
//
//
//        // load classes from modules into Soot
//        // Here, getClassUnderModulePath() expects the module path to be set using the Options class as seen above
////        val map = ModulePathSourceLocator.v().getClassUnderModulePath(modulePath)
////        for (module in map.keys) {
////            for (klass in map[module]!!) {
////                println(klass)
////                loadClass(klass, false, module)
////                // the loadClass() method is defined below
////            }
////        }
////
////        Scene.v().loadNecessaryClasses()
//        // Set Soot's internal classpath
//        //resetSoot()
//
//        Options.v().set_soot_classpath(classpath)
//        Options.v().set_process_dir(Collections.singletonList(classpath))
//        Options.v().set_prepend_classpath(true)
//        Options.v().set_src_prec(Options.src_prec_class)
//        // Enable whole-program mode
//
//        // Enable whole-program mode
//        Options.v().set_whole_program(true)
//        Options.v().set_app(true)
//
//
//        // Call-graph options
//
//        // Call-graph options
//        Options.v().setPhaseOption("cg", "safe-newinstance:true")
//        Options.v().setPhaseOption("cg.cha", "enabled:false")
//
//        // Enable SPARK call-graph construction
//
//        // Enable SPARK call-graph construction
//        Options.v().setPhaseOption("cg.spark", "enabled:true")
//        Options.v().setPhaseOption("cg.spark", "verbose:true")
//        Options.v().setPhaseOption("cg.spark", "on-fly-cg:true")
//
//        Options.v().set_allow_phantom_refs(true)
//
//        Options.v().set_output_format(Options.output_format_jimple)
//
//        // Set the main class of the application to be analysed
//
//        // Set the main class of the application to be analysed
//        Options.v().set_main_class("MainKt")
//
//        // Load the main class
//
//        // Load the main class
//        val c = Scene.v().loadClass("MainKt", SootClass.BODIES)
//        //val b = Scene.v().loadClass("PrjBuilder", SootClass.BODIES)
//        println(c.methods)
//        //println(b.methods)
//        c.setApplicationClass()
//
//        // Load the "main" method of the main class and set it as a Soot entry point
//
//        // Load the "main" method of the main class and set it as a Soot entry point
//        val entryPoint = c.getMethodByName("main")
//        val entryPoints: MutableList<SootMethod> = ArrayList()
//        entryPoints.add(entryPoint)
//        Scene.v().entryPoints = entryPoints
//
//        Scene.v().loadNecessaryClasses()
//
//        //PackManager.v().getPack("wjtp").add(Transform("wjtp.herosifds", IFDSDataFlowTransformer()))
//        //PackManager.v().getPack("cg").apply()
//        //PackManager.v().runPacks()
//
//        val testClass = Scene.v().getSootClass(mainClass)
//        val callGraph: CallGraph = Scene.v().callGraph
//        //val icg = InterproceduralCFG(callGraph)
//        //val view = ICFGDotVisualizer("out.dot", entryPoint, cg)
////        var numOfEdges = 0
////        for (sc in Scene.v().applicationClasses) {
////            for (m in sc.methods) {
////                val targets: Iterator<MethodOrMethodContext> = Targets(callGraph.edgesOutOf(m))
////                while (targets.hasNext()) {
////                    numOfEdges++
////                    val tgt = targets.next() as SootMethod
////                    println("$m may call $tgt")
////                }
////            }
////        }
////        println("Total edges: $numOfEdges")
//        //parseOutput(testClass, callGraph)
//
//        //runCGPack(mainClass)
//
//
//        //soot.Main.main(arrayOf())
//        //Main.main()
//    }


    @Test
    fun constructICFG() {
        val classpath1 =
            "C:\\Users\\valer\\IdeaProjects\\forkfgtest\\01\\kotlin\\main\\me\\valer\\ktlibminer"
        val classpath = "C:\\Users\\valer\\IdeaProjects\\KtLibMiner\\build\\libs\\KtLibMiner-1.0-SNAPSHOT.jar"
        try {
            dotIcfg = DotGraph("")
            argsList = ArrayList<String>()
            argsList.clear()
            argsList.addAll(
                listOf(
                    "-w",
                    "-pp",
                    "-allow-phantom-refs",
                    "-process-dir",
                    classpath
                )
            )
            //println("PRINTING THE VALUE OF I")
            //println(i.toString())
            // Options.v().set_prepend_classpath(true)
            Scene.v().sootClassPath =
                ("C:/Program Files/Java/jdk1.8.0_261/jre/lib/rt.jar;" +
                        "C:/Program Files/Java/jdk1.8.0_261/jre/lib/jce.jar;" +
                        "C:/Program Files/Java/jdk1.8.0_261/jre/lib/jsse.jar;$classpath")
            if (PackManager.v().hasPhase("wjtp.ifds")) {
                if (Scene.v().hasMainClass()) {
                    System.out.println(Scene.v().hasMainClass())

                    // Scene.v().loadNecessaryClasses();
                    // Scene.v().loadBasicClasses();
                    //
                    var mainClass = Scene.v().mainClass
                    var main_method: SootMethod? = null
                    // mainClass = Scene.v().mainClass
                    println("ALL CLAAAASSSSSSSSSSS")
                    System.out.println(Scene.v().classes)
                    var mainMethodFlag = 0
                    for (sc: SootClass in Scene.v().classes) {
                        // System.out.println(sc.getName());
                        if ((sc.name == mainClass.toString())) {
                            mainClass = sc
                            println("All methods inside are: ")
                            println(sc.methods.toString())
                            for (methods: SootMethod in sc.methods) {
                                println(methods.name)
                                if ((methods.name == "main")) {
                                    main_method = methods
                                    println("Main method found!. Terminating Search!")
                                    println(main_method.getName())
                                    mainMethodFlag = 1
                                    break
                                }
                            }
                            if (mainMethodFlag == 1) {
                                // indicates already found main method
                                println("Exiting Main Class search!")
                                break
                            }
                            // break the loop
                        }
                    }
                    println("Setting the Entry Point!")
                    val entry_points = ArrayList<SootMethod?>()
                    entry_points.add(main_method)
                    Scene.v().entryPoints = entry_points
                    println("Entry Points are: ")
                    System.out.println(Scene.v().entryPoints)
                    icfg = JimpleBasedInterproceduralCFG()
                    var startPoint: Unit? = null
                    System.out.println(icfg!!.getStartPointsOf(main_method))
                    for (temp in icfg!!.getStartPointsOf(main_method)) {
                        startPoint = temp
                        println("START POINT SET")
                        System.out.println(icfg!!.getSuccsOf(temp))
                        break
                    }

                    // System.out.println(icfg.getSuccsOf(startPoint));
                    visited = ArrayList<Unit>()
                    G.v().out.println(
                        (mainClass.toString())
                    )
                    // startPoint?.let { graphTraverse(it, icfg!!) }
                    // dotIcfg!!.plot(mainClass.toString())
                } else {
                    println("Not a malware with Main method...!")
                }
            } else {
                PackManager.v().getPack("wjtp")
                    .add(Transform("wjtp.ifds", object : SceneTransformer() {
                        override fun internalTransform(phaseName: String?, options: MutableMap<String, String>?) {

                            // System.out.println(b.toString());

                            // System.out.println(Scene.v().getClasses());

                            // Scene.v().loadClassAndSupport("Main");
                            if (Scene.v().hasMainClass()) {
                                println(Scene.v().hasMainClass())

                                // Scene.v().loadNecessaryClasses();
                                // Scene.v().loadBasicClasses();
                                //
                                var mainClass: SootClass? = null
                                var main_method: SootMethod? = null
                                mainClass = Scene.v().mainClass
                                var mainMethodFlag = 0
                                for (sc: SootClass in Scene.v().classes) {
                                    // System.out.println(sc.getName());
                                    if ((sc.name ==
                                                mainClass.toString())
                                    ) {
                                        mainClass = sc
                                        println("All methods inside are: ")
                                        println(
                                            sc.methods
                                                .toString()
                                        )
                                        for (methods: SootMethod in sc
                                            .methods) {
                                            println(
                                                methods
                                                    .name
                                            )
                                            if ((methods.name ==
                                                        "main")
                                            ) {
                                                main_method = methods
                                                println("Main method found!. Terminating Search!")
                                                println(
                                                    main_method!!
                                                        .name
                                                )
                                                mainMethodFlag = 1
                                                break
                                            }
                                        }
                                        if (mainMethodFlag == 1) {
                                            // indicates already found main
                                            // method
                                            println("Exiting Main Class search!")
                                            break
                                        }
                                        // break the loop
                                    }
                                }
                                println("Setting the Entry Point!")
                                val entry_points = ArrayList<SootMethod?>()
                                entry_points.add(main_method)
                                Scene.v().entryPoints = entry_points
                                println("Entry Points are: ")
                                System.out.println(
                                    Scene.v()
                                        .entryPoints
                                )
                                icfg = JimpleBasedInterproceduralCFG()

                                var startPoint: Unit? = null
                                println(icfg!!.getStartPointsOf(main_method))
                                for (temp in icfg!!.getStartPointsOf(main_method)) {
                                    startPoint = temp
                                    println("START POINT SET")
                                    println(icfg!!.getSuccsOf(temp))
                                    break
                                }

                                // System.out.println(icfg.getSuccsOf(startPoint));
                                G.v().out.println(
                                    (mainClass.toString())
                                )
                                // if (startPoint != null) graphTraverse(startPoint!!, icfg!!)
                                //println()
                                //visit(Scene.v().callGraph, main_method!!)
                            } else {
                                println("Not a malware with main method")
                                //G.reset();
                            }
                        }
                    }))
            }

            val args: Array<String> = argsList.toTypedArray()
            Main.main(args)
            G.reset()
            println("Processing Next Class!")
        } catch (e: Exception) {
            println("IN Ecetpsion")
            e.printStackTrace()
        }
        dotIcfg!!.plot("testg.dot")
    }


    @Test
    fun testCreatorICFG() {
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        // CreatorICFG.javaPaths = "C:/Program Files/Java/jdk1.8.0_261/jre/lib/rt.jar;"
        CreatorICFG.getICFG(classpath)
        //println(CreatorICFG.icfg)
        //dotIcfg = DotGraph("")
        // graphTraverse(CreatorICFG.startPoint, CreatorICFG.icfg)
        // CreatorICFG.mainMethod?.let { visit(CreatorICFG.callGraph, it) }
    }

    fun graphTraverse(startPoint: Unit, icfg: InterproceduralCFG<Unit, SootMethod>?) {
        val currentSuccessors = icfg!!.getSuccsOf(startPoint)
        val tempMethods = icfg.getCalleesOfCallAt(startPoint)


//        for (m in tempMethods) {
//            println(m.declaringClass)
//        }
        if (currentSuccessors.size == 0) {
            println("Traversal complete")
            return
        } else {
            for (succ in currentSuccessors) {
                println("Succesor: $succ")
                val useBoxes = succ.useBoxes
                //println("useBoxes: $useBoxes")
                try {
                    if (succ is JInvokeStmt) {
                        println(succ.invokeExpr.method)
                    }
                } catch (e: Exception) {
                    println(e.message)
                }

                if (!visited.contains(succ)) {
                    dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                    visited.add(succ!!)
                    graphTraverse(succ, icfg)
                } else {
                    dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                }
            }
        }
    }

    fun visit(cg: CallGraph, method: SootMethod) {
        val identifier = method.signature
        visited2[method.signature] = true
        dotIcfg!!.drawNode(identifier)
        // iterate over unvisited parents
//        val ptargets = Sources(cg.edgesInto(method))
//        while (ptargets.hasNext()) {
//            val parent = ptargets.next() as SootMethod
//            if (!visited2.containsKey(parent.signature)) visit(cg, parent)
//        }
        // iterate over unvisited children
        val ctargets: Iterator<MethodOrMethodContext> = Targets(cg.edgesOutOf(method))
        while (ctargets.hasNext()) {
            val child = ctargets.next() as SootMethod

            dotIcfg!!.drawEdge(identifier, child.signature)
            println("$method may call $child")
            if (!visited2.containsKey(child.signature)) visit(cg, child)

        }
    }

//    fun graphTraverse2(startPoint: Unit, n_icfg: InterproceduralCFG<Unit, SootMethod>) {
//        val currentSuccessors = n_icfg.getSuccsOf(startPoint)
//        println("current start point:")
//        println(startPoint)
//        println("current successors:")
//        println(currentSuccessors)
//        if (currentSuccessors.size == 0) {
//            if (endNodesStack.isNullOrEmpty()) {
//                println("Traversal complete")
//                return
//            } else {
//                val endNodes = endNodesStack.removeLast()
//                endNodes.forEach { endNode ->
//                    dotIcfg!!.drawEdge(startPoint.toString(), endNode.toString())
//                    graphTraverse(endNode, n_icfg)
//                }
//                return
//            }
//        } else {
//            val tempMethods =
//                n_icfg.getCalleesOfCallAt(startPoint)
//                    .filter { method -> Scene.v().mainClass.methods.contains(method) }
//            if (tempMethods.isNotEmpty()) {
//                println("Temp methods:")
//                println(tempMethods)
//                println(n_icfg.getCallersOf(tempMethods.first()))
//                tempMethods.forEach { temp_method ->
//                    val tempStartPoint = n_icfg.getStartPointsOf(temp_method).first()
//                    dotIcfg!!.drawEdge(startPoint.toString(), tempStartPoint.toString())
//                    visited.add(tempStartPoint)
//                    endNodesStack!!.addLast(currentSuccessors)
//                    graphTraverse(tempStartPoint, n_icfg)
//                }
//            } else {
//                for (succ in currentSuccessors) {
//                    println("Succesor: $succ")
//                    if (!visited.contains(succ)) {
//                        dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
//                        visited.add(succ)
//                        graphTraverse(succ, n_icfg)
//                    } else {
//                        dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
//                        if (!endNodesStack.isNullOrEmpty()) graphTraverse(succ, n_icfg)
//                    }
//                }
//            }
//
//        }
//    }
}