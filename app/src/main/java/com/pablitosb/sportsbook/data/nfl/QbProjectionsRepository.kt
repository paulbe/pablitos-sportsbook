package com.pablitosb.sportsbook.data.nfl

import com.pablitosb.sportsbook.data.remote.EspnNflClient
import com.pablitosb.sportsbook.data.remote.optArr
import com.pablitosb.sportsbook.data.remote.optFloatish
import com.pablitosb.sportsbook.data.remote.optObj
import com.pablitosb.sportsbook.data.remote.toObjList
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QbProjectionsRepository(
    private val api: EspnNflClient = EspnNflClient(),
    private val zone: ZoneId = ZONE,
) {
    private val depthCache = ConcurrentHashMap<String, StarterQb>()
    private val missDepth = ConcurrentHashMap.newKeySet<String>()
    private val statsCache = ConcurrentHashMap<String, TeamPassStats>()
    private val logCache = ConcurrentHashMap<String, QbLog>()
    private val gate = Semaphore(8)

    suspend fun currentWeek(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val json = api.getJson("/apis/site/v2/sports/football/nfl/scoreboard")
        val year = json.optObj("season")?.optInt("year") ?: LocalDate.now(zone).year
        val week = json.optObj("week")?.optInt("number") ?: 1
        year to week.coerceIn(1, 18)
    }

    suspend fun load(seasonYear: Int, week: Int, force: Boolean = false): QbBoard {
        if (force) clearCaches()
        return try {
            withContext(Dispatchers.IO) { loadUnsafe(seasonYear, week) }
        } catch (e: QbLoadException) {
            throw e
        } catch (e: Exception) {
            throw QbLoadException("Couldn’t load QB projections for $seasonYear week $week.", e)
        }
    }

    private suspend fun loadUnsafe(seasonYear: Int, week: Int): QbBoard = coroutineScope {
        val board = api.getJson(
            "/apis/site/v2/sports/football/nfl/scoreboard?dates=$seasonYear&seasontype=2&week=$week",
        )
        val events = board.optArr("events").toObjList()
        val games = events.mapNotNull { parseGame(it) }
        if (games.isEmpty()) {
            return@coroutineScope QbBoard(
                seasonYear = seasonYear,
                week = week,
                fetchedAt = Instant.now(),
                sourceLabel = "EXAMPLE • ESPN",
                note = NOTE,
                qbs = emptyList(),
                emptyReason = "No NFL games posted for week $week.",
            )
        }
        val teamIds = games.flatMap { listOf(it.homeId, it.awayId) }.distinct()
        val depths = teamIds.map { id ->
            async { id to runCatching { starterQb(id) }.getOrNull() }
        }.awaitAll().toMap()
        val stats = teamIds.map { id ->
            async { id to runCatching { teamStats(id) }.getOrDefault(TeamPassStats()) }
        }.awaitAll().toMap()
        val qbIds = depths.values.mapNotNull { it?.id }.distinct()
        val logs = qbIds.map { id ->
            async { id to runCatching { gamelog(id) }.getOrDefault(QbLog()) }
        }.awaitAll().toMap()

        val projected = games.flatMap { game ->
            listOf(
                projectSide(game, home = true, depths, stats, logs),
                projectSide(game, home = false, depths, stats, logs),
            )
        }.filterNotNull()
        val ranked = QbSorter.sort(projected, QbSort.PROJ_YDS, ascending = false)
            .mapIndexed { i, q -> q.copy(rank = i + 1) }
        QbBoard(
            seasonYear = seasonYear,
            week = week,
            fetchedAt = Instant.now(),
            sourceLabel = "EXAMPLE • ESPN",
            note = NOTE,
            qbs = ranked,
            emptyReason = if (ranked.isEmpty()) "No starting QBs on the depth chart for week $week." else null,
        )
    }

    private fun projectSide(
        game: NflGame,
        home: Boolean,
        depths: Map<String, StarterQb?>,
        stats: Map<String, TeamPassStats>,
        logs: Map<String, QbLog>,
    ): QbProjection? {
        val teamId = if (home) game.homeId else game.awayId
        val oppId = if (home) game.awayId else game.homeId
        val teamAbbr = if (home) game.homeAbbr else game.awayAbbr
        val qb = depths[teamId] ?: return null
        val log = logs[qb.id] ?: QbLog()
        val team = stats[teamId] ?: TeamPassStats()
        val opp = stats[oppId] ?: TeamPassStats()
        val spread = game.homeSpread?.let { if (home) it else -it }
        val result = QbProjectionCalculator.project(
            QbInputs(
                attL3 = log.attL3,
                attL3Games = log.attL3Games,
                attSeasonPerGame = log.attSeasonPerGame,
                teamPassAttPerGame = team.passAttPerGame,
                teamPlaysPerGame = team.playsPerGame,
                ypaL5 = log.ypaL5,
                ypaL5Games = log.ypaL5Games,
                ypaSeason = log.ypaSeason,
                seasonAtt = log.seasonAtt,
                seasonCompPct = log.seasonCompPct,
                seasonPassTd = log.seasonTd,
                compPctL5 = log.compPctL5,
                oppYpaAllowed = opp.ypaAllowed,
                rushAttL3 = log.rushAttL3,
                rushAttL3Games = log.rushAttL3Games,
                rushAttSeasonPerGame = log.rushAttSeasonPerGame,
                teamRushAttPerGame = team.rushAttPerGame,
                ypcL5 = log.ypcL5,
                ypcL5Games = log.ypcL5Games,
                ypcSeason = log.ypcSeason,
                seasonRushAtt = log.seasonRushAtt,
                seasonRushTd = log.seasonRushTd,
                oppYpcAllowed = opp.ypcAllowed,
                spreadForTeam = spread,
                total = game.total,
            ),
        )
        return QbProjection(
            rank = 1,
            name = qb.name,
            team = teamAbbr,
            opponent = if (home) game.awayAbbr else game.homeAbbr,
            awayAbbr = game.awayAbbr,
            homeAbbr = game.homeAbbr,
            homeAway = if (home) "home" else "away",
            gameTimeLabel = game.timeLabel,
            projYds = result.projYds,
            ydsFloor = result.ydsFloor,
            ydsCeiling = result.ydsCeiling,
            fdProj = result.fdProj,
            fdFloor = result.fdFloor,
            fdCeiling = result.fdCeiling,
            compPct = result.compPct,
            iay = result.iay,
            matchupPct = result.matchupPct,
            rushMatchupPct = result.rushMatchupPct,
            projRushYds = result.projRushYds,
            rushFloor = result.rushFloor,
            rushCeiling = result.rushCeiling,
            expAtt = result.expAtt,
            expYpa = result.expYpa,
            expRushAtt = result.expRushAtt,
            expYpc = result.expYpc,
            expRushTd = result.expRushTd,
            espnId = qb.id,
        )
    }

    private fun parseGame(event: JSONObject): NflGame? {
        val comp = event.optArr("competitions").toObjList().firstOrNull() ?: return null
        val sides = comp.optArr("competitors").toObjList()
        val home = sides.firstOrNull { it.optString("homeAway") == "home" } ?: return null
        val away = sides.firstOrNull { it.optString("homeAway") == "away" } ?: return null
        val homeTeam = home.optObj("team") ?: return null
        val awayTeam = away.optObj("team") ?: return null
        val odds = comp.optArr("odds").toObjList().firstOrNull()
        val dateIso = event.optString("date").ifBlank { comp.optString("date") }
        val start = runCatching { Instant.parse(dateIso) }.getOrNull()
        return NflGame(
            homeId = homeTeam.optString("id"),
            awayId = awayTeam.optString("id"),
            homeAbbr = homeTeam.optString("abbreviation"),
            awayAbbr = awayTeam.optString("abbreviation"),
            timeLabel = formatGameTime(start),
            homeSpread = odds?.optFloatish("spread"),
            total = odds?.optFloatish("overUnder"),
        )
    }

    private suspend fun starterQb(teamId: String): StarterQb? {
        depthCache[teamId]?.let { return it }
        if (missDepth.contains(teamId)) return null
        val json = runCatching {
            gate.withPermit {
                api.getJson("/apis/site/v2/sports/football/nfl/teams/$teamId/depthcharts")
            }
        }.getOrNull() ?: run {
            missDepth.add(teamId)
            return null
        }
        val charts = json.optArr("depthchart").toObjList()
        for (chart in charts) {
            val positions = chart.optObj("positions") ?: continue
            val qb = positions.optObj("qb") ?: continue
            val first = qb.optArr("athletes").toObjList().firstOrNull() ?: continue
            val id = first.optString("id")
            val name = first.optString("displayName")
            if (id.isNotBlank() && name.isNotBlank()) {
                val starter = StarterQb(id, name)
                depthCache[teamId] = starter
                return starter
            }
        }
        missDepth.add(teamId)
        return null
    }

    private suspend fun teamStats(teamId: String): TeamPassStats = cachedValue(statsCache, teamId) {
        val json = gate.withPermit {
            api.getJson("/apis/site/v2/sports/football/nfl/teams/$teamId/statistics")
        }
        val results = json.optObj("results")
        val offense = results?.optObj("stats")?.optArr("categories")?.toObjList().orEmpty()
        val opponent = opponentCategories(results)
        val pass = offense.firstOrNull { it.optString("name") == "passing" }
        val rush = offense.firstOrNull { it.optString("name") == "rushing" }
        val oppPass = opponent.firstOrNull { it.optString("name") == "passing" }
        val oppRush = opponent.firstOrNull { it.optString("name") == "rushing" }
        val games = stat(pass, "teamGamesPlayed")?.coerceAtLeast(1f) ?: 17f
        val passAtt = stat(pass, "passingAttempts") ?: 0f
        val plays = stat(rush, "totalOffensivePlays") ?: 0f
        val rushAtt = stat(rush, "rushingAttempts") ?: 0f
        TeamPassStats(
            passAttPerGame = if (passAtt > 0f) passAtt / games else 0f,
            playsPerGame = if (plays > 0f) plays / games else 0f,
            ypaAllowed = stat(oppPass, "yardsPerPassAttempt") ?: 0f,
            rushAttPerGame = if (rushAtt > 0f) rushAtt / games else 0f,
            ypcAllowed = stat(oppRush, "yardsPerRushAttempt") ?: 0f,
        )
    }

    private suspend fun gamelog(athleteId: String): QbLog = cachedValue(logCache, athleteId) {
        val json = gate.withPermit {
            api.getJson("/apis/common/v3/sports/football/nfl/athletes/$athleteId/gamelog")
        }
        val names = json.optJSONArray("names")
        val nameList = buildList {
            if (names != null) for (i in 0 until names.length()) add(names.optString(i))
        }
        val attI = nameList.indexOf("passingAttempts")
        val ydsI = nameList.indexOf("passingYards")
        val ypaI = nameList.indexOf("yardsPerPassAttempt")
        val cmpI = nameList.indexOf("completionPct")
        val tdI = nameList.indexOf("passingTouchdowns")
        val rushAttI = nameList.indexOf("rushingAttempts")
        val rushYdsI = nameList.indexOf("rushingYards")
        val ypcI = nameList.indexOf("yardsPerRushAttempt")
        val rushTdI = nameList.indexOf("rushingTouchdowns")
        val meta = json.optObj("events")
        val regular = json.optArr("seasonTypes").toObjList()
            .firstOrNull { it.optString("displayName").contains("Regular", ignoreCase = true) }
        val rows = regular?.optArr("categories")?.toObjList()?.firstOrNull()
            ?.optArr("events")?.toObjList().orEmpty()
        data class Start(
            val date: Instant,
            val att: Float,
            val ypa: Float,
            val cmp: Float,
            val yds: Float,
            val td: Float,
            val rushAtt: Float,
            val rushYds: Float,
            val ypc: Float,
            val rushTd: Float,
        )
        val starts = rows.mapNotNull { row ->
            val id = row.optString("eventId")
            val stats = row.optJSONArray("stats") ?: return@mapNotNull null
            fun at(i: Int): Float {
                if (i < 0 || i >= stats.length()) return 0f
                return stats.optString(i).replace(",", "").toFloatOrNull() ?: 0f
            }
            val date = meta?.optObj(id)?.optString("gameDate")
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: Instant.EPOCH
            Start(
                date, at(attI), at(ypaI), at(cmpI), at(ydsI), at(tdI),
                at(rushAttI), at(rushYdsI), at(ypcI), at(rushTdI),
            )
        }.filter { it.att > 0f }.sortedByDescending { it.date }
        val last3 = starts.take(3)
        val last5 = starts.take(5)
        val rushYpcGames = last5.filter { it.rushAtt > 0f }
        val seasonAtt = starts.sumOf { it.att.toDouble() }.toFloat()
        val seasonYds = starts.sumOf { it.yds.toDouble() }.toFloat()
        val seasonTd = starts.sumOf { it.td.toDouble() }.toInt()
        val seasonRushAtt = starts.sumOf { it.rushAtt.toDouble() }.toFloat()
        val seasonRushYds = starts.sumOf { it.rushYds.toDouble() }.toFloat()
        val seasonRushTd = starts.sumOf { it.rushTd.toDouble() }.toInt()
        val games = starts.size.coerceAtLeast(1)
        QbLog(
            attL3 = if (last3.isNotEmpty()) last3.map { it.att }.average().toFloat() else 0f,
            attL3Games = last3.size,
            attSeasonPerGame = seasonAtt / games,
            ypaL5 = if (last5.isNotEmpty()) last5.map { it.ypa }.average().toFloat() else 0f,
            ypaL5Games = last5.size,
            ypaSeason = if (seasonAtt > 0f) seasonYds / seasonAtt else 0f,
            seasonAtt = seasonAtt.toInt(),
            seasonCompPct = if (starts.isNotEmpty()) starts.map { it.cmp }.average().toFloat() else 0f,
            seasonTd = seasonTd,
            compPctL5 = if (last5.isNotEmpty()) last5.map { it.cmp }.average().toFloat() else 0f,
            rushAttL3 = if (last3.isNotEmpty()) last3.map { it.rushAtt }.average().toFloat() else 0f,
            rushAttL3Games = last3.size,
            rushAttSeasonPerGame = seasonRushAtt / games,
            ypcL5 = if (rushYpcGames.isNotEmpty()) rushYpcGames.map { it.ypc }.average().toFloat() else 0f,
            ypcL5Games = rushYpcGames.size,
            ypcSeason = if (seasonRushAtt > 0f) seasonRushYds / seasonRushAtt else 0f,
            seasonRushAtt = seasonRushAtt.toInt(),
            seasonRushTd = seasonRushTd,
        )
    }

    private fun opponentCategories(results: JSONObject?): List<JSONObject> {
        if (results == null) return emptyList()
        val asArr = results.optArr("opponent").toObjList()
        if (asArr.isNotEmpty()) return asArr
        return results.optObj("opponent")?.optArr("categories")?.toObjList().orEmpty()
    }

    private fun stat(category: JSONObject?, name: String): Float? {
        val stats = category?.optArr("stats")?.toObjList().orEmpty()
        return stats.firstOrNull { it.optString("name") == name }?.optFloatish("value")
    }

    private fun formatGameTime(start: Instant?): String {
        if (start == null) return ""
        return start.atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }

    private suspend fun <T : Any> cachedValue(map: ConcurrentHashMap<String, T>, key: String, load: suspend () -> T): T {
        map[key]?.let { return it }
        val value = load()
        map.putIfAbsent(key, value)
        return map[key] ?: value
    }

    private fun clearCaches() {
        depthCache.clear()
        missDepth.clear()
        statsCache.clear()
        logCache.clear()
    }

    private data class StarterQb(val id: String, val name: String)

    private data class NflGame(
        val homeId: String,
        val awayId: String,
        val homeAbbr: String,
        val awayAbbr: String,
        val timeLabel: String,
        val homeSpread: Float?,
        val total: Float?,
    )

    private data class TeamPassStats(
        val passAttPerGame: Float = 0f,
        val playsPerGame: Float = 0f,
        val ypaAllowed: Float = 0f,
        val rushAttPerGame: Float = 0f,
        val ypcAllowed: Float = 0f,
    )

    private data class QbLog(
        val attL3: Float = 0f,
        val attL3Games: Int = 0,
        val attSeasonPerGame: Float = 0f,
        val ypaL5: Float = 0f,
        val ypaL5Games: Int = 0,
        val ypaSeason: Float = 0f,
        val seasonAtt: Int = 0,
        val seasonCompPct: Float = 0f,
        val seasonTd: Int = 0,
        val compPctL5: Float = 0f,
        val rushAttL3: Float = 0f,
        val rushAttL3Games: Int = 0,
        val rushAttSeasonPerGame: Float = 0f,
        val ypcL5: Float = 0f,
        val ypcL5Games: Int = 0,
        val ypcSeason: Float = 0f,
        val seasonRushAtt: Int = 0,
        val seasonRushTd: Int = 0,
    )

    companion object {
        val ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
        const val NOTE =
            "Schedule, depth-chart QBs, recent pass/rush attempts, YPA/YPC, team pass/rush rate, and opponent YPA/YPC allowed come from ESPN. " +
                "IAY is a YPA-blend depth proxy — not Next Gen intended air yards. CPOE is shrunk to 0 (no NGS feed). " +
                "Designed-rush proxy = team rush att × 0.10 (pocket) or 0.22 (dual-threat). " +
                "Proj Rush matchup is 75% rush D YPC allowed + 25% pass YPA allowed. " +
                "Proj FD = 0.04×pass yds + 4×E[pass TD] + 0.1×rush yds + 6×E[rush TD]. Weather multiplier is 1.0."
    }
}
