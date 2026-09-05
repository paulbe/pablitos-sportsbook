package com.pablitosb.sportsbook.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.navigation.Dest
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.BrandMark
import com.pablitosb.sportsbook.ui.components.DateChip

@Composable
fun HomeScreen(onOpen: (Dest) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark(modifier = Modifier.weight(1f))
                DateChip(date = java.time.LocalDate.now(java.time.ZoneId.of("America/Los_Angeles")))
            }
            Spacer(Modifier.height(28.dp))
            BaseballRule()
            Spacer(Modifier.height(10.dp))
            Text(
                text = "PERSONAL BASEBALL EDGE TOOLKIT",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TextPrimary,
                fontSize = 11.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(22.dp))
            HomeTile(
                icon = Icons.Outlined.Sports,
                title = "Projected Starters",
                subtitle = "Live MLB probables · PROG → REG",
                onClick = { onOpen(Dest.Starters) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.SportsBaseball,
                title = "Daily HR Probability",
                subtitle = "Live lineups · game HR%",
                onClick = { onOpen(Dest.HrProb) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.Shield,
                title = "DFS Lineups",
                subtitle = "Optimizer · EXAMPLE or imported slate",
                onClick = { onOpen(Dest.Dfs) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.Leaderboard,
                title = "FD DFS Projections",
                subtitle = "Live FanDuel pts board · choose slate",
                onClick = { onOpen(Dest.FdProj) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.GpsFixed,
                title = "Underdog Props",
                subtitle = "Model board · import lines for edge",
                onClick = { onOpen(Dest.Props) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.EmojiEvents,
                title = "Today’s Top Picks",
                subtitle = "Best Ks, HRs, FD value, and TB · today",
                onClick = { onOpen(Dest.TopPicks) },
            )
            Spacer(Modifier.height(12.dp))
            HomeTile(
                icon = Icons.Outlined.Timeline,
                title = "Total Bases",
                subtitle = "Live slate · expected TB",
                onClick = { onOpen(Dest.TotalBases) },
            )
        }
        HomeFooter(
            onModels = { onOpen(Dest.Models) },
            onSettings = { onOpen(Dest.Settings) },
        )
    }
}

@Composable
private fun BaseballRule() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(AccentGreen.copy(alpha = 0.55f)),
        )
        Icon(
            imageVector = Icons.Outlined.SportsBaseball,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .size(16.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(AccentGreen.copy(alpha = 0.55f)),
        )
    }
}

@Composable
private fun HomeTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF152033), CardFill),
                ),
            )
            .border(1.dp, CardStroke, shape)
            .drawBehind {
                drawLine(
                    color = AccentGreen,
                    start = Offset(0f, 18.dp.toPx()),
                    end = Offset(0f, size.height - 18.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NavySurface)
                .border(1.dp, AccentGreen.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = AccentGreen, fontSize = 13.sp)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = AccentGreen,
        )
    }
}

@Composable
private fun HomeFooter(
    onModels: () -> Unit,
    onSettings: () -> Unit,
) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AccentGreen.copy(alpha = 0.35f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FooterItem(
                icon = Icons.Outlined.BarChart,
                label = "Models",
                onClick = onModels,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(AccentGreen.copy(alpha = 0.4f)),
            )
            FooterItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = onSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FooterItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = AccentGreen, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
