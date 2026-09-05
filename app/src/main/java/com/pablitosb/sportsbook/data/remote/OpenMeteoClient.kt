package com.pablitosb.sportsbook.data.remote

import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Free, no-key hourly forecast from Open-Meteo.
 *
 * Forecast: `https://api.open-meteo.com/v1/forecast`
 * Archive (older slates): `https://archive-api.open-meteo.com/v1/archive`
 *
 * Cached per rounded lat/lon + slate date + fetch hour so a 15-game slate
 * is one request per park, refreshed at most hourly.
 */
class OpenMeteoClient(
    private val http: OkHttpClient = client,
    private val now: () -> Instant = Instant::now,
) {
    private val cache = ConcurrentHashMap<String, CacheRow>()

    data class HourlyWx(
        val at: Instant,
        val tempF: Int?,
        val precipPct: Int?,
        val windMph: Int?,
        val windFromDeg: Int?,
        val weatherCode: Int?,
    )

    fun hourAt(lat: Double, lon: Double, at: Instant, slate: LocalDate): HourlyWx? {
        val hours = runCatching { series(lat, lon, slate) }.getOrDefault(emptyList())
        if (hours.isEmpty()) return null
        return hours.minByOrNull { Duration.between(it.at, at).abs() }
            ?.takeIf { Duration.between(it.at, at).abs() <= Duration.ofHours(3) }
    }

    fun series(lat: Double, lon: Double, slate: LocalDate): List<HourlyWx> {
        val bucket = now().epochSecond / 3600
        val key = "${String.format(Locale.US, "%.3f", lat)},${String.format(Locale.US, "%.3f", lon)}|$slate|$bucket"
        cache[key]?.let { return it.hours }
        val today = LocalDate.now(ZoneOffset.UTC)
        val body = if (slate.isBefore(today.minusDays(2))) {
            get(archiveUrl(lat, lon, slate))
        } else {
            get(forecastUrl(lat, lon))
        }
        val hours = parse(body)
        cache[key] = CacheRow(hours)
        prune(bucket)
        return hours
    }

    fun get(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Open-Meteo HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun prune(currentBucket: Long) {
        if (cache.size < 48) return
        cache.keys.toList().forEach { k ->
            val b = k.substringAfterLast('|').toLongOrNull() ?: return@forEach
            if (currentBucket - b > 2) cache.remove(k)
        }
    }

    private data class CacheRow(val hours: List<HourlyWx>)

    companion object {
        const val USER_AGENT = "PablitosSportsbook/1.6 (personal; Android)"
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()

        fun forecastUrl(lat: Double, lon: Double): String =
            "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$lat&longitude=$lon" +
                "&hourly=temperature_2m,precipitation_probability,weather_code," +
                "wind_speed_10m,wind_direction_10m" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph" +
                "&timezone=auto&past_days=3&forecast_days=16"

        fun archiveUrl(lat: Double, lon: Double, date: LocalDate): String =
            "https://archive-api.open-meteo.com/v1/archive?" +
                "latitude=$lat&longitude=$lon&start_date=$date&end_date=$date" +
                "&hourly=temperature_2m,precipitation,precipitation_probability,weather_code," +
                "wind_speed_10m,wind_direction_10m" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto"

        fun parse(json: String): List<HourlyWx> {
            if (json.isBlank()) return emptyList()
            val root = JSONObject(json)
            val hourly = root.optObj("hourly") ?: return emptyList()
            val times = hourly.optArr("time")
            val offset = root.optInt("utc_offset_seconds", 0)
            val zone = ZoneOffset.ofTotalSeconds(offset)
            val temps = hourly.optArr("temperature_2m")
            val pops = hourly.optArr("precipitation_probability")
            val codes = hourly.optArr("weather_code")
            val speeds = hourly.optArr("wind_speed_10m")
            val dirs = hourly.optArr("wind_direction_10m")
            val precipMm = hourly.optArr("precipitation")
            val out = ArrayList<HourlyWx>(times.length())
            for (i in 0 until times.length()) {
                val stamp = times.optString(i)
                if (stamp.isNullOrBlank()) continue
                val at = runCatching {
                    LocalDateTime.parse(stamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(zone)
                }.getOrNull() ?: continue
                val pop = pops.optNumberAt(i)?.toInt()
                    ?: precipMm.optNumberAt(i)?.let { mm -> if (mm > 0.2) 70 else if (mm > 0) 40 else 0 }
                out += HourlyWx(
                    at = at,
                    tempF = temps.optNumberAt(i)?.toInt(),
                    precipPct = pop,
                    windMph = speeds.optNumberAt(i)?.toInt(),
                    windFromDeg = dirs.optNumberAt(i)?.toInt(),
                    weatherCode = codes.optNumberAt(i)?.toInt(),
                )
            }
            return out
        }
    }
}
