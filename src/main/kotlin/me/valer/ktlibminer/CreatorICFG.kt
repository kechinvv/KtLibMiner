package me.valer.ktlibminer

import heros.InterproceduralCFG
import soot.*
import soot.Unit
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import java.io.File

object CreatorICFG {
    var icfg: InterproceduralCFG<Unit, SootMethod>? = null

    /**
     * For stable work need to set path to jt.jar manually
     **/
    var javaPaths = ""

    init {
        val sep = File.separator
        val pathSep = File.pathSeparator
        javaPaths = System.getProperty("java.home") + sep + "jre" + sep + "lib" + sep + "rt.jar" + pathSep

        if (!PackManager.v().hasPack("wjtp.ifds")) PackManager.v().getPack("wjtp")
            .add(Transform("wjtp.ifds", object : SceneTransformer() {
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
                    } else println("Not a malware with main method")
                }
            }))
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