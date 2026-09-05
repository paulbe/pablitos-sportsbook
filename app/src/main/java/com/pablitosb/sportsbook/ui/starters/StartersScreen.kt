package com.pablitosb.sportsbook.ui.starters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.mock.MockRepository
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.OpponentRed
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.StableSlate
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.DateChip
import com.pablitosb.sportsbook.ui.components.OutlookChip
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.Sparkline
import java.util.Locale

@Composable
fun StartersScreen(onBack: () -> Unit) {
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
                Text("Projected Starters", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Ranked by progression → regression outlook",
                    color = TextMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(12.dp))
                LegendCard()
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("SP / MATCHUP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.4f))
                    Text("OUTLOOK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.7f))
                    Text("PROJ K%", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
                    Text("NEXT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.5f))
                }
                Spacer(Modifier.height(6.dp))
                SectionRule()
            }
            items(MockRepository.starters, key = { it.rank }) { starter ->
                StarterRow(starter)
                SectionRule()
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun LegendCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
            .background(NavySurface, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text("OUTLOOK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlookChip(Outlook.PROG)
            Spacer(Modifier.width(8.dp))
            Text("improving stuff / trajectory", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlookChip(Outlook.REG)
            Spacer(Modifier.width(8.dp))
            Text("regressing toward mean / declining signals", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StarterRow(starter: Starter) {
    val scoreColor = when {
        starter.outlookScore > 3 -> AccentGreen
        starter.outlookScore < -3 -> RegRed
        else -> StableSlate
    }
    val trendColor = when (starter.outlook) {
        Outlook.REG -> RegRed
        else -> AccentGreen
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = starter.rank.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(22.dp),
        )
        PlayerAvatar(name = starter.name, team = starter.team, size = 38.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1.15f)) {
            Text(starter.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Row {
                Text(starter.team, color = TextPrimary, fontSize = 11.sp)
                Text(" vs ", color = TextMuted, fontSize = 11.sp)
                Text(starter.opponent, color = OpponentRed, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Stadium, null, tint = TextMuted, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(starter.venue, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (starter.weather == Weather.SUN) Icons.Outlined.WbSunny else Icons.Outlined.Cloud,
                    null,
                    tint = AccentGreen,
                    modifier = Modifier.size(11.dp),
                )
                Text(" ${starter.tempF}°", color = TextMuted, fontSize = 10.sp)
            }
        }
        Column(Modifier.weight(0.55f), horizontalAlignment = Alignment.Start) {
            OutlookChip(starter.outlook)
            Text(
                text = (if (starter.outlookScore > 0) "+" else "") + starter.outlookScore,
                color = scoreColor,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.Start) {
            Text(
                text = String.format(Locale.US, "%.1f%%", starter.projKPct),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
        Column(Modifier.weight(0.7f), horizontalAlignment = Alignment.End) {
            Text(
                text = String.format(Locale.US, "%.1f Ks", starter.nextStartKs),
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Sparkline(
                values = starter.trend,
                color = trendColor,
                modifier = Modifier
                    .width(52.dp)
                    .height(18.dp),
            )
        }
    }
}
