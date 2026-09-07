package com.pablitosb.sportsbook.data.nfl

/**
 * v1 next-game pass-yards model.
 *
 * Live ESPN: attempts, YPA, team pass rate / plays, opponent YPA allowed, odds.
 * Not live NGS: CAY / IAY / CPOE. CPOE bump is 0. IAY display is a YPA-blend
 * depth proxy — labeled EXAMPLE in the UI.
 *
 * ```
 * E[Att]  = 0.55·Att_L3 + 0.25·Att_season + 0.20·team_pass_att
 *           × script_mult × pace_mult
 * E[YPA]  = 0.40·YPA_L5 + 0.35·YPA_season + 0.25·lgYPA
 *           × opp_mult × weather_mult
 * E[Yds]  = clamp(E[Att] × E[YPA])
 * Matchup% = (opp_mult − 1) × 100
 * ```
 */
object QbProjectionCalculator {
    const val LEAGUE_YPA = 7.05f
    const val LEAGUE_ATT = 33.0f
    const val LEAGUE_PLAYS = 62.0f
    const val LEAGUE_COMP = 64.5f
    const val CPOE_PRIOR_ATT = 50f
    const val FD_PASS_YD = 0.04f
    const val FD_PASS_TD = 4f

    data class Result(
        val expAtt: Float,
        val expYpa: Float,
        val projYds: Float,
        val ydsFloor: Float,
        val ydsCeiling: Float,
        val fdProj: Float,
        val fdFloor: Float,
        val fdCeiling: Float,
        val compPct: Float,
        val iay: Float,
        val matchupPct: Float,
        val oppMult: Float,
        val scriptMult: Float,
        val paceMult: Float,
        val expPassTd: Float,
    )

    fun shrink(obs: Float, n: Float, prior: Float, priorN: Float): Float {
        val t = n + priorN
        return if (t <= 0f) prior else (obs * n + prior * priorN) / t
    }

    fun scriptMult(spreadForTeam: Float?, total: Float?): Float {
        val fromSpread = 1f + (spreadForTeam ?: 0f) * 0.008f
        val fromTotal = 1f + ((total ?: 44.5f) - 44.5f) * 0.006f
        return (fromSpread * fromTotal).coerceIn(0.90f, 1.12f)
    }

    fun paceMult(teamPlaysPerGame: Float): Float {
        if (teamPlaysPerGame <= 0f) return 1f
        return (teamPlaysPerGame / LEAGUE_PLAYS).coerceIn(0.92f, 1.08f)
    }

    fun oppMult(oppYpaAllowed: Float): Float {
        if (oppYpaAllowed <= 0f) return 1f
        return (oppYpaAllowed / LEAGUE_YPA).coerceIn(0.88f, 1.14f)
    }

    fun project(input: QbInputs): Result {
        val attL3n = input.attL3Games.toFloat()
        val attL3s = shrink(
            input.attL3,
            attL3n,
            LEAGUE_ATT,
            if (attL3n < 3f) 6f else 2f,
        )
        val attSeason = if (input.attSeasonPerGame > 0f) input.attSeasonPerGame else LEAGUE_ATT
        val teamPass = if (input.teamPassAttPerGame > 0f) input.teamPassAttPerGame else LEAGUE_ATT
        val script = scriptMult(input.spreadForTeam, input.total)
        val pace = paceMult(input.teamPlaysPerGame)
        val expAtt = (0.55f * attL3s + 0.25f * attSeason + 0.20f * teamPass)
            .times(script * pace)
            .coerceIn(22f, 48f)

        val ypaL5s = shrink(
            input.ypaL5,
            input.ypaL5Games.toFloat(),
            LEAGUE_YPA,
            if (input.ypaL5Games < 5) 8f else 3f,
        )
        val ypaSeason = if (input.ypaSeason > 0f) input.ypaSeason else LEAGUE_YPA
        val ypaBlend = 0.40f * ypaL5s + 0.35f * ypaSeason + 0.25f * LEAGUE_YPA
        val cpoeBump = shrink(0f, input.seasonAtt.toFloat(), 0f, CPOE_PRIOR_ATT)
        val opp = oppMult(input.oppYpaAllowed)
        val weather = 1f
        val expYpa = ((ypaBlend + cpoeBump) * opp * weather).coerceIn(5.4f, 9.2f)

        val projYds = (expAtt * expYpa).coerceIn(145f, 400f)
        val ydsFloor = (projYds * 0.82f).coerceAtLeast(120f)
        val ydsCeil = (projYds * 1.22f).coerceAtMost(430f)

        val tdRate = if (input.seasonAtt > 0) input.seasonPassTd.toFloat() / input.seasonAtt else 0.045f
        val expTd = (tdRate * expAtt).coerceIn(0.4f, 3.6f)
        val fdProj = projYds * FD_PASS_YD + expTd * FD_PASS_TD
        val fdFloor = ydsFloor * FD_PASS_YD + expTd * 0.55f * FD_PASS_TD
        val fdCeil = ydsCeil * FD_PASS_YD + expTd * 1.45f * FD_PASS_TD

        val compSample = if (input.ypaL5Games > 0) input.compPctL5 else input.seasonCompPct
        val compN = if (input.ypaL5Games > 0) input.ypaL5Games * 32f else input.seasonAtt.toFloat()
        val compPct = shrink(compSample, compN, LEAGUE_COMP, 40f).coerceIn(52f, 78f)
        val iay = 0.55f * ypaL5s + 0.45f * LEAGUE_YPA

        return Result(
            expAtt = expAtt,
            expYpa = expYpa,
            projYds = projYds,
            ydsFloor = ydsFloor,
            ydsCeiling = ydsCeil,
            fdProj = fdProj,
            fdFloor = fdFloor.coerceAtMost(fdProj - 0.4f),
            fdCeiling = fdCeil.coerceAtLeast(fdProj + 0.8f),
            compPct = compPct,
            iay = iay,
            matchupPct = (opp - 1f) * 100f,
            oppMult = opp,
            scriptMult = script,
            paceMult = pace,
            expPassTd = expTd,
        )
    }
}
