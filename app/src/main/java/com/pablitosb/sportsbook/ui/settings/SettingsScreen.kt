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
        "Theme" to "Dark navy + green (locked for v1)",
        "Slate" to "MLB main slate • sample board",
        "Data source" to "Mock / sample — no live APIs yet",
        "Sportsbooks" to "FanDuel DFS • Underdog Fantasy props",
        "Version" to "1.0.0 scaffold",
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
            "Live Statcast, FanDuel, and Underdog sync are intentionally out of scope for this build.",
            color = AccentGreen,
            fontSize = 12.sp,
        )
    }
}
