package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import com.pablitosb.sportsbook.data.projections.ProjectionService
import com.pablitosb.sportsbook.data.projections.SlateLoadException
import java.time.LocalDate

data class HrBoard(
    val slate: ProjectionBoard,
    val batters: List<HrBatter>,
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
            throw SlateLoadException("Couldn’t build the HR board for $date.", e)
        }
        val batters = board.hitters
            .sortedByDescending { it.gameHrProb }
            .mapIndexed { index, h ->
                HrBatter(
                    rank = index + 1,
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
                )
            }
        return HrBoard(board, batters)
    }
}
