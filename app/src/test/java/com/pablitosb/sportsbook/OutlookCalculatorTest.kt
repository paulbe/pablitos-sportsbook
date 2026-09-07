package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.mlb.TeamOffense
import com.pablitosb.sportsbook.data.starters.OutlookCalculator
import com.pablitosb.sportsbook.data.starters.ProjOutsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlookCalculatorTest {

    @Test
    fun expectedBfIsProjIpTimesSeasonBfPerIp() {
        val sample = workhorse()
        val bf = OutlookCalculator.expectedBf(sample, 6.0f)
        assertEquals(6.0f * (500f / 110f), bf, 0.01f)
        assertTrue(bf in 12f..32f)
    }

    @Test
    fun expectedBfClampsToBand() {
        val highRate = workhorse(seasonBf = 900, seasonIp = 80f)
        assertEquals(32f, OutlookCalculator.expectedBf(highRate, 7.2f), 0.01f)
        val lowRate = workhorse(seasonBf = 200, seasonIp = 120f)
        assertEquals(12f, OutlookCalculator.expectedBf(lowRate, 3.5f), 0.01f)
        assertTrue(OutlookCalculator.expectedBf(lowRate, 7.2f) in 12f..32f)
    }

    @Test
    fun openerOrShortLastStartHaircutsBf() {
        val opener = workhorse(seasonIp = 36f, seasonGs = 12, lastStartIp = 5.0f)
        val full = OutlookCalculator.expectedBf(workhorse(), 6.0f)
        val cut = OutlookCalculator.expectedBf(opener, 6.0f)
        assertEquals(3.0f, opener.seasonIp / opener.seasonGs, 0.01f)
        assertEquals(0.65f, cut / (6.0f * (500f / 36f)).coerceIn(12f, 32f), 0.01f)

        val shortLast = workhorse(lastStartIp = 2.1f)
        val shortCut = OutlookCalculator.expectedBf(shortLast, 6.0f)
        assertEquals(full * 0.65f, shortCut, 0.01f)
    }

    @Test
    fun projKPctUnchangedWhenIpChanges() {
        val sample = workhorse()
        val a = OutlookCalculator.project(sample, 3.5f)
        val b = OutlookCalculator.project(sample, 7.2f)
        assertEquals(a.projK, b.projK, 0.00001f)
        assertTrue(b.nextStartKs > a.nextStartKs)
        assertEquals(a.projK * OutlookCalculator.expectedBf(sample, 3.5f), a.nextStartKs, 0.001f)
        assertEquals(b.projK * OutlookCalculator.expectedBf(sample, 7.2f), b.nextStartKs, 0.001f)
    }

    @Test
    fun matchupProjIpKeepsKsAlignedWithOuts() {
        val sample = workhorse()
        val work = OutlookCalculator.workload(sample)
        val easy = ProjOutsCalculator.project(
            work,
            ProjOutsCalculator.Context(
                opponent = TeamOffense(0.250f, 0.650f, 4000),
                envBoostPct = -12,
                rain = false,
            ),
        )
        val tough = ProjOutsCalculator.project(
            work,
            ProjOutsCalculator.Context(
                opponent = TeamOffense(0.190f, 0.820f, 4000),
                envBoostPct = 18,
                rain = false,
            ),
        )
        val easyKs = OutlookCalculator.project(sample, easy.projIp).nextStartKs
        val toughKs = OutlookCalculator.project(sample, tough.projIp).nextStartKs
        assertTrue(easy.projIp > tough.projIp)
        assertTrue(easyKs > toughKs)
        val bfPerIp = 500f / 110f
        assertEquals(sample.let { OutlookCalculator.project(it).projK } * (easy.projIp * bfPerIp).coerceIn(12f, 32f), easyKs, 0.05f)
    }

    @Test
    fun missingSampleUsesLeagueBf() {
        val p = OutlookCalculator.project(null)
        val ip = OutlookCalculator.fallbackProjIp(null)
        assertEquals(OutlookCalculator.LEAGUE_K * OutlookCalculator.expectedBf(null, ip), p.nextStartKs, 0.001f)
        assertEquals(OutlookCalculator.DEFAULT_BF, OutlookCalculator.expectedBf(null, ProjOutsCalculator.LEAGUE_IP_GS), 0.05f)
    }

    private fun workhorse(
        seasonSo: Int = 140,
        seasonBf: Int = 500,
        seasonGs: Int = 20,
        seasonIp: Float = 110f,
        lastStartIp: Float? = 6.0f,
    ) = OutlookCalculator.PitchingSample(
        seasonSo = seasonSo,
        seasonBf = seasonBf,
        seasonGs = seasonGs,
        seasonStrikePct = 0.65f,
        recentSo = 38,
        recentBf = 130,
        lastStartBf = 26,
        lastStartKs = listOf(6f, 7f, 8f, 5f, 7f),
        seasonIp = seasonIp,
        recentIp = 30f,
        recentGs = 5,
        lastStartIp = lastStartIp,
        last5Ip = listOf(6f, 6f, 6f, 6f, 6f),
        seasonEr = 40,
    )
}
