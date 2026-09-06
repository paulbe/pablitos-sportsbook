package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.mlb.OppKTier
import com.pablitosb.sportsbook.data.model.HrBatter

/** Invert Option 1 pitcher K% colors: low-K pitchers help the batter. */
object BatterOppTint {
    fun favorable(tier: OppKTier): Boolean = tier == OppKTier.LOW
    fun tough(tier: OppKTier): Boolean = tier == OppKTier.HIGH
}

enum class HrSort {
    GAME_HR,
    PROJ_FD,
    PROJ_TB,
    HRR,
}

object HrSorter {
    fun defaultAscending(key: HrSort): Boolean = false

    fun sort(batters: List<HrBatter>, key: HrSort, ascending: Boolean): List<HrBatter> {
        val by = when (key) {
            HrSort.GAME_HR -> compareBy<HrBatter> { it.gameHrPct }
            HrSort.PROJ_FD -> compareBy<HrBatter> { it.fdProj }.thenBy { it.fdCeiling }
            HrSort.PROJ_TB -> compareBy<HrBatter> { it.projTb }
            HrSort.HRR -> compareBy<HrBatter> { it.projHrr }
        }
        return batters.sortedWith(if (ascending) by else by.reversed())
    }
}
