package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.projections.SlateGame
import com.pablitosb.sportsbook.data.remote.MlbStatsClient
import com.pablitosb.sportsbook.data.remote.optArr
import com.pablitosb.sportsbook.data.remote.optIntOrNull
import com.pablitosb.sportsbook.data.remote.optObj
import com.pablitosb.sportsbook.data.remote.toObjList
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.LocalTime
import java.time.ZoneId
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

enum class DfsSlateKind { MAIN, EARLY, LATE, SHOWDOWN, IMPORTED }

enum class DfsSlateOrigin { FANDUEL_LIVE, MLB_DERIVED, IMPORTED }

data class DfsSlateOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val gamePks: Set<Int>,
    val kind: DfsSlateKind,
    val origin: DfsSlateOrigin,
    val gameCount: Int,
)

sealed class FdApiResult {
    data class Live(val slates: List<DfsSlateOption>) : FdApiResult()
    data object AuthRequired : FdApiResult()
    data class Unavailable(val detail: String) : FdApiResult()
}

/**
 * FanDuel DFS listing client.
 *
 * `GET https://api.fanduel.com/fixture-lists` is the stable JSON the unofficial
 * clients use, but it returns 401 without a user session. We try it every load
 * and never invent live FanDuel salaries when it fails.
 */
class FdSlateClient(
    private val http: OkHttpClient = MlbStatsClient.defaultClient,
) {
    fun tryListMlbSlates(): FdApiResult {
        return try {
            val request = Request.Builder()
                .url("https://api.fanduel.com/fixture-lists")
                .header("Accept", "application/json")
                .header("User-Agent", MlbStatsClient.USER_AGENT)
                .build()
            http.newCall(request).execute().use { response ->
                when (response.code) {
                    401, 403 -> FdApiResult.AuthRequired
                    in 200..299 -> parse(response.body?.string().orEmpty())
                    else -> FdApiResult.Unavailable("FanDuel HTTP ${response.code}")
                }
            }
        } catch (e: Exception) {
            FdApiResult.Unavailable(e.message ?: "FanDuel request failed")
        }
    }

    private fun parse(body: String): FdApiResult {
        if (body.isBlank()) return FdApiResult.Unavailable("Empty FanDuel body")
        return try {
            val json = JSONObject(body)
            val lists = json.optArr("fixture_lists").toObjList()
            val mlb = lists.mapNotNull { row -> toOption(row) }
            if (mlb.isEmpty()) FdApiResult.Unavailable("No MLB fixture lists in FanDuel payload")
            else FdApiResult.Live(mlb)
        } catch (e: Exception) {
            FdApiResult.Unavailable("Couldn’t parse FanDuel fixture-lists")
        }
    }

    private fun toOption(row: JSONObject): DfsSlateOption? {
        val sport = row.optString("sport").ifBlank {
            row.optObj("competition")?.optString("sport").orEmpty()
        }
        if (sport.isNotBlank() && !sport.contains("MLB", true) && !sport.contains("baseball", true)) {
            return null
        }
        val id = row.optString("id").ifBlank { row.optIntOrNull("id")?.toString() } ?: return null
        val label = row.optString("label").ifBlank { row.optString("description") }.ifBlank { "FanDuel slate" }
        val kind = when {
            label.contains("early", true) -> DfsSlateKind.EARLY
            label.contains("late", true) || label.contains("night", true) -> DfsSlateKind.LATE
            label.contains("showdown", true) || label.contains("single", true) -> DfsSlateKind.SHOWDOWN
            else -> DfsSlateKind.MAIN
        }
        val fixtures = row.optArr("fixtures").toObjList()
        val pks = fixtures.mapNotNull { it.optIntOrNull("id") ?: it.optIntOrNull("game_id") }.toSet()
        return DfsSlateOption(
            id = "fd-$id",
            title = label,
            subtitle = "FanDuel live · ${pks.size.coerceAtLeast(fixtures.size)} game(s)",
            gamePks = pks,
            kind = kind,
            origin = DfsSlateOrigin.FANDUEL_LIVE,
            gameCount = pks.size.coerceAtLeast(fixtures.size),
        )
    }
}

object MlbSlateBuilder {
    private val earlyCutoff = LocalTime.of(17, 0)

    fun build(
        games: List<SlateGame>,
        zone: ZoneId = StartersRepository.SLATE_ZONE,
    ): List<DfsSlateOption> {
        val live = games.filter { !it.postponed }
        if (live.isEmpty()) return emptyList()
        val early = live.filter { localTime(it, zone)?.isBefore(earlyCutoff) == true }
        val late = live.filter { localTime(it, zone)?.let { t -> !t.isBefore(earlyCutoff) } != false && localTime(it, zone) != null }
        val unknown = live.filter { localTime(it, zone) == null }
        val out = mutableListOf<DfsSlateOption>()
        out += option("main", "Main", "All ${live.size} games today · classic $35k", live, DfsSlateKind.MAIN)
        if (early.size in 1 until live.size) {
            out += option("early", "Early", "${early.size} afternoon game(s) before 5pm PT", early, DfsSlateKind.EARLY)
        }
        val lateGames = (late + unknown).distinctBy { it.gamePk }
        if (lateGames.size in 1 until live.size) {
            out += option("late", "Late", "${lateGames.size} evening game(s)", lateGames, DfsSlateKind.LATE)
        }
        live.forEach { game ->
            val title = "Showdown · ${game.awayAbbr} @ ${game.homeAbbr}"
            out += DfsSlateOption(
                id = "sd-${game.gamePk}",
                title = title,
                subtitle = listOf(game.gameTimeLabel, "classic 9-spot on one game (not FD MVP format)")
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                gamePks = setOf(game.gamePk),
                kind = DfsSlateKind.SHOWDOWN,
                origin = DfsSlateOrigin.MLB_DERIVED,
                gameCount = 1,
            )
        }
        return out
    }

    private fun option(
        id: String,
        title: String,
        subtitle: String,
        games: List<SlateGame>,
        kind: DfsSlateKind,
    ) = DfsSlateOption(
        id = id,
        title = title,
        subtitle = subtitle,
        gamePks = games.map { it.gamePk }.toSet(),
        kind = kind,
        origin = DfsSlateOrigin.MLB_DERIVED,
        gameCount = games.size,
    )

    private fun localTime(game: SlateGame, zone: ZoneId): LocalTime? {
        val instant = game.startUtc ?: return null
        return instant.atZone(zone).toLocalTime()
    }

    fun defaultId(options: List<DfsSlateOption>): String =
        options.firstOrNull { it.kind == DfsSlateKind.MAIN }?.id
            ?: options.firstOrNull()?.id
            ?: "main"
}
