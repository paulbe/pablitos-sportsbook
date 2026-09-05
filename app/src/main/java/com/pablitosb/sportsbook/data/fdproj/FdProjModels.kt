package com.pablitosb.sportsbook.data.fdproj

import com.pablitosb.sportsbook.data.dfs.DfsSlateOption
import com.pablitosb.sportsbook.data.dfs.SalarySource
import java.time.Instant
import java.time.LocalDate

enum class FdProjSort { PROJ, VALUE, SALARY, POS }

enum class FdPosFilter { ALL, P, C, B1, B2, B3, SS, OF, DH }

data class FdProjRow(
    val mlbId: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val pos: String,
    val salary: Int,
    val proj: Float,
    val ceiling: Float,
    val value: Float?,
    val isPitcher: Boolean,
    val inPostedLineup: Boolean,
    val gameTimeLabel: String,
    val driver: String,
    val ownPlaceholder: String = "—",
)

data class FdProjBoard(
    val slateDate: LocalDate,
    val fetchedAt: Instant,
    val rows: List<FdProjRow>,
    val slates: List<DfsSlateOption>,
    val selectedSlateId: String,
    val salarySource: SalarySource,
    val salaryNote: String,
    val fdApiNote: String,
    val sourceLabel: String,
    val emptyReason: String? = null,
)

object FdProjSorter {
    fun filter(rows: List<FdProjRow>, pos: FdPosFilter): List<FdProjRow> {
        if (pos == FdPosFilter.ALL) return rows
        return rows.filter { matches(it, pos) }
    }

    fun sort(rows: List<FdProjRow>, key: FdProjSort, ascending: Boolean): List<FdProjRow> {
        val ordered = when (key) {
            FdProjSort.PROJ -> rows.sortedByDescending { it.proj }
            FdProjSort.VALUE -> rows.sortedWith(
                compareBy<FdProjRow> { it.value == null }.thenByDescending { it.value ?: 0f },
            )
            FdProjSort.SALARY -> rows.sortedByDescending { it.salary }
            FdProjSort.POS -> rows.sortedWith(compareBy<FdProjRow> { it.pos }.thenByDescending { it.proj })
        }
        val flip = when (key) {
            FdProjSort.PROJ, FdProjSort.SALARY, FdProjSort.VALUE -> ascending
            FdProjSort.POS -> ascending
        }
        return if (flip) {
            if (key == FdProjSort.VALUE) {
                val missing = ordered.filter { it.value == null }
                ordered.filter { it.value != null }.reversed() + missing
            } else {
                ordered.reversed()
            }
        } else {
            ordered
        }
    }

    fun value(proj: Float, salary: Int): Float? =
        if (salary > 0) proj / (salary / 1000f) else null

    private fun matches(row: FdProjRow, pos: FdPosFilter): Boolean {
        val p = row.pos.uppercase()
        return when (pos) {
            FdPosFilter.ALL -> true
            FdPosFilter.P -> row.isPitcher || p == "P"
            FdPosFilter.C -> p.contains("C") && !p.contains("CF")
            FdPosFilter.B1 -> p.contains("1B")
            FdPosFilter.B2 -> p.contains("2B")
            FdPosFilter.B3 -> p.contains("3B")
            FdPosFilter.SS -> p.contains("SS")
            FdPosFilter.OF -> p.contains("OF") || p == "LF" || p == "CF" || p == "RF"
            FdPosFilter.DH -> p.contains("DH") || p.contains("UTIL")
        }
    }
}
