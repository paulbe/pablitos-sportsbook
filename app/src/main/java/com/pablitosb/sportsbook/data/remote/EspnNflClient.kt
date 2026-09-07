package com.pablitosb.sportsbook.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class EspnNflClient(
    private val client: OkHttpClient = defaultClient,
) {
    fun getJson(pathAndQuery: String): JSONObject {
        val url = if (pathAndQuery.startsWith("http")) pathAndQuery else BASE + pathAndQuery
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("ESPN NFL ${response.code} for $url")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("Empty ESPN NFL body")
            return JSONObject(body)
        }
    }

    companion object {
        const val BASE = "https://site.web.api.espn.com"
        const val USER_AGENT = "PablitosSportsbook/1.23 (personal; Android)"

        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
