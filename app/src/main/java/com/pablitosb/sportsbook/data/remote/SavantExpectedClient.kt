package com.pablitosb.sportsbook.data.remote

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Baseball Savant public expected-statistics CSV (no key).
 *
 * `GET https://baseballsavant.mlb.com/leaderboard/expected_statistics?type=pitcher&year=YYYY&min=1&csv=true`
 *
 * Pitcher rows: `player_id` = MLBAM id, `est_woba` = season xwOBA **against**
 * (lower is better). We never invent a value — missing / failed fetch → null.
 */
class SavantExpectedClient(
    private val http: OkHttpClient = client,
) {
    @Volatile
    private var cacheYear: Int? = null

    @Volatile
    private var cache: Map<Int, Float> = emptyMap()

    fun pitcherXwoba(season: Int): Map<Int, Float> {
        val hit = cache
        if (cacheYear == season && hit.isNotEmpty()) return hit
        return try {
            val body = getCsv(season)
            val parsed = parse(body)
            if (parsed.isNotEmpty()) {
                cacheYear = season
                cache = parsed
            }
            parsed
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getCsv(season: Int): String {
        val url =
            "https://baseballsavant.mlb.com/leaderboard/expected_statistics?type=pitcher&year=$season&position=&team=&min=1&csv=true"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/csv,text/plain,*/*")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Savant HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    companion object {
        const val USER_AGENT = "PablitosSportsbook/1.5 (personal; Android)"
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()

        fun parse(csv: String): Map<Int, Float> {
            val lines = csv.lineSequence().filter { it.isNotBlank() }.toList()
            if (lines.size < 2) return emptyMap()
            val header = parseCsvLine(lines.first().trim().removePrefix("\uFEFF"))
            val idIdx = header.indexOfFirst { it.equals("player_id", true) }
            val xIdx = header.indexOfFirst { it.equals("est_woba", true) || it.equals("xwoba", true) }
            if (idIdx < 0 || xIdx < 0) return emptyMap()
            return lines.drop(1).mapNotNull { line ->
                val cols = parseCsvLine(line)
                val id = cols.getOrNull(idIdx)?.toIntOrNull() ?: return@mapNotNull null
                val x = cols.getOrNull(xIdx)?.toFloatOrNull() ?: return@mapNotNull null
                if (x <= 0f || x >= 1f) return@mapNotNull null
                id to x
            }.toMap()
        }

        fun parseCsvLine(line: String): List<String> {
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            var quotes = false
            var i = 0
            while (i < line.length) {
                when (val c = line[i]) {
                    '"' -> {
                        if (quotes && i + 1 < line.length && line[i + 1] == '"') {
                            sb.append('"')
                            i++
                        } else {
                            quotes = !quotes
                        }
                    }
                    ',' -> if (quotes) sb.append(c) else {
                        out += sb.toString()
                        sb.clear()
                    }
                    else -> sb.append(c)
                }
                i++
            }
            out += sb.toString()
            return out
        }
    }
}
