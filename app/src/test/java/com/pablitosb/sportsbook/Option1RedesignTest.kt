package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.mlb.Matchup
import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.mlb.OppKTier
import com.pablitosb.sportsbook.data.mlb.TeamOffense
import com.pablitosb.sportsbook.data.starters.ProjOutsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Option1RedesignTest {

    @Test
    fun matchupIsAlwaysAwayAtHome() {
        assertEquals("PHI @ ATL", Matchup.awayAtHome("PHI", "ATL"))
        assertEquals("LAD @ ARI", Matchup.label(awayAbbr = "LAD", homeAbbr = "ARI"))
        assertEquals("SD @ COL", Matchup.label(team = "COL", opponent = "SD", homeAway = "home"))
        assertEquals("HOU @ TEX", Matchup.label(team = "HOU", opponent = "TEX", homeAway = "away"))
        assertTrue(!Matchup.awayAtHome("NYY", "BOS").contains("vs", ignoreCase = true))
    }

    @Test
    fun oppKFallbackTertilesAreDocumented() {
        assertEquals(0.216f, OppKScale.FALLBACK_LOW, 0.0001f)
        assertEquals(0.234f, OppKScale.FALLBACK_HIGH, 0.0001f)
        val scale = OppKScale.fallback()
        assertEquals(OppKTier.LOW, scale.tier(0.200f))
        assertEquals(OppKTier.MID, scale.tier(0.225f))
        assertEquals(OppKTier.HIGH, scale.tier(0.250f))
        assertEquals(OppKTier.UNKNOWN, scale.tier(null))
        assertTrue(scale.legend().contains("21.6"))
        assertTrue(scale.legend().contains("23.4"))
    }

    @Test
    fun oppKLiveTertilesSplitThirtyTeams() {
        val rates = (0 until 30).map { 0.190f + it * 0.003f }
        val scale = OppKScale.fromRates(rates)
        assertTrue(scale.fromLiveTertiles)
        assertEquals(OppKTier.LOW, scale.tier(rates.first()))
        assertEquals(OppKTier.HIGH, scale.tier(rates.last()))
        assertEquals(OppKTier.MID, scale.tier(rates[15]))
    }

    @Test
    fun oppKSparseSampleUsesFallback() {
        val scale = OppKScale.fromRates(listOf(0.20f, 0.22f, 0.26f))
        assertTrue(!scale.fromLiveTertiles)
        assertEquals(OppKScale.FALLBACK_LOW, scale.lowMax, 0.0001f)
    }

    @Test
    fun projOutsIsThreeTimesCappedIp() {
        val work = ProjOutsCalculator.Workload(
            seasonIp = 120f,
            seasonGs = 20,
            recentIp = 30f,
            recentGs = 5,
            lastStartIp = 6.0f,
            last5Ip = listOf(6f, 6f, 6f, 6f, 6f),
        )
        val neutral = ProjOutsCalculator.project(
            work,
            ProjOutsCalculator.Context(opponent = TeamOffense(0.225f, 0.711f, 4000), envBoostPct = 0, rain = false),
        )
        assertEquals(neutral.projIp * 3f, neutral.projOuts, 0.001f)
        assertTrue(neutral.projIp in 3.5f..7.2f)
    }

    @Test
    fun projOutsCutsIpForHitterWeatherAndHotOffense() {
        val work = ProjOutsCalculator.Workload(
            seasonIp = 120f,
            seasonGs = 20,
            recentIp = 30f,
            recentGs = 5,
            lastStartIp = 6.0f,
            last5Ip = listOf(6f, 6f, 6f, 6f, 6f),
        )
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
        val rain = ProjOutsCalculator.project(
            work,
            ProjOutsCalculator.Context(
                opponent = TeamOffense(0.225f, 0.711f, 4000),
                envBoostPct = 18,
                rain = true,
            ),
        )
        assertTrue(easy.projOuts > tough.projOuts)
        assertTrue(rain.projOuts < easy.projOuts)
    }

    @Test
    fun projOutsEarlyExitShortensStart() {
        val stable = ProjOutsCalculator.Workload(
            seasonIp = 100f,
            seasonGs = 18,
            recentIp = 30f,
            recentGs = 5,
            lastStartIp = 6.1f,
            last5Ip = listOf(6f, 6f, 6.2f, 5.8f, 6.1f),
        )
        val early = ProjOutsCalculator.Workload(
            seasonIp = 100f,
            seasonGs = 18,
            recentIp = 18f,
            recentGs = 5,
            lastStartIp = 3.2f,
            last5Ip = listOf(6f, 4.1f, 3.0f, 4.2f, 3.2f),
        )
        val ctx = ProjOutsCalculator.Context(null, 0, false)
        val a = ProjOutsCalculator.project(stable, ctx)
        val b = ProjOutsCalculator.project(early, ctx)
        assertTrue(a.projOuts > b.projOuts)
    }

    @Test
    fun projOutsMissingLogsUsesLeaguePriorInsideCap() {
        val r = ProjOutsCalculator.project(null, ProjOutsCalculator.Context(null, 0, false))
        assertTrue(r.projIp in 3.5f..7.2f)
        assertEquals(r.projIp * 3f, r.projOuts, 0.001f)
    }
}
