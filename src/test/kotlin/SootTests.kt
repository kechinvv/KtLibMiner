import heros.IFDSTabulationProblem
import heros.InterproceduralCFG
import heros.solver.IFDSSolver
import org.junit.jupiter.api.Test
import soot.*
import soot.Unit
import soot.jimple.toolkits.callgraph.CallGraph
import soot.jimple.toolkits.ide.exampleproblems.IFDSLocalInfoFlow
import soot.jimple.toolkits.ide.exampleproblems.IFDSReachingDefinitions
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.options.Options
import soot.util.dot.DotGraph
import java.util.*


class SootTests {

    var visited = ArrayList<Unit>()
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null
    var argsList = ArrayList<String>()
    var dotIcfg: DotGraph? = null


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
        //for (Integer i = 0; i < 2; i++) {
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
                    startPoint?.let { graphTraverse(it, icfg!!) }
                    dotIcfg!!.plot(mainClass.toString())
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


                                //val problem = IFDSReachingDefinitions(icfg)

                                //val solver = IFDSSolver(problem)

                                //println("Starting solver")
                                //solver.solve()
                                //println("Done")

                                var startPoint: Unit? = null
                                println(icfg!!.getStartPointsOf(main_method))
                                for (temp in icfg!!.getStartPointsOf(main_method)) {
                                    startPoint = temp
                                    println("START POINT SET")
                                    println(icfg!!.getSuccsOf(temp))
                                    break
                                }

                                // System.out.println(icfg.getSuccsOf(startPoint));
                                val visited = ArrayList<Unit>()
                                G.v().out.println(
                                    (mainClass.toString())
                                )
                                if (startPoint != null) graphTraverse(startPoint!!, icfg!!)
                                dotIcfg!!.plot(mainClass.toString())
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

    fun graphTraverse(startPoint: Unit, n_icfg: InterproceduralCFG<Unit, SootMethod>) {
        val currentSuccessors = n_icfg.getSuccsOf(startPoint)
        if (currentSuccessors.size == 0) {
            println("Traversal complete")
            return
        } else {
            for (succ in currentSuccessors) {
                println("Succesor: $succ")
                if (!visited.contains(succ)) {
                    dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                    visited.add(succ)
                    graphTraverse(succ, n_icfg)
                } else {
                    dotIcfg!!.drawEdge(startPoint.toString(), succ.toString())
                }
            }
        }
    }
}