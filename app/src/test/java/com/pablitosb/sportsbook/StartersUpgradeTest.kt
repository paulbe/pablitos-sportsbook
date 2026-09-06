package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.mlb.ParkFactors
import com.pablitosb.sportsbook.data.starters.StartersSort
import com.pablitosb.sportsbook.data.starters.StartersSorter
import com.pablitosb.sportsbook.data.mlb.ParkSites
import com.pablitosb.sportsbook.data.mlb.RoofKind
import com.pablitosb.sportsbook.data.remote.OpenMeteoClient
import com.pablitosb.sportsbook.data.remote.SavantExpectedClient
import com.pablitosb.sportsbook.data.starters.ParkWeather
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartersUpgradeTest {

    @Test
    fun inFromCfCoolDryIsPitcherWx() {
        val snap = ParkWeather.parse("Clear", "68", "9 mph, In From CF")
        assertEquals(WindRel.IN_CF, snap.windRel)
        assertEquals(9, snap.windMph)
        assertEquals("In from CF 9 mph", snap.windLabel)
        assertEquals(WxTag.PITCHER_WX, snap.tag)
        assertEquals(Weather.SUN, snap.icon)
    }

    @Test
    fun outToRfHotIsHrWeather() {
        val snap = ParkWeather.parse("Sunny", "88", "14 mph, Out To RF")
        assertEquals(WindRel.OUT_RF, snap.windRel)
        assertEquals(WxTag.HR_WEATHER, snap.tag)
        assertEquals("Out to RF 14 mph", snap.windLabel)
    }

    @Test
    fun rainConditionIsRainRisk() {
        val snap = ParkWeather.parse("Rain", "73", "6 mph, R To L")
        assertEquals(WxTag.RAIN_RISK, snap.tag)
        assertEquals(WindRel.CROSS_RL, snap.windRel)
        assertEquals("R → L 6 mph", snap.windLabel)
        assertEquals(Weather.RAIN, snap.icon)
    }

    @Test
    fun precipPercentInConditionIsRainRisk() {
        val snap = ParkWeather.parse("70% chance of showers later", "73", "6 mph, L To R")
        assertEquals(70, snap.precipPct)
        assertEquals(WxTag.RAIN_RISK, snap.tag)
        assertEquals(WindRel.CROSS_LR, snap.windRel)
    }

    @Test
    fun hotNoWindIsHrWeather() {
        val snap = ParkWeather.parse("Sunny", "88", "0 mph, None")
        assertEquals(WindRel.NONE, snap.windRel)
        assertEquals(WxTag.HR_WEATHER, snap.tag)
        assertEquals("Calm 0 mph", snap.windLabel)
    }

    @Test
    fun roofClosedDryIsPitcherWx() {
        val snap = ParkWeather.parse("Roof closed", "78", "0 mph, None")
        assertEquals(WxTag.PITCHER_WX, snap.tag)
    }

    @Test
    fun missingWeatherIsNeutral() {
        val snap = ParkWeather.parse("", "", "")
        assertEquals(WxTag.NEUTRAL, snap.tag)
        assertEquals(WindRel.UNKNOWN, snap.windRel)
        assertEquals("Wind n/a", snap.windLabel)
        assertNull(snap.windMph)
        assertEquals(0, snap.tempF)
    }

    @Test
    fun crossBreezeMildIsNeutral() {
        val snap = ParkWeather.parse("Cloudy", "73", "6 mph, L To R")
        assertEquals(WindRel.CROSS_LR, snap.windRel)
        assertEquals(WxTag.NEUTRAL, snap.tag)
    }

    @Test
    fun classifyWindAliases() {
        assertEquals(WindRel.IN_LF, ParkWeather.classifyWind("8 mph, In From Left"))
        assertEquals(WindRel.OUT_CF, ParkWeather.classifyWind("Out To Center"))
        assertEquals(WindRel.CROSS_RL, ParkWeather.classifyWind("Right to Left"))
        assertEquals(WindRel.NONE, ParkWeather.classifyWind("0 mph, Calm"))
    }

    @Test
    fun savantQuotedHeaderMapsPlayerIdToEstWoba() {
        val csv = """
            "last_name, first_name",player_id,year,est_woba
            "Sale, Chris",519242,2026,0.285
            "Glasnow, Tyler",607192,2026,0.312
            "Missing, Id",,2026,0.300
            "Bad, Value",123,2026,1.40
        """.trimIndent()
        val map = SavantExpectedClient.parse(csv)
        assertEquals(2, map.size)
        assertEquals(0.285f, map[519242]!!, 0.0001f)
        assertEquals(0.312f, map[607192]!!, 0.0001f)
        assertTrue(!map.containsKey(123))
    }

    @Test
    fun savantMissingColumnsOrEmptyIsEmptyMap() {
        assertTrue(SavantExpectedClient.parse("").isEmpty())
        assertTrue(SavantExpectedClient.parse("foo,bar\n1,2").isEmpty())
    }

    @Test
    fun savantQuotedCsvLineKeepsCommaInsideName() {
        val cols = SavantExpectedClient.parseCsvLine("\"Sale, Chris\",519242,0.285")
        assertEquals(listOf("Sale, Chris", "519242", "0.285"), cols)
    }

    @Test
    fun yankeeCfBearingMapsWindSectors() {
        val cf = 75f
        assertEquals(WindRel.IN_CF, ParkWeather.relativeToPark(75f, cf))
        assertEquals(WindRel.IN_RF, ParkWeather.relativeToPark(120f, cf))
        assertEquals(WindRel.CROSS_RL, ParkWeather.relativeToPark(165f, cf))
        assertEquals(WindRel.OUT_LF, ParkWeather.relativeToPark(210f, cf))
        assertEquals(WindRel.OUT_CF, ParkWeather.relativeToPark(255f, cf))
        assertEquals(WindRel.OUT_RF, ParkWeather.relativeToPark(300f, cf))
        assertEquals(WindRel.CROSS_LR, ParkWeather.relativeToPark(345f, cf))
        assertEquals(WindRel.IN_LF, ParkWeather.relativeToPark(30f, cf))
    }

    @Test
    fun forecastInFromCfCoolDryIsPitcherWx() {
        val snap = ParkWeather.fromForecast(
            tempF = 68,
            windMph = 9,
            windFromDeg = 75,
            precipPct = 5,
            weatherCode = 0,
            cfBearingDeg = 75f,
            indoor = false,
        )
        assertEquals(WindRel.IN_CF, snap.windRel)
        assertEquals("In from CF 9 mph", snap.windLabel)
        assertEquals(WxTag.PITCHER_WX, snap.tag)
        assertEquals(Weather.SUN, snap.icon)
        assertEquals(5, snap.precipPct)
    }

    @Test
    fun forecastOutHotIsHrWeather() {
        val snap = ParkWeather.fromForecast(
            tempF = 88,
            windMph = 14,
            windFromDeg = 255,
            precipPct = 0,
            weatherCode = 1,
            cfBearingDeg = 75f,
            indoor = false,
        )
        assertEquals(WindRel.OUT_CF, snap.windRel)
        assertEquals(WxTag.HR_WEATHER, snap.tag)
    }

    @Test
    fun forecastPrecipIsRainRisk() {
        val snap = ParkWeather.fromForecast(
            tempF = 73,
            windMph = 6,
            windFromDeg = 165,
            precipPct = 70,
            weatherCode = 61,
            cfBearingDeg = 75f,
            indoor = false,
        )
        assertEquals(WxTag.RAIN_RISK, snap.tag)
        assertEquals(70, snap.precipPct)
        assertEquals(Weather.RAIN, snap.icon)
        assertEquals(WindRel.CROSS_RL, snap.windRel)
    }

    @Test
    fun domeIgnoresOutdoorWindAndHeat() {
        val snap = ParkWeather.fromForecast(
            tempF = 95,
            windMph = 18,
            windFromDeg = 255,
            precipPct = 80,
            weatherCode = 95,
            cfBearingDeg = 359f,
            indoor = true,
            mlbCondition = "Dome",
        )
        assertEquals(WxTag.PITCHER_WX, snap.tag)
        assertEquals(WindRel.NONE, snap.windRel)
        assertEquals("Dome / roof", snap.windLabel)
        assertNull(snap.precipPct)
    }

    @Test
    fun forecastWinsOverEmptyMlb() {
        val mlb = ParkWeather.parse("", "", "")
        val forecast = ParkWeather.fromForecast(72, 8, 13, 10, 2, 13f, false)
        val resolved = ParkWeather.resolve(forecast, mlb, indoor = false)
        assertEquals(forecast.windLabel, resolved.windLabel)
        assertEquals(72, resolved.tempF)
    }

    @Test
    fun missingForecastKeepsMlbOrNeutral() {
        val mlb = ParkWeather.parse("", "", "")
        val resolved = ParkWeather.resolve(null, mlb, indoor = false)
        assertEquals(WxTag.NEUTRAL, resolved.tag)
        assertEquals("Wind n/a", resolved.windLabel)
    }

    @Test
    fun tropicanaIsFixedDome() {
        assertEquals(RoofKind.DOME, ParkSites.roof(12, "Tropicana Field"))
        assertTrue(ParkSites.isIndoor(RoofKind.DOME, ""))
        assertTrue(!ParkSites.isIndoor(RoofKind.RETRACTABLE, ""))
        assertTrue(ParkSites.isIndoor(RoofKind.RETRACTABLE, "Roof closed"))
    }

    @Test
    fun coorsCalmIsHrWeatherEvenWithoutWind() {
        val snap = ParkWeather.fromForecast(
            tempF = 70,
            windMph = 0,
            windFromDeg = 0,
            precipPct = 5,
            weatherCode = 0,
            cfBearingDeg = 4f,
            indoor = false,
            hrParkFactor = 1.28f,
        )
        assertEquals(WxTag.HR_WEATHER, snap.tag)
        assertEquals("HR park · PF 1.28", snap.parkHint)
    }

    @Test
    fun coorsMildOutIsHrWeather() {
        val snap = ParkWeather.fromForecast(
            tempF = 68,
            windMph = 4,
            windFromDeg = 184,
            precipPct = 0,
            weatherCode = 1,
            cfBearingDeg = 4f,
            indoor = false,
            hrParkFactor = ParkFactors.hrMultiplier(19, "Coors Field"),
        )
        assertEquals(WindRel.OUT_CF, snap.windRel)
        assertEquals(WxTag.HR_WEATHER, snap.tag)
    }

    @Test
    fun petcoMildOutStaysNeutral() {
        val snap = ParkWeather.fromForecast(
            tempF = 72,
            windMph = 8,
            windFromDeg = 180,
            precipPct = 0,
            weatherCode = 0,
            cfBearingDeg = 0f,
            indoor = false,
            hrParkFactor = 0.90f,
        )
        assertEquals(WindRel.OUT_CF, snap.windRel)
        assertEquals(WxTag.NEUTRAL, snap.tag)
        assertEquals("Pitcher park · PF 0.90", snap.parkHint)
    }

    @Test
    fun petcoStrongOutAndHeatOverridesToHr() {
        val snap = ParkWeather.fromForecast(
            tempF = 88,
            windMph = 12,
            windFromDeg = 180,
            precipPct = 0,
            weatherCode = 0,
            cfBearingDeg = 0f,
            indoor = false,
            hrParkFactor = 0.90f,
        )
        assertEquals(WxTag.HR_WEATHER, snap.tag)
    }

    @Test
    fun petcoCalmBiasesPitcherWx() {
        val snap = ParkWeather.fromForecast(
            tempF = 70,
            windMph = 2,
            windFromDeg = 0,
            precipPct = 5,
            weatherCode = 2,
            cfBearingDeg = 0f,
            indoor = false,
            hrParkFactor = 0.90f,
        )
        assertEquals(WxTag.PITCHER_WX, snap.tag)
    }

    @Test
    fun rainRiskIgnoresCoorsParkFactor() {
        val snap = ParkWeather.fromForecast(
            tempF = 70,
            windMph = 4,
            windFromDeg = 184,
            precipPct = 70,
            weatherCode = 61,
            cfBearingDeg = 4f,
            indoor = false,
            hrParkFactor = 1.28f,
        )
        assertEquals(WxTag.RAIN_RISK, snap.tag)
        assertEquals("HR park · PF 1.28", snap.parkHint)
    }

    @Test
    fun leagueAverageOutSixStillHr() {
        val snap = ParkWeather.parse("Sunny", "70", "6 mph, Out To CF", hrParkFactor = 1.00f)
        assertEquals(WxTag.HR_WEATHER, snap.tag)
        assertEquals("PF 1.00", snap.parkHint)
    }

    @Test
    fun openMeteoParsePicksHourlyFields() {
        val json = """
            {
              "utc_offset_seconds": -14400,
              "hourly": {
                "time": ["2026-09-05T16:00"],
                "temperature_2m": [78.2],
                "precipitation_probability": [12],
                "weather_code": [3],
                "wind_speed_10m": [8.4],
                "wind_direction_10m": [338]
              }
            }
        """.trimIndent()
        val hours = OpenMeteoClient.parse(json)
        assertEquals(1, hours.size)
        assertEquals(78, hours[0].tempF)
        assertEquals(12, hours[0].precipPct)
        assertEquals(8, hours[0].windMph)
        assertEquals(338, hours[0].windFromDeg)
        assertEquals(3, hours[0].weatherCode)
        assertEquals(Instant.parse("2026-09-05T20:00:00Z"), hours[0].at)
    }

    @Test
    fun envBoostCoorsCalmIsStronglyPositive() {
        val boost = ParkWeather.envBoostPct(
            rain = false,
            indoor = false,
            windRel = WindRel.NONE,
            windMph = 0,
            tempF = 70,
            hrParkFactor = 1.28f,
        )
        assertEquals(28, boost)
        assertEquals("Weather boost +28%", ParkWeather.boostLabel(boost))
    }

    @Test
    fun envBoostPetcoInWindIsNegative() {
        val boost = ParkWeather.envBoostPct(
            rain = false,
            indoor = false,
            windRel = WindRel.IN_CF,
            windMph = 8,
            tempF = 68,
            hrParkFactor = 0.90f,
        )
        assertEquals(-21, boost)
        assertEquals("Weather boost -21%", ParkWeather.boostLabel(boost))
    }

    @Test
    fun envBoostRainIsLargeNegativeNotKBoost() {
        val boost = ParkWeather.envBoostPct(
            rain = true,
            indoor = false,
            windRel = WindRel.OUT_CF,
            windMph = 12,
            tempF = 88,
            hrParkFactor = 1.28f,
            precipPct = 70,
        )
        assertTrue(boost in -45..-20)
    }

    @Test
    fun envBoostIndoorMutesParkOnly() {
        assertEquals(7, ParkWeather.envBoostPct(false, true, WindRel.OUT_CF, 20, 95, 1.28f))
        assertEquals(-3, ParkWeather.envBoostPct(false, true, WindRel.OUT_CF, 20, 95, 0.90f))
    }

    @Test
    fun sortXwobaAscendingMissingLast() {
        val list = listOf(sp("NoX", xwoba = null, ks = 9f), sp("High", xwoba = 0.340f), sp("Low", xwoba = 0.250f))
        val sorted = StartersSorter.sort(list, StartersSort.XWOBA, ascending = true)
        assertEquals(listOf("Low", "High", "NoX"), sorted.map { it.name })
    }

    @Test
    fun sortProjKsAndProjOuts() {
        val list = listOf(
            sp("Ace", ks = 8.2f, outs = 18.6f),
            sp("Soft", ks = 4.1f, outs = 14.1f),
        )
        assertEquals("Ace", StartersSorter.sort(list, StartersSort.PROJ_KS, false).first().name)
        assertEquals("Soft", StartersSorter.sort(list, StartersSort.PROJ_KS, true).first().name)
        assertEquals("Ace", StartersSorter.sort(list, StartersSort.PROJ_OUTS, false).first().name)
        assertEquals("Soft", StartersSorter.sort(list, StartersSort.PROJ_OUTS, true).first().name)
    }

    private fun sp(
        name: String,
        score: Int = 0,
        ks: Float = 5f,
        xwoba: Float? = null,
        boost: Int = 0,
        rain: Boolean = false,
        start: Instant? = null,
        outs: Float = 15f,
    ) = Starter(
        rank = 1,
        name = name,
        team = "NYY",
        opponent = "BOS",
        venue = "Yankee Stadium",
        weather = Weather.SUN,
        tempF = 70,
        outlook = Outlook.STABLE,
        outlookScore = score,
        projKPct = 25f,
        nextStartKs = ks,
        trend = emptyList(),
        xwoba = xwoba,
        wxTag = if (rain) WxTag.RAIN_RISK else WxTag.NEUTRAL,
        envBoostPct = boost,
        gameStart = start,
        projOuts = outs,
        projIp = outs / 3f,
    )
}
