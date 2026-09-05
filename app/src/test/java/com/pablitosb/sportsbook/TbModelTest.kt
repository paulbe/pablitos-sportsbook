package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.hr.HrCalculator
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.tb.TbBatter
import com.pablitosb.sportsbook.data.tb.TbCalculator
import com.pablitosb.sportsbook.data.tb.TbSort
import com.pablitosb.sportsbook.data.tb.TbSorter
import com.pablitosb.sportsbook.data.toppicks.TopPicksSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TbModelTest {

    @Test
    fun sluggerProjectsMoreTbThanContactHitter() {
        val ctx = hrCtx(park = 0, pitcher = 0, pa = 4.35f, pPa = 0.040f)
        val slugger = TbCalculator.project(
            TbCalculator.Sample(hits = 100, doubles = 22, triples = 2, hr = 36, pa = 400, ab = 350),
            ctx.copy(pPa = 0.055f),
        )
        val contact = TbCalculator.project(
            TbCalculator.Sample(hits = 112, doubles = 18, triples = 3, hr = 8, pa = 400, ab = 355),
            ctx.copy(pPa = 0.022f),
        )
        assertTrue(slugger.expectedTb > contact.expectedTb)
        assertTrue(slugger.tbPerPa > contact.tbPerPa)
        assertEquals(4.35f * slugger.tbPerPa, slugger.expectedTb, 0.01f)
    }

    @Test
    fun hrParkRaisesExpectedTb() {
        val sample = TbCalculator.Sample(hits = 90, doubles = 20, triples = 2, hr = 20, pa = 380, ab = 335)
        val bandbox = TbCalculator.project(sample, hrCtx(park = 18, pitcher = 6, pPa = 0.048f))
        val cavern = TbCalculator.project(sample, hrCtx(park = -12, pitcher = -8, pPa = 0.028f))
        assertTrue(bandbox.expectedTb > cavern.expectedTb)
    }

    @Test
    fun topTbRanksByProjTb() {
        val picks = TopPicksSelector.tbSpots(
            listOf(
                tb("Role", 1.4f),
                tb("Judge", 2.6f),
                tb("Soto", 2.1f),
            ),
        )
        assertEquals(listOf("Judge", "Soto", "Role"), picks.map { it.name })
        assertTrue(picks.first().why.contains("2.60 Proj TB"))
    }

    @Test
    fun sorterFlipsTbPa() {
        val a = tb("A", 2.0f, tbPa = 0.40f)
        val b = tb("B", 1.8f, tbPa = 0.50f)
        val desc = TbSorter.sort(listOf(a, b), TbSort.TB_PA, ascending = false)
        assertEquals("B", desc.first().name)
    }

    private fun hrCtx(
        park: Int,
        pitcher: Int,
        pa: Float = 4.25f,
        pPa: Float = 0.035f,
    ) = HrCalculator.Result(
        talentPa = 0.032f,
        pPa = pPa,
        expectedPa = pa,
        expectedHr = pPa * pa,
        gameHrProb = 0.14f,
        talentGamePct = 13,
        parkAdjPct = park,
        weatherAdjPct = 2,
        pitcherAdjPct = pitcher,
        platoonAdjPct = 4,
        pitcherHr9 = 1.2f,
        seasonHrPct = 4.5f,
        regressionLean = false,
    )

    private fun tb(name: String, proj: Float, tbPa: Float = 0.40f) = TbBatter(
        rank = 1,
        mlbId = name.hashCode(),
        name = name,
        team = "NYY",
        opponent = "BOS",
        pos = "OF",
        battingOrder = 2,
        inPostedLineup = true,
        pitcherName = "Sale",
        pitcherHand = "LHP",
        parkName = "Yankee Stadium",
        weather = Weather.SUN,
        tempF = 74,
        projTb = proj,
        tbPerPa = tbPa,
        slgProxy = 0.450f,
        expectedPa = 4.4f,
        parkAdjPct = 8,
        pitcherAdjPct = 4,
        gameHrPct = 12f,
    )
}
