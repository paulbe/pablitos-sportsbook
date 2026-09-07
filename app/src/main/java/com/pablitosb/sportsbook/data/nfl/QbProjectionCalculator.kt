package com.pablitosb.sportsbook.data.nfl

/**
 * v1 next-game pass + rush model.
 *
 * Live ESPN: attempts, YPA, rush att/YPC, team pass/rush rate, opponent YPA/YPC
 * allowed, odds. Not live NGS: CAY / IAY / CPOE. CPOE bump is 0. IAY display
 * is a YPA-blend depth proxy — labeled EXAMPLE in the UI.
 *
 * ```
 * E[Att]     = 0.55·Att_L3 + 0.25·Att_season + 0.20·team_pass_att
 *              × script_mult × pace_mult
 * E[YPA]     = 0.40·YPA_L5 + 0.35·YPA_season + 0.25·lgYPA × opp_mult
 * E[Yds]     = clamp(E[Att] × E[YPA])
 * E[RushAtt] = 0.60·RushAtt_L3 + 0.25·RushAtt_season + 0.15·designed_proxy
 * E[YPC]     = 0.50·YPC_L5 + 0.30·YPC_season + 0.20·lg_QB_YPC
 * E[RushYds] = clamp(E[RushAtt] × E[YPC] × rush_script × opp_rush_mult, 0–120)
 * Matchup%   = (opp_pass_mult − 1) × 100
 * Rush match = (0.75·opp_rush_mult + 0.25·opp_pass_mult − 1) × 100
 * ```
 */
object QbProjectionCalculator {
    const val LEAGUE_YPA = 7.05f
    const val LEAGUE_ATT = 33.0f
    const val LEAGUE_PLAYS = 62.0f
    const val LEAGUE_COMP = 64.5f
    const val LEAGUE_QB_RUSH_ATT = 4.2f
    const val LEAGUE_QB_YPC = 5.4f
    const val LEAGUE_RUSH_YPC = 4.35f
    const val CPOE_PRIOR_ATT = 50f
    const val FD_PASS_YD = 0.04f
    const val FD_PASS_TD = 4f
    const val FD_RUSH_YD = 0.1f
    const val FD_RUSH_TD = 6f

    data class Result(
        val expAtt: Float,
        val expYpa: Float,
        val projYds: Float,
        val ydsFloor: Float,
        val ydsCeiling: Float,
        val projRushYds: Float,
        val rushFloor: Float,
        val rushCeiling: Float,
        val fdProj: Float,
        val fdFloor: Float,
        val fdCeiling: Float,
        val compPct: Float,
        val iay: Float,
        val matchupPct: Float,
        val rushMatchupPct: Float,
        val oppMult: Float,
        val rushOppMult: Float,
        val scriptMult: Float,
        val rushScriptMult: Float,
        val paceMult: Float,
        val expPassTd: Float,
        val expRushAtt: Float,
        val expYpc: Float,
        val expRushTd: Float,
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

    fun rushScriptMult(spreadForTeam: Float?, total: Float?, dualThreat: Boolean): Float {
        val spread = spreadForTeam ?: 0f
        val tot = total ?: 44.5f
        var m = 1f
        if (spread > 2.5f) m += if (dualThreat) 0.06f else 0.02f
        if (dualThreat && tot >= 48f) m += 0.04f
        if (spread <= -10f) m -= 0.10f
        else if (spread <= -6.5f) m -= 0.04f
        return m.coerceIn(0.85f, 1.15f)
    }

    fun paceMult(teamPlaysPerGame: Float): Float {
        if (teamPlaysPerGame <= 0f) return 1f
        return (teamPlaysPerGame / LEAGUE_PLAYS).coerceIn(0.92f, 1.08f)
    }

    fun oppMult(oppYpaAllowed: Float): Float {
        if (oppYpaAllowed <= 0f) return 1f
        return (oppYpaAllowed / LEAGUE_YPA).coerceIn(0.88f, 1.14f)
    }

    fun rushOppMult(oppYpcAllowed: Float): Float {
        if (oppYpcAllowed <= 0f) return 1f
        return (oppYpcAllowed / LEAGUE_RUSH_YPC).coerceIn(0.88f, 1.16f)
    }

    fun designedRushProxy(rushAttL3: Float, rushAttSeason: Float, teamRushAttPerGame: Float): Float {
        val dual = rushAttL3 >= 6f || rushAttSeason >= 5.5f
        val share = if (dual) 0.22f else 0.10f
        val teamRush = if (teamRushAttPerGame > 0f) teamRushAttPerGame else 26f
        return (teamRush * share).coerceIn(1.5f, 12f)
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

        val dualThreat = input.rushAttL3 >= 6f || input.rushAttSeasonPerGame >= 5.5f
        val rushL3n = input.rushAttL3Games.toFloat()
        val rushL3s = shrink(
            input.rushAttL3,
            rushL3n,
            LEAGUE_QB_RUSH_ATT,
            if (rushL3n < 3f) 8f else 2f,
        )
        val rushSeason = if (input.rushAttSeasonPerGame > 0f) input.rushAttSeasonPerGame else LEAGUE_QB_RUSH_ATT
        val designed = designedRushProxy(input.rushAttL3, input.rushAttSeasonPerGame, input.teamRushAttPerGame)
        val expRushAtt = (0.60f * rushL3s + 0.25f * rushSeason + 0.15f * designed).coerceIn(1.2f, 16f)

        val ypcL5s = shrink(
            input.ypcL5,
            input.ypcL5Games.toFloat(),
            LEAGUE_QB_YPC,
            if (input.ypcL5Games < 5) 8f else 3f,
        )
        val ypcSeason = if (input.ypcSeason > 0f) input.ypcSeason else LEAGUE_QB_YPC
        val expYpc = (0.50f * ypcL5s + 0.30f * ypcSeason + 0.20f * LEAGUE_QB_YPC).coerceIn(3.2f, 8.5f)
        val rushScript = rushScriptMult(input.spreadForTeam, input.total, dualThreat)
        val rushOpp = rushOppMult(input.oppYpcAllowed)
        val projRush = (expRushAtt * expYpc * rushScript * rushOpp).coerceIn(0f, 120f)
        val rushFloor = (projRush * 0.50f).coerceAtLeast(0f)
        val rushCeil = (projRush * 1.65f).coerceAtMost(130f)

        val tdRate = if (input.seasonAtt > 0) input.seasonPassTd.toFloat() / input.seasonAtt else 0.045f
        val expTd = (tdRate * expAtt).coerceIn(0.4f, 3.6f)
        val rushTdRate = if (input.seasonRushAtt > 0) {
            input.seasonRushTd.toFloat() / input.seasonRushAtt
        } else {
            1f / 40f
        }
        val expRushTd = (rushTdRate * expRushAtt).coerceIn(0.02f, 1.4f)

        val fdProj = projYds * FD_PASS_YD + expTd * FD_PASS_TD +
            projRush * FD_RUSH_YD + expRushTd * FD_RUSH_TD
        val fdFloor = ydsFloor * FD_PASS_YD + expTd * 0.55f * FD_PASS_TD +
            rushFloor * FD_RUSH_YD + expRushTd * 0.40f * FD_RUSH_TD
        val fdCeil = ydsCeil * FD_PASS_YD + expTd * 1.45f * FD_PASS_TD +
            rushCeil * FD_RUSH_YD + expRushTd * 1.60f * FD_RUSH_TD

        val compSample = if (input.ypaL5Games > 0) input.compPctL5 else input.seasonCompPct
        val compN = if (input.ypaL5Games > 0) input.ypaL5Games * 32f else input.seasonAtt.toFloat()
        val compPct = shrink(compSample, compN, LEAGUE_COMP, 40f).coerceIn(52f, 78f)
        val iay = 0.55f * ypaL5s + 0.45f * LEAGUE_YPA
        val rushBlend = 0.75f * rushOpp + 0.25f * opp

        return Result(
            expAtt = expAtt,
            expYpa = expYpa,
            projYds = projYds,
            ydsFloor = ydsFloor,
            ydsCeiling = ydsCeil,
            projRushYds = projRush,
            rushFloor = rushFloor,
            rushCeiling = rushCeil,
            fdProj = fdProj,
            fdFloor = fdFloor.coerceAtMost(fdProj - 0.4f),
            fdCeiling = fdCeil.coerceAtLeast(fdProj + 0.8f),
            compPct = compPct,
            iay = iay,
            matchupPct = (opp - 1f) * 100f,
            rushMatchupPct = (rushBlend - 1f) * 100f,
            oppMult = opp,
            rushOppMult = rushOpp,
            scriptMult = script,
            rushScriptMult = rushScript,
            paceMult = pace,
            expPassTd = expTd,
            expRushAtt = expRushAtt,
            expYpc = expYpc,
            expRushTd = expRushTd,
        )
    }
}
