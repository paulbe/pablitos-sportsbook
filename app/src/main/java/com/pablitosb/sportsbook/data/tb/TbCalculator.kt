package com.pablitosb.sportsbook.data.tb

import com.pablitosb.sportsbook.data.hr.HrCalculator
import com.pablitosb.sportsbook.data.mlb.StatMath

/**
 * Expected total bases (v1).
 *
 * ```
 * TB ≈ PA × (1·1B/PA + 2·2B/PA + 3·3B/PA + 4·HR/PA)
 * ```
 *
 * Season rates from MLB Stats API (H, 2B, 3B, HR, PA). Singles =
 * H − 2B − 3B − HR. Each rate shrinks toward a 2020s PA prior (80 PA).
 *
 * Context (same park / weather / pitcher / platoon stack as Daily HR):
 * - HR/PA uses [HrCalculator] `p_PA` (full multipliers).
 * - 2B/3B get a muted tilt of those multipliers (extras move with HR parks).
 * - 1B gets a light tilt (singles are mostly park-neutral).
 *
 * SLG proxy = (TB/PA) / (AB/PA). Not Statcast xTB.
 */
object TbCalculator {
    const val LEAGUE_1B_PA = 0.145f
    const val LEAGUE_2B_PA = 0.045f
    const val LEAGUE_3B_PA = 0.0045f
    const val LEAGUE_AB_PA = 0.885f
    const val SHRINK_PA = 80f

    data class Sample(
        val hits: Int,
        val doubles: Int,
        val triples: Int,
        val hr: Int,
        val pa: Int,
        val ab: Int,
    )

    data class Result(
        val expectedTb: Float,
        val tbPerPa: Float,
        val slgProxy: Float,
        val rate1b: Float,
        val rate2b: Float,
        val rate3b: Float,
        val rateHr: Float,
    )

    fun project(sample: Sample, hr: HrCalculator.Result): Result {
        val pa = sample.pa.coerceAtLeast(0)
        val singles = (sample.hits - sample.doubles - sample.triples - sample.hr).coerceAtLeast(0)
        val raw1b = StatMath.rate(singles, pa) ?: LEAGUE_1B_PA
        val raw2b = StatMath.rate(sample.doubles, pa) ?: LEAGUE_2B_PA
        val raw3b = StatMath.rate(sample.triples, pa) ?: LEAGUE_3B_PA
        val n = pa.coerceAtLeast(1).toFloat()
        val talent1b = StatMath.shrink(raw1b, n, LEAGUE_1B_PA, SHRINK_PA).coerceIn(0.06f, 0.24f)
        val talent2b = StatMath.shrink(raw2b, n, LEAGUE_2B_PA, SHRINK_PA).coerceIn(0.010f, 0.090f)
        val talent3b = StatMath.shrink(raw3b, n, LEAGUE_3B_PA, SHRINK_PA).coerceIn(0.0005f, 0.020f)

        val park = 1f + hr.parkAdjPct / 100f
        val wx = 1f + hr.weatherAdjPct / 100f
        val pitcher = 1f + hr.pitcherAdjPct / 100f
        val platoon = 1f + hr.platoonAdjPct / 100f

        val r1 = (talent1b * tilt(park, 0.15f) * tilt(wx, 0.25f) * tilt(pitcher, 0.20f) * tilt(platoon, 0.40f))
            .coerceIn(0.05f, 0.26f)
        val r2 = (talent2b * tilt(park, 0.45f) * tilt(wx, 0.55f) * tilt(pitcher, 0.45f) * tilt(platoon, 0.55f))
            .coerceIn(0.008f, 0.10f)
        val r3 = (talent3b * tilt(park, 0.50f) * tilt(wx, 0.70f) * tilt(pitcher, 0.40f) * tilt(platoon, 0.50f))
            .coerceIn(0.0004f, 0.025f)
        val rHr = hr.pPa.coerceIn(0.004f, 0.14f)

        val tbPa = (1f * r1 + 2f * r2 + 3f * r3 + 4f * rHr).coerceIn(0.12f, 0.85f)
        val abPa = StatMath.shrink(
            StatMath.rate(sample.ab, pa) ?: LEAGUE_AB_PA,
            n,
            LEAGUE_AB_PA,
            SHRINK_PA,
        ).coerceIn(0.78f, 0.95f)
        val slg = (tbPa / abPa).coerceIn(0.20f, 0.85f)
        return Result(
            expectedTb = tbPa * hr.expectedPa,
            tbPerPa = tbPa,
            slgProxy = slg,
            rate1b = r1,
            rate2b = r2,
            rate3b = r3,
            rateHr = rHr,
        )
    }

    /** Blend a context multiplier toward 1.00. Weight 1 = full HR-style tilt. */
    fun tilt(factor: Float, weight: Float): Float =
        (1f + (factor - 1f) * weight).coerceIn(0.70f, 1.40f)
}
