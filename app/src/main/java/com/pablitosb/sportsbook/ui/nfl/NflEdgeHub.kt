package com.pablitosb.sportsbook.ui.nfl

data class NflPlaceholder(
    val title: String,
    val subtitle: String = "Placeholder",
)

object NflEdgeHub {
    val placeholders: List<NflPlaceholder> = listOf(
        NflPlaceholder("1 Coming soon"),
        NflPlaceholder("2 Coming soon"),
        NflPlaceholder("3 Coming soon"),
        NflPlaceholder("4 Coming soon"),
    )
}
