package com.pablitosb.sportsbook.data.starters

import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Park-relative weather.
 *
 * Primary source is Open-Meteo hourly at the park lat/long (first-pitch hour).
 * MLB schedule hydrate (`weather.condition` / `temp` / `wind`) is an optional
 * supplement — often empty until near first pitch, and used for roof-closed
 * wording when present.
 *
 * Wind-from (meteorological degrees) is rotated by the park CF bearing
 * (MLB `location.azimuthAngle` = home → CF, clockwise from true north):
 * - Δ ≈ 0°   In from CF
 * - Δ ≈ +45° In from RF
 * - Δ ≈ +90° R → L
 * - Δ ≈ +135° Out to LF
 * - Δ ≈ 180° Out to CF
 * - Δ ≈ −45° In from LF
 * - Δ ≈ −90° L → R
 * - Δ ≈ −135° Out to RF
 *
 * Tag heuristics (first match wins)
 * 1. **RAIN RISK** — precip ≥ 40%, rain WMO code with precip ≥ 25%,
 *    thunderstorm code, or MLB condition rain/delay text.
 * 2. **HR WEATHER** — outdoor and (wind out ≥ 6 mph **or** temp ≥ 82°F).
 * 3. **PITCHER WX** — outdoor wind in ≥ 6 mph and temp ≤ 72 dry;
 *    or indoor (fixed dome / roof closed) and dry.
 * 4. **NEUTRAL** — otherwise (including failed fetch).
 *
 * Indoor parks ignore outdoor wind/temp for tags. We never invent numbers:
 * missing forecast + empty MLB → Neutral / Wind n/a.
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

    fun empty(): Snapshot = parse("", "", "")

    fun parse(condition: String, tempRaw: String, windRaw: String): Snapshot {
        val conditionClean = condition.trim()
        val temp = tempRaw.toIntOrNull() ?: 0
        val mph = Regex("""(\d+)\s*mph""", RegexOption.IGNORE_CASE)
            .find(windRaw)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val rel = classifyWind(windRaw)
        val precip = Regex("""(\d+)\s*%""").find(conditionClean)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val indoor = isRoofClosed(conditionClean)
        return assemble(
            condition = conditionClean,
            tempF = temp,
            windMph = mph,
            windRel = rel,
            precipPct = precip,
            indoor = indoor,
            rainHint = isRain(conditionClean),
            weatherCode = null,
            windFromDeg = null,
            fallbackWindRaw = windRaw,
        )
    }

    fun fromForecast(
        tempF: Int?,
        windMph: Int?,
        windFromDeg: Int?,
        precipPct: Int?,
        weatherCode: Int?,
        cfBearingDeg: Float?,
        indoor: Boolean,
        mlbCondition: String = "",
    ): Snapshot {
        val mph = windMph
        val rel = when {
            indoor -> WindRel.NONE
            (mph ?: 0) <= 1 -> WindRel.NONE
            cfBearingDeg != null && windFromDeg != null ->
                relativeToPark(windFromDeg.toFloat(), cfBearingDeg)
            else -> WindRel.UNKNOWN
        }
        val rainHint = isRain(mlbCondition) || isRainCode(weatherCode)
        val condition = listOf(
            wmoLabel(weatherCode),
            mlbCondition.trim().takeIf { it.isNotBlank() },
        ).filterNotNull().joinToString(" · ").ifBlank { "Open-Meteo" }
        return assemble(
            condition = condition,
            tempF = tempF ?: 0,
            windMph = if (indoor) 0 else mph,
            windRel = rel,
            precipPct = precipPct,
            indoor = indoor,
            rainHint = rainHint,
            weatherCode = weatherCode,
            windFromDeg = windFromDeg,
            fallbackWindRaw = "",
            indoorLabel = indoor,
        )
    }

    /**
     * Forecast wins when it has a temp or wind. MLB hydrate fills gaps only.
     * Indoor (dome / roof closed) always overrides outdoor wind.
     */
    fun resolve(forecast: Snapshot?, mlb: Snapshot, indoor: Boolean): Snapshot {
        if (indoor) {
            val base = forecast ?: mlb
            return fromForecast(
                tempF = base.tempF.takeIf { it > 0 },
                windMph = 0,
                windFromDeg = null,
                precipPct = null,
                weatherCode = null,
                cfBearingDeg = null,
                indoor = true,
                mlbCondition = mlb.condition.ifBlank { "Roof closed" },
            )
        }
        if (forecast != null && (forecast.tempF > 0 || forecast.windMph != null)) {
            return forecast
        }
        return mlb
    }

    /**
     * @param windFromDeg meteorological from-direction (0 = from N, 90 = from E)
     * @param cfBearingDeg home plate → CF
     */
    fun relativeToPark(windFromDeg: Float, cfBearingDeg: Float): WindRel {
        val delta = normalize180(windFromDeg - cfBearingDeg)
        return when {
            abs(delta) <= 22.5f -> WindRel.IN_CF
            delta > 22.5f && delta <= 67.5f -> WindRel.IN_RF
            delta > 67.5f && delta <= 112.5f -> WindRel.CROSS_RL
            delta > 112.5f && delta <= 157.5f -> WindRel.OUT_LF
            abs(delta) > 157.5f -> WindRel.OUT_CF
            delta < -22.5f && delta >= -67.5f -> WindRel.IN_LF
            delta < -67.5f && delta >= -112.5f -> WindRel.CROSS_LR
            delta < -112.5f && delta >= -157.5f -> WindRel.OUT_RF
            else -> WindRel.UNKNOWN
        }
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

    private fun assemble(
        condition: String,
        tempF: Int,
        windMph: Int?,
        windRel: WindRel,
        precipPct: Int?,
        indoor: Boolean,
        rainHint: Boolean,
        weatherCode: Int?,
        windFromDeg: Int?,
        fallbackWindRaw: String,
        indoorLabel: Boolean = indoor,
    ): Snapshot {
        val rain = when {
            indoor -> false
            precipPct != null && precipPct >= 40 -> true
            isRainCode(weatherCode) && (precipPct ?: 0) >= 25 -> true
            isStormCode(weatherCode) -> true
            rainHint && !indoor -> true
            else -> false
        }
        val tag = when {
            rain -> WxTag.RAIN_RISK
            indoor -> WxTag.PITCHER_WX
            isWindOut(windRel) && (windMph ?: 0) >= 6 || tempF >= 82 -> WxTag.HR_WEATHER
            isWindIn(windRel) && (windMph ?: 0) >= 6 && tempF in 1..72 && !rain -> WxTag.PITCHER_WX
            else -> WxTag.NEUTRAL
        }
        val icon = when {
            rain -> Weather.RAIN
            weatherCode != null -> iconForCode(weatherCode)
            condition.contains("sun", true) || condition.contains("clear", true) -> Weather.SUN
            else -> Weather.CLOUD
        }
        val windLabel = when {
            indoorLabel -> "Dome / roof"
            else -> formatWind(windRel, windMph, fallbackWindRaw, windFromDeg)
        }
        return Snapshot(
            condition = condition,
            tempF = if (indoor && tempF <= 0) 72 else tempF,
            windRaw = fallbackWindRaw.trim(),
            windMph = if (indoor) 0 else windMph,
            windRel = if (indoor) WindRel.NONE else windRel,
            windLabel = windLabel,
            precipPct = if (indoor) null else precipPct,
            tag = tag,
            icon = icon,
        )
    }

    private fun formatWind(rel: WindRel, mph: Int?, raw: String, fromDeg: Int?): String {
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
            WindRel.UNKNOWN -> fromDeg?.let { "From ${cardinal(it)}" }
                ?: raw.substringAfter(',', raw).trim().ifBlank { "Wind n/a" }
        }
        return listOf(dir, speed).filter { it.isNotBlank() }.joinToString(" ")
    }

    fun cardinal(deg: Int): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val i = (((deg % 360 + 360) % 360) / 45f).roundToInt() % 8
        return dirs[i]
    }

    fun wmoLabel(code: Int?): String? = when (code) {
        null -> null
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 85..86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> null
    }

    private fun iconForCode(code: Int): Weather = when (code) {
        0, 1 -> Weather.SUN
        in 51..67, in 80..82, in 95..99 -> Weather.RAIN
        else -> Weather.CLOUD
    }

    private fun isRain(condition: String): Boolean {
        val c = condition.lowercase()
        return listOf("rain", "shower", "storm", "drizzle", "delay", "postpon").any { c.contains(it) }
    }

    private fun isRoofClosed(condition: String): Boolean {
        val c = condition.lowercase()
        return c.contains("roof") || c.contains("dome") || c.contains("indoor")
    }

    private fun isRainCode(code: Int?) =
        code != null && (code in 51..67 || code in 80..82 || isStormCode(code))

    private fun isStormCode(code: Int?) = code != null && code in 95..99

    private fun isWindOut(rel: WindRel) =
        rel == WindRel.OUT_CF || rel == WindRel.OUT_LF || rel == WindRel.OUT_RF

    private fun isWindIn(rel: WindRel) =
        rel == WindRel.IN_CF || rel == WindRel.IN_LF || rel == WindRel.IN_RF

    private fun normalize180(deg: Float): Float {
        var d = deg % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }
}
