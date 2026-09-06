package com.pablitosb.sportsbook.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.navigation.Dest
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.ScreenTopBar

@Composable
fun BetaScreen(
    onBack: () -> Unit,
    onOpen: (Dest) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        ScreenTopBar(onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(8.dp))
            Text("Beta", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Work-in-progress boards. Same screens as before — parked here until they graduate.",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(18.dp))
            HomeTile(
                icon = Icons.Outlined.Shield,
                title = "DFS Lineups",
                subtitle = "Optimizer · EXAMPLE or imported slate",
                badge = "BETA",
                onClick = { onOpen(Dest.Dfs) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.GpsFixed,
                title = "Underdog Props",
                subtitle = "Model board · import lines for edge",
                badge = "BETA",
                onClick = { onOpen(Dest.Props) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.Leaderboard,
                title = "FD DFS Projections",
                subtitle = "Live FanDuel pts board · choose slate",
                badge = "BETA",
                onClick = { onOpen(Dest.FdProj) },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Salaries and Underdog lines are EXAMPLE or import-only — not live book feeds.",
                color = AccentGreen,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
