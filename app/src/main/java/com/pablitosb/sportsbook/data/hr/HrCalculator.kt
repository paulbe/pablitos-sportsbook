package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.mlb.StatMath
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Game HR probability (v1).
 *
 * Pr(at least one HR) = 1 − (1 − p_PA)^PA
 * p_PA = talent × park × weather × pitcher × platoon
 *
 * Talent (no Statcast barrel/xHR on MLB Stats API):
 * - rawHrPa = HR / PA
 * - iso = SLG − AVG (ISO/FB-style proxy)
 * - fb = airOuts / (airOuts + groundOuts)
 * - blend = 0.70·rawHrPa + 0.20·(ISO / 0.160)·lgHrPa + 0.10·(FB / 0.48)·lgHrPa
 * - talent = shrink(blend toward 0.032 with an 80-PA prior)
 *
 * Context:
 * - park = static venue HR multiplier (see ParkFactors)
 * - weather = wind out/in × temperature (dome ≈ 1.00)
 * - pitcher = (pitcher HR/9 / 1.15) × mild FB-tendency bump
 * - platoon = 1.08 opposite, 0.92 same-side, 1.03 switch vs RHP
 *
 * Expected PA from batting order (4.65…3.85), else 4.10.
 */
object HrCalculator {
    const val LEAGUE_HR_PA = 0.032f
    const val LEAGUE_ISO = 0.160f
    const val LEAGUE_FB = 0.48f
    const val LEAGUE_HR9 = 1.15f
    const val SHRINK_PA = 80f

    private val paByOrder = floatArrayOf(0f, 4.65f, 4.55f, 4.45f, 4.35f, 4.25f, 4.15f, 4.05f, 3.95f, 3.85f)

    data class HittingSample(
        val hr: Int,
        val pa: Int,
        val avg: Float,
        val slg: Float,
        val airOuts: Int,
        val groundOuts: Int,
        val batSide: String,
        val order: Int?,
    )

    data class PitcherContext(
        val hr: Int,
        val ip: Float,
        val airOuts: Int,
        val groundOuts: Int,
        val throws: String,
    )

    data class Result(
        val talentPa: Float,
        val pPa: Float,
        val expectedPa: Float,
        val expectedHr: Float,
        val gameHrProb: Float,
        val talentGamePct: Int,
        val parkAdjPct: Int,
        val weatherAdjPct: Int,
        val pitcherAdjPct: Int,
        val platoonAdjPct: Int,
        val pitcherHr9: Float,
        val seasonHrPct: Float,
        val regressionLean: Boolean,
    )

    fun expectedPa(order: Int?): Float {
        val o = order ?: return 4.10f
        return paByOrder.getOrElse(o) { 4.10f }
    }

    fun project(
        hit: HittingSample,
        park: Float,
        weather: Float,
        pitcher: PitcherContext?,
    ): Result {
        val pa = hit.pa.coerceAtLeast(0)
        val rawHrPa = StatMath.rate(hit.hr, pa) ?: LEAGUE_HR_PA
        val iso = (hit.slg - hit.avg).coerceIn(0f, 0.50f)
        val fb = StatMath.rate(hit.airOuts, hit.airOuts + hit.groundOuts) ?: LEAGUE_FB
        val isoScaled = LEAGUE_HR_PA * (iso / LEAGUE_ISO)
        val fbScaled = LEAGUE_HR_PA * (fb / LEAGUE_FB)
        val blend = (0.70f * rawHrPa + 0.20f * isoScaled + 0.10f * fbScaled).coerceIn(0.005f, 0.12f)
        val talent = StatMath.shrink(blend, pa.coerceAtLeast(1).toFloat(), LEAGUE_HR_PA, SHRINK_PA)
            .coerceIn(0.008f, 0.10f)

        val ip = pitcher?.ip ?: 0f
        val hr9 = if (pitcher != null && ip > 0f) pitcher.hr * 9f / ip else LEAGUE_HR9
        val pitcherFb = if (pitcher != null) {
            StatMath.rate(pitcher.airOuts, pitcher.airOuts + pitcher.groundOuts) ?: LEAGUE_FB
        } else {
            LEAGUE_FB
        }
        val pitcherFactor = ((hr9 / LEAGUE_HR9) * (1f + (pitcherFb - LEAGUE_FB) * 0.25f))
            .coerceIn(0.70f, 1.40f)

        val throws = pitcher?.throws?.uppercase() ?: "R"
        val bat = hit.batSide.uppercase()
        val platoon = when {
            bat.startsWith("S") && throws.startsWith("R") -> 1.03f
            bat.startsWith("S") -> 1.00f
            bat.isBlank() || throws.isBlank() -> 1.00f
            bat.first() != throws.first() -> 1.08f
            else -> 0.92f
        }

        val parkF = park.coerceIn(0.80f, 1.35f)
        val wxF = weather.coerceIn(0.80f, 1.25f)
        val pPa = (talent * parkF * wxF * pitcherFactor * platoon).coerceIn(0.004f, 0.14f)
        val expPa = expectedPa(hit.order)
        val gameProb = (1f - (1f - pPa).pow(expPa)).coerceIn(0.01f, 0.55f)
        val talentGame = 1f - (1f - talent).pow(expPa)
        val seasonHrPct = (rawHrPa * 100f)
        return Result(
            talentPa = talent,
            pPa = pPa,
            expectedPa = expPa,
            expectedHr = pPa * expPa,
            gameHrProb = gameProb,
            talentGamePct = (talentGame * 100f).roundToInt(),
            parkAdjPct = ((parkF - 1f) * 100f).roundToInt(),
            weatherAdjPct = ((wxF - 1f) * 100f).roundToInt(),
            pitcherAdjPct = ((pitcherFactor - 1f) * 100f).roundToInt(),
            platoonAdjPct = ((platoon - 1f) * 100f).roundToInt(),
            pitcherHr9 = hr9,
            seasonHrPct = seasonHrPct,
            regressionLean = pa >= 80 && rawHrPa > talent * 1.35f && rawHrPa > LEAGUE_HR_PA * 1.15f,
        )
    }
}
