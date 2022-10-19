package me.valer.ktlibminer

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import me.valer.ktlibminer.repository.RemoteRepository

const val SLEEP: Long = 30_000
const val maxBound = 10000
const val maxPage = 10
const val maxRes = 1000
const val linkGH = "https://api.github.com/search/code"

class ProjectsSequence(val lib: String) : Sequence<RemoteRepository> {
    private var page = 1
    private var lbound = 0
    private var rbound = 50

    private var total = 0

    private val token = javaClass.getResource("/token.txt")!!.readText().trim()

    private val inner = generateSequence {
        if (lbound < maxBound) null
        else {
            if (page > maxPage) {
                nextBounds()
                page = 1
            }
            val response = makeRequest()
            val json = JsonParser.parseString(response).asJsonObject
            val reps = getReps(json)
            reps
        }
    }.flatten()

    private fun nextBounds() {
        val delta = (rbound - lbound) * 2
        lbound = rbound
        rbound += delta
    }

    private fun makeRequest(): String {
        return khttp.get(
            url = linkGH,
            params = mapOf(
                "q" to "import+$lib in:file language:kotlin size:$lbound..$rbound",
                "per_page" to "100",
                "page" to page.toString()
            ),
            headers = mapOf("Authorization" to "Token $token")
        ).text
    }

    private fun getReps(json: JsonObject): List<RemoteRepository> {
        val reps = mutableListOf<RemoteRepository>()
        if (json.has("message")) Thread.sleep(SLEEP)
        else if (json.get("total_count").asInt > maxRes && (rbound - lbound > 1)) rbound -= (rbound - lbound) / 2
        else {
            val items = json.getAsJsonArray("items")
            if (items.size() == 0) {
                nextBounds()
                page = 1
            } else {
                items.forEach { reps.add(RemoteRepository((it as JsonObject).get("repository") as JsonObject)) }
                total += reps.size
                page++
            }
        }
        return reps
    }

    override fun iterator(): Iterator<RemoteRepository> = inner.iterator()

}