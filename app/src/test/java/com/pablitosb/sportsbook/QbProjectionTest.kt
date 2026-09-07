package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.mlb.Matchup
import com.pablitosb.sportsbook.data.nfl.QbInputs
import com.pablitosb.sportsbook.data.nfl.QbProjection
import com.pablitosb.sportsbook.data.nfl.QbProjectionCalculator
import com.pablitosb.sportsbook.data.nfl.QbSort
import com.pablitosb.sportsbook.data.nfl.QbSorter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QbProjectionTest {

    @Test
    fun shrinkThinSampleMovesTowardPrior() {
        val thin = QbProjectionCalculator.shrink(40f, 1f, 33f, 6f)
        assertEquals(34f, thin, 0.01f)
        val full = QbProjectionCalculator.shrink(40f, 3f, 33f, 2f)
        assertEquals(37.2f, full, 0.01f)
    }

    @Test
    fun cpoeBumpIsZeroWithoutNgs() {
        val bump = QbProjectionCalculator.shrink(0f, 400f, 0f, QbProjectionCalculator.CPOE_PRIOR_ATT)
        assertEquals(0f, bump, 0.0001f)
    }

    @Test
    fun scriptMultFavoritePassesLessThanDog() {
        val fav = QbProjectionCalculator.scriptMult(-7f, 44.5f)
        val dog = QbProjectionCalculator.scriptMult(7f, 44.5f)
        assertEquals(0.944f, fav, 0.001f)
        assertEquals(1.056f, dog, 0.001f)
        assertTrue(fav < dog)
        assertEquals(1.12f, QbProjectionCalculator.scriptMult(40f, 70f), 0.001f)
        assertEquals(0.90f, QbProjectionCalculator.scriptMult(-40f, 30f), 0.001f)
    }

    @Test
    fun paceAndOppMultClamp() {
        assertEquals(0.92f, QbProjectionCalculator.paceMult(40f), 0.001f)
        assertEquals(1.08f, QbProjectionCalculator.paceMult(90f), 0.001f)
        assertEquals(1.0f, QbProjectionCalculator.paceMult(62f), 0.001f)
        val plus = QbProjectionCalculator.oppMult(7.05f * 1.10f)
        assertEquals(1.10f, plus, 0.001f)
        assertEquals(1.14f, QbProjectionCalculator.oppMult(12f), 0.001f)
        assertEquals(0.88f, QbProjectionCalculator.oppMult(4f), 0.001f)
    }

    @Test
    fun matchupPctIsOppMultMinusOne() {
        val input = baseInputs(oppYpaAllowed = QbProjectionCalculator.LEAGUE_YPA * 1.08f)
        val result = QbProjectionCalculator.project(input)
        assertEquals(8f, result.matchupPct, 0.15f)
        assertEquals((result.oppMult - 1f) * 100f, result.matchupPct, 0.01f)
    }

    @Test
    fun expAttBlendsL3SeasonAndTeamPassThenScriptPace() {
        val input = baseInputs(
            attL3 = 40f,
            attL3Games = 3,
            attSeasonPerGame = 32f,
            teamPassAttPerGame = 30f,
            teamPlaysPerGame = 62f,
            spreadForTeam = 0f,
            total = 44.5f,
        )
        val result = QbProjectionCalculator.project(input)
        val attL3s = QbProjectionCalculator.shrink(40f, 3f, 33f, 2f)
        val expected = (0.55f * attL3s + 0.25f * 32f + 0.20f * 30f)
        assertEquals(expected, result.expAtt, 0.05f)
    }

    @Test
    fun expYdsIsAttTimesYpaAndClamped() {
        val result = QbProjectionCalculator.project(baseInputs())
        val raw = result.expAtt * result.expYpa
        assertEquals(raw.coerceIn(145f, 400f), result.projYds, 0.05f)
        assertTrue(result.ydsFloor < result.projYds)
        assertTrue(result.ydsCeiling > result.projYds)
    }

    @Test
    fun fanDuelUsesPassYardsAndPassTd() {
        val input = baseInputs(seasonAtt = 400, seasonPassTd = 32)
        val result = QbProjectionCalculator.project(input)
        val expectedTd = (32f / 400f * result.expAtt).coerceIn(0.4f, 3.6f)
        val expectedFd = result.projYds * 0.04f + expectedTd * 4f
        assertEquals(expectedTd, result.expPassTd, 0.02f)
        assertEquals(expectedFd, result.fdProj, 0.05f)
        assertTrue(result.fdFloor < result.fdProj)
        assertTrue(result.fdCeiling > result.fdProj)
    }

    @Test
    fun compAndIayAreDisplayRates() {
        val result = QbProjectionCalculator.project(
            baseInputs(compPctL5 = 70f, ypaL5 = 8.2f, ypaL5Games = 5, seasonCompPct = 64f),
        )
        assertTrue(result.compPct in 52f..78f)
        assertTrue(result.iay > 7f)
        assertTrue(result.iay < 8.2f)
    }

    @Test
    fun sortHighToLowByActiveMetric() {
        val rows = listOf(
            sampleQb("Low", projYds = 180f, fdProj = 10f, compPct = 58f, iay = 6.2f),
            sampleQb("High", projYds = 280f, fdProj = 22f, compPct = 71f, iay = 8.4f),
            sampleQb("Mid", projYds = 220f, fdProj = 15f, compPct = 64f, iay = 7.1f),
        )
        assertEquals(listOf("High", "Mid", "Low"), QbSorter.sort(rows, QbSort.PROJ_YDS, false).map { it.name })
        assertEquals(listOf("High", "Mid", "Low"), QbSorter.sort(rows, QbSort.PROJ_FD, false).map { it.name })
        assertEquals(listOf("High", "Mid", "Low"), QbSorter.sort(rows, QbSort.COMP, false).map { it.name })
        assertEquals(listOf("High", "Mid", "Low"), QbSorter.sort(rows, QbSort.IAY, false).map { it.name })
        assertEquals(listOf("Low", "Mid", "High"), QbSorter.sort(rows, QbSort.PROJ_YDS, true).map { it.name })
    }

    @Test
    fun nflMatchupStaysAwayAtHome() {
        assertEquals("BUF @ LAR", Matchup.awayAtHome("BUF", "LAR"))
        assertTrue(!Matchup.awayAtHome("BUF", "LAR").contains("vs", ignoreCase = true))
    }

    private fun baseInputs(
        attL3: Float = 34f,
        attL3Games: Int = 3,
        attSeasonPerGame: Float = 33f,
        teamPassAttPerGame: Float = 33f,
        teamPlaysPerGame: Float = 62f,
        ypaL5: Float = 7.2f,
        ypaL5Games: Int = 5,
        ypaSeason: Float = 7.1f,
        seasonAtt: Int = 350,
        seasonCompPct: Float = 64.5f,
        seasonPassTd: Int = 24,
        compPctL5: Float = 65f,
        oppYpaAllowed: Float = 7.05f,
        spreadForTeam: Float? = 0f,
        total: Float? = 44.5f,
    ) = QbInputs(
        attL3 = attL3,
        attL3Games = attL3Games,
        attSeasonPerGame = attSeasonPerGame,
        teamPassAttPerGame = teamPassAttPerGame,
        teamPlaysPerGame = teamPlaysPerGame,
        ypaL5 = ypaL5,
        ypaL5Games = ypaL5Games,
        ypaSeason = ypaSeason,
        seasonAtt = seasonAtt,
        seasonCompPct = seasonCompPct,
        seasonPassTd = seasonPassTd,
        compPctL5 = compPctL5,
        oppYpaAllowed = oppYpaAllowed,
        spreadForTeam = spreadForTeam,
        total = total,
    )

    private fun sampleQb(
        name: String,
        projYds: Float,
        fdProj: Float,
        compPct: Float,
        iay: Float,
    ) = QbProjection(
        rank = 1,
        name = name,
        team = "BUF",
        opponent = "LAR",
        awayAbbr = "BUF",
        homeAbbr = "LAR",
        homeAway = "away",
        gameTimeLabel = "8:20 PM",
        projYds = projYds,
        ydsFloor = projYds * 0.82f,
        ydsCeiling = projYds * 1.22f,
        fdProj = fdProj,
        fdFloor = fdProj - 2f,
        fdCeiling = fdProj + 3f,
        compPct = compPct,
        iay = iay,
        matchupPct = 4f,
        expAtt = 34f,
        expYpa = 7.2f,
        espnId = name,
    )
}
