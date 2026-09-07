package com.pablitosb.sportsbook.data.nfl

import java.time.Instant
import java.time.LocalDate

data class QbProjection(
    val rank: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val awayAbbr: String,
    val homeAbbr: String,
    val homeAway: String,
    val gameTimeLabel: String,
    val projYds: Float,
    val ydsFloor: Float,
    val ydsCeiling: Float,
    val fdProj: Float,
    val fdFloor: Float,
    val fdCeiling: Float,
    val compPct: Float,
    val iay: Float,
    val matchupPct: Float,
    val expAtt: Float,
    val expYpa: Float,
    val espnId: String,
)

data class QbBoard(
    val seasonYear: Int,
    val week: Int,
    val fetchedAt: Instant,
    val sourceLabel: String,
    val note: String,
    val qbs: List<QbProjection>,
    val emptyReason: String? = null,
    val slateDate: LocalDate? = null,
)

class QbLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class QbInputs(
    val attL3: Float,
    val attL3Games: Int,
    val attSeasonPerGame: Float,
    val teamPassAttPerGame: Float,
    val teamPlaysPerGame: Float,
    val ypaL5: Float,
    val ypaL5Games: Int,
    val ypaSeason: Float,
    val seasonAtt: Int,
    val seasonCompPct: Float,
    val seasonPassTd: Int,
    val compPctL5: Float,
    val oppYpaAllowed: Float,
    val spreadForTeam: Float? = null,
    val total: Float? = null,
)
