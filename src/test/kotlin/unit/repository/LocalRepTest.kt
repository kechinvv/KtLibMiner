package unit.repository

import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.repository.LocalRepository
import me.valer.ktlibminer.repository.RemoteRepository
import okhttp3.OkHttpClient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.test.assertTrue

class LocalRepTest {
    val client = OkHttpClient()

    @Test
    fun testFindJarExist() {
        val rep = RemoteRepository(repsTest["jar"]!!.first, repsTest["jar"]!!.second, client)
        val local = rep.cloneTo(Path("src/test/resources/reps"))
        assertTrue { local.findJar().isNotEmpty() }
        assertTrue { local.delete() }
    }


    @Test
    fun testFindJarNotExist() {
        val rep = RemoteRepository(repsTest["zip"]!!.first, repsTest["zip"]!!.second, client)
        val local = rep.cloneTo(Path("src/test/resources/reps"))
        assertTrue { local.findJar().isEmpty() }
        assertTrue { local.delete() }
    }

    @Test
    fun testJarExist() {
        val rep = RemoteRepository(repsTest["jar"]!!.first, repsTest["jar"]!!.second, client)
        val local = rep.cloneTo(Path("src/test/resources/reps"))
        assertTrue { local.jar != null }
        assertTrue { local.delete() }
    }

    @Test
    fun testJarNotExist() {
        val rep = RemoteRepository(repsTest["zip"]!!.first, repsTest["zip"]!!.second, client)
        val local = rep.cloneTo(Path("src/test/resources/reps"))
        assertTrue { local.jar == null }
        assertTrue { local.delete() }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupConf(): Unit {
            Configurations.ghToken = System.getenv("token")
        }
    }
}