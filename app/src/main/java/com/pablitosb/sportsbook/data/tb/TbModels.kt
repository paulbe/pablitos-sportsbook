package com.pablitosb.sportsbook.data.tb

import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.projections.HitterProjection
import com.pablitosb.sportsbook.data.projections.ProjectionBoard
import java.time.Instant
import java.time.LocalDate

enum class TbSort { PROJ_TB, TB_PA, SLG }

data class TbBatter(
    val rank: Int,
    val mlbId: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val pos: String,
    val battingOrder: Int?,
    val inPostedLineup: Boolean,
    val pitcherName: String,
    val pitcherHand: String,
    val parkName: String,
    val weather: Weather,
    val tempF: Int,
    val projTb: Float,
    val tbPerPa: Float,
    val slgProxy: Float,
    val expectedPa: Float,
    val parkAdjPct: Int,
    val pitcherAdjPct: Int,
    val gameHrPct: Float,
)

data class TbBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val sourceLabel: String,
    val batters: List<TbBatter>,
    val emptyReason: String? = null,
)

object TbSorter {
    fun sort(rows: List<TbBatter>, key: TbSort, ascending: Boolean): List<TbBatter> {
        val ordered = when (key) {
            TbSort.PROJ_TB -> rows.sortedByDescending { it.projTb }
            TbSort.TB_PA -> rows.sortedByDescending { it.tbPerPa }
            TbSort.SLG -> rows.sortedByDescending { it.slgProxy }
        }
        return if (ascending) ordered.reversed() else ordered
    }

    fun fromHitters(hitters: List<HitterProjection>): List<TbBatter> {
        return hitters
            .sortedByDescending { it.expectedTb }
            .mapIndexed { index, h ->
                TbBatter(
                    rank = index + 1,
                    mlbId = h.mlbId,
                    name = h.name,
                    team = h.team,
                    opponent = h.opponent,
                    pos = h.pos,
                    battingOrder = h.battingOrder,
                    inPostedLineup = h.inPostedLineup,
                    pitcherName = h.opposingPitcherName.substringAfterLast(' ')
                        .ifBlank { h.opposingPitcherName },
                    pitcherHand = h.opposingPitcherHand,
                    parkName = h.parkName,
                    weather = h.weather,
                    tempF = h.tempF,
                    projTb = h.expectedTb,
                    tbPerPa = h.tbPerPa,
                    slgProxy = h.slgProxy,
                    expectedPa = h.expectedPa,
                    parkAdjPct = h.parkAdjPct,
                    pitcherAdjPct = h.pitcherAdjPct,
                    gameHrPct = h.gameHrProb * 100f,
                )
            }
    }
}

class TbRepository(
    private val projections: com.pablitosb.sportsbook.data.projections.ProjectionService =
        com.pablitosb.sportsbook.data.projections.ProjectionService.shared,
) {
    suspend fun load(date: LocalDate, force: Boolean = false): TbBoard {
        val slate: ProjectionBoard = try {
            projections.load(date, force)
        } catch (e: com.pablitosb.sportsbook.data.projections.SlateLoadException) {
            throw e
        } catch (e: Exception) {
            throw com.pablitosb.sportsbook.data.projections.SlateLoadException(
                "Couldn’t build the total-bases board for $date.",
                e,
            )
        }
        val batters = TbSorter.fromHitters(slate.hitters)
        return TbBoard(
            slateDate = slate.slateDate,
            fetchedAt = slate.fetchedAt,
            sourceLabel = slate.sourceLabel,
            batters = batters,
            emptyReason = if (batters.isEmpty()) {
                slate.emptyReason ?: "No hitters available for $date. Lineups may not be posted yet."
            } else {
                null
            },
        )
    }
}
