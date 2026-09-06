package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.projections.HitterProjection

/**
 * FanDuel hitter points for Daily Batters.
 *
 * 1B=3 · 2B=6 · 3B=9 · HR=12 · RBI=3.5 · R=3.2 · BB/HBP=3 · SB=6
 *
 * Expected counting stats come from season rates × expected PA, with HR
 * from the existing game-HR model (talent × park × weather × pitcher × platoon).
 *
 * ```
 * E[R]   = 0.11·PA + 0.40·HR + 0.15·(BB+HBP)
 * E[RBI] = 0.10·PA + 0.55·HR
 * Proj   = 3·1B + 6·2B + 9·3B + 12·HR + 3.5·RBI + 3.2·R + 3·(BB+HBP) + 6·SB
 * ```
 *
 * Floor / ceiling stress the same counting stats (shorter night / bigger night).
 */
object BatterFdCalculator {
    data class Counting(
        val singles: Float,
        val doubles: Float,
        val triples: Float,
        val hr: Float,
        val runs: Float,
        val rbi: Float,
        val walks: Float,
        val sb: Float,
    ) {
        val hits: Float get() = singles + doubles + triples + hr
        val hrr: Float get() = hits + runs + rbi
    }

    data class Result(
        val floor: Float,
        val proj: Float,
        val ceiling: Float,
        val counting: Counting,
    )

    fun fromHitter(h: HitterProjection): Counting {
        val pa = h.seasonPa
        val expPa = h.expectedPa
        val perPa = { n: Int -> if (pa > 0) n.toFloat() / pa * expPa else 0f }
        val expHr = h.expectedHr
        val exp2b = perPa(h.seasonDoubles)
        val exp3b = perPa(h.seasonTriples)
        val expHits = h.expectedHits
        val exp1b = (expHits - exp2b - exp3b - expHr).coerceAtLeast(0f)
        val expBb = perPa(h.seasonBb + h.seasonHbp)
        val gp = if (h.seasonPa > 0) (h.seasonPa / 4).coerceAtLeast(1) else 1
        val expSb = h.seasonSb.toFloat() / gp
        val expR = 0.11f * expPa + 0.40f * expHr + 0.15f * expBb
        val expRbi = 0.10f * expPa + 0.55f * expHr
        return Counting(exp1b, exp2b, exp3b, expHr, expR, expRbi, expBb, expSb)
    }

    fun project(h: HitterProjection): Result = project(fromHitter(h))

    fun project(c: Counting): Result {
        val proj = score(c)
        val floor = score(
            c.copy(
                singles = c.singles * 0.72f,
                doubles = c.doubles * 0.68f,
                triples = c.triples * 0.55f,
                hr = c.hr * 0.50f,
                runs = c.runs * 0.70f,
                rbi = c.rbi * 0.70f,
                walks = c.walks * 0.78f,
                sb = c.sb * 0.45f,
            ),
        )
        val ceil = score(
            c.copy(
                singles = c.singles * 1.18f,
                doubles = c.doubles * 1.28f,
                triples = c.triples * 1.35f,
                hr = c.hr * 1.55f,
                runs = c.runs * 1.28f,
                rbi = c.rbi * 1.28f,
                walks = c.walks * 1.12f,
                sb = c.sb * 1.50f,
            ),
        )
        return Result(
            floor = floor.coerceAtMost(proj - 0.4f),
            proj = proj,
            ceiling = ceil.coerceAtLeast(proj + 0.8f),
            counting = c,
        )
    }

    fun score(c: Counting): Float =
        c.singles * 3f + c.doubles * 6f + c.triples * 9f + c.hr * 12f +
            c.rbi * 3.5f + c.runs * 3.2f + c.walks * 3f + c.sb * 6f
}
