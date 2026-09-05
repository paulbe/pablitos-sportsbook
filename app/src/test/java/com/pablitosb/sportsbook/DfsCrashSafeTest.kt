package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.dfs.DfsOptimizer
import com.pablitosb.sportsbook.data.dfs.DfsSlateKind
import com.pablitosb.sportsbook.data.dfs.FdScoring
import com.pablitosb.sportsbook.data.dfs.MlbSlateBuilder
import com.pablitosb.sportsbook.data.dfs.SalarySlate
import com.pablitosb.sportsbook.data.dfs.SlatePlayer
import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.projections.SlateGame
import com.pablitosb.sportsbook.data.props.PropsRepository
import com.pablitosb.sportsbook.ui.dfs.DfsViewModel
import com.pablitosb.sportsbook.ui.props.PropsViewModel
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DfsCrashSafeTest {

    @Test
    fun dfsViewModelHasApplicationOnlyConstructor() {
        val ctor = DfsViewModel::class.java.constructors.single()
        assertEquals(1, ctor.parameterCount)
        assertEquals("android.app.Application", ctor.parameterTypes[0].name)
    }

    @Test
    fun propsViewModelHasApplicationOnlyConstructor() {
        val ctor = PropsViewModel::class.java.constructors.single()
        assertEquals(1, ctor.parameterCount)
        assertEquals("android.app.Application", ctor.parameterTypes[0].name)
    }

    @Test
    fun salaryParseSkipsJunkAndComments() {
        val rows = SalarySlate.parse(
            """
            # EXAMPLE
            name,team,pos,salary
            Aaron Judge,NYY,OF,4500
            bad-row
            ,NYY,OF,2000
            """.trimIndent(),
        )
        assertEquals(1, rows.size)
        assertEquals("Aaron Judge", rows[0].name)
        assertEquals(4500, rows[0].salary)
    }

    @Test
    fun propsParseIsCrashSafe() {
        val rows = PropsRepository.parseImport(
            """
            player,market,line,side,odds
            Tarik Skubal,pitcher_ks,7.5,higher,-120
            not-enough-cols
            """.trimIndent(),
        )
        assertEquals(1, rows.size)
        assertEquals("Tarik Skubal", rows[0].player)
    }

    @Test
    fun optimizerEmptyPoolDoesNotThrow() {
        val result = DfsOptimizer.build(emptyList(), ContestType.GPP, 4, 3, 1L)
        assertTrue(result.lineups.isEmpty())
        assertTrue(result.error != null)
    }

    @Test
    fun optimizerBuildsClassicLineup() {
        val pool = testPool()
        val result = DfsOptimizer.build(pool, ContestType.CASH, 3, 2, 2L)
        assertTrue(result.lineups.isNotEmpty())
        result.lineups.forEach { lineup ->
            assertEquals(9, lineup.players.size)
            assertTrue(lineup.salary <= FdScoring.SALARY_CAP)
        }
    }

    @Test
    fun mlbSlateBuilderSplitsMainEarlyLateShowdown() {
        val zone = ZoneId.of("America/Los_Angeles")
        val early = game(1, "NYY", "BOS", Instant.parse("2026-09-05T17:10:00Z")) // 10:10 PT
        val late = game(2, "LAD", "SF", Instant.parse("2026-09-06T02:10:00Z")) // 19:10 PT
        val slates = MlbSlateBuilder.build(listOf(early, late), zone)
        assertTrue(slates.any { it.kind == DfsSlateKind.MAIN && it.gameCount == 2 })
        assertTrue(slates.any { it.kind == DfsSlateKind.EARLY && it.gamePks == setOf(1) })
        assertTrue(slates.any { it.kind == DfsSlateKind.LATE && it.gamePks == setOf(2) })
        assertEquals(2, slates.count { it.kind == DfsSlateKind.SHOWDOWN })
    }

    private fun game(pk: Int, away: String, home: String, start: Instant) = SlateGame(
        gamePk = pk,
        venueId = 1,
        venueName = "Park",
        weather = Weather.SUN,
        tempF = 70,
        wind = "",
        weatherFactor = 1f,
        parkHrFactor = 1f,
        homeAbbr = home,
        awayAbbr = away,
        homeId = 1,
        awayId = 2,
        homePitcherId = null,
        awayPitcherId = null,
        homePitcherName = "",
        awayPitcherName = "",
        postponed = false,
        gameTimeLabel = "1:00 PM PT",
        startUtc = start,
        homeLineupPosted = true,
        awayLineupPosted = true,
    )

    private fun testPool(): List<SlatePlayer> {
        val hitters = listOf(
            hitter(1, "C", "C/1B"),
            hitter(2, "1B", "C/1B"),
            hitter(3, "2B", "2B"),
            hitter(4, "3B", "3B"),
            hitter(5, "SS", "SS"),
            hitter(6, "OF1", "OF"),
            hitter(7, "OF2", "OF"),
            hitter(8, "OF3", "OF"),
            hitter(9, "DH", "UTIL"),
            hitter(10, "OF4", "OF"),
        )
        val pitchers = listOf(
            SlatePlayer(100, "Ace", "NYY", "P", 9000, 28f, 35f, setOf("P"), true, true, 1),
            SlatePlayer(101, "Two", "BOS", "P", 8000, 24f, 30f, setOf("P"), true, true, 1),
            SlatePlayer(102, "Three", "LAD", "P", 7000, 20f, 26f, setOf("P"), true, true, 1),
        )
        return pitchers + hitters
    }

    private fun hitter(id: Int, name: String, slot: String) = SlatePlayer(
        mlbId = id,
        name = name,
        team = if (id % 2 == 0) "NYY" else "BOS",
        pos = slot,
        salary = 2500 + id * 100,
        proj = 10f + id,
        ceiling = 14f + id,
        fdSlots = if (slot == "UTIL") setOf("UTIL") else setOf(slot, "UTIL"),
        isPitcher = false,
        inPostedLineup = true,
        gamePk = 1,
    )
}
