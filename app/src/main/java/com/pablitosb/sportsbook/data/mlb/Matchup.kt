package com.pablitosb.sportsbook.data.mlb

/**
 * Game matchups are always **AWAY @ HOME**. Never "vs".
 *
 * If explicit away/home abbreviations are missing, [resolve] falls back to
 * the player's [team] / [opponent] plus [homeAway] (`"home"` or `"away"`).
 * Unknown side defaults to the player as the away club so the string still
 * uses `@` instead of inventing a home park.
 */
object Matchup {
    fun awayAtHome(away: String, home: String): String {
        val a = away.ifBlank { "TBD" }
        val h = home.ifBlank { "TBD" }
        return "$a @ $h"
    }

    fun sides(team: String, opponent: String, homeAway: String): Pair<String, String> {
        return if (homeAway.equals("home", ignoreCase = true)) {
            opponent to team
        } else {
            team to opponent
        }
    }

    fun resolve(
        awayAbbr: String,
        homeAbbr: String,
        team: String = "",
        opponent: String = "",
        homeAway: String = "",
    ): Pair<String, String> {
        if (awayAbbr.isNotBlank() && homeAbbr.isNotBlank()) return awayAbbr to homeAbbr
        return sides(team, opponent, homeAway)
    }

    fun label(
        awayAbbr: String = "",
        homeAbbr: String = "",
        team: String = "",
        opponent: String = "",
        homeAway: String = "",
    ): String {
        val (away, home) = resolve(awayAbbr, homeAbbr, team, opponent, homeAway)
        return awayAtHome(away, home)
    }
}
