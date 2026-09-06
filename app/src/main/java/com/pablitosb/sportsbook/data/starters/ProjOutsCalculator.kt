package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.mlb.TeamOffense

/**
 * Matchup-adjusted projected outs for a starter.
 *
 * ```
 * seasonIpGs  = season IP / GS
 * recentIpGs  = last-5 GS IP / GS
 * blended     = 0.55·recent + 0.45·season   (or whichever side exists)
 * baseIp      = shrink(blended, GS, league 5.40, prior 8 GS)
 *
 * oppMult     = 1 − (opp OPS − 0.711) × 0.40     // high offense → fewer IP
 * wxMult      = rain ? 0.86 : 1 − WeatherBoost% × 0.22
 * workMult    = early-exit / heavy-workload haircut
 *
 * projIp      = clamp(baseIp × opp × wx × work, 3.5 … 7.2)
 * projOuts    = projIp × 3
 * ```
 *
 * Weather boost is the existing park + wind + temp number (positive =
 * hitter-friendly). Rain is a large IP cut, not a K boost.
 */
object ProjOutsCalculator {
    const val LEAGUE_IP_GS = 5.40f
    const val LEAGUE_OPS = 0.711f
    const val SHRINK_GS = 8f
    const val MIN_IP = 3.5f
    const val MAX_IP = 7.2f
    const val EARLY_EXIT_IP = 5.0f
    const val HEAVY_START_IP = 7.0f

    data class Workload(
        val seasonIp: Float,
        val seasonGs: Int,
        val recentIp: Float,
        val recentGs: Int,
        val lastStartIp: Float?,
        val last5Ip: List<Float>,
    )

    data class Context(
        val opponent: TeamOffense?,
        val envBoostPct: Int,
        val rain: Boolean,
    )

    data class Result(
        val projIp: Float,
        val projOuts: Float,
    )

    fun project(work: Workload?, ctx: Context): Result {
        val seasonIpGs = ipGs(work?.seasonIp, work?.seasonGs)
        val recentIpGs = ipGs(work?.recentIp, work?.recentGs)
        val blended = when {
            seasonIpGs != null && recentIpGs != null -> 0.55f * recentIpGs + 0.45f * seasonIpGs
            recentIpGs != null -> recentIpGs
            seasonIpGs != null -> seasonIpGs
            else -> LEAGUE_IP_GS
        }
        val n = (work?.seasonGs ?: 0).coerceAtLeast(1).toFloat()
        val base = StatMath.shrink(blended, n, LEAGUE_IP_GS, SHRINK_GS)

        val oppMult = ctx.opponent?.ops?.let { ops ->
            (1f - (ops - LEAGUE_OPS) * 0.40f).coerceIn(0.88f, 1.10f)
        } ?: 1f

        val wxMult = when {
            ctx.rain -> 0.86f
            else -> (1f - ctx.envBoostPct / 100f * 0.22f).coerceIn(0.88f, 1.10f)
        }

        val last = work?.lastStartIp
        val early = work?.last5Ip.orEmpty().count { it > 0f && it < EARLY_EXIT_IP }
        val recentHeavy = (work?.recentGs ?: 0) > 0 &&
            (work?.recentIp ?: 0f) / (work?.recentGs ?: 1) >= 6.4f
        val workMult = when {
            last != null && last < EARLY_EXIT_IP && early >= 2 -> 0.88f
            last != null && last < EARLY_EXIT_IP -> 0.92f
            early >= 3 -> 0.90f
            last != null && last >= HEAVY_START_IP -> 0.96f
            recentHeavy -> 0.96f
            else -> 1f
        }

        val ip = (base * oppMult * wxMult * workMult).coerceIn(MIN_IP, MAX_IP)
        return Result(projIp = ip, projOuts = ip * 3f)
    }

    private fun ipGs(ip: Float?, gs: Int?): Float? {
        if (ip == null || gs == null || gs <= 0 || ip <= 0f) return null
        return ip / gs
    }
}
