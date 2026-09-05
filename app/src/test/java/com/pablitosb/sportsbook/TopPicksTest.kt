package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.fdproj.FdProjRow
import com.pablitosb.sportsbook.data.fdproj.FdProjSorter
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.toppicks.TopPicksSelector
import com.pablitosb.sportsbook.ui.toppicks.TopPicksViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopPicksTest {

    @Test
    fun viewModelHasNoArgConstructor() {
        val noArg = TopPicksViewModel::class.java.constructors.any { it.parameterCount == 0 }
        assertTrue(noArg)
    }

    @Test
    fun kSpotsRankByProjKsRainLast() {
        val rainAce = starter("Rain Ace", ks = 10f, outlook = 12, wx = WxTag.RAIN_RISK)
        val mid = starter("Mid", ks = 7.5f, outlook = 2, wx = WxTag.NEUTRAL)
        val ace = starter("Ace", ks = 9.2f, outlook = 4, wx = WxTag.PITCHER_WX)
        val picks = TopPicksSelector.kSpots(listOf(rainAce, mid, ace))
        assertEquals(listOf("Ace", "Mid", "Rain Ace"), picks.map { it.name })
        assertTrue(picks.first().why.contains("9.2 Proj Ks"))
        assertTrue(picks.last().why.contains("RAIN RISK"))
    }

    @Test
    fun hrSpotsRankByGameHr() {
        val a = batter("Judge", 18.4f, park = 8)
        val b = batter("Soto", 12.1f, park = 4)
        val c = batter("Rookie", 9.0f, park = 0)
        val picks = TopPicksSelector.hrSpots(listOf(b, c, a))
        assertEquals(listOf("Judge", "Soto", "Rookie"), picks.map { it.name })
        assertTrue(picks.first().why.contains("park +8%"))
    }

    @Test
    fun fdValuePrefersPtsPerThousandWhenSalaryPresent() {
        val cheap = fd("Value", proj = 12f, salary = 2500)
        val ace = fd("Ace", proj = 28f, salary = 11000)
        val (picks, byValue) = TopPicksSelector.fdValue(listOf(ace, cheap))
        assertTrue(byValue)
        assertEquals(listOf("Value", "Ace"), picks.map { it.name })
        assertTrue(picks.first().why.contains("pts/$1k") || picks.first().why.contains("pts/\$1k"))
    }

    @Test
    fun fdFallsBackToProjWhenNoSalary() {
        val a = fd("Star", proj = 22f, salary = 0)
        val b = fd("Role", proj = 8f, salary = 0)
        val (picks, byValue) = TopPicksSelector.fdValue(listOf(b, a))
        assertTrue(!byValue)
        assertEquals(listOf("Star", "Role"), picks.map { it.name })
        assertTrue(picks.first().why.contains("no salary"))
    }

    private fun starter(
        name: String,
        ks: Float,
        outlook: Int,
        wx: WxTag,
    ) = Starter(
        rank = 1,
        name = name,
        team = "NYY",
        opponent = "BOS",
        venue = "Yankee Stadium",
        weather = Weather.SUN,
        tempF = 72,
        outlook = if (outlook >= 5) Outlook.PROG else if (outlook <= -5) Outlook.REG else Outlook.STABLE,
        outlookScore = outlook,
        projKPct = 0.25f,
        nextStartKs = ks,
        trend = emptyList(),
        wxTag = wx,
        envBoostPct = if (wx == WxTag.RAIN_RISK) -28 else 6,
        parkHint = "PF 1.00",
    )

    private fun batter(name: String, hr: Float, park: Int) = HrBatter(
        rank = 1,
        name = name,
        team = "NYY",
        opponent = "BOS",
        pitcherHand = "RHP",
        gameHrPct = hr,
        seasonHrPct = 6f,
        xHrPct = 8,
        parkAdjPct = park,
        parkName = "Yankee Stadium",
        weather = Weather.SUN,
        tempF = 72,
        pitcherAdjPct = 4,
        pitcherName = "Sale",
        pitcherHr9 = 1.1f,
        sourceNote = "Lineup",
    )

    private fun fd(name: String, proj: Float, salary: Int) = FdProjRow(
        mlbId = name.hashCode(),
        name = name,
        team = "NYY",
        opponent = "BOS",
        pos = "OF",
        salary = salary,
        proj = proj,
        ceiling = proj * 1.2f,
        value = FdProjSorter.value(proj, salary),
        isPitcher = false,
        inPostedLineup = true,
        gameTimeLabel = "1:00 PM PT",
        driver = "12% HR",
    )
}
