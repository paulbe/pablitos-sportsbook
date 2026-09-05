package com.pablitosb.sportsbook.data.mlb

/**
 * Roof type + fallback lat/long/CF azimuth when schedule `venue(location)` is thin.
 *
 * [cfBearingDeg] is home plate → center field, same meaning as MLB
 * `location.azimuthAngle` (degrees clockwise from true north).
 */
enum class RoofKind { OPEN, DOME, RETRACTABLE }

data class ParkSite(
    val venueId: Int,
    val name: String,
    val lat: Double,
    val lon: Double,
    val cfBearingDeg: Float?,
    val roof: RoofKind,
)

object ParkSites {
    private val byId: Map<Int, ParkSite> = listOf(
        site(1, "Angel Stadium", 33.80019, -117.88240, 43.61f, RoofKind.OPEN),
        site(2, "Camden Yards", 39.28379, -76.62169, 31f, RoofKind.OPEN),
        site(3, "Fenway Park", 42.34646, -71.09744, 45f, RoofKind.OPEN),
        site(4, "Rate Field", 41.83000, -87.63417, 127f, RoofKind.OPEN),
        site(5, "Progressive Field", 41.49586, -81.68526, 0f, RoofKind.OPEN),
        site(7, "Kauffman Stadium", 39.05157, -94.48048, 46f, RoofKind.OPEN),
        site(12, "Tropicana Field", 27.76778, -82.65250, 359f, RoofKind.DOME),
        site(14, "Rogers Centre", 43.64155, -79.38915, 345f, RoofKind.RETRACTABLE),
        site(15, "Chase Field", 33.44530, -112.06669, 0f, RoofKind.RETRACTABLE),
        site(17, "Wrigley Field", 41.94817, -87.65550, 37f, RoofKind.OPEN),
        site(19, "Coors Field", 39.75604, -104.99414, 4f, RoofKind.OPEN),
        site(22, "Dodger Stadium", 34.07368, -118.24053, 26f, RoofKind.OPEN),
        site(31, "PNC Park", 40.44690, -80.00575, 116f, RoofKind.OPEN),
        site(32, "American Family Field", 43.02838, -87.97099, 129f, RoofKind.RETRACTABLE),
        site(680, "T-Mobile Park", 47.59133, -122.33251, 49f, RoofKind.RETRACTABLE),
        site(2392, "Daikin Park", 29.75697, -95.35551, 343f, RoofKind.RETRACTABLE),
        site(2394, "Comerica Park", 42.33912, -83.04870, 150f, RoofKind.OPEN),
        site(2395, "Oracle Park", 37.77838, -122.38945, 85f, RoofKind.OPEN),
        site(2523, "Steinbrenner Field", 27.97997, -82.50702, 60f, RoofKind.OPEN),
        site(2529, "Sutter Health Park", 38.57994, -121.51246, 46f, RoofKind.OPEN),
        site(2602, "Great American Ball Park", 39.09739, -84.50661, 122f, RoofKind.OPEN),
        site(2680, "Petco Park", 32.70786, -117.15728, 0f, RoofKind.OPEN),
        site(2681, "Citizens Bank Park", 39.90539, -75.16717, 9f, RoofKind.OPEN),
        site(2889, "Busch Stadium", 38.62257, -90.19287, 62f, RoofKind.OPEN),
        site(3289, "Citi Field", 40.75753, -73.84559, 13f, RoofKind.OPEN),
        site(3309, "Nationals Park", 38.87286, -77.00750, 28f, RoofKind.OPEN),
        site(3312, "Target Field", 44.98183, -93.27789, 129f, RoofKind.OPEN),
        site(3313, "Yankee Stadium", 40.82919, -73.92650, 75f, RoofKind.OPEN),
        site(4169, "loanDepot park", 25.77796, -80.21952, 128f, RoofKind.RETRACTABLE),
        site(4705, "Truist Park", 33.89067, -84.46764, 145f, RoofKind.OPEN),
        site(5325, "Globe Life Field", 32.74730, -97.08182, 30f, RoofKind.RETRACTABLE),
    ).associateBy { it.venueId }

    fun get(venueId: Int?): ParkSite? = venueId?.let { byId[it] }

    fun roof(venueId: Int?, venueName: String): RoofKind {
        get(venueId)?.let { return it.roof }
        val n = venueName.lowercase()
        return when {
            n.contains("tropicana") -> RoofKind.DOME
            n.contains("rogers") || n.contains("chase") || n.contains("american family") ||
                n.contains("t-mobile") || n.contains("daikin") || n.contains("minute maid") ||
                n.contains("loandepot") || n.contains("globe life") -> RoofKind.RETRACTABLE
            else -> RoofKind.OPEN
        }
    }

    fun isIndoor(roof: RoofKind, mlbCondition: String): Boolean {
        if (roof == RoofKind.DOME) return true
        val c = mlbCondition.lowercase()
        val closed = c.contains("roof closed") || c.contains("dome") || c.contains("indoor") ||
            (c.contains("roof") && !c.contains("open"))
        return (roof == RoofKind.RETRACTABLE && closed) || (roof == RoofKind.OPEN && closed)
    }

    private fun site(
        id: Int,
        name: String,
        lat: Double,
        lon: Double,
        bearing: Float,
        roof: RoofKind,
    ) = ParkSite(id, name, lat, lon, bearing, roof)
}
