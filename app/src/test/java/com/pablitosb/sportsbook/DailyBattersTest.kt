package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.hr.BatterFdCalculator
import com.pablitosb.sportsbook.data.hr.BatterOppTint
import com.pablitosb.sportsbook.data.hr.HrSort
import com.pablitosb.sportsbook.data.hr.HrSorter
import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.mlb.OppKTier
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyBattersTest {

    @Test
    fun fdScoreUsesFanDuelHitterTable() {
        val c = BatterFdCalculator.Counting(
            singles = 1f,
            doubles = 0.2f,
            triples = 0.05f,
            hr = 0.3f,
            runs = 0.6f,
            rbi = 0.7f,
            walks = 0.4f,
            sb = 0.1f,
        )
        val expected = 1f * 3f + 0.2f * 6f + 0.05f * 9f + 0.3f * 12f +
            0.7f * 3.5f + 0.6f * 3.2f + 0.4f * 3f + 0.1f * 6f
        assertEquals(expected, BatterFdCalculator.score(c), 0.001f)
    }

    @Test
    fun fdFloorProjCeilingAreOrdered() {
        val r = BatterFdCalculator.project(
            BatterFdCalculator.Counting(
                singles = 0.8f,
                doubles = 0.25f,
                triples = 0.03f,
                hr = 0.35f,
                runs = 0.7f,
                rbi = 0.8f,
                walks = 0.45f,
                sb = 0.12f,
            ),
        )
        assertTrue(r.floor < r.proj)
        assertTrue(r.proj < r.ceiling)
        assertEquals(r.proj, BatterFdCalculator.score(r.counting), 0.001f)
    }

    @Test
    fun hrrIsHitsPlusRunsPlusRbi() {
        val c = BatterFdCalculator.Counting(
            singles = 0.7f,
            doubles = 0.2f,
            triples = 0.05f,
            hr = 0.25f,
            runs = 0.55f,
            rbi = 0.65f,
            walks = 0.3f,
            sb = 0.08f,
        )
        assertEquals(c.singles + c.doubles + c.triples + c.hr, c.hits, 0.001f)
        assertEquals(c.hits + c.runs + c.rbi, c.hrr, 0.001f)
    }

    @Test
    fun defaultSortIsHighToLowForEveryFilter() {
        HrSort.entries.forEach { key ->
            assertFalse(HrSorter.defaultAscending(key))
        }
        val list = listOf(
            batter("Low", hr = 4f, fd = 12f, ceil = 16f, tb = 0.8f, hrr = 1.1f),
            batter("High", hr = 18f, fd = 28f, ceil = 36f, tb = 2.4f, hrr = 3.2f),
            batter("Mid", hr = 9f, fd = 19f, ceil = 24f, tb = 1.5f, hrr = 2.0f),
        )
        assertEquals("High", HrSorter.sort(list, HrSort.GAME_HR, false).first().name)
        assertEquals("High", HrSorter.sort(list, HrSort.PROJ_FD, false).first().name)
        assertEquals("High", HrSorter.sort(list, HrSort.PROJ_TB, false).first().name)
        assertEquals("High", HrSorter.sort(list, HrSort.HRR, false).first().name)
        assertEquals("Low", HrSorter.sort(list, HrSort.GAME_HR, true).first().name)
    }

    @Test
    fun projFdTiesBreakByCeiling() {
        val list = listOf(
            batter("Ceil", hr = 10f, fd = 22f, ceil = 34f, tb = 1.5f, hrr = 2f),
            batter("Floor", hr = 12f, fd = 22f, ceil = 26f, tb = 1.8f, hrr = 2.2f),
        )
        val sorted = HrSorter.sort(list, HrSort.PROJ_FD, ascending = false)
        assertEquals(listOf("Ceil", "Floor"), sorted.map { it.name })
    }

    @Test
    fun batterOppTintInvertsPitcherKColors() {
        val scale = OppKScale.fallback()
        assertTrue(BatterOppTint.favorable(scale.tier(0.200f)))
        assertFalse(BatterOppTint.tough(scale.tier(0.200f)))
        assertTrue(BatterOppTint.tough(scale.tier(0.250f)))
        assertFalse(BatterOppTint.favorable(scale.tier(0.250f)))
        assertFalse(BatterOppTint.favorable(scale.tier(0.225f)))
        assertFalse(BatterOppTint.tough(scale.tier(0.225f)))
        assertEquals(OppKTier.UNKNOWN, scale.tier(null))
        assertFalse(BatterOppTint.favorable(OppKTier.UNKNOWN))
        val legend = scale.batterLegend()
        assertTrue(legend.contains("favorable"))
        assertTrue(legend.contains("21.6"))
        assertTrue(legend.contains("23.4"))
        assertFalse(legend.contains("team SO/PA"))
    }

    private fun batter(
        name: String,
        hr: Float,
        fd: Float,
        ceil: Float,
        tb: Float,
        hrr: Float,
    ) = HrBatter(
        rank = 1,
        name = name,
        team = "NYY",
        opponent = "BOS",
        pitcherHand = "RHP",
        gameHrPct = hr,
        seasonHrPct = 6f,
        xHrPct = 8,
        parkAdjPct = 4,
        parkName = "Yankee Stadium",
        weather = Weather.SUN,
        tempF = 72,
        pitcherAdjPct = 2,
        pitcherName = "Sale",
        pitcherHr9 = 1.1f,
        fdProj = fd,
        fdCeiling = ceil,
        projTb = tb,
        projHrr = hrr,
    )
}
