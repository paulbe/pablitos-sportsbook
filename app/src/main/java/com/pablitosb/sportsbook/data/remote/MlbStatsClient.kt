package com.pablitosb.sportsbook.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class MlbStatsClient(
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
                throw IOException("MLB Stats API ${response.code} for $url")
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("Empty MLB Stats API body")
            return JSONObject(body)
        }
    }

    companion object {
        const val BASE = "https://statsapi.mlb.com"
        const val USER_AGENT = "PablitosSportsbook/1.1 (personal; Android)"

        val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()
    }
}

internal fun JSONObject.optObj(key: String): JSONObject? =
    if (has(key) && !isNull(key) && opt(key) is JSONObject) getJSONObject(key) else null

internal fun JSONObject.optArr(key: String): JSONArray =
    if (has(key) && !isNull(key) && opt(key) is JSONArray) getJSONArray(key) else JSONArray()

internal fun JSONArray.toObjList(): List<JSONObject> =
    buildList {
        for (i in 0 until length()) {
            val value = opt(i)
            if (value is JSONObject) add(value)
        }
    }

internal fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return when (val raw = opt(key)) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull()
        else -> null
    }
}

internal fun JSONObject.optFloatish(key: String): Float? {
    if (!has(key) || isNull(key)) return null
    return when (val raw = opt(key)) {
        is Number -> raw.toFloat()
        is String -> raw.replace("%", "").toFloatOrNull()
        else -> null
    }
}
