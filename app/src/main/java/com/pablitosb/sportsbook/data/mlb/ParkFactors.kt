package com.pablitosb.sportsbook.data.mlb

import java.util.Locale

/**
 * Multi-year **HR** park factors (1.00 = MLB average).
 *
 * Hand-compiled from published Baseball-Reference / FanGraphs-style
 * 3-year HR PF (not a live Statcast park feed). Same table the Daily HR
 * board uses. Coors 1.28, GABP 1.18, Yankee Stadium 1.15; Oracle 0.88,
 * Petco / Kauffman 0.90, loanDepot 0.92.
 */
object ParkFactors {
    const val HR_PARK = 1.12f
    const val PITCHER_PARK = 0.94f
    const val BANDBOX = 1.20f

    private val byVenueId: Map<Int, Float> = mapOf(
        1 to 0.96f, // Angel Stadium
        2 to 1.12f, // Camden Yards
        3 to 1.08f, // Fenway
        4 to 1.10f, // Rate Field
        5 to 0.98f, // Progressive
        7 to 0.90f, // Kauffman
        12 to 0.94f, // Tropicana
        14 to 1.05f, // Rogers Centre
        15 to 1.02f, // Chase
        17 to 1.02f, // Wrigley
        19 to 1.28f, // Coors
        22 to 0.98f, // Dodger Stadium
        31 to 0.92f, // PNC
        32 to 1.04f, // American Family
        680 to 0.95f, // T-Mobile
        2392 to 1.04f, // Daikin / Minute Maid
        2394 to 0.96f, // Comerica
        2395 to 0.88f, // Oracle
        2529 to 1.06f, // Sutter Health (A's)
        2602 to 1.18f, // Great American
        2680 to 0.90f, // Petco
        2681 to 1.12f, // Citizens Bank
        2889 to 0.95f, // Busch
        3289 to 1.02f, // Citi
        3309 to 1.00f, // Nationals
        3312 to 0.98f, // Target
        3313 to 1.15f, // Yankee Stadium
        4169 to 0.92f, // loanDepot
        4705 to 1.08f, // Truist
        5325 to 1.08f, // Globe Life
    )

    fun hrMultiplier(venueId: Int?, venueName: String): Float {
        venueId?.let { id -> byVenueId[id]?.let { return it } }
        val n = venueName.lowercase()
        return when {
            n.contains("coors") -> 1.28f
            n.contains("great american") -> 1.18f
            n.contains("yankee") -> 1.15f
            n.contains("citizens") -> 1.12f
            n.contains("camden") -> 1.12f
            n.contains("fenway") -> 1.08f
            n.contains("oracle") -> 0.88f
            n.contains("petco") -> 0.90f
            n.contains("kauffman") -> 0.90f
            n.contains("loandepot") -> 0.92f
            else -> 1.00f
        }
    }

    fun isHrPark(pf: Float) = pf >= HR_PARK

    fun isPitcherPark(pf: Float) = pf <= PITCHER_PARK

    /** Subtitle on the starters weather card, e.g. `HR park · PF 1.28`. */
    fun hint(pf: Float): String {
        val idx = String.format(Locale.US, "PF %.2f", pf)
        return when {
            isHrPark(pf) -> "HR park · $idx"
            isPitcherPark(pf) -> "Pitcher park · $idx"
            else -> idx
        }
    }
}
