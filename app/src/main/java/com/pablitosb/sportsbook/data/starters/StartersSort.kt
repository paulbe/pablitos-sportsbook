package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Starter

enum class StartersSort {
    PROG,
    PROJ_KS,
    XWOBA,
    PROJ_OUTS,
}

object StartersSorter {
    fun defaultAscending(key: StartersSort): Boolean = when (key) {
        StartersSort.PROG -> false
        StartersSort.PROJ_KS -> false
        StartersSort.XWOBA -> true
        StartersSort.PROJ_OUTS -> false
    }

    fun sort(
        starters: List<Starter>,
        key: StartersSort,
        ascending: Boolean,
    ): List<Starter> {
        val ordered = when (key) {
            StartersSort.PROG -> starters.sortedWith(
                compareByDescending<Starter> { it.outlookScore }.thenByDescending { it.nextStartKs },
            )
            StartersSort.PROJ_KS -> starters.sortedByDescending { it.nextStartKs }
            StartersSort.XWOBA -> starters.sortedWith(
                compareBy<Starter> { it.xwoba == null }.thenBy { it.xwoba ?: Float.MAX_VALUE },
            )
            StartersSort.PROJ_OUTS -> starters.sortedByDescending { it.projOuts }
        }
        val needsFlip = when (key) {
            StartersSort.PROG, StartersSort.PROJ_KS, StartersSort.PROJ_OUTS -> ascending
            StartersSort.XWOBA -> !ascending
        }
        return if (needsFlip) {
            if (key == StartersSort.XWOBA) {
                val missing = ordered.filter { it.xwoba == null }
                val have = ordered.filter { it.xwoba != null }.reversed()
                have + missing
            } else {
                ordered.reversed()
            }
        } else {
            ordered
        }
    }
}
