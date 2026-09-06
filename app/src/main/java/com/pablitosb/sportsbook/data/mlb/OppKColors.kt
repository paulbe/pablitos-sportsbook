package com.pablitosb.sportsbook.data.mlb

import java.util.Locale

/**
 * Opponent **team K%** (SO / PA) color on Projected Starters.
 *
 * Pitcher's own abbreviation stays white. Only the opponent is tinted:
 * - **red** — low K% (contact lineup, fewer punchouts)
 * - **grey** — middle tertile
 * - **green** — high K% (K-prone lineup)
 *
 * Cuts are the 33rd / 67th percentiles of the current season's 30 MLB
 * club rates when we have ≥ 20 teams. Otherwise documented 2024–25
 * tertile-style fallbacks: **21.6%** / **23.4%**.
 */
enum class OppKTier { LOW, MID, HIGH, UNKNOWN }

data class TeamOffense(
    val kRate: Float,
    val ops: Float?,
    val pa: Int,
)

data class OppKScale(
    val lowMax: Float,
    val highMin: Float,
    val fromLiveTertiles: Boolean,
) {
    fun tier(rate: Float?): OppKTier {
        if (rate == null) return OppKTier.UNKNOWN
        return when {
            rate < lowMax -> OppKTier.LOW
            rate > highMin -> OppKTier.HIGH
            else -> OppKTier.MID
        }
    }

    fun legend(): String {
        val lo = String.format(Locale.US, "%.1f", lowMax * 100f)
        val hi = String.format(Locale.US, "%.1f", highMin * 100f)
        val src = if (fromLiveTertiles) "this season’s MLB tertiles" else "2024–25 MLB tertile cuts"
        return "Opp K% (team SO/PA, $src): red < $lo% · grey mid · green > $hi%"
    }

    /** Daily Batters invert: low opposing-pitcher K% is favorable (green). */
    fun batterLegend(): String {
        val lo = String.format(Locale.US, "%.1f", lowMax * 100f)
        val hi = String.format(Locale.US, "%.1f", highMin * 100f)
        val src = if (fromLiveTertiles) "today’s starter K% tertiles" else "2024–25 MLB tertile cuts"
        return "Opp pitcher K% ($src): green (favorable) < $lo% · grey mid · red (tough) > $hi%"
    }

    companion object {
        /** ~33rd percentile of 2024–25 MLB team SO/PA. */
        const val FALLBACK_LOW = 0.216f

        /** ~67th percentile of 2024–25 MLB team SO/PA. */
        const val FALLBACK_HIGH = 0.234f

        fun fallback(): OppKScale = OppKScale(FALLBACK_LOW, FALLBACK_HIGH, fromLiveTertiles = false)

        fun fromRates(rates: List<Float>): OppKScale {
            if (rates.size < 20) return fallback()
            val sorted = rates.sorted()
            return OppKScale(
                lowMax = percentile(sorted, 1f / 3f),
                highMin = percentile(sorted, 2f / 3f),
                fromLiveTertiles = true,
            )
        }

        fun percentile(sorted: List<Float>, p: Float): Float {
            if (sorted.isEmpty()) return 0f
            if (sorted.size == 1) return sorted[0]
            val idx = (sorted.lastIndex) * p.coerceIn(0f, 1f)
            val lo = idx.toInt()
            val hi = (lo + 1).coerceAtMost(sorted.lastIndex)
            val t = idx - lo
            return sorted[lo] * (1f - t) + sorted[hi] * t
        }
    }
}
