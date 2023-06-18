package unit.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.repository.RemoteRepository
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.io.path.Path
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

val repsTest = mapOf(
    "jar" to Pair("https://github.com/kechinvv/KtLibMiner", "kechinvv/KtLibMiner"),
    "archive" to Pair("https://github.com/kechinvv/libminer_test", "kechinvv/libminer_test"),
    "zip" to Pair("https://github.com/kechinvv/BabyBlockchain", "kechinvv/BabyBlockchain"),
    "simple" to Pair("https://github.com/kechinvv/ModelViewer", "kechinvv/ModelViewer")
)


class RemoteRepTest {
    val client = OkHttpClient()

    @Test
    fun testJsonConstructor() {
        repsTest.forEach { (_, v) ->
            val tempMap = mapOf("html_url" to v.first, "full_name" to v.second)
            val json = JsonParser.parseString(Gson().toJson(tempMap)).asJsonObject
            val rep = RemoteRepository(json, client)
            assertTrue { rep.url == v.first && rep.name == v.second }
        }
    }


    @Test
    fun testHasJar() {
        repsTest.forEach { (k, v) ->
            val rep = RemoteRepository(v.first, v.second, client)
            if (k == "jar") assertTrue { rep.hasJar() }
            else assertFalse { rep.hasJar() }
        }
    }

    @Test
    fun testGetAssets() {
        repsTest.forEach { (k, v) ->
            val rep = RemoteRepository(v.first, v.second, client)
            val assets = rep.getAssets()
            if (k == "jar" || k == "zip") assertNotNull(assets)
            else assertNull(assets)
        }
    }

    @Test
    fun testClone() {
        repsTest.forEach { (_, v) ->
            assertDoesNotThrow {
                val rep = RemoteRepository(v.first, v.second, client)
                val local = rep.cloneTo(Path("src/test/resources/reps"))
                assertTrue { local.delete() }
            }
        }
    }


    companion object {
        @JvmStatic
        @BeforeAll
        fun setupConf(): Unit {
            Configurations.ghToken = System.getenv("token")
        }
    }
}