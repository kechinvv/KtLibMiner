package me.valer.ktlibminer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.valer.ktlibminer.repository.RemoteRepository

class Finder(var lib: String, var goal: Int) {
    private var page = 1
    private var lbound = 0
    private var rbound = 50
    val maxBound = 10000
    var total = 0

    // private val linkSC = "https://searchcode.com/api/codesearch_I/"
    private val linkGH = "https://api.github.com/search/code"
    private val token = javaClass.getResource("/token.txt")!!.readText().trim()

    fun findReps(): MutableList<RemoteRepository> {
        var res = mutableListOf<RemoteRepository>()
        if (page > 10) {
            nextBounds()
            page = 1
        }
        while (true) {
            if (lbound >= maxBound || total >= goal) return res
            try {
                res = makeRequest()
                page++
                break
            } catch (e: GithubException) {
                when (e.state) {
                    State.EMPTY -> {
                        nextBounds()
                        page = 1
                    }
                    State.ABORTED -> Thread.sleep(30_000)
                    State.OVER -> rbound -= (rbound - lbound) / 2
                }
            }
        }
        return res
    }


    private fun nextBounds() {
        val delta = (rbound - lbound) * 2
        lbound = rbound
        rbound += delta
    }

    private fun makeRequest(): MutableList<RemoteRepository> {
        val response = khttp.get(
            url = linkGH,
            params = mapOf(
                "q" to "import+$lib in:file language:kotlin size:$lbound..$rbound",
                "per_page" to "100",
                "page" to page.toString()
            ),
            //params = mapOf("q" to lib, "in" to "file", "language" to "kotlin"),
            headers = mapOf("Authorization" to "Token $token")
        )
        println(response.text)
        return extractData(response.text)
    }


    private fun extractData(response: String): MutableList<RemoteRepository> {
        val json = JsonParser.parseString(response).asJsonObject
        if (json.has("message")) throw GithubException(State.ABORTED)
        if (json.get("total_count").asInt > 1000 && (rbound - lbound > 1)) throw GithubException(State.OVER)
        val items = json.getAsJsonArray("items")
        if (items.size() == 0) throw GithubException(State.EMPTY)
        val reps = mutableListOf<RemoteRepository>()
        for (item in items) {
            reps.add(RemoteRepository((item as JsonObject).get("repository") as JsonObject))
        }
        //println(reps)
        //print(items.size())
        total += items.size()
        return reps
    }
}

class GithubException(val state: State) : Exception()
enum class State { ABORTED, OVER, EMPTY }

/**
fun findSC() {
val response = khttp.get(
url = linkSC,
params = mapOf("q" to lib, "lan" to "145", "p" to page.toString(), "per_page" to "100", "src" to "2")
)
println(response)
}
 **/