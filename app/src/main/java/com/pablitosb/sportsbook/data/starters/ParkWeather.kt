package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag

/**
 * Park-relative weather from MLB Stats API schedule hydrate
 * (`weather.condition`, `weather.temp`, `weather.wind`).
 *
 * Wind strings we have seen: `"6 mph, In From CF"`, `"8 mph, Out To LF"`,
 * `"6 mph, L To R"`, `"0 mph, None"`. There is usually **no precip %** on
 * this feed — rain tags come from the condition text (and a % only if MLB
 * embeds one).
 *
 * Tag heuristics (first match wins)
 * 1. **RAIN RISK** — condition mentions rain / shower / storm / drizzle /
 *    delay / postpon, or a parsed precip %.
 * 2. **HR WEATHER** — wind out (any field) at ≥6 mph, **or** temp ≥ 82°F
 *    and the roof is not closed.
 * 3. **PITCHER WX** — wind in at ≥6 mph **and** temp ≤ 72 **and** dry;
 *    or a closed roof / dome with no rain.
 * 4. **NEUTRAL** — otherwise (including missing weather).
 */
object ParkWeather {
    data class Snapshot(
        val condition: String,
        val tempF: Int,
        val windRaw: String,
        val windMph: Int?,
        val windRel: WindRel,
        val windLabel: String,
        val precipPct: Int?,
        val tag: WxTag,
        val icon: Weather,
    )

    fun parse(condition: String, tempRaw: String, windRaw: String): Snapshot {
        val conditionClean = condition.trim()
        val temp = tempRaw.toIntOrNull() ?: 0
        val mph = Regex("""(\d+)\s*mph""", RegexOption.IGNORE_CASE)
            .find(windRaw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val rel = classifyWind(windRaw)
        val precip = Regex("""(\d+)\s*%""").find(conditionClean)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val rain = isRain(conditionClean) || (precip != null && precip >= 40)
        val roof = isRoofClosed(conditionClean)
        val tag = when {
            rain -> WxTag.RAIN_RISK
            !roof && (isWindOut(rel) && (mph ?: 0) >= 6 || temp >= 82) -> WxTag.HR_WEATHER
            (isWindIn(rel) && (mph ?: 0) >= 6 && temp in 1..72 && !rain) || (roof && !rain) ->
                WxTag.PITCHER_WX
            else -> WxTag.NEUTRAL
        }
        val icon = when {
            rain -> Weather.RAIN
            conditionClean.contains("sun", true) || conditionClean.contains("clear", true) -> Weather.SUN
            else -> Weather.CLOUD
        }
        val windLabel = formatWind(rel, mph, windRaw)
        return Snapshot(
            condition = conditionClean,
            tempF = temp,
            windRaw = windRaw.trim(),
            windMph = mph,
            windRel = rel,
            windLabel = windLabel,
            precipPct = precip,
            tag = tag,
            icon = icon,
        )
    }

    fun classifyWind(raw: String): WindRel {
        val w = raw.lowercase()
        return when {
            w.contains("none") || w.contains("calm") -> WindRel.NONE
            w.contains("in from cf") || w.contains("in from center") -> WindRel.IN_CF
            w.contains("in from lf") || w.contains("in from left") -> WindRel.IN_LF
            w.contains("in from rf") || w.contains("in from right") -> WindRel.IN_RF
            w.contains("out to cf") || w.contains("out to center") -> WindRel.OUT_CF
            w.contains("out to lf") || w.contains("out to left") -> WindRel.OUT_LF
            w.contains("out to rf") || w.contains("out to right") -> WindRel.OUT_RF
            w.contains("l to r") || w.contains("l → r") || w.contains("left to right") -> WindRel.CROSS_LR
            w.contains("r to l") || w.contains("r → l") || w.contains("right to left") -> WindRel.CROSS_RL
            w.contains("in from") -> WindRel.IN_CF
            w.contains("out to") -> WindRel.OUT_CF
            w.isBlank() -> WindRel.UNKNOWN
            else -> WindRel.UNKNOWN
        }
    }

    private fun formatWind(rel: WindRel, mph: Int?, raw: String): String {
        val speed = mph?.let { "$it mph" }.orEmpty()
        val dir = when (rel) {
            WindRel.IN_CF -> "In from CF"
            WindRel.IN_LF -> "In from LF"
            WindRel.IN_RF -> "In from RF"
            WindRel.OUT_CF -> "Out to CF"
            WindRel.OUT_LF -> "Out to LF"
            WindRel.OUT_RF -> "Out to RF"
            WindRel.CROSS_LR -> "L → R"
            WindRel.CROSS_RL -> "R → L"
            WindRel.NONE -> "Calm"
            WindRel.UNKNOWN -> raw.substringAfter(',', raw).trim().ifBlank { "Wind n/a" }
        }
        return listOf(dir, speed).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun isRain(condition: String): Boolean {
        val c = condition.lowercase()
        return listOf("rain", "shower", "storm", "drizzle", "delay", "postpon").any { c.contains(it) }
    }

    private fun isRoofClosed(condition: String): Boolean {
        val c = condition.lowercase()
        return c.contains("roof") || c.contains("dome") || c.contains("indoor")
    }

    private fun isWindOut(rel: WindRel) =
        rel == WindRel.OUT_CF || rel == WindRel.OUT_LF || rel == WindRel.OUT_RF

    private fun isWindIn(rel: WindRel) =
        rel == WindRel.IN_CF || rel == WindRel.IN_LF || rel == WindRel.IN_RF
}
