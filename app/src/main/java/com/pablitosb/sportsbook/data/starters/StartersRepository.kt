package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.remote.MlbStatsClient
import com.pablitosb.sportsbook.data.remote.optArr
import com.pablitosb.sportsbook.data.remote.optFloatish
import com.pablitosb.sportsbook.data.remote.optIntOrNull
import com.pablitosb.sportsbook.data.remote.optObj
import com.pablitosb.sportsbook.data.remote.toObjList
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class StartersBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val starters: List<Starter>,
    val sourceLabel: String = "Live · MLB",
)

class StartersLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class StartersRepository(
    private val api: MlbStatsClient = MlbStatsClient(),
    private val zone: ZoneId = SLATE_ZONE,
) {
    suspend fun loadToday(): StartersBoard = withContext(Dispatchers.IO) {
        val slate = LocalDate.now(zone)
        val fetchedAt = Instant.now()
        val abbrev = runCatching { loadTeamAbbrevs() }.getOrDefault(emptyMap())
        val raw = try {
            loadProbablePitchers(slate, abbrev)
        } catch (e: Exception) {
            throw StartersLoadException(
                "Couldn’t reach MLB Stats API for $slate. Check the connection and retry.",
                e,
            )
        }
        if (raw.isEmpty()) {
            return@withContext StartersBoard(
                slateDate = slate,
                fetchedAt = fetchedAt,
                starters = emptyList(),
            )
        }
        val season = slate.year
        val samples = fetchSamples(raw.map { it.mlbId }, season)
        val projected = raw.map { row ->
            val projection = OutlookCalculator.project(samples[row.mlbId])
            row to projection
        }.sortedWith(
            compareByDescending<Pair<RawStarter, OutlookCalculator.Projection>> { it.second.outlookScore }
                .thenByDescending { it.second.projK },
        )
        val starters = projected.mapIndexed { index, (row, projection) ->
            Starter(
                rank = index + 1,
                name = row.name,
                team = row.team,
                opponent = row.opponent,
                venue = row.venue,
                weather = row.weather,
                tempF = row.tempF,
                outlook = projection.outlook,
                outlookScore = projection.outlookScore,
                projKPct = projection.projK * 100f,
                nextStartKs = projection.nextStartKs,
                trend = projection.trend,
                gameTimeLabel = row.gameTimeLabel,
                mlbId = row.mlbId,
                homeAway = row.homeAway,
            )
        }
        StartersBoard(slateDate = slate, fetchedAt = fetchedAt, starters = starters)
    }

    private fun loadTeamAbbrevs(): Map<Int, String> {
        val json = api.getJson("/api/v1/teams?sportId=1")
        return json.optArr("teams").toObjList().mapNotNull { team ->
            val id = team.optIntOrNull("id") ?: return@mapNotNull null
            val abbr = team.optString("abbreviation").ifBlank { null } ?: return@mapNotNull null
            id to abbr
        }.toMap()
    }

    private fun loadProbablePitchers(slate: LocalDate, abbrev: Map<Int, String>): List<RawStarter> {
        val date = slate.toString()
        val json = api.getJson(
            "/api/v1/schedule?sportId=1&date=$date&hydrate=probablePitcher,venue,weather",
        )
        val dates = json.optArr("dates").toObjList()
        val games = dates.flatMap { it.optArr("games").toObjList() }
        val rows = mutableListOf<RawStarter>()
        for (game in games) {
            val venue = game.optObj("venue")?.optString("name").orEmpty()
            val weatherObj = game.optObj("weather")
            val condition = weatherObj?.optString("condition").orEmpty()
            val temp = weatherObj?.optString("temp")?.toIntOrNull() ?: 0
            val weather = if (condition.contains("sun", ignoreCase = true) ||
                condition.contains("clear", ignoreCase = true)
            ) {
                Weather.SUN
            } else {
                Weather.CLOUD
            }
            val gameTime = formatGameTime(game.optString("gameDate"))
            val teams = game.optObj("teams") ?: continue
            val home = teams.optObj("home")
            val away = teams.optObj("away")
            val homeId = home?.optObj("team")?.optIntOrNull("id")
            val awayId = away?.optObj("team")?.optIntOrNull("id")
            val homeAbbr = homeId?.let { abbrev[it] } ?: shortName(home?.optObj("team"))
            val awayAbbr = awayId?.let { abbrev[it] } ?: shortName(away?.optObj("team"))
            addPitcher(rows, away, awayAbbr, homeAbbr, venue, weather, temp, gameTime, "away")
            addPitcher(rows, home, homeAbbr, awayAbbr, venue, weather, temp, gameTime, "home")
        }
        return rows.distinctBy { it.mlbId to it.gameTimeLabel }
    }

    private fun addPitcher(
        out: MutableList<RawStarter>,
        side: JSONObject?,
        team: String,
        opponent: String,
        venue: String,
        weather: Weather,
        tempF: Int,
        gameTime: String,
        homeAway: String,
    ) {
        val pitcher = side?.optObj("probablePitcher") ?: return
        val id = pitcher.optIntOrNull("id") ?: return
        val name = pitcher.optString("fullName").ifBlank { return }
        out += RawStarter(
            mlbId = id,
            name = name,
            team = team,
            opponent = opponent,
            venue = venue.ifBlank { "TBD" },
            weather = weather,
            tempF = tempF,
            gameTimeLabel = gameTime,
            homeAway = homeAway,
        )
    }

    private suspend fun fetchSamples(
        ids: List<Int>,
        season: Int,
    ): Map<Int, OutlookCalculator.PitchingSample> = coroutineScope {
        val unique = ids.distinct()
        val gate = Semaphore(6)
        unique.map { id ->
            async {
                gate.withPermit {
                    id to runCatching { loadSample(id, season) }.getOrNull()
                }
            }
        }.awaitAll().mapNotNull { (id, sample) -> sample?.let { id to it } }.toMap()
    }

    private fun loadSample(id: Int, season: Int): OutlookCalculator.PitchingSample {
        val json = api.getJson(
            "/api/v1/people/$id/stats?stats=season,gameLog&group=pitching&season=$season",
        )
        val blocks = json.optArr("stats").toObjList()
        val seasonStat = blocks.firstOrNull { it.optObj("type")?.optString("displayName") == "season" }
            ?.optArr("splits")?.toObjList()?.firstOrNull()?.optObj("stat")
        val logs = blocks.firstOrNull { it.optObj("type")?.optString("displayName") == "gameLog" }
            ?.optArr("splits")?.toObjList().orEmpty()
            .filter { it.optObj("stat")?.optIntOrNull("gamesStarted") ?: 0 >= 1 }

        val seasonSo = seasonStat?.optIntOrNull("strikeOuts") ?: 0
        val seasonBf = seasonStat?.optIntOrNull("battersFaced") ?: 0
        val seasonGs = seasonStat?.optIntOrNull("gamesStarted") ?: 0
        val strikePct = seasonStat?.optFloatish("strikePercentage")

        val recent = logs.takeLast(RECENT_STARTS)
        val recentSo = recent.sumOf { it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0 }
        val recentBf = recent.sumOf { it.optObj("stat")?.optIntOrNull("battersFaced") ?: 0 }
        val lastBf = recent.lastOrNull()?.optObj("stat")?.optIntOrNull("battersFaced")
        val ksTrend = logs.takeLast(6).map { (it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0).toFloat() }

        return OutlookCalculator.PitchingSample(
            seasonSo = seasonSo,
            seasonBf = seasonBf,
            seasonGs = seasonGs,
            seasonStrikePct = strikePct,
            recentSo = recentSo,
            recentBf = recentBf,
            lastStartBf = lastBf,
            lastStartKs = ksTrend,
        )
    }

    private fun shortName(team: JSONObject?): String {
        val name = team?.optString("name").orEmpty()
        return name.split(" ").lastOrNull()?.take(3)?.uppercase(Locale.US) ?: "MLB"
    }

    private fun formatGameTime(iso: String): String {
        if (iso.isBlank()) return ""
        return runCatching {
            val zoned = ZonedDateTime.parse(iso).withZoneSameInstant(zone)
            zoned.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
        }.getOrDefault("")
    }

    private data class RawStarter(
        val mlbId: Int,
        val name: String,
        val team: String,
        val opponent: String,
        val venue: String,
        val weather: Weather,
        val tempF: Int,
        val gameTimeLabel: String,
        val homeAway: String,
    )

    companion object {
        val SLATE_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
        const val RECENT_STARTS = 5
    }
}
