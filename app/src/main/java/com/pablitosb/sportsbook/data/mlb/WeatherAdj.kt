package com.pablitosb.sportsbook.data.mlb

import com.pablitosb.sportsbook.data.model.Weather

object WeatherAdj {
    fun factor(condition: String, tempF: Int, wind: String): Float {
        val c = condition.lowercase()
        if (c.contains("dome") || c.contains("roof") || c.contains("indoor") || c.contains("closed")) {
            return 1.00f
        }
        var f = 1.00f
        val mph = Regex("""(\d+)\s*mph""", RegexOption.IGNORE_CASE)
            .find(wind)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val w = wind.lowercase()
        f *= when {
            w.contains("out to") -> 1f + (mph * 0.012f).coerceAtMost(0.16f)
            w.contains("in from") -> 1f - (mph * 0.012f).coerceAtMost(0.16f)
            else -> 1f
        }
        f *= when {
            tempF >= 85 -> 1.05f
            tempF >= 75 -> 1.02f
            tempF in 1..50 -> 0.94f
            tempF in 51..60 -> 0.97f
            else -> 1f
        }
        return f.coerceIn(0.80f, 1.25f)
    }

    fun icon(condition: String): Weather {
        val c = condition.lowercase()
        return if (c.contains("sun") || c.contains("clear")) Weather.SUN else Weather.CLOUD
    }
}
