package com.pablitosb.sportsbook.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.ScreenTopBar

@Composable
fun ModelsScreen(onBack: () -> Unit) {
    val cards = listOf(
        Triple(
            "SP Progression",
            "Prog · Proj Ks · xwOBA · Proj Outs · Proj FD — Weather boost, opp K% colors",
            "Proj FD = 3×Ks + Outs + 6×P(W) + 4×P(QS) − 3×E[ER]",
        ),
        Triple(
            "Daily Batters",
            "Game HR% · Proj FD · Proj TB · H+R+RBI — Games filter, Weather boost, inverted opp K%",
            "HR% = talent × park × weather × pitcher × platoon · FD = FanDuel hitter table",
        ),
        Triple("DFS Optimizer (beta)", "FanDuel salary, stacks, and ownership levers", "Home → Beta → DFS Lineups"),
        Triple("FD DFS Projections (beta)", "Same live FD-point pipeline, ranked board", "Home → Beta → FD DFS Projections"),
        Triple("Prop Edge (beta)", "Model win% minus Underdog implied%", "Home → Beta → Underdog Props"),
        Triple(
            "Today’s Top Picks",
            "Pitchers | Batters — top 10 by the same filters as Starters / Daily Batters",
            "Single active metric · Weather boost · Option 1 / Option 2 coloring",
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        ScreenTopBar(onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Text("Models", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Live MLB Stats API for talent and matchups. FanDuel / Underdog books are import-only.",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(16.dp))
            cards.forEach { (title, formula, use) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, CardStroke, RoundedCornerShape(14.dp))
                        .background(CardFill, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formula, color = AccentGreen, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(use, color = TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
