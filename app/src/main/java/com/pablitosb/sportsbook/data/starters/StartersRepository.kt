package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.mlb.ParkFactors
import com.pablitosb.sportsbook.data.mlb.ParkSites
import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.mlb.TeamOffense
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.remote.MlbStatsClient
import com.pablitosb.sportsbook.data.remote.OpenMeteoClient
import com.pablitosb.sportsbook.data.remote.SavantExpectedClient
import com.pablitosb.sportsbook.data.remote.optArr
import com.pablitosb.sportsbook.data.remote.optDoubleOrNull
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

enum class SlateMode { LIVE, RESULTS }

data class StartersBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val starters: List<Starter>,
    val mode: SlateMode,
    val sourceLabel: String,
    val reconstructed: Boolean,
    val postponedCount: Int = 0,
    val emptyReason: String? = null,
    val oppKScale: OppKScale = OppKScale.fallback(),
)

class StartersLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

class StartersRepository(
    private val api: MlbStatsClient = MlbStatsClient(),
    private val savant: SavantExpectedClient = SavantExpectedClient(),
    private val weatherApi: OpenMeteoClient = OpenMeteoClient(),
    private val zone: ZoneId = SLATE_ZONE,
) {
    suspend fun loadToday(): StartersBoard = load(LocalDate.now(zone))

    suspend fun load(slate: LocalDate): StartersBoard = withContext(Dispatchers.IO) {
        val today = LocalDate.now(zone)
        val fetchedAt = Instant.now()
        val resultsMode = slate.isBefore(today)
        val abbrev = runCatching { loadTeamAbbrevs() }.getOrDefault(emptyMap())
        val raw = try {
            loadSlatePitchers(slate, abbrev, resultsMode)
        } catch (e: Exception) {
            throw StartersLoadException(
                "Couldn’t reach MLB Stats API for $slate. Check the connection and retry.",
                e,
            )
        }
        val postponed = raw.count { it.postponed }
        val usable = raw.filter { !it.postponed }
        if (usable.isEmpty()) {
            val reason = when {
                postponed > 0 ->
                    "Every game on $slate was postponed or canceled. Flip a day to try another slate."
                resultsMode ->
                    "No completed starts found for $slate."
                else ->
                    "MLB hasn’t posted probable starters for $slate yet. Check back later, or pick another date."
            }
            return@withContext StartersBoard(
                slateDate = slate,
                fetchedAt = fetchedAt,
                starters = emptyList(),
                mode = if (resultsMode) SlateMode.RESULTS else SlateMode.LIVE,
                sourceLabel = if (resultsMode) "Results · pred vs actual" else "Live slate",
                reconstructed = resultsMode,
                postponedCount = postponed,
                emptyReason = reason,
            )
        }
        val logsById = fetchGameLogs(usable.map { it.mlbId }, slate.year)
        val xwobaById = runCatching { savant.pitcherXwoba(slate.year) }.getOrDefault(emptyMap())
        val offense = loadTeamOffense(slate.year)
        val oppKScale = OppKScale.fromRates(offense.values.map { it.kRate })
        val projected = usable.map { row ->
            val sample = sampleAsOf(logsById[row.mlbId].orEmpty(), slate)
            val projection = OutlookCalculator.project(sample)
            Triple(row, projection, sample)
        }.sortedWith(
            compareByDescending<Triple<RawStarter, OutlookCalculator.Projection, OutlookCalculator.PitchingSample?>> { it.second.outlookScore }
                .thenByDescending { it.second.projK },
        )
        val starters = projected.mapIndexed { index, (row, projection, sample) ->
            val predKs = projection.nextStartKs
            val predKPct = projection.projK * 100f
            val dayLog = logsById[row.mlbId].orEmpty().firstOrNull { log ->
                logDate(log) == slate && (log.optObj("stat")?.optIntOrNull("gamesStarted") ?: 0) >= 1
            }?.optObj("stat")
            val actualKs = row.actualSo ?: dayLog?.optIntOrNull("strikeOuts")
            val actualBf = row.actualBf ?: dayLog?.optIntOrNull("battersFaced")
            val actualKPct = if (actualKs != null && actualBf != null && actualBf > 0) {
                actualKs.toFloat() / actualBf * 100f
            } else {
                null
            }
            val opp = offense[row.opponent.uppercase(Locale.US)]
            val outs = ProjOutsCalculator.project(
                sample?.workload(),
                ProjOutsCalculator.Context(
                    opponent = opp,
                    envBoostPct = row.wx.envBoostPct,
                    rain = row.wx.tag == WxTag.RAIN_RISK,
                ),
            )
            val (awayAbbr, homeAbbr) = if (row.homeAway == "home") {
                row.opponent to row.team
            } else {
                row.team to row.opponent
            }
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
                projKPct = predKPct,
                nextStartKs = predKs,
                trend = projection.trend,
                gameTimeLabel = row.gameTimeLabel,
                mlbId = row.mlbId,
                homeAway = row.homeAway,
                actualKs = actualKs,
                actualBf = actualBf,
                actualKPct = actualKPct,
                ksDelta = actualKs?.let { it - predKs },
                kPctDelta = actualKPct?.let { it - predKPct },
                resultNote = row.resultNote,
                windLabel = row.wx.windLabel,
                windRel = row.wx.windRel,
                windMph = row.wx.windMph,
                wxTag = row.wx.tag,
                precipPct = row.wx.precipPct,
                weatherCondition = row.wx.condition,
                xwoba = xwobaById[row.mlbId],
                ace = projection.outlookScore >= 8 || predKPct >= 28f,
                hrParkFactor = row.wx.hrParkFactor,
                parkHint = row.wx.parkHint,
                envBoostPct = row.wx.envBoostPct,
                gameStart = row.gameStart,
                projIp = outs.projIp,
                projOuts = outs.projOuts,
                oppKRate = opp?.kRate,
                awayAbbr = awayAbbr,
                homeAbbr = homeAbbr,
            )
        }
        StartersBoard(
            slateDate = slate,
            fetchedAt = fetchedAt,
            starters = starters,
            mode = if (resultsMode) SlateMode.RESULTS else SlateMode.LIVE,
            sourceLabel = if (resultsMode) "Results · pred vs actual" else "Live slate",
            reconstructed = resultsMode,
            postponedCount = postponed,
            oppKScale = oppKScale,
        )
    }

    private fun loadTeamOffense(season: Int): Map<String, TeamOffense> {
        val primary = runCatching { fetchTeamOffense(season) }.getOrDefault(emptyMap())
        if (primary.size >= 20) return primary
        val prior = runCatching { fetchTeamOffense(season - 1) }.getOrDefault(emptyMap())
        return if (prior.size > primary.size) prior else primary
    }

    private fun fetchTeamOffense(season: Int): Map<String, TeamOffense> {
        val json = api.getJson(
            "/api/v1/teams/stats?sportIds=1&season=$season&group=hitting&stats=season",
        )
        return json.optArr("stats").toObjList()
            .flatMap { it.optArr("splits").toObjList() }
            .mapNotNull { split ->
                val abbr = split.optObj("team")?.optString("abbreviation")
                    ?.ifBlank { null }
                    ?.uppercase(Locale.US)
                    ?: return@mapNotNull null
                val stat = split.optObj("stat") ?: return@mapNotNull null
                val so = stat.optIntOrNull("strikeOuts") ?: return@mapNotNull null
                val pa = stat.optIntOrNull("plateAppearances") ?: return@mapNotNull null
                if (pa < 50) return@mapNotNull null
                abbr to TeamOffense(
                    kRate = so.toFloat() / pa,
                    ops = stat.optFloatish("ops"),
                    pa = pa,
                )
            }.toMap()
    }

    private fun loadTeamAbbrevs(): Map<Int, String> {
        val json = api.getJson("/api/v1/teams?sportId=1")
        return json.optArr("teams").toObjList().mapNotNull { team ->
            val id = team.optIntOrNull("id") ?: return@mapNotNull null
            val abbr = team.optString("abbreviation").ifBlank { null } ?: return@mapNotNull null
            id to abbr
        }.toMap()
    }

    private suspend fun loadSlatePitchers(
        slate: LocalDate,
        abbrev: Map<Int, String>,
        resultsMode: Boolean,
    ): List<RawStarter> {
        val json = api.getJson(
            "/api/v1/schedule?sportId=1&date=$slate&hydrate=probablePitcher,venue(location),weather",
        )
        val games = json.optArr("dates").toObjList().flatMap { it.optArr("games").toObjList() }
        val forecastByGame = fetchForecasts(games, slate)
        val rows = mutableListOf<RawStarter>()
        for (game in games) {
            val status = game.optObj("status")
            val detailed = status?.optString("detailedState").orEmpty()
            val abstractState = status?.optString("abstractGameState").orEmpty()
            val postponed = detailed.contains("Postponed", ignoreCase = true) ||
                detailed.contains("Cancelled", ignoreCase = true) ||
                detailed.contains("Canceled", ignoreCase = true) ||
                detailed.contains("Suspended", ignoreCase = true)
            val venueObj = game.optObj("venue")
            val venue = venueObj?.optString("name").orEmpty()
            val venueId = venueObj?.optIntOrNull("id")
            val weatherObj = game.optObj("weather")
            val hrPf = ParkFactors.hrMultiplier(venueId, venue)
            val mlbWx = ParkWeather.parse(
                condition = weatherObj?.optString("condition").orEmpty(),
                tempRaw = weatherObj?.optString("temp").orEmpty(),
                windRaw = weatherObj?.optString("wind").orEmpty(),
                hrParkFactor = hrPf,
            )
            val gamePk = game.optIntOrNull("gamePk")
            val indoor = ParkSites.isIndoor(ParkSites.roof(venueId, venue), mlbWx.condition)
            val wx = ParkWeather.resolve(forecastByGame[gamePk], mlbWx, indoor, hrPf)
            val gameTime = formatGameTime(game.optString("gameDate"))
            val teams = game.optObj("teams") ?: continue
            val home = teams.optObj("home")
            val away = teams.optObj("away")
            val homeId = home?.optObj("team")?.optIntOrNull("id")
            val awayId = away?.optObj("team")?.optIntOrNull("id")
            val homeAbbr = homeId?.let { abbrev[it] } ?: shortName(home?.optObj("team"))
            val awayAbbr = awayId?.let { abbrev[it] } ?: shortName(away?.optObj("team"))

            val box = if (resultsMode && !postponed && gamePk != null && abstractState == "Final") {
                runCatching { loadBoxStarters(gamePk) }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            val gameStart = parseGameInstant(game.optString("gameDate"))
            addPitcher(rows, away, awayAbbr, homeAbbr, venue, wx, gameTime, gameStart, "away", postponed, box)
            addPitcher(rows, home, homeAbbr, awayAbbr, venue, wx, gameTime, gameStart, "home", postponed, box)
        }
        return rows.distinctBy { Triple(it.mlbId, it.gameTimeLabel, it.homeAway) }
    }

    private suspend fun fetchForecasts(
        games: List<JSONObject>,
        slate: LocalDate,
    ): Map<Int, ParkWeather.Snapshot> = coroutineScope {
        val gate = Semaphore(4)
        games.mapNotNull { game ->
            val gamePk = game.optIntOrNull("gamePk") ?: return@mapNotNull null
            val venue = game.optObj("venue")
            val venueId = venue?.optIntOrNull("id")
            val venueName = venue?.optString("name").orEmpty()
            val loc = venue?.optObj("location")
            val coords = loc?.optObj("defaultCoordinates")
            val fallback = ParkSites.get(venueId)
            val lat = coords?.optDoubleOrNull("latitude") ?: fallback?.lat
            val lon = coords?.optDoubleOrNull("longitude") ?: fallback?.lon
            if (lat == null || lon == null) return@mapNotNull null
            val bearing = loc?.optFloatish("azimuthAngle") ?: fallback?.cfBearingDeg
            val mlbCondition = game.optObj("weather")?.optString("condition").orEmpty()
            val indoor = ParkSites.isIndoor(ParkSites.roof(venueId, venueName), mlbCondition)
            val at = parseGameInstant(game.optString("gameDate"))
                ?: slate.atTime(19, 0).atZone(zone).toInstant()
            async {
                gate.withPermit {
                    val hour = runCatching { weatherApi.hourAt(lat, lon, at, slate) }.getOrNull()
                    val hrPf = ParkFactors.hrMultiplier(venueId, venueName)
                    val snap = hour?.let {
                        ParkWeather.fromForecast(
                            tempF = it.tempF,
                            windMph = it.windMph,
                            windFromDeg = it.windFromDeg,
                            precipPct = it.precipPct,
                            weatherCode = it.weatherCode,
                            cfBearingDeg = bearing,
                            indoor = indoor,
                            mlbCondition = mlbCondition,
                            hrParkFactor = hrPf,
                        )
                    }
                    gamePk to snap
                }
            }
        }.awaitAll().mapNotNull { (pk, snap) -> snap?.let { pk to it } }.toMap()
    }

    private fun parseGameInstant(iso: String): Instant? =
        if (iso.isBlank()) null else runCatching { Instant.parse(iso) }.getOrNull()

    private fun loadBoxStarters(gamePk: Int): Map<String, BoxStarter> {
        val box = api.getJson("/api/v1/game/$gamePk/boxscore")
        val teams = box.optObj("teams") ?: return emptyMap()
        return listOf("away", "home").mapNotNull { side ->
            val club = teams.optObj(side) ?: return@mapNotNull null
            val ids = club.optArr("pitchers")
            if (ids.length() == 0) return@mapNotNull null
            val firstId = when (val raw = ids.opt(0)) {
                is Number -> raw.toInt()
                is String -> raw.toIntOrNull()
                else -> null
            } ?: return@mapNotNull null
            val player = club.optObj("players")?.optObj("ID$firstId") ?: return@mapNotNull null
            val pitching = player.optObj("stats")?.optObj("pitching")
            val name = player.optObj("person")?.optString("fullName").orEmpty()
            side to BoxStarter(
                mlbId = firstId,
                name = name,
                so = pitching?.optIntOrNull("strikeOuts"),
                bf = pitching?.optIntOrNull("battersFaced"),
            )
        }.toMap()
    }

    private fun addPitcher(
        out: MutableList<RawStarter>,
        side: JSONObject?,
        team: String,
        opponent: String,
        venue: String,
        wx: ParkWeather.Snapshot,
        gameTime: String,
        gameStart: Instant?,
        homeAway: String,
        postponed: Boolean,
        boxBySide: Map<String, BoxStarter>,
    ) {
        val probable = side?.optObj("probablePitcher")
        val probableId = probable?.optIntOrNull("id")
        val probableName = probable?.optString("fullName").orEmpty()
        val box = boxBySide[homeAway]
        val id = box?.mlbId ?: probableId ?: return
        val name = box?.name?.ifBlank { null } ?: probableName.ifBlank { return }
        val note = when {
            postponed -> "PPD"
            box != null && probableId != null && box.mlbId != probableId ->
                "Actual SP (probable was $probableName)"
            else -> ""
        }
        out += RawStarter(
            mlbId = id,
            name = name,
            team = team,
            opponent = opponent,
            venue = venue.ifBlank { "TBD" },
            weather = wx.icon,
            tempF = wx.tempF,
            wx = wx,
            gameTimeLabel = gameTime,
            gameStart = gameStart,
            homeAway = homeAway,
            postponed = postponed,
            actualSo = if (postponed) null else box?.so,
            actualBf = if (postponed) null else box?.bf,
            resultNote = note,
        )
    }

    private suspend fun fetchGameLogs(
        ids: List<Int>,
        season: Int,
    ): Map<Int, List<JSONObject>> = coroutineScope {
        val unique = ids.distinct()
        val gate = Semaphore(6)
        unique.map { id ->
            async {
                gate.withPermit {
                    id to runCatching { loadGameLogs(id, season) }.getOrDefault(emptyList())
                }
            }
        }.awaitAll().toMap()
    }

    private fun loadGameLogs(id: Int, season: Int): List<JSONObject> {
        val json = api.getJson(
            "/api/v1/people/$id/stats?stats=season,gameLog&group=pitching&season=$season",
        )
        return json.optArr("stats").toObjList()
            .firstOrNull { it.optObj("type")?.optString("displayName") == "gameLog" }
            ?.optArr("splits")?.toObjList().orEmpty()
    }

    /** Season + last-5 GS using only appearances strictly before [slate] (reconstructed as-of). */
    private fun sampleAsOf(logs: List<JSONObject>, slate: LocalDate): OutlookCalculator.PitchingSample? {
        val prior = logs.filter { logDate(it)?.isBefore(slate) == true }
        if (prior.isEmpty()) return null
        val starts = prior.filter { it.optObj("stat")?.optIntOrNull("gamesStarted") ?: 0 >= 1 }
        val seasonSo = prior.sumOf { it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0 }
        val seasonBf = prior.sumOf { it.optObj("stat")?.optIntOrNull("battersFaced") ?: 0 }
        val seasonGs = starts.size
        val strikes = prior.sumOf { it.optObj("stat")?.optIntOrNull("strikes") ?: 0 }
        val pitches = prior.sumOf { it.optObj("stat")?.optIntOrNull("numberOfPitches") ?: 0 }
        val strikePct = if (pitches > 0) strikes.toFloat() / pitches else null
        val recent = starts.takeLast(RECENT_STARTS)
        val startIp = starts.map { StatMath.parseInnings(it.optObj("stat")?.optString("inningsPitched")) }
        val recentIpList = recent.map { StatMath.parseInnings(it.optObj("stat")?.optString("inningsPitched")) }
        val recentIp = recentIpList.sum()
        return OutlookCalculator.PitchingSample(
            seasonSo = seasonSo,
            seasonBf = seasonBf,
            seasonGs = seasonGs,
            seasonStrikePct = strikePct,
            recentSo = recent.sumOf { it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0 },
            recentBf = recent.sumOf { it.optObj("stat")?.optIntOrNull("battersFaced") ?: 0 },
            lastStartBf = recent.lastOrNull()?.optObj("stat")?.optIntOrNull("battersFaced"),
            lastStartKs = starts.takeLast(6).map { (it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0).toFloat() },
            seasonIp = startIp.sum(),
            recentIp = recentIp,
            recentGs = recent.size,
            lastStartIp = recentIpList.lastOrNull()?.takeIf { it > 0f },
            last5Ip = recentIpList,
        )
    }

    private fun OutlookCalculator.PitchingSample.workload(): ProjOutsCalculator.Workload =
        ProjOutsCalculator.Workload(
            seasonIp = seasonIp,
            seasonGs = seasonGs,
            recentIp = recentIp,
            recentGs = recentGs,
            lastStartIp = lastStartIp,
            last5Ip = last5Ip,
        )

    private fun logDate(split: JSONObject): LocalDate? =
        runCatching { LocalDate.parse(split.optString("date")) }.getOrNull()

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

    private data class BoxStarter(
        val mlbId: Int,
        val name: String,
        val so: Int?,
        val bf: Int?,
    )

    private data class RawStarter(
        val mlbId: Int,
        val name: String,
        val team: String,
        val opponent: String,
        val venue: String,
        val weather: Weather,
        val tempF: Int,
        val wx: ParkWeather.Snapshot = ParkWeather.parse("", "", ""),
        val gameTimeLabel: String,
        val gameStart: Instant? = null,
        val homeAway: String,
        val postponed: Boolean = false,
        val actualSo: Int? = null,
        val actualBf: Int? = null,
        val resultNote: String = "",
    )

    companion object {
        val SLATE_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
        const val RECENT_STARTS = 5
    }
}
