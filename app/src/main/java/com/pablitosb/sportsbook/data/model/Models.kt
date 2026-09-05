package com.pablitosb.sportsbook.data.model

enum class Outlook { PROG, STABLE, REG }

data class Starter(
    val rank: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val venue: String,
    val weather: Weather,
    val tempF: Int,
    val outlook: Outlook,
    val outlookScore: Int,
    val projKPct: Float,
    val nextStartKs: Float,
    val trend: List<Float>,
    val gameTimeLabel: String = "",
    val mlbId: Int = 0,
    val homeAway: String = "",
    val actualKs: Int? = null,
    val actualBf: Int? = null,
    val actualKPct: Float? = null,
    val ksDelta: Float? = null,
    val kPctDelta: Float? = null,
    val resultNote: String = "",
)

enum class Weather { SUN, CLOUD }

data class HrBatter(
    val rank: Int,
    val name: String,
    val team: String,
    val opponent: String,
    val pitcherHand: String,
    val gameHrPct: Float,
    val seasonHrPct: Float,
    val xHrPct: Int,
    val parkAdjPct: Int,
    val parkName: String,
    val weather: Weather,
    val tempF: Int,
    val pitcherAdjPct: Int,
    val pitcherName: String,
    val pitcherHr9: Float,
    val regressionLean: Boolean = false,
)

data class DfsPlayer(
    val pos: String,
    val name: String,
    val salary: Int,
    val proj: Float,
)

enum class LineupKind { CASH_CORE, NYY_STACK, LAD_STACK, LEVERAGE, CONTRARIAN }

data class DfsLineup(
    val index: Int,
    val kind: LineupKind,
    val title: String,
    val contest: String,
    val stackNote: String,
    val salary: Int,
    val salaryCap: Int = 35_000,
    val proj: Float,
    val ceiling: Int,
    val avgOwnPct: Int,
    val players: List<DfsPlayer>,
)

enum class Confidence { VERY_HIGH, HIGH, MEDIUM, LOW }

data class UnderdogProp(
    val rank: Int,
    val player: String,
    val team: String,
    val propLabel: String,
    val line: String,
    val odds: Int,
    val modelProb: Float,
    val impliedProb: Float,
    val confidence: Confidence,
) {
    val edgePct: Float get() = modelProb - impliedProb
}

enum class ContestType { CASH, GPP }
