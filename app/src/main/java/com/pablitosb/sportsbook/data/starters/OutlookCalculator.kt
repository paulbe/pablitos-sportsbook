package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Outlook
import kotlin.math.roundToInt

/**
 * Projected-starter outlook (v1).
 *
 * Hypotheses
 * - H1: MLB Stats API `probablePitcher` is today’s official probable SP.
 * - H2: SwStr% / Whiff% / CSW% are not on `people/.../stats` pitching splits, so
 *   we treat **K% (SO/BF)** as the skill signal and **strike%** as a thin
 *   process proxy (more strikes → slightly higher implied K%).
 * - H3: Last 5 games started ≈ recent 15–30 day form for a typical SP.
 * - H4: League priors for the 2020s: K% 22.5%, strike% 64%, 26 BF / start.
 *
 * Formulas
 * - seasonK = SO_season / BF_season
 * - recentK = SO_last5GS / BF_last5GS (falls back to seasonK)
 * - processK = leagueK + (strike% − 0.64) × 0.40
 * - rawK = 0.50·recentK + 0.30·seasonK + 0.20·processK
 * - projK = (rawK·n + leagueK·80) / (n + 80) where n = season BF (min 1)
 * - expectedBF = season BF/GS, else last-start BF, else 26
 * - nextStartKs = projK × expectedBF
 * - outlookScore = round( (projK−leagueK)×100 + (recentK−seasonK)×180 )
 * - PROG if score ≥ 5, REG if score ≤ −5, else STABLE
 *
 * Rank by outlookScore descending, then projK descending.
 */
object OutlookCalculator {
    const val LEAGUE_K = 0.225f
    const val LEAGUE_STRIKE = 0.64f
    const val DEFAULT_BF = 26f
    const val SHRINK_PRIOR_BF = 80f

    data class PitchingSample(
        val seasonSo: Int,
        val seasonBf: Int,
        val seasonGs: Int,
        val seasonStrikePct: Float?,
        val recentSo: Int,
        val recentBf: Int,
        val lastStartBf: Int?,
        val lastStartKs: List<Float>,
        val seasonIp: Float = 0f,
        val recentIp: Float = 0f,
        val recentGs: Int = 0,
        val lastStartIp: Float? = null,
        val last5Ip: List<Float> = emptyList(),
        val seasonEr: Int = 0,
    )

    data class Projection(
        val projK: Float,
        val nextStartKs: Float,
        val outlookScore: Int,
        val outlook: Outlook,
        val trend: List<Float>,
    )

    fun project(sample: PitchingSample?): Projection {
        if (sample == null || sample.seasonBf <= 0 && sample.recentBf <= 0) {
            return Projection(
                projK = LEAGUE_K,
                nextStartKs = LEAGUE_K * DEFAULT_BF,
                outlookScore = 0,
                outlook = Outlook.STABLE,
                trend = emptyList(),
            )
        }
        val seasonK = rate(sample.seasonSo, sample.seasonBf) ?: LEAGUE_K
        val recentK = rate(sample.recentSo, sample.recentBf) ?: seasonK
        val strike = sample.seasonStrikePct ?: LEAGUE_STRIKE
        val processK = (LEAGUE_K + (strike - LEAGUE_STRIKE) * 0.40f).coerceIn(0.10f, 0.40f)
        val recentWeight = if (sample.recentBf >= 40) 0.50f else 0.30f
        val seasonWeight = if (sample.recentBf >= 40) 0.30f else 0.50f
        val rawK = recentWeight * recentK + seasonWeight * seasonK + 0.20f * processK
        val n = sample.seasonBf.coerceAtLeast(1).toFloat()
        val projK = ((rawK * n + LEAGUE_K * SHRINK_PRIOR_BF) / (n + SHRINK_PRIOR_BF))
            .coerceIn(0.08f, 0.42f)

        val expectedBf = when {
            sample.seasonGs > 0 && sample.seasonBf > 0 ->
                sample.seasonBf.toFloat() / sample.seasonGs
            sample.lastStartBf != null && sample.lastStartBf > 0 ->
                sample.lastStartBf.toFloat()
            else -> DEFAULT_BF
        }.coerceIn(15f, 32f)

        val quality = (projK - LEAGUE_K) * 100f
        val trajectory = (recentK - seasonK) * 180f
        val score = (quality + trajectory).roundToInt().coerceIn(-20, 20)
        val outlook = when {
            score >= 5 -> Outlook.PROG
            score <= -5 -> Outlook.REG
            else -> Outlook.STABLE
        }
        return Projection(
            projK = projK,
            nextStartKs = projK * expectedBf,
            outlookScore = score,
            outlook = outlook,
            trend = sample.lastStartKs,
        )
    }

    private fun rate(so: Int, bf: Int): Float? {
        if (bf <= 0) return null
        return so.toFloat() / bf.toFloat()
    }
}
