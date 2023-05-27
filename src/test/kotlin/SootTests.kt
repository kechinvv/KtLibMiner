import heros.InterproceduralCFG
import me.valer.ktlibminer.SceneExtractor
import me.valer.ktlibminer.storage.DatabaseController
import mint.app.Mint
import org.junit.jupiter.api.Test
import soot.*
import soot.Unit
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG
import soot.util.dot.DotGraph


class SootTests {

    @Test
    fun testCreatorICFG() {
        DatabaseController.initDB()
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        val extractor = SceneExtractor("java.io.File")
        extractor.runAnalyze(classpath)
        DatabaseController.closeConnection()
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