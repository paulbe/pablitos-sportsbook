package com.pablitosb.sportsbook.data.nfl

enum class QbSort {
    PROJ_YDS,
    PROJ_RUSH,
    PROJ_FD,
    COMP,
    IAY,
}

object QbSorter {
    fun defaultAscending(key: QbSort): Boolean = false

    fun sort(qbs: List<QbProjection>, key: QbSort, ascending: Boolean): List<QbProjection> {
        val by = when (key) {
            QbSort.PROJ_YDS -> compareBy<QbProjection> { it.projYds }.thenBy { it.ydsCeiling }
            QbSort.PROJ_RUSH -> compareBy<QbProjection> { it.projRushYds }.thenBy { it.rushCeiling }
            QbSort.PROJ_FD -> compareBy<QbProjection> { it.fdProj }.thenBy { it.fdCeiling }
            QbSort.COMP -> compareBy<QbProjection> { it.compPct }
            QbSort.IAY -> compareBy<QbProjection> { it.iay }
        }
        return qbs.sortedWith(if (ascending) by else by.reversed())
    }
}
