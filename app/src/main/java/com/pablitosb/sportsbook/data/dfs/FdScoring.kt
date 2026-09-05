package com.pablitosb.sportsbook.data.dfs

import com.pablitosb.sportsbook.data.hr.HrCalculator
import com.pablitosb.sportsbook.data.mlb.StatMath
import com.pablitosb.sportsbook.data.projections.HitterProjection
import com.pablitosb.sportsbook.data.projections.PitcherProjection
import kotlin.math.exp

/**
 * FanDuel MLB classic scoring used to turn our projections into expected points.
 *
 * Hitters: 1B 3 · 2B 6 · 3B 9 · HR 12 · RBI 3.5 · R 3.2 · BB 3 · SB 6
 * Pitchers: W 6 · QS 4 · ER −3 · SO 3 · IP 3 (per inning)
 *
 * Not live FanDuel scoring-rule confirmation — this is the long-standing public table.
 */
object FdScoring {
    const val SALARY_CAP = 35_000
    val SLOTS = listOf("P", "C/1B", "2B", "3B", "SS", "OF", "OF", "OF", "UTIL")

    fun hitterPoints(
        hit: HrCalculator.HittingSample,
        hr: HrCalculator.Result,
        seasonHits: Int,
        doubles: Int,
        triples: Int,
        hrCount: Int,
        bb: Int,
        sb: Int,
        runs: Int,
        rbi: Int,
        pa: Int,
        games: Int,
    ): Float {
        val expPa = hr.expectedPa
        val perPa = { n: Int -> if (pa > 0) n.toFloat() / pa * expPa else 0f }
        val expHr = hr.expectedHr
        val exp2b = perPa(doubles)
        val exp3b = perPa(triples)
        val expHits = perPa(seasonHits)
        val exp1b = (expHits - exp2b - exp3b - expHr).coerceAtLeast(0f)
        val expBb = perPa(bb)
        val gamesSafe = games.coerceAtLeast(1)
        val expSb = sb.toFloat() / gamesSafe
        val expR = 0.11f * expPa + 0.40f * expHr + 0.15f * expBb
        val expRbi = 0.10f * expPa + 0.55f * expHr
        return exp1b * 3f + exp2b * 6f + exp3b * 9f + expHr * 12f +
            expRbi * 3.5f + expR * 3.2f + expBb * 3f + expSb * 6f
    }

    fun hitterPoints(h: HitterProjection): Float {
        val expPa = h.expectedPa
        val pa = h.seasonPa
        val perPa = { n: Int -> if (pa > 0) n.toFloat() / pa * expPa else 0f }
        val expHr = h.expectedHr
        val exp2b = perPa(h.seasonDoubles)
        val exp3b = perPa(h.seasonTriples)
        val expHits = h.expectedHits
        val exp1b = (expHits - exp2b - exp3b - expHr).coerceAtLeast(0f)
        val expBb = perPa(h.seasonBb)
        val gp = if (h.seasonPa > 0) (h.seasonPa / 4).coerceAtLeast(1) else 1
        val expSb = h.seasonSb.toFloat() / gp
        val expR = 0.11f * expPa + 0.40f * expHr + 0.15f * expBb
        val expRbi = 0.10f * expPa + 0.55f * expHr
        return exp1b * 3f + exp2b * 6f + exp3b * 9f + expHr * 12f +
            expRbi * 3.5f + expR * 3.2f + expBb * 3f + expSb * 6f
    }

    fun pitcherPoints(projKs: Float, expectedIp: Float, era: Float): Float {
        val er = (era * expectedIp / 9f).coerceAtLeast(0f)
        val qs = (1f / (1f + exp(-(expectedIp - 6.0f) * 1.4f))) *
            (1f / (1f + exp((era - 3.8f) * 0.7f)))
        val win = (0.22f + (expectedIp - 5.2f) * 0.06f - (era - 3.8f) * 0.04f).coerceIn(0.12f, 0.48f)
        return win * 6f + qs * 4f + er * -3f + projKs * 3f + expectedIp * 3f
    }

    fun pitcherPoints(p: PitcherProjection): Float =
        pitcherPoints(p.nextStartKs, p.expectedIp, p.era)

    fun exampleSalary(isPitcher: Boolean, proj: Float, rankAmongSide: Int, sideCount: Int): Int {
        val frac = if (sideCount <= 1) 0.5f else 1f - rankAmongSide.toFloat() / (sideCount - 1).coerceAtLeast(1)
        return if (isPitcher) {
            (6_200 + proj * 145 + frac * 1_800).toInt().coerceIn(5_500, 12_500) / 100 * 100
        } else {
            (2_100 + proj * 155 + frac * 700).toInt().coerceIn(2_000, 6_200) / 100 * 100
        }
    }

    fun fdSlotsFor(pos: String): Set<String> {
        return when (pos.uppercase()) {
            "P" -> setOf("P")
            "C", "1B" -> setOf("C/1B", "UTIL")
            "2B" -> setOf("2B", "UTIL")
            "3B" -> setOf("3B", "UTIL")
            "SS" -> setOf("SS", "UTIL")
            "LF", "CF", "RF", "OF" -> setOf("OF", "UTIL")
            "TWP" -> setOf("OF", "UTIL")
            "DH" -> setOf("UTIL")
            else -> setOf("UTIL")
        }
    }

    fun primaryPos(pos: String): String {
        return when (pos.uppercase()) {
            "P" -> "P"
            "C", "1B" -> "C/1B"
            "2B" -> "2B"
            "3B" -> "3B"
            "SS" -> "SS"
            "LF", "CF", "RF", "OF", "TWP" -> "OF"
            else -> "UTIL"
        }
    }
}
