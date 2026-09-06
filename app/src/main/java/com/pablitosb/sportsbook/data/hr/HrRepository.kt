package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import com.pablitosb.sportsbook.data.projections.ProjectionService
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import com.pablitosb.sportsbook.data.starters.ParkWeather
import java.time.LocalDate
import java.util.Locale

data class HrBoard(
    val slate: ProjectionBoard,
    val batters: List<HrBatter>,
    val oppKScale: OppKScale = OppKScale.fallback(),
)

class HrRepository(
    private val projections: ProjectionService = ProjectionService.shared,
) {
    suspend fun load(date: LocalDate, force: Boolean = false): HrBoard {
        val board = try {
            projections.load(date, force)
        } catch (e: SlateLoadException) {
            throw e
        } catch (e: Exception) {
            throw SlateLoadException("Couldn’t build the Daily Batters board for $date.", e)
        }
        val games = board.games.associateBy { it.gamePk }
        val pitchers = board.pitchers
        val kRates = pitchers.mapNotNull { p ->
            if (p.seasonBf > 0) p.seasonSo.toFloat() / p.seasonBf else p.projKPct.takeIf { it > 0f }?.div(100f)
        }
        val scale = OppKScale.fromRates(kRates)
        val mapped = board.hitters.map { h ->
            val game = games[h.gamePk]
            val boost = game?.let {
                ParkWeather.parse(
                    condition = it.condition,
                    tempRaw = if (it.tempF > 0) it.tempF.toString() else "",
                    windRaw = it.wind,
                    hrParkFactor = it.parkHrFactor,
                ).envBoostPct
            } ?: 0
            val opp = pitchers.firstOrNull {
                it.gamePk == h.gamePk && it.team.equals(h.opponent, ignoreCase = true)
            }
            val oppK = when {
                opp != null && opp.seasonBf > 0 -> opp.seasonSo.toFloat() / opp.seasonBf
                opp != null && opp.projKPct > 0f -> opp.projKPct / 100f
                else -> null
            }
            val fd = BatterFdCalculator.project(h)
            HrBatter(
                rank = 1,
                name = h.name,
                team = h.team,
                opponent = h.opponent,
                pitcherHand = h.opposingPitcherHand,
                gameHrPct = h.gameHrProb * 100f,
                seasonHrPct = h.seasonHrPct,
                xHrPct = h.talentGamePct,
                parkAdjPct = h.parkAdjPct,
                parkName = h.parkName,
                weather = h.weather,
                tempF = h.tempF,
                pitcherAdjPct = h.pitcherAdjPct,
                pitcherName = h.opposingPitcherName.substringAfterLast(' ').ifBlank { h.opposingPitcherName },
                pitcherHr9 = h.opposingHr9,
                regressionLean = h.regressionLean,
                mlbId = h.mlbId,
                battingOrder = h.battingOrder,
                sourceNote = if (h.inPostedLineup) "Lineup" else "Roster",
                expectedPa = h.expectedPa,
                awayAbbr = h.awayAbbr,
                homeAbbr = h.homeAbbr,
                homeAway = h.homeAway,
                gameTimeLabel = h.gameTimeLabel.ifBlank { game?.gameTimeLabel.orEmpty() },
                envBoostPct = boost,
                fdFloor = fd.floor,
                fdProj = fd.proj,
                fdCeiling = fd.ceiling,
                projTb = h.expectedTb,
                projHits = fd.counting.hits,
                projRuns = fd.counting.runs,
                projRbi = fd.counting.rbi,
                projHrr = fd.counting.hrr,
                oppPitcherK = oppK,
            )
        }
        val batters = HrSorter.sort(mapped, HrSort.GAME_HR, ascending = false)
            .mapIndexed { index, b -> b.copy(rank = index + 1) }
        return HrBoard(board, batters, scale)
    }
}
