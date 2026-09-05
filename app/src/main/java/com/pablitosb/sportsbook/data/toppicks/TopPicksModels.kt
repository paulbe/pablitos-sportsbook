package com.pablitosb.sportsbook.data.toppicks

import com.pablitosb.sportsbook.data.fdproj.FdProjRow
import com.pablitosb.sportsbook.data.fdproj.FdProjSorter
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.starters.ParkWeather
import com.pablitosb.sportsbook.data.tb.TbBatter
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

enum class TopPicksSection { ALL, SP_K, HR, FD_VALUE, TB }

data class TopPick(
    val mlbId: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val pos: String,
    val metric: String,
    val metricValue: Float,
    val why: String,
    val gameTimeLabel: String = "",
)

data class TopPicksBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val sourceLabel: String,
    val kSpots: List<TopPick>,
    val hrSpots: List<TopPick>,
    val fdValue: List<TopPick>,
    val tbSpots: List<TopPick>,
    val fdRankedByValue: Boolean,
    val fdSalaryNote: String,
    val kNote: String? = null,
    val hrNote: String? = null,
    val fdNote: String? = null,
    val tbNote: String? = null,
    val emptyReason: String? = null,
) {
    val hasAny: Boolean
        get() = kSpots.isNotEmpty() || hrSpots.isNotEmpty() || fdValue.isNotEmpty() || tbSpots.isNotEmpty()
}

object TopPicksSelector {
    const val LIMIT = 5

    /** Rain last, then Proj Ks, then outlook score. */
    fun kSpots(starters: List<Starter>, limit: Int = LIMIT): List<TopPick> {
        return starters
            .sortedWith(
                compareBy<Starter> { it.wxTag == WxTag.RAIN_RISK }
                    .thenByDescending { it.nextStartKs }
                    .thenByDescending { it.outlookScore },
            )
            .take(limit)
            .mapIndexed { index, s ->
                TopPick(
                    mlbId = if (s.mlbId != 0) s.mlbId else s.name.hashCode() + index,
                    name = s.name,
                    team = s.team,
                    opponent = s.opponent,
                    pos = "P",
                    metric = String.format(Locale.US, "%.1f", s.nextStartKs),
                    metricValue = s.nextStartKs,
                    why = whyK(s),
                    gameTimeLabel = s.gameTimeLabel,
                )
            }
    }

    fun hrSpots(batters: List<HrBatter>, limit: Int = LIMIT): List<TopPick> {
        return batters
            .sortedByDescending { it.gameHrPct }
            .take(limit)
            .mapIndexed { index, b ->
                TopPick(
                    mlbId = if (b.mlbId != 0) b.mlbId else b.name.hashCode() + index,
                    name = b.name,
                    team = b.team,
                    opponent = b.opponent,
                    pos = b.battingOrder?.let { "#$it" }.orEmpty(),
                    metric = String.format(Locale.US, "%.1f%%", b.gameHrPct),
                    metricValue = b.gameHrPct,
                    why = whyHr(b),
                    gameTimeLabel = "",
                )
            }
    }

    fun fdValue(rows: List<FdProjRow>, limit: Int = LIMIT): Pair<List<TopPick>, Boolean> {
        val hasValue = rows.any { it.value != null }
        val ranked = if (hasValue) {
            FdProjSorter.sort(rows, com.pablitosb.sportsbook.data.fdproj.FdProjSort.VALUE, ascending = false)
        } else {
            FdProjSorter.sort(rows, com.pablitosb.sportsbook.data.fdproj.FdProjSort.PROJ, ascending = false)
        }
        val picks = ranked.take(limit).mapIndexed { index, row ->
            TopPick(
                mlbId = if (row.mlbId != 0) row.mlbId else row.name.hashCode() + index,
                name = row.name,
                team = row.team,
                opponent = row.opponent,
                pos = row.pos,
                metric = if (hasValue && row.value != null) {
                    String.format(Locale.US, "%.2f", row.value)
                } else {
                    String.format(Locale.US, "%.1f", row.proj)
                },
                metricValue = if (hasValue) row.value ?: row.proj else row.proj,
                why = whyFd(row, hasValue),
                gameTimeLabel = row.gameTimeLabel,
            )
        }
        return picks to hasValue
    }

    fun tbSpots(batters: List<TbBatter>, limit: Int = LIMIT): List<TopPick> {
        return batters
            .sortedByDescending { it.projTb }
            .take(limit)
            .mapIndexed { index, b ->
                TopPick(
                    mlbId = if (b.mlbId != 0) b.mlbId else b.name.hashCode() + index,
                    name = b.name,
                    team = b.team,
                    opponent = b.opponent,
                    pos = b.pos,
                    metric = String.format(Locale.US, "%.2f", b.projTb),
                    metricValue = b.projTb,
                    why = whyTb(b),
                    gameTimeLabel = "",
                )
            }
    }

    fun whyTb(b: TbBatter): String {
        val parts = mutableListOf(String.format(Locale.US, "%.2f Proj TB", b.projTb))
        parts += String.format(Locale.US, "%.3f TB/PA", b.tbPerPa)
        if (b.parkAdjPct != 0) parts += String.format(Locale.US, "park %+d%%", b.parkAdjPct)
        if (b.pitcherName.isNotBlank()) parts += "vs ${b.pitcherName}"
        if (b.parkName.isNotBlank()) parts += b.parkName
        return parts.joinToString(" · ")
    }

    fun whyK(s: Starter): String {
        val parts = mutableListOf(s.outlook.name)
        parts += String.format(Locale.US, "%.1f Proj Ks", s.nextStartKs)
        when (s.wxTag) {
            WxTag.RAIN_RISK -> parts += "RAIN RISK"
            WxTag.HR_WEATHER -> parts += "HR weather"
            WxTag.PITCHER_WX -> parts += "Pitcher wx"
            WxTag.NEUTRAL -> Unit
        }
        if (s.wxTag != WxTag.RAIN_RISK && s.envBoostPct != 0) {
            parts += ParkWeather.boostLabel(s.envBoostPct)
        }
        if (s.parkHint.isNotBlank()) parts += s.parkHint
        return parts.joinToString(" · ")
    }

    fun whyHr(b: HrBatter): String {
        val parts = mutableListOf<String>()
        if (b.parkAdjPct != 0) parts += String.format(Locale.US, "park %+d%%", b.parkAdjPct)
        if (b.pitcherName.isNotBlank()) {
            parts += "vs ${b.pitcherName} (${String.format(Locale.US, "%.1f", b.pitcherHr9)} HR/9)"
        }
        if (b.pitcherHand.isNotBlank()) parts += b.pitcherHand
        if (b.parkName.isNotBlank()) parts += b.parkName
        if (b.sourceNote.isNotBlank()) parts += b.sourceNote
        return parts.joinToString(" · ").ifBlank { "Game HR probability" }
    }

    fun whyFd(row: FdProjRow, byValue: Boolean): String {
        val parts = mutableListOf<String>()
        if (byValue && row.value != null) {
            parts += String.format(Locale.US, "%.2f pts/\$1k", row.value)
            parts += String.format(Locale.US, "%.1f FD pts", row.proj)
            parts += "$" + "%,d".format(Locale.US, row.salary)
        } else {
            parts += String.format(Locale.US, "%.1f Proj FD pts", row.proj)
            if (row.salary > 0) parts += "$" + "%,d".format(Locale.US, row.salary)
            else parts += "no salary"
        }
        if (row.pos.isNotBlank()) parts += row.pos
        if (row.driver.isNotBlank()) parts += row.driver
        return parts.joinToString(" · ")
    }
}
