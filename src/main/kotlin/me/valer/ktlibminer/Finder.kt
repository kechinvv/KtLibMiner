package me.valer.ktlibminer

import com.google.gson.JsonParser

class Finder(var lib: String) {
    var page = 0
    var sizeLeftLim = 0
    var sizeRightLim = 10
    private val linkSC = "https://searchcode.com/api/codesearch_I/"
    private val linkGH = "https://api.github.com/search/code"
    private val token = javaClass.getResource("/token.txt")!!.readText().trim()

    fun findGH() {
        val response = khttp.get(
            url = linkGH,
            params = mapOf(
                "q" to "$lib in:file language:kotlin size:$sizeLeftLim..$sizeRightLim",
                "per_page" to "100",
                "page" to page.toString()
            ),
            //params = mapOf("q" to lib, "in" to "file", "language" to "kotlin"),
            headers = mapOf("Authorization" to "Token $token")
        )
        println(response.text)
        println(response.request.url)
        getRepsGH(response.text)
    }

/**
    fun findSC() {
        val response = khttp.get(
            url = linkSC,
            params = mapOf("q" to lib, "lan" to "145", "p" to page.toString(), "per_page" to "100", "src" to "2")
        )
        println(response)
    }
**/

    private fun getRepsGH(response: String) {
        val json = JsonParser.parseString(response).asJsonObject
        val items = json.getAsJsonArray("items");
        print(items.size())
    }
}