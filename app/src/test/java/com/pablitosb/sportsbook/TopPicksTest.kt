package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.hr.HrSort
import com.pablitosb.sportsbook.data.hr.HrSorter
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.starters.StartersSort
import com.pablitosb.sportsbook.data.starters.StartersSorter
import com.pablitosb.sportsbook.data.toppicks.TopPicksSelector
import com.pablitosb.sportsbook.ui.toppicks.TopPicksViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopPicksTest {

    @Test
    fun viewModelHasNoArgConstructor() {
        val noArg = TopPicksViewModel::class.java.constructors.any { it.parameterCount == 0 }
        assertTrue(noArg)
    }

    @Test
    fun pitchersReuseStartersSortAndTakeLimit() {
        val list = (1..15).map { i ->
            starter("P$i", ks = i.toFloat(), outs = i.toFloat(), fd = i.toFloat(), xwoba = 0.400f - i * 0.005f)
        }
        val topKs = TopPicksSelector.pitchers(list, StartersSort.PROJ_KS, ascending = false)
        assertEquals(TopPicksSelector.LIMIT, topKs.size)
        assertEquals("P15", topKs.first().name)
        assertEquals("P6", topKs.last().name)
        val expected = StartersSorter.sort(list, StartersSort.PROJ_KS, false).take(TopPicksSelector.LIMIT)
        assertEquals(expected.map { it.name }, topKs.map { it.name })
    }

    @Test
    fun pitchersXwobaLowerIsBetter() {
        val list = listOf(
            starter("High", ks = 8f, outs = 16f, fd = 30f, xwoba = 0.340f),
            starter("Low", ks = 6f, outs = 14f, fd = 22f, xwoba = 0.250f),
            starter("Mid", ks = 7f, outs = 15f, fd = 26f, xwoba = 0.300f),
        )
        assertTrue(StartersSorter.defaultAscending(StartersSort.XWOBA))
        val top = TopPicksSelector.pitchers(list, StartersSort.XWOBA, ascending = true)
        assertEquals(listOf("Low", "Mid", "High"), top.map { it.name })
    }

    @Test
    fun pitchersProjFdSortsByProjThenCeiling() {
        val list = listOf(
            starter("Ceil", ks = 7f, outs = 15f, fd = 30f, xwoba = 0.30f, fdCeil = 44f),
            starter("Floor", ks = 8f, outs = 16f, fd = 30f, xwoba = 0.28f, fdCeil = 34f),
        )
        val top = TopPicksSelector.pitchers(list, StartersSort.PROJ_FD, ascending = false)
        assertEquals(listOf("Ceil", "Floor"), top.map { it.name })
    }

    @Test
    fun outlookChipOnlyOnProgFilter() {
        assertTrue(TopPicksSelector.showOutlookChip(StartersSort.PROG))
        assertFalse(TopPicksSelector.showOutlookChip(StartersSort.PROJ_KS))
        assertFalse(TopPicksSelector.showOutlookChip(StartersSort.XWOBA))
        assertFalse(TopPicksSelector.showOutlookChip(StartersSort.PROJ_OUTS))
        assertFalse(TopPicksSelector.showOutlookChip(StartersSort.PROJ_FD))
    }

    @Test
    fun battersReuseHrSortAndTakeLimit() {
        val list = (1..14).map { i ->
            batter("B$i", hr = i.toFloat(), fd = i.toFloat(), tb = i.toFloat(), hrr = i.toFloat())
        }
        val topHr = TopPicksSelector.batters(list, HrSort.GAME_HR, ascending = false)
        assertEquals(TopPicksSelector.LIMIT, topHr.size)
        assertEquals("B14", topHr.first().name)
        val expected = HrSorter.sort(list, HrSort.GAME_HR, false).take(TopPicksSelector.LIMIT)
        assertEquals(expected.map { it.name }, topHr.map { it.name })
    }

    @Test
    fun battersEachFilterSortsHighToLow() {
        val list = listOf(
            batter("Low", hr = 4f, fd = 12f, tb = 0.8f, hrr = 1.1f, fdCeil = 16f),
            batter("High", hr = 18f, fd = 28f, tb = 2.4f, hrr = 3.2f, fdCeil = 36f),
            batter("Mid", hr = 9f, fd = 19f, tb = 1.5f, hrr = 2.0f, fdCeil = 24f),
        )
        assertEquals("High", TopPicksSelector.batters(list, HrSort.GAME_HR, false).first().name)
        assertEquals("High", TopPicksSelector.batters(list, HrSort.PROJ_FD, false).first().name)
        assertEquals("High", TopPicksSelector.batters(list, HrSort.PROJ_TB, false).first().name)
        assertEquals("High", TopPicksSelector.batters(list, HrSort.HRR, false).first().name)
    }

    private fun starter(
        name: String,
        ks: Float,
        outs: Float,
        fd: Float,
        xwoba: Float?,
        fdCeil: Float = fd + 6f,
    ) = Starter(
        rank = 1,
        name = name,
        team = "ATL",
        opponent = "PHI",
        venue = "Citizens Bank",
        weather = Weather.SUN,
        tempF = 74,
        outlook = Outlook.PROG,
        outlookScore = 8,
        projKPct = 0.28f,
        nextStartKs = ks,
        trend = emptyList(),
        xwoba = xwoba,
        projOuts = outs,
        fdProj = fd,
        fdCeiling = fdCeil,
        envBoostPct = 8,
        awayAbbr = "ATL",
        homeAbbr = "PHI",
        homeAway = "away",
    )

    private fun batter(
        name: String,
        hr: Float,
        fd: Float,
        tb: Float,
        hrr: Float,
        fdCeil: Float = fd + 8f,
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
        fdCeiling = fdCeil,
        projTb = tb,
        projHrr = hrr,
        envBoostPct = 6,
    )
}
