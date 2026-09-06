package com.pablitosb.sportsbook.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SettingsScreen(onBack: () -> Unit) {
    val rows = listOf(
        "Theme" to "Dark navy + green (locked)",
        "Slate timezone" to "America/Los_Angeles",
        "MLB feed" to "statsapi.mlb.com — starters, HR, TB, DFS proj, props model",
        "Savant xwOBA" to "Public expected-stats CSV · missing row shows —",
        "Matchups" to "AWAY @ HOME on every board (never vs)",
        "Park weather" to "Open-Meteo + multi-year HR park factor (same as Daily HR)",
        "Projected Starters" to "Prog · Proj Ks · xwOBA · Proj Outs · Proj FD · Weather boost",
        "Opp K colors" to "Red < 21.6% · grey mid · green > 23.4% (MLB tertiles)",
        "Proj Outs" to "Matchup-adjusted IP × 3 (recent/season shrink + OPS + wx + exits)",
        "Proj FD" to "3×Ks + Outs + 6×P(W) + 4×P(QS) − 3×E[ER] · Floor / Proj / Ceiling",
        "FanDuel salaries" to "EXAMPLE formula / import / Sample CSV — not live FD",
        "Underdog lines" to "Model board / import — not live UD",
        "FD DFS Projections" to "Live MLB → FanDuel pts · EXAMPLE salaries unless imported",
        "Home" to "Starters · HR Prob · Top Picks · Beta",
        "Beta menu" to "DFS Lineups · Underdog Props · FD DFS Projections",
        "Today’s Top Picks" to "SP Ks · HR · TB · FD value",
        "TB formula" to "PA × (1·1B + 2·2B + 3·3B + 4·HR) · park/pitcher/weather",
        "Version" to "1.18.0 (v0.1.18-debug)",
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
        Spacer(Modifier.height(8.dp))
        Text("Settings", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Personal baseball edge toolkit", color = TextMuted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardStroke, RoundedCornerShape(14.dp))
                .background(CardFill, RoundedCornerShape(14.dp))
                .padding(16.dp),
        ) {
            rows.forEachIndexed { index, (label, value) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(0.4f))
                    Text(value, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(0.6f))
                }
                if (index != rows.lastIndex) Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Starters rows show one selected stat plus Weather boost %. Raw wind/temp/rain chips are hidden. Opponent abbreviation is colored by team K% tertiles. Pitcher xwOBA is season-to-date Savant (missing = —).",
            color = AccentGreen,
            fontSize = 12.sp,
        )
    }
}
