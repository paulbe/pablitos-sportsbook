package com.pablitosb.sportsbook.ui.hr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.mock.MockRepository
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.ChipFill
import com.pablitosb.sportsbook.theme.MatchupBlue
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.OpponentRed
import com.pablitosb.sportsbook.theme.ParkPurple
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.DateChip
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import java.util.Locale

@Composable
fun HrProbabilityScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenTopBar(onBack = onBack)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                DateChip()
                Spacer(Modifier.height(10.dp))
                Text("Daily HR Probability", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ranked by game HR chance • talent × park × pitcher × weather",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FactorChip("Barrel/xHR talent", Icons.Outlined.BarChart, AccentGreen)
                    FactorChip("Matchup adj", Icons.Outlined.Balance, MatchupBlue)
                    FactorChip("Park/Wx", Icons.Outlined.Park, ParkPurple)
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("BATTER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("GAME HR%", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Text("SEASON", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                SectionRule()
            }
            items(MockRepository.hrBatters, key = { it.rank }) { batter ->
                HrRow(batter)
                SectionRule()
            }
            item {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Pr(HR) ≈ talent HR% × park × weather × pitcher × platoon.",
                        color = AccentGreen,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FactorChip(label: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HrRow(batter: HrBatter) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = AccentGreen,
                    start = Offset(0f, 10.dp.toPx()),
                    end = Offset(0f, size.height - 10.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                )
            }
            .padding(start = 10.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                batter.rank.toString(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.width(20.dp),
            )
            PlayerAvatar(batter.name, batter.team, size = 38.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(batter.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row {
                    Text(batter.team, color = TextPrimary, fontSize = 11.sp)
                    Text(" vs ", color = TextMuted, fontSize = 11.sp)
                    Text("${batter.opponent} ", color = OpponentRed, fontSize = 11.sp)
                    Text(batter.pitcherHand, color = MatchupBlue, fontSize = 11.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.US, "%.1f%%", batter.gameHrPct),
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    String.format(Locale.US, "%.1f%% szn", batter.seasonHrPct),
                    color = TextPrimary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniChip("xHR ${batter.xHrPct}%", AccentGreen)
            MiniChip(
                signed(batter.parkAdjPct) + "  ${batter.parkName}",
                ParkPurple,
            )
            WeatherBit(batter.weather, batter.tempF)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniChip(
                "${signed(batter.pitcherAdjPct)}  ${batter.pitcherName}  ${String.format(Locale.US, "%.2f HR/9", batter.pitcherHr9)}",
                if (batter.pitcherAdjPct >= 0) MatchupBlue else RegRed,
            )
            if (batter.regressionLean) {
                Spacer(Modifier.width(6.dp))
                MiniChip("REG LEAN", RegRed)
            }
        }
    }
}

@Composable
private fun MiniChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(ChipFill, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WeatherBit(weather: Weather, tempF: Int) {
    Row(
        modifier = Modifier
            .background(CardFill, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (weather == Weather.SUN) Icons.Outlined.WbSunny else Icons.Outlined.Cloud,
            null,
            tint = AccentGreen,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text("$tempF°", color = TextMuted, fontSize = 11.sp)
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value%" else "$value%"
