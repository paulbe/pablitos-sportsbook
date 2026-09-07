package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Outlook
import kotlin.math.roundToInt

/**
 * Projected-starter outlook (proj K% v1, expected BF v2).
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
 * - expectedBF (v2) = clamp(projIp × season BF/IP, 12…32); ×0.65 if
 *   season IP/GS < 4.0 or last start IP < 3.0. Prefer the same projIp
 *   already computed for Proj Outs; otherwise the Outs fallback (neutral
 *   opp / wx / work).
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

    fun fallbackProjIp(sample: PitchingSample?): Float =
        ProjOutsCalculator.project(
            sample?.let { workload(it) },
            ProjOutsCalculator.Context(opponent = null, envBoostPct = 0, rain = false),
        ).projIp

    /**
     * v2 expected batters faced. [projIp] should be the Proj Outs IP when
     * the board has a matchup; otherwise [fallbackProjIp].
     */
    fun expectedBf(sample: PitchingSample?, projIp: Float): Float {
        val bfPerIp = if (sample != null && sample.seasonIp > 0f && sample.seasonBf > 0) {
            sample.seasonBf.toFloat() / sample.seasonIp
        } else {
            DEFAULT_BF / ProjOutsCalculator.LEAGUE_IP_GS
        }
        var bf = (projIp * bfPerIp).coerceIn(12f, 32f)
        val seasonIpGs = if (sample != null && sample.seasonGs > 0 && sample.seasonIp > 0f) {
            sample.seasonIp / sample.seasonGs
        } else {
            null
        }
        val last = sample?.lastStartIp
        if ((seasonIpGs != null && seasonIpGs < 4.0f) || (last != null && last < 3.0f)) {
            bf *= 0.65f
        }
        return bf
    }

    fun project(sample: PitchingSample?, projIp: Float? = null): Projection {
        if (sample == null || sample.seasonBf <= 0 && sample.recentBf <= 0) {
            val ip = projIp ?: fallbackProjIp(null)
            return Projection(
                projK = LEAGUE_K,
                nextStartKs = LEAGUE_K * expectedBf(null, ip),
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

        val ip = projIp ?: fallbackProjIp(sample)
        val bf = expectedBf(sample, ip)

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
            nextStartKs = projK * bf,
            outlookScore = score,
            outlook = outlook,
            trend = sample.lastStartKs,
        )
    }

    fun workload(sample: PitchingSample): ProjOutsCalculator.Workload =
        ProjOutsCalculator.Workload(
            seasonIp = sample.seasonIp,
            seasonGs = sample.seasonGs,
            recentIp = sample.recentIp,
            recentGs = sample.recentGs,
            lastStartIp = sample.lastStartIp,
            last5Ip = sample.last5Ip,
        )

    private fun rate(so: Int, bf: Int): Float? {
        if (bf <= 0) return null
        return so.toFloat() / bf.toFloat()
    }
}
