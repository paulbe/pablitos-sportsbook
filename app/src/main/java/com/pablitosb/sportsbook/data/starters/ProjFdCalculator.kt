package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.mlb.TeamOffense
import kotlin.math.exp

/**
 * FanDuel pitcher points for Projected Starters.
 *
 * Public FD table: **SO = 3**, **IP = 3 / inning** (1 per out), **W = 6**,
 * **QS = 4**, **ER = −3**.
 *
 * ```
 * Proj FD = 3×ProjKs + ProjOuts + 6×P(W) + 4×P(QS) − 3×E[ER]
 * ```
 *
 * [ProjOuts] already equals IP×3, so it is the IP scoring term.
 *
 * Heuristics (best-effort — not a pitch-level or Vegas model):
 * - **matchup ERA** = shrink(season ERA, IP, league 4.20, prior 40 IP)
 *   then × (1 + (opp OPS − 0.711)×0.55) × (1 + WeatherBoost%×0.18)
 *   Rain +8% ERA.
 * - **E[ER]** = matchupERA × projIP / 9
 * - **P(QS)** = σ((IP−6.0)×1.4) × σ(−(ERA−3.80)×0.70)
 *   (6+ IP and a low-ERA stand-in for ≤3 ER). Home ×1.04, rain ×0.75.
 * - **P(W)** = 0.22 + (IP−5.2)×0.06 − (ERA−3.80)×0.04
 *   +0.03 home, −0.12×(oppOPS−0.711), −0.06 rain. Clamp 0.10…0.50.
 *
 * Floor / ceiling are the same formula on stressed inputs:
 * - Floor: 0.78×IP, 0.72×Ks, 1.35×ER, 0.45×P(W), 0.40×P(QS)
 * - Ceiling: 1.18×IP (cap 7.2), 1.22×Ks, 0.65×ER, 1.45×P(W) cap 0.65,
 *   1.50×P(QS) cap 0.80
 */
object ProjFdCalculator {
    const val LEAGUE_ERA = 4.20f
    const val LEAGUE_OPS = 0.711f
    const val SHRINK_IP = 40f

    data class Context(
        val seasonEra: Float?,
        val seasonIp: Float,
        val opponent: TeamOffense?,
        val envBoostPct: Int,
        val rain: Boolean,
        val homeStart: Boolean,
    )

    data class Result(
        val floor: Float,
        val proj: Float,
        val ceiling: Float,
        val pWin: Float,
        val pQs: Float,
        val expectedEr: Float,
    )

    fun project(projKs: Float, projIp: Float, ctx: Context): Result {
        val ip = projIp.coerceIn(3.5f, 7.2f)
        val ks = projKs.coerceAtLeast(0f)
        val outs = ip * 3f
        val era = matchupEra(ctx)
        val er = (era * ip / 9f).coerceAtLeast(0.2f)
        val pQs = pQualityStart(ip, era, ctx)
        val pW = pWin(ip, era, ctx)
        val proj = score(ks, outs, pW, pQs, er)

        val floor = score(
            ks = ks * 0.72f,
            outs = (ip * 0.78f).coerceAtLeast(3.0f) * 3f,
            pWin = (pW * 0.45f).coerceIn(0.04f, 0.28f),
            pQs = (pQs * 0.40f).coerceIn(0.02f, 0.35f),
            er = (er * 1.35f).coerceAtMost(6.5f),
        )
        val ceil = score(
            ks = ks * 1.22f,
            outs = (ip * 1.18f).coerceAtMost(7.2f) * 3f,
            pWin = (pW * 1.45f).coerceIn(0.16f, 0.65f),
            pQs = (pQs * 1.50f).coerceIn(0.10f, 0.80f),
            er = (er * 0.65f).coerceAtLeast(0.15f),
        )
        return Result(
            floor = floor.coerceAtMost(proj - 0.4f),
            proj = proj,
            ceiling = ceil.coerceAtLeast(proj + 0.8f),
            pWin = pW,
            pQs = pQs,
            expectedEr = er,
        )
    }

    fun score(ks: Float, outs: Float, pWin: Float, pQs: Float, er: Float): Float =
        3f * ks + outs + 6f * pWin + 4f * pQs - 3f * er

    fun matchupEra(ctx: Context): Float {
        val raw = ctx.seasonEra?.takeIf { it in 0.5f..9.5f } ?: LEAGUE_ERA
        val n = ctx.seasonIp.coerceAtLeast(1f)
        val shrunk = StatMath.shrink(raw, n, LEAGUE_ERA, SHRINK_IP)
        val opsTilt = ctx.opponent?.ops?.let { 1f + (it - LEAGUE_OPS) * 0.55f } ?: 1f
        val wxTilt = 1f + ctx.envBoostPct / 100f * 0.18f
        val rainTilt = if (ctx.rain) 1.08f else 1f
        return (shrunk * opsTilt * wxTilt * rainTilt).coerceIn(2.2f, 7.2f)
    }

    fun pQualityStart(ip: Float, era: Float, ctx: Context): Float {
        val depth = sigmoid((ip - 6.0f) * 1.4f)
        val clean = sigmoid(-(era - 3.80f) * 0.70f)
        var p = depth * clean
        if (ctx.homeStart) p *= 1.04f
        if (ctx.rain) p *= 0.75f
        return p.coerceIn(0.05f, 0.72f)
    }

    fun pWin(ip: Float, era: Float, ctx: Context): Float {
        var p = 0.22f + (ip - 5.2f) * 0.06f - (era - 3.80f) * 0.04f
        if (ctx.homeStart) p += 0.03f
        ctx.opponent?.ops?.let { p -= (it - LEAGUE_OPS) * 0.12f }
        if (ctx.rain) p -= 0.06f
        return p.coerceIn(0.10f, 0.50f)
    }

    private fun sigmoid(x: Float): Float = (1f / (1f + exp(-x))).toFloat()
}
