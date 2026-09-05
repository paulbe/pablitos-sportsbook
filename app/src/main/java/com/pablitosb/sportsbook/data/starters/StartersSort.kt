package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.WxTag

enum class StartersSort {
    PROG,
    PROJ_KS,
    XWOBA,
    BOOST,
    TIME,
}

object StartersSorter {
    fun defaultAscending(key: StartersSort): Boolean = when (key) {
        StartersSort.PROG -> false
        StartersSort.PROJ_KS -> false
        StartersSort.XWOBA -> true
        StartersSort.BOOST -> false
        StartersSort.TIME -> true
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
            StartersSort.BOOST -> starters.sortedWith(
                compareBy<Starter> { it.wxTag == WxTag.RAIN_RISK }
                    .thenByDescending { it.envBoostPct },
            )
            StartersSort.TIME -> starters.sortedWith(
                compareBy<Starter> { it.gameStart == null }.thenBy { it.gameStart },
            )
        }
        val needsFlip = when (key) {
            StartersSort.PROG, StartersSort.PROJ_KS, StartersSort.BOOST -> ascending
            StartersSort.XWOBA, StartersSort.TIME -> !ascending
        }
        return if (needsFlip) {
            if (key == StartersSort.BOOST) {
                val rain = ordered.filter { it.wxTag == WxTag.RAIN_RISK }
                val dry = ordered.filter { it.wxTag != WxTag.RAIN_RISK }.reversed()
                dry + rain
            } else if (key == StartersSort.XWOBA) {
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
