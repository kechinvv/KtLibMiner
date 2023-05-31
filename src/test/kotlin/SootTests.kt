import me.valer.ktlibminer.analysis.SceneExtractor
import me.valer.ktlibminer.inference.FSMInference
import me.valer.ktlibminer.storage.DatabaseController
import mint.app.Mint
import org.junit.jupiter.api.Test


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

    @Test
    fun testInf() {
        DatabaseController.initDB()
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        // val classpath = "D:\\ktlibminertest\\reps\\xBrownieCodezV2_ClassFixer\\ClassFixer.jar"
        //val extractor = SceneExtractor("java.util.zip.ZipOutputStream")
        val extractor = SceneExtractor("java.io.File")
        //Configurations.kAlg = 1
        extractor.runAnalyze(classpath)
        DatabaseController.openConnection()
        //DatabaseController.clearError()
        FSMInference("D:/ktlibminertest/").inferenceAll()
        DatabaseController.closeConnection()
    }

}