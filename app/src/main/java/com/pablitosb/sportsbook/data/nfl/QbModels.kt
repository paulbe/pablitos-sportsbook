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
    val rushMatchupPct: Float,
    val projRushYds: Float,
    val rushFloor: Float,
    val rushCeiling: Float,
    val expAtt: Float,
    val expYpa: Float,
    val expRushAtt: Float,
    val expYpc: Float,
    val expRushTd: Float,
    val espnId: String,
) {
    fun matchupFor(sort: QbSort): Float = if (sort == QbSort.PROJ_RUSH) rushMatchupPct else matchupPct
}

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
    val rushAttL3: Float = 0f,
    val rushAttL3Games: Int = 0,
    val rushAttSeasonPerGame: Float = 0f,
    val teamRushAttPerGame: Float = 0f,
    val ypcL5: Float = 0f,
    val ypcL5Games: Int = 0,
    val ypcSeason: Float = 0f,
    val seasonRushAtt: Int = 0,
    val seasonRushTd: Int = 0,
    val oppYpcAllowed: Float = 0f,
    val spreadForTeam: Float? = null,
    val total: Float? = null,
)
