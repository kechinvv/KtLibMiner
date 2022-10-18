package me.valer.ktlibminer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.valer.ktlibminer.repository.RemoteRepository

const val SLEEP: Long = 30_000
const val maxBound = 10000
const val maxPage = 10
const val maxRes = 1000

class Finder(var lib: String, var goal: Int) {
    private var page = 1
    private var lbound = 0
    private var rbound = 50

    var total = 0

    private val linkGH = "https://api.github.com/search/code"
    private val token = javaClass.getResource("/token.txt")!!.readText().trim()

    fun findReps(): MutableList<RemoteRepository> {
        var reps = mutableListOf<RemoteRepository>()
        var response: String
        if (page > maxPage) {
            nextBounds()
            page = 1
        }
        while (true) {
            if (lbound >= maxBound || total >= goal) return reps
            try {
                response = makeRequest()
                reps = getRemoteReps(response)
                page++
                break
            } catch (e: GithubException) {
                when (e.state) {
                    State.EMPTY -> {
                        nextBounds()
                        page = 1
                    }
                    State.ABORTED -> Thread.sleep(SLEEP)
                    State.OVER -> rbound -= (rbound - lbound) / 2
                }
            }
        }
        return reps
    }


    private fun nextBounds() {
        val delta = (rbound - lbound) * 2
        lbound = rbound
        rbound += delta
    }

    private fun makeRequest(): String {
        val response = khttp.get(
            url = linkGH,
            params = mapOf(
                "q" to "import+$lib in:file language:kotlin size:$lbound..$rbound",
                "per_page" to "100",
                "page" to page.toString()
            ),
            headers = mapOf("Authorization" to "Token $token")
        )
        println(response.text)
        return response.text
    }


    private fun getRemoteReps(response: String): MutableList<RemoteRepository> {
        val json = JsonParser.parseString(response).asJsonObject
        if (json.has("message")) throw GithubException(State.ABORTED)
        if (json.get("total_count").asInt > maxRes && (rbound - lbound > 1)) throw GithubException(State.OVER)
        val items = json.getAsJsonArray("items")
        if (items.size() == 0) throw GithubException(State.EMPTY)

        val reps = mutableListOf<RemoteRepository>()
        for (item in items) {
            reps.add(RemoteRepository((item as JsonObject).get("repository") as JsonObject))
        }
        total += items.size()
        return reps
    }
}

class GithubException(val state: State) : Exception()
enum class State { ABORTED, OVER, EMPTY }

