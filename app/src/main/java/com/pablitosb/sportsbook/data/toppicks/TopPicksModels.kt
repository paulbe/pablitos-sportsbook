package com.pablitosb.sportsbook.data.toppicks

import com.pablitosb.sportsbook.data.hr.HrBoard
import com.pablitosb.sportsbook.data.hr.HrSort
import com.pablitosb.sportsbook.data.hr.HrSorter
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.starters.StartersBoard
import com.pablitosb.sportsbook.data.starters.StartersSort
import com.pablitosb.sportsbook.data.starters.StartersSorter
import java.time.Instant
import java.time.LocalDate

enum class TopPicksSide { PITCHERS, BATTERS }

data class TopPicksBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val sourceLabel: String,
    val startersBoard: StartersBoard?,
    val hrBoard: HrBoard?,
    val pitchersNote: String? = null,
    val battersNote: String? = null,
    val emptyReason: String? = null,
) {
    val hasAny: Boolean
        get() = !startersBoard?.starters.isNullOrEmpty() || !hrBoard?.batters.isNullOrEmpty()
}

object TopPicksSelector {
    const val LIMIT = 10

    fun pitchers(
        starters: List<Starter>,
        key: StartersSort,
        ascending: Boolean,
        limit: Int = LIMIT,
    ): List<Starter> = StartersSorter.sort(starters, key, ascending).take(limit)

    fun batters(
        batters: List<HrBatter>,
        key: HrSort,
        ascending: Boolean,
        limit: Int = LIMIT,
    ): List<HrBatter> = HrSorter.sort(batters, key, ascending).take(limit)

    fun showOutlookChip(key: StartersSort): Boolean = key == StartersSort.PROG
}
