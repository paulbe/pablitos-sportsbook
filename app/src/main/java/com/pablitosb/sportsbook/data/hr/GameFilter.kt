package com.pablitosb.sportsbook.data.hr

import com.pablitosb.sportsbook.data.mlb.Matchup
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.projections.SlateGame
import java.time.Instant

data class GameChoice(
    val gamePk: Int,
    val label: String,
)

object GameFilter {
    fun chipLabel(selected: Int, total: Int): String = when {
        total <= 0 -> "No games"
        selected >= total -> "All games"
        selected == 1 -> "1 game"
        else -> "$selected games"
    }

    fun choices(games: List<SlateGame>): List<GameChoice> {
        return games
            .sortedWith(
                compareBy<SlateGame> { it.startUtc ?: Instant.MAX }
                    .thenBy { it.gameTimeLabel }
                    .thenBy { it.awayAbbr }
                    .thenBy { it.homeAbbr },
            )
            .map { game ->
                val matchup = Matchup.awayAtHome(game.awayAbbr, game.homeAbbr)
                val time = game.gameTimeLabel
                GameChoice(
                    gamePk = game.gamePk,
                    label = if (time.isBlank()) matchup else "$matchup  ·  $time",
                )
            }
    }

    fun keep(batters: List<HrBatter>, selectedPks: Set<Int>): List<HrBatter> {
        if (selectedPks.isEmpty()) return emptyList()
        return batters.filter { it.gamePk in selectedPks }
    }

    fun canApply(selectedPks: Set<Int>): Boolean = selectedPks.isNotEmpty()
}
