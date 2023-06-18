package sys

import me.valer.ktlibminer.analysis.SceneExtractor
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.repository.LocalRepository
import me.valer.ktlibminer.repository.ProjectsSequence
import me.valer.ktlibminer.storage.DatabaseController
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FuncTest {

    val paths = mapOf(
        "all" to Path("D:\\lib_miner_test_all"),
        "jar" to Path("D:\\lib_miner_test_jar"),
        "maven_test" to Path("C:\\Users\\valer\\IdeaProjects\\forkfgtest\\ascclemens\\khttp"),
        "gradle_test" to Path("C:\\Users\\valer\\IdeaProjects\\forkfgtest\\tacomoon\\kotlin-http-client-dsl")
    )

    @Test
    fun downloadAllTest() {
        val n = 25
        Configurations.ghToken = System.getenv("token")
        Configurations.allProj = true

        val repsPath = paths["all"]
        Files.createDirectories(repsPath!!)

        download(n, repsPath)
        assertEquals(n, Files.list(repsPath).count().toInt())
    }


    @Test
    fun downloadJarTest() {
        val n = 25
        Configurations.ghToken = System.getenv("token")
        Configurations.allProj = false

        val repsPath = paths["jar"]
        Files.createDirectories(repsPath!!)

        download(n, repsPath)
        assertEquals(n, Files.list(repsPath).count().toInt())
    }

    @Test
    fun testBuildGradle() {
        Configurations.gradleVersion = "7.5.1"
        val prj =
            LocalRepository(paths["gradle_test"]!!, null)
        val res = prj.build()
        assertTrue { res.isNotEmpty() }
    }

    @Test
    fun testBuildMaven() {
        val prj = LocalRepository(paths["maven_test"]!!, null)
        val res = prj.build()
        assertTrue { res.isNotEmpty() }
    }

    @Test
    fun testInf() {
        DatabaseController.initDB()
        val classpath = "C:\\Users\\valer\\IdeaProjects\\libminer_test\\build\\libs\\libminer_test-1.0-SNAPSHOT.jar"
        val extractor = SceneExtractor("java.io.File")
        extractor.runAnalyze(classpath)
        DatabaseController.closeConnection()
    }

    private fun download(n: Int, repsPath: Path) {
        val analyzedPrjStorage = HashSet<String>()
        val client = OkHttpClient()
        val seq = ProjectsSequence("java.util.zip.ZipOutputStream", client)
        seq.filter { !analyzedPrjStorage.contains(it.name) && (it.hasJar() || Configurations.allProj) }.map {
            analyzedPrjStorage.add(it.name)
            it.cloneTo(Path(repsPath.toString(), it.name.replace('/', '_')))
        }.take(n).last()
        println(analyzedPrjStorage)
        println("Size = ${analyzedPrjStorage.size}")
    }
}