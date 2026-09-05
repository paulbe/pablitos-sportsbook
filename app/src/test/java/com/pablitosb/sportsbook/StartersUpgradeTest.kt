package com.pablitosb.sportsbook

import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.remote.SavantExpectedClient
import com.pablitosb.sportsbook.data.starters.ParkWeather
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
}
