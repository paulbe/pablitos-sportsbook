package com.pablitosb.sportsbook.ui.nfl

import com.pablitosb.sportsbook.navigation.Dest

data class NflPlaceholder(
    val title: String,
    val subtitle: String = "Placeholder",
    val dest: Dest? = null,
)

object NflEdgeHub {
    val placeholders: List<NflPlaceholder> = listOf(
        NflPlaceholder("1 QB Projections Weekly", "Next-game pass yards", dest = Dest.QbWeekly),
        NflPlaceholder("2 Coming soon"),
        NflPlaceholder("3 Coming soon"),
        NflPlaceholder("4 Coming soon"),
    )
}
