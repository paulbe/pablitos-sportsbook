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
    val windLabel: String = "",
    val windRel: WindRel = WindRel.UNKNOWN,
    val windMph: Int? = null,
    val wxTag: WxTag = WxTag.NEUTRAL,
    val precipPct: Int? = null,
    val weatherCondition: String = "",
    val xwoba: Float? = null,
    val ace: Boolean = false,
    val hrParkFactor: Float = 1f,
    val parkHint: String = "",
)

enum class Weather { SUN, CLOUD, RAIN }

enum class WindRel { IN_CF, IN_LF, IN_RF, OUT_CF, OUT_LF, OUT_RF, CROSS_LR, CROSS_RL, NONE, UNKNOWN }

enum class WxTag { RAIN_RISK, HR_WEATHER, PITCHER_WX, NEUTRAL }

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
    val mlbId: Int = 0,
    val battingOrder: Int? = null,
    val sourceNote: String = "",
    val expectedPa: Float = 0f,
)

data class DfsPlayer(
    val pos: String,
    val name: String,
    val salary: Int,
    val proj: Float,
    val team: String = "",
    val mlbId: Int = 0,
)

enum class LineupKind { CASH_CORE, STACK_A, STACK_B, LEVERAGE, CONTRARIAN }

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

enum class PropLineSource { MODEL_BOARD, IMPORTED }

data class UnderdogProp(
    val rank: Int,
    val player: String,
    val team: String,
    val propLabel: String,
    val line: String,
    val odds: Int? = null,
    val modelProb: Float,
    val impliedProb: Float? = null,
    val confidence: Confidence,
    val source: PropLineSource = PropLineSource.MODEL_BOARD,
    val market: String = "",
) {
    val edgePct: Float? get() = impliedProb?.let { modelProb - it }
}

enum class ContestType { CASH, GPP }
