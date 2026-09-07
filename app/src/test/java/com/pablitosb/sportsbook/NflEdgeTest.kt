package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.navigation.Dest
import com.pablitosb.sportsbook.ui.nfl.NflEdgeHub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NflEdgeTest {

    @Test
    fun tileOneOpensQbWeeklyAndRestStayComingSoon() {
        val tiles = NflEdgeHub.placeholders
        assertEquals(4, tiles.size)
        assertEquals("1 QB Projections Weekly", tiles[0].title)
        assertEquals("Next-game pass yards", tiles[0].subtitle)
        assertEquals(Dest.QbWeekly, tiles[0].dest)
        assertEquals("qbweekly", Dest.QbWeekly.route)
        assertEquals(
            listOf("2 Coming soon", "3 Coming soon", "4 Coming soon"),
            tiles.drop(1).map { it.title },
        )
        assertEquals(listOf("Placeholder", "Placeholder", "Placeholder"), tiles.drop(1).map { it.subtitle })
        tiles.drop(1).forEach { assertNull(it.dest) }
    }
}
