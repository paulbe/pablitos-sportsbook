package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.starters.OutlookCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutlookCalculatorTest {

    @Test
    fun expectedBfIsSeasonBfPerGs() {
        val sample = workhorse(seasonBf = 500, seasonGs = 20)
        assertEquals(25f, OutlookCalculator.expectedBf(sample), 0.01f)
        val proj = OutlookCalculator.project(sample)
        assertEquals(proj.projK * 25f, proj.nextStartKs, 0.01f)
    }

    @Test
    fun expectedBfFallsBackToLastStartThenDefault() {
        val lastOnly = workhorse(seasonBf = 0, seasonGs = 0, lastStartBf = 24)
        assertEquals(24f, OutlookCalculator.expectedBf(lastOnly), 0.01f)
        assertEquals(26f, OutlookCalculator.expectedBf(null), 0.01f)
        val empty = OutlookCalculator.project(null)
        assertEquals(OutlookCalculator.LEAGUE_K * 26f, empty.nextStartKs, 0.01f)
    }

    @Test
    fun expectedBfClampsFifteenToThirtyTwo() {
        val short = workhorse(seasonBf = 80, seasonGs = 20)
        assertEquals(15f, OutlookCalculator.expectedBf(short), 0.01f)
        val longStart = workhorse(seasonBf = 800, seasonGs = 20)
        assertEquals(32f, OutlookCalculator.expectedBf(longStart), 0.01f)
    }

    @Test
    fun projKsDoesNotUseProjIpOrOpenerHaircut() {
        val opener = workhorse(seasonBf = 500, seasonGs = 20, seasonIp = 36f, lastStartIp = 2.1f)
        val workhorse = workhorse(seasonBf = 500, seasonGs = 20, seasonIp = 110f, lastStartIp = 6.0f)
        assertEquals(
            OutlookCalculator.expectedBf(workhorse),
            OutlookCalculator.expectedBf(opener),
            0.01f,
        )
        assertEquals(25f, OutlookCalculator.expectedBf(opener), 0.01f)
        val a = OutlookCalculator.project(opener)
        val b = OutlookCalculator.project(workhorse)
        assertEquals(a.projK * 25f, a.nextStartKs, 0.01f)
        assertTrue(a.nextStartKs > 0f)
        assertTrue(b.nextStartKs > 0f)
    }

    private fun workhorse(
        seasonSo: Int = 140,
        seasonBf: Int = 500,
        seasonGs: Int = 20,
        lastStartBf: Int? = 26,
        seasonIp: Float = 110f,
        lastStartIp: Float? = 6.0f,
    ) = OutlookCalculator.PitchingSample(
        seasonSo = seasonSo,
        seasonBf = seasonBf,
        seasonGs = seasonGs,
        seasonStrikePct = 0.65f,
        recentSo = 38,
        recentBf = 130,
        lastStartBf = lastStartBf,
        lastStartKs = listOf(6f, 7f, 8f, 5f, 7f),
        seasonIp = seasonIp,
        recentIp = 30f,
        recentGs = 5,
        lastStartIp = lastStartIp,
        last5Ip = listOf(6f, 6f, 6f, 6f, 6f),
        seasonEr = 40,
    )
}
