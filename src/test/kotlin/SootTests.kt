import heros.InterproceduralCFG
import me.valer.ktlibminer.SceneExtractor
import org.junit.jupiter.api.Test
import soot.*
import soot.Unit
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


    @Test
    fun testCreatorICFG() {
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        // CreatorICFG.javaPaths = "C:/Program Files/Java/jdk1.8.0_261/jre/lib/rt.jar;"
        SceneExtractor.runAnalyze(classpath)
        //println(CreatorICFG.icfg)
        //dotIcfg = DotGraph("")
        // graphTraverse(CreatorICFG.startPoint, CreatorICFG.icfg)
        // CreatorICFG.mainMethod?.let { visit(CreatorICFG.callGraph, it) }
    }


}