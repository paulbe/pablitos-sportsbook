package com.pablitosb.sportsbook.data.projections

import com.pablitosb.sportsbook.data.dfs.FdScoring
import com.pablitosb.sportsbook.data.hr.HrCalculator
import com.pablitosb.sportsbook.data.tb.TbCalculator
import com.pablitosb.sportsbook.data.mlb.ParkFactors
import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.mlb.WeatherAdj
import com.pablitosb.sportsbook.data.remote.MlbStatsClient
import com.pablitosb.sportsbook.data.remote.optArr
import com.pablitosb.sportsbook.data.remote.optFloatish
import com.pablitosb.sportsbook.data.remote.optIntOrNull
import com.pablitosb.sportsbook.data.remote.optObj
import com.pablitosb.sportsbook.data.remote.toObjList
import com.pablitosb.sportsbook.data.starters.OutlookCalculator
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Duration
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ProjectionService(
    private val api: MlbStatsClient = MlbStatsClient(),
    private val zone: ZoneId = StartersRepository.SLATE_ZONE,
) {
    private val mutex = Mutex()
    private var cache: ProjectionBoard? = null

    suspend fun load(slate: LocalDate, force: Boolean = false): ProjectionBoard = mutex.withLock {
        val hit = cache
        if (!force && hit != null && hit.slateDate == slate &&
            Duration.between(hit.fetchedAt, Instant.now()).seconds < 90
        ) {
            return hit
        }
        val board = withContext(Dispatchers.IO) { fetch(slate) }
        cache = board
        board
    }

    private suspend fun fetch(slate: LocalDate): ProjectionBoard {
        val fetchedAt = Instant.now()
        val abbrev = runCatching { loadTeamAbbrevs() }.getOrDefault(emptyMap())
        lineupStore.clear()
        val games = try {
            loadGames(slate, abbrev)
        } catch (e: Exception) {
            throw SlateLoadException(
                "Couldn’t reach MLB Stats API for $slate. Check the connection and retry.",
                e,
            )
        }
        val live = games.filter { !it.postponed }
        if (live.isEmpty()) {
            return ProjectionBoard(
                slateDate = slate,
                fetchedAt = fetchedAt,
                games = games,
                hitters = emptyList(),
                pitchers = emptyList(),
                sourceLabel = "Live slate",
                lineupGames = 0,
                rosterGames = 0,
                emptyReason = if (games.any { it.postponed }) {
                    "Every game on $slate was postponed or canceled."
                } else {
                    "MLB has no games posted for $slate."
                },
            )
        }

        val hitting = try {
            loadSeasonMap(slate.year, "hitting")
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t load season hitting stats. Retry.", e)
        }
        val pitching = try {
            loadSeasonMap(slate.year, "pitching")
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t load season pitching stats. Retry.", e)
        }

        val teamIds = live.flatMap { listOf(it.homeId, it.awayId) }.distinct()
        val rosters = fetchRosters(teamIds)
        val lineupHitters = lineupStore.values.flatMap { it.home + it.away }.filter { it.id > 0 }
        val rosterHitters = collectRosterHitters(live, rosters)
        val chosen = chooseHitters(live, lineupHitters, rosterHitters)

        val pitcherRefs = live.flatMap { game ->
            listOfNotNull(
                game.homePitcherId?.let { it to game },
                game.awayPitcherId?.let { it to game },
            )
        }
        val peopleIds = (chosen.map { it.id } + pitcherRefs.map { it.first }).distinct()
        val people = fetchPeople(peopleIds)

        val logsById = fetchGameLogs(pitcherRefs.map { it.first }.distinct(), slate.year)

        val pitchers = pitcherRefs.mapNotNull { (id, game) ->
            val person = people[id]
            val stat = pitching[id]
            val name = person?.name ?: stat?.name ?: sidePitcherName(game, id)
            val throws = person?.pitchHand ?: "R"
            val sample = sampleAsOf(logsById[id].orEmpty(), slate)
            val outlook = OutlookCalculator.project(sample)
            val ip = StatMath.parseInnings(stat?.stat?.optString("inningsPitched"))
            val gs = stat?.stat?.optIntOrNull("gamesStarted") ?: 0
            val expectedIp = when {
                gs > 0 && ip > 0f -> (ip / gs).coerceIn(4.0f, 7.4f)
                else -> (outlook.nextStartKs / (outlook.projK.coerceAtLeast(0.15f)) / 4.25f)
                    .coerceIn(4.5f, 6.6f)
            }
            val era = stat?.stat?.optFloatish("era") ?: 4.20f
            val hr = stat?.stat?.optIntOrNull("homeRuns") ?: 0
            val hr9 = if (ip > 0f) hr * 9f / ip else HrCalculator.LEAGUE_HR9
            val team = if (id == game.homePitcherId) game.homeAbbr else game.awayAbbr
            val opp = if (id == game.homePitcherId) game.awayAbbr else game.homeAbbr
            val homeAway = if (id == game.homePitcherId) "home" else "away"
            PitcherProjection(
                mlbId = id,
                name = name,
                team = team,
                opponent = opp,
                throwsHand = throws,
                nextStartKs = outlook.nextStartKs,
                projKPct = outlook.projK * 100f,
                expectedIp = expectedIp,
                era = era,
                hr9 = hr9,
                seasonHr = hr,
                seasonIp = ip,
                seasonSo = stat?.stat?.optIntOrNull("strikeOuts") ?: 0,
                seasonBf = stat?.stat?.optIntOrNull("battersFaced") ?: 0,
                airOuts = stat?.stat?.optIntOrNull("airOuts") ?: 0,
                groundOuts = stat?.stat?.optIntOrNull("groundOuts") ?: 0,
                fdPoints = FdScoring.pitcherPoints(outlook.nextStartKs, expectedIp, era),
                outlook = outlook.outlook,
                gamePk = game.gamePk,
                awayAbbr = game.awayAbbr,
                homeAbbr = game.homeAbbr,
                homeAway = homeAway,
            )
        }.distinctBy { it.mlbId }

        val pitcherById = pitchers.associateBy { it.mlbId }
        val hitters = chosen.mapNotNull { ref ->
            val game = live.firstOrNull { it.gamePk == ref.gamePk } ?: return@mapNotNull null
            val person = people[ref.id]
            val stat = hitting[ref.id]?.stat
            val name = person?.name ?: hitting[ref.id]?.name ?: ref.name
            val pos = person?.pos?.ifBlank { null } ?: ref.pos
            val bat = person?.batSide ?: "R"
            val vsHome = ref.teamId == game.awayId
            val oppPitcherId = if (vsHome) game.homePitcherId else game.awayPitcherId
            val oppPitcher = oppPitcherId?.let { pitcherById[it] }
            val oppName = oppPitcher?.name ?: if (vsHome) game.homePitcherName else game.awayPitcherName
            val oppHand = oppPitcher?.throwsHand ?: "R"
            val sample = HrCalculator.HittingSample(
                hr = stat?.optIntOrNull("homeRuns") ?: 0,
                pa = stat?.optIntOrNull("plateAppearances") ?: 0,
                avg = stat?.optFloatish("avg") ?: 0.240f,
                slg = stat?.optFloatish("slg") ?: 0.400f,
                airOuts = stat?.optIntOrNull("airOuts") ?: 0,
                groundOuts = stat?.optIntOrNull("groundOuts") ?: 0,
                batSide = bat,
                order = ref.order,
            )
            val pctx = HrCalculator.PitcherContext(
                hr = oppPitcher?.seasonHr ?: 0,
                ip = oppPitcher?.seasonIp ?: 0f,
                airOuts = oppPitcher?.airOuts ?: 0,
                groundOuts = oppPitcher?.groundOuts ?: 0,
                throws = oppHand,
            )
            val hr = HrCalculator.project(sample, game.parkHrFactor, game.weatherFactor, pctx)
            val hits = stat?.optIntOrNull("hits") ?: 0
            val pa = sample.pa
            val expectedHits = if (pa > 0) hits.toFloat() / pa * hr.expectedPa else 0.85f
            val team = if (ref.teamId == game.homeId) game.homeAbbr else game.awayAbbr
            val opponent = if (ref.teamId == game.homeId) game.awayAbbr else game.homeAbbr
            val homeAway = if (ref.teamId == game.homeId) "home" else "away"
            val doubles = stat?.optIntOrNull("doubles") ?: 0
            val triples = stat?.optIntOrNull("triples") ?: 0
            val hrCount = sample.hr
            val ab = stat?.optIntOrNull("atBats") ?: 0
            val tb = TbCalculator.project(
                TbCalculator.Sample(
                    hits = hits,
                    doubles = doubles,
                    triples = triples,
                    hr = hrCount,
                    pa = pa,
                    ab = ab,
                ),
                hr,
            )
            val fd = FdScoring.hitterPoints(
                hit = sample,
                hr = hr,
                seasonHits = hits,
                doubles = doubles,
                triples = triples,
                hrCount = hrCount,
                bb = stat?.optIntOrNull("baseOnBalls") ?: 0,
                sb = stat?.optIntOrNull("stolenBases") ?: 0,
                runs = stat?.optIntOrNull("runs") ?: 0,
                rbi = stat?.optIntOrNull("rbi") ?: 0,
                pa = pa,
                games = stat?.optIntOrNull("gamesPlayed") ?: 0,
            )
            HitterProjection(
                mlbId = ref.id,
                name = name,
                team = team,
                opponent = opponent,
                pos = pos,
                fdSlots = FdScoring.fdSlotsFor(pos),
                batSide = bat,
                battingOrder = ref.order,
                inPostedLineup = ref.fromLineup,
                gamePk = game.gamePk,
                parkName = game.venueName,
                weather = game.weather,
                tempF = game.tempF,
                opposingPitcherName = oppName.ifBlank { "TBD" },
                opposingPitcherHand = if (oppHand.uppercase().startsWith("L")) "LHP" else "RHP",
                opposingHr9 = hr.pitcherHr9,
                seasonPa = pa,
                seasonHr = hrCount,
                seasonHits = hits,
                seasonDoubles = doubles,
                seasonTriples = triples,
                seasonBb = stat?.optIntOrNull("baseOnBalls") ?: 0,
                seasonHbp = stat?.optIntOrNull("hitByPitch") ?: 0,
                seasonSb = stat?.optIntOrNull("stolenBases") ?: 0,
                seasonR = stat?.optIntOrNull("runs") ?: 0,
                seasonRbi = stat?.optIntOrNull("rbi") ?: 0,
                seasonAb = ab,
                avg = sample.avg,
                slg = sample.slg,
                expectedPa = hr.expectedPa,
                pPa = hr.pPa,
                gameHrProb = hr.gameHrProb,
                expectedHr = hr.expectedHr,
                expectedHits = expectedHits,
                seasonHrPct = hr.seasonHrPct,
                talentGamePct = hr.talentGamePct,
                parkAdjPct = hr.parkAdjPct,
                pitcherAdjPct = hr.pitcherAdjPct,
                regressionLean = hr.regressionLean,
                fdPoints = fd,
                expectedTb = tb.expectedTb,
                tbPerPa = tb.tbPerPa,
                slgProxy = tb.slgProxy,
                awayAbbr = game.awayAbbr,
                homeAbbr = game.homeAbbr,
                homeAway = homeAway,
                gameTimeLabel = game.gameTimeLabel,
            )
        }.sortedByDescending { it.gameHrProb }

        val lineupGames = live.count { it.homeLineupPosted || it.awayLineupPosted }
        val rosterGames = live.size - live.count { it.homeLineupPosted && it.awayLineupPosted }
        return ProjectionBoard(
            slateDate = slate,
            fetchedAt = fetchedAt,
            games = games,
            hitters = hitters,
            pitchers = pitchers.sortedByDescending { it.fdPoints },
            sourceLabel = when {
                lineupGames == live.size -> "Live lineups"
                lineupGames > 0 -> "Lineups + roster"
                else -> "Active rosters"
            },
            lineupGames = lineupGames,
            rosterGames = rosterGames,
        )
    }

    private fun chooseHitters(
        games: List<SlateGame>,
        lineup: List<HitterRef>,
        roster: List<HitterRef>,
    ): List<HitterRef> {
        val out = mutableListOf<HitterRef>()
        for (game in games) {
            val homeLu = lineup.filter { it.gamePk == game.gamePk && it.teamId == game.homeId }
            val awayLu = lineup.filter { it.gamePk == game.gamePk && it.teamId == game.awayId }
            if (game.homeLineupPosted && homeLu.isNotEmpty()) out += homeLu else {
                out += roster.filter { it.gamePk == game.gamePk && it.teamId == game.homeId }
            }
            if (game.awayLineupPosted && awayLu.isNotEmpty()) out += awayLu else {
                out += roster.filter { it.gamePk == game.gamePk && it.teamId == game.awayId }
            }
        }
        return out.distinctBy { it.id to it.gamePk }
    }

    private fun loadGames(slate: LocalDate, abbrev: Map<Int, String>): List<SlateGame> {
        val json = api.getJson(
            "/api/v1/schedule?sportId=1&date=$slate&hydrate=lineups,probablePitcher,venue,weather",
        )
        val rawGames = json.optArr("dates").toObjList().flatMap { it.optArr("games").toObjList() }
        return rawGames.mapNotNull { game -> parseGame(game, abbrev) }
    }

    private fun parseGame(game: JSONObject, abbrev: Map<Int, String>): SlateGame? {
        val status = game.optObj("status")
        val detailed = status?.optString("detailedState").orEmpty()
        val postponed = detailed.contains("Postponed", ignoreCase = true) ||
            detailed.contains("Cancelled", ignoreCase = true) ||
            detailed.contains("Canceled", ignoreCase = true)
        val venue = game.optObj("venue")
        val venueName = venue?.optString("name").orEmpty().ifBlank { "TBD" }
        val venueId = venue?.optIntOrNull("id")
        val weatherObj = game.optObj("weather")
        val condition = weatherObj?.optString("condition").orEmpty()
        val wind = weatherObj?.optString("wind").orEmpty()
        val temp = weatherObj?.optString("temp")?.toIntOrNull() ?: 0
        val teams = game.optObj("teams") ?: return null
        val home = teams.optObj("home")
        val away = teams.optObj("away")
        val homeId = home?.optObj("team")?.optIntOrNull("id") ?: return null
        val awayId = away?.optObj("team")?.optIntOrNull("id") ?: return null
        val homeAbbr = abbrev[homeId] ?: shortName(home?.optObj("team"))
        val awayAbbr = abbrev[awayId] ?: shortName(away?.optObj("team"))
        val homeP = home?.optObj("probablePitcher")
        val awayP = away?.optObj("probablePitcher")
        val lineups = game.optObj("lineups")
        val homeLu = lineups?.optArr("homePlayers")?.toObjList().orEmpty()
        val awayLu = lineups?.optArr("awayPlayers")?.toObjList().orEmpty()
        return SlateGame(
            gamePk = game.optIntOrNull("gamePk") ?: return null,
            venueId = venueId,
            venueName = venueName,
            weather = WeatherAdj.icon(condition),
            tempF = temp,
            wind = wind,
            condition = condition,
            weatherFactor = WeatherAdj.factor(condition, temp, wind),
            parkHrFactor = ParkFactors.hrMultiplier(venueId, venueName),
            homeAbbr = homeAbbr,
            awayAbbr = awayAbbr,
            homeId = homeId,
            awayId = awayId,
            homePitcherId = homeP?.optIntOrNull("id"),
            awayPitcherId = awayP?.optIntOrNull("id"),
            homePitcherName = homeP?.optString("fullName").orEmpty(),
            awayPitcherName = awayP?.optString("fullName").orEmpty(),
            postponed = postponed,
            gameTimeLabel = formatGameTime(game.optString("gameDate")),
            startUtc = runCatching { Instant.parse(game.optString("gameDate")) }.getOrNull(),
            homeLineupPosted = homeLu.size >= 8,
            awayLineupPosted = awayLu.size >= 8,
        ).also {
            lineupStore[it.gamePk] = LineupBundle(
                home = homeLu.mapIndexed { i, p -> playerRef(p, it.gamePk, homeId, i + 1, true) },
                away = awayLu.mapIndexed { i, p -> playerRef(p, it.gamePk, awayId, i + 1, true) },
            )
        }
    }

    private val lineupStore = mutableMapOf<Int, LineupBundle>()

    private fun collectRosterHitters(games: List<SlateGame>, rosters: Map<Int, List<RosterPlayer>>): List<HitterRef> {
        return games.flatMap { game ->
            listOf(game.homeId, game.awayId).flatMap { teamId ->
                rosters[teamId].orEmpty()
                    .filter { !it.pos.equals("P", ignoreCase = true) }
                    .map { p ->
                        HitterRef(
                            id = p.id,
                            name = p.name,
                            pos = p.pos,
                            teamId = teamId,
                            gamePk = game.gamePk,
                            order = null,
                            fromLineup = false,
                        )
                    }
            }
        }
    }

    private fun playerRef(p: JSONObject, gamePk: Int, teamId: Int, order: Int, fromLineup: Boolean): HitterRef {
        val pos = p.optObj("primaryPosition")?.optString("abbreviation").orEmpty().ifBlank { "DH" }
        return HitterRef(
            id = p.optIntOrNull("id") ?: 0,
            name = p.optString("fullName"),
            pos = pos,
            teamId = teamId,
            gamePk = gamePk,
            order = order,
            fromLineup = fromLineup,
        )
    }

    private fun loadTeamAbbrevs(): Map<Int, String> {
        val json = api.getJson("/api/v1/teams?sportId=1")
        return json.optArr("teams").toObjList().mapNotNull { team ->
            val id = team.optIntOrNull("id") ?: return@mapNotNull null
            val abbr = team.optString("abbreviation").ifBlank { null } ?: return@mapNotNull null
            id to abbr
        }.toMap()
    }

    private fun loadSeasonMap(season: Int, group: String): Map<Int, SeasonRow> {
        val json = api.getJson(
            "/api/v1/stats?stats=season&group=$group&season=$season&sportIds=1&limit=2000&playerPool=all",
        )
        val splits = json.optArr("stats").toObjList()
            .firstOrNull()?.optArr("splits")?.toObjList().orEmpty()
        return splits.mapNotNull { split ->
            val player = split.optObj("player") ?: return@mapNotNull null
            val id = player.optIntOrNull("id") ?: return@mapNotNull null
            val stat = split.optObj("stat") ?: return@mapNotNull null
            id to SeasonRow(id, player.optString("fullName"), stat)
        }.toMap()
    }

    private suspend fun fetchRosters(teamIds: List<Int>): Map<Int, List<RosterPlayer>> = coroutineScope {
        val gate = Semaphore(6)
        teamIds.map { id ->
            async {
                gate.withPermit {
                    id to runCatching { loadRoster(id) }.getOrDefault(emptyList())
                }
            }
        }.awaitAll().toMap()
    }

    private fun loadRoster(teamId: Int): List<RosterPlayer> {
        val json = api.getJson("/api/v1/teams/$teamId/roster?rosterType=active")
        return json.optArr("roster").toObjList().mapNotNull { row ->
            val person = row.optObj("person") ?: return@mapNotNull null
            val id = person.optIntOrNull("id") ?: return@mapNotNull null
            val pos = row.optObj("position")?.optString("abbreviation").orEmpty().ifBlank { "DH" }
            RosterPlayer(id, person.optString("fullName"), pos)
        }
    }

    private suspend fun fetchPeople(ids: List<Int>): Map<Int, PersonBits> = coroutineScope {
        val unique = ids.distinct().filter { it > 0 }
        val chunks = unique.chunked(40)
        val gate = Semaphore(4)
        chunks.map { chunk ->
            async {
                gate.withPermit {
                    runCatching { loadPeople(chunk) }.getOrDefault(emptyMap())
                }
            }
        }.awaitAll().fold(mutableMapOf()) { acc, map -> acc.apply { putAll(map) } }
    }

    private fun loadPeople(ids: List<Int>): Map<Int, PersonBits> {
        val json = api.getJson("/api/v1/people?personIds=${ids.joinToString(",")}")
        return json.optArr("people").toObjList().mapNotNull { p ->
            val id = p.optIntOrNull("id") ?: return@mapNotNull null
            id to PersonBits(
                name = p.optString("fullName"),
                batSide = p.optObj("batSide")?.optString("code").orEmpty().ifBlank { "R" },
                pitchHand = p.optObj("pitchHand")?.optString("code").orEmpty().ifBlank { "R" },
                pos = p.optObj("primaryPosition")?.optString("abbreviation").orEmpty(),
            )
        }.toMap()
    }

    private suspend fun fetchGameLogs(ids: List<Int>, season: Int): Map<Int, List<JSONObject>> = coroutineScope {
        val gate = Semaphore(6)
        ids.distinct().map { id ->
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

    private fun sampleAsOf(logs: List<JSONObject>, slate: LocalDate): OutlookCalculator.PitchingSample? {
        val prior = logs.filter { logDate(it)?.isBefore(slate) == true }
        if (prior.isEmpty()) return null
        val starts = prior.filter { it.optObj("stat")?.optIntOrNull("gamesStarted") ?: 0 >= 1 }
        val seasonSo = prior.sumOf { it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0 }
        val seasonBf = prior.sumOf { it.optObj("stat")?.optIntOrNull("battersFaced") ?: 0 }
        val strikes = prior.sumOf { it.optObj("stat")?.optIntOrNull("strikes") ?: 0 }
        val pitches = prior.sumOf { it.optObj("stat")?.optIntOrNull("numberOfPitches") ?: 0 }
        val recent = starts.takeLast(5)
        return OutlookCalculator.PitchingSample(
            seasonSo = seasonSo,
            seasonBf = seasonBf,
            seasonGs = starts.size,
            seasonStrikePct = if (pitches > 0) strikes.toFloat() / pitches else null,
            recentSo = recent.sumOf { it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0 },
            recentBf = recent.sumOf { it.optObj("stat")?.optIntOrNull("battersFaced") ?: 0 },
            lastStartBf = recent.lastOrNull()?.optObj("stat")?.optIntOrNull("battersFaced"),
            lastStartKs = starts.takeLast(6).map { (it.optObj("stat")?.optIntOrNull("strikeOuts") ?: 0).toFloat() },
        )
    }

    private fun logDate(split: JSONObject): LocalDate? =
        runCatching { LocalDate.parse(split.optString("date")) }.getOrNull()

    private fun sidePitcherName(game: SlateGame, id: Int): String =
        if (id == game.homePitcherId) game.homePitcherName else game.awayPitcherName

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

    private data class SeasonRow(val id: Int, val name: String, val stat: JSONObject)
    private data class RosterPlayer(val id: Int, val name: String, val pos: String)
    private data class PersonBits(val name: String, val batSide: String, val pitchHand: String, val pos: String)
    private data class HitterRef(
        val id: Int,
        val name: String,
        val pos: String,
        val teamId: Int,
        val gamePk: Int,
        val order: Int?,
        val fromLineup: Boolean,
    )
    private data class LineupBundle(val home: List<HitterRef>, val away: List<HitterRef>)

    companion object {
        val shared = ProjectionService()
    }
}
