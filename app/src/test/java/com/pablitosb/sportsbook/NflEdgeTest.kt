package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.ui.nfl.NflEdgeHub
import org.junit.Assert.assertEquals
import org.junit.Test

class NflEdgeTest {

    @Test
    fun fourComingSoonPlaceholders() {
        assertEquals(4, NflEdgeHub.placeholders.size)
        assertEquals(
            listOf("1 Coming soon", "2 Coming soon", "3 Coming soon", "4 Coming soon"),
            NflEdgeHub.placeholders.map { it.title },
        )
        assertEquals(listOf("Placeholder", "Placeholder", "Placeholder", "Placeholder"), NflEdgeHub.placeholders.map { it.subtitle })
    }
}
