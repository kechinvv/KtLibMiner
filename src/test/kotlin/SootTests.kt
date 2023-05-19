import heros.InterproceduralCFG
import me.valer.ktlibminer.SceneExtractor
import mint.app.Mint
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

    @Test
    fun testCreatorICFG() {
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        // CreatorICFG.javaPaths = "C:/Program Files/Java/jdk1.8.0_261/jre/lib/rt.jar;"
        val extractor = SceneExtractor("java.io.File")
        extractor.runAnalyze(classpath)
        //println(CreatorICFG.icfg)
        //dotIcfg = DotGraph("")
        // graphTraverse(CreatorICFG.startPoint, CreatorICFG.icfg)
        // CreatorICFG.mainMethod?.let { visit(CreatorICFG.callGraph, it) }
    }

    @Test
    fun testMint() {
        Mint.main(
            arrayOf(
                "-input",
                "C:\\Users\\valer\\IdeaProjects\\KtLibMiner\\src\\test\\resources\\MJExample2.txt",
                "-k",
                "2",
                "-visout",
                "C:/Users/valer/IdeaProjects/KtLibMiner/src/test/resources/test.txt"
            )
        )
    }


}