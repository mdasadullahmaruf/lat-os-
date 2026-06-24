package com.mdasadullahmaruf.latos

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Online search gateway
 * Uses DuckDuckGo Instant Answer API (privacy-focused, no tracking)
 * 
 * TODO: Add SearXNG self-hosted option
 */
class SearchGateway {

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        try {
            // DuckDuckGo Instant Answer API
            val url = URL("https://api.duckduckgo.com/?q=${query.replace(" ", "+")}&format=json&no_html=1&skip_disambig=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "LatOS/1.0")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            // Extract abstract (summary)
            val abstract = json.optString("Abstract", "")
            val abstractText = json.optString("AbstractText", "")

            if (abstract.isNotEmpty()) {
                abstract
            } else if (abstractText.isNotEmpty()) {
                abstractText
            } else {
                "No direct answer found. Try: https://duckduckgo.com/?q=${query.replace(" ", "+")}"
            }

        } catch (e: Exception) {
            "Search failed: ${e.message}"
        }
    }
}
