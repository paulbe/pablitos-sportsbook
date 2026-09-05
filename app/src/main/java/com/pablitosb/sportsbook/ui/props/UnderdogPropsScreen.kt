package com.pablitosb.sportsbook.ui.props

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.mock.MockRepository
import com.pablitosb.sportsbook.data.model.Confidence
import com.pablitosb.sportsbook.data.model.UnderdogProp
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.ConfidenceMeter
import com.pablitosb.sportsbook.ui.components.DateChip
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.StubButton
import java.util.Locale

@Composable
fun UnderdogPropsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var minEdge by remember { mutableStateOf(3f) }
    var selected by remember { mutableStateOf<Int?>(1) }
    val rows = MockRepository.props.filter { it.edgePct >= minEdge }
    val plusEv = MockRepository.plusEvCount
    val avg = MockRepository.avgEdge

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

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
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Underdog Props", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Best chance to win • model vs line", color = TextMuted, fontSize = 13.sp)
                    }
                    DateChip()
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardFill)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.EmojiEvents, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$plusEv +EV plays", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("Avg edge ", color = TextMuted, fontSize = 13.sp)
                    Text(String.format(Locale.US, "%.1f%%", avg), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterPill(Icons.Outlined.SportsBaseball, "Baseball")
                    FilterPill(null, "Higher/Lower")
                    FilterPill(null, "Min edge ${minEdge.toInt()}%", onClick = {
                        minEdge = if (minEdge >= 5f) 0f else minEdge + 1f
                    })
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardFill)
                            .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
                            .clickable { toast("Advanced filters coming later") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Tune, null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("PLAYER / PROP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                    Text("MODEL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.55f))
                    Text("EDGE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f))
                    Text("CONF", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.55f))
                }
                Spacer(Modifier.height(6.dp))
                SectionRule()
            }
            items(rows, key = { it.rank }) { prop ->
                PropRow(
                    prop = prop,
                    selected = selected == prop.rank,
                    onClick = { selected = prop.rank },
                )
                SectionRule()
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StubButton(
                    label = "Refresh lines",
                    onClick = { toast("Lines refreshed from mock book") },
                    modifier = Modifier.weight(1f),
                    leading = { Icon(Icons.Outlined.Refresh, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
                )
                StubButton(
                    label = "Sync Underdog",
                    onClick = { toast("Underdog sync stub — no live login in v1") },
                    modifier = Modifier.weight(1f),
                )
                StubButton(
                    label = "Add to slip",
                    onClick = {
                        val pick = rows.firstOrNull { it.rank == selected } ?: rows.firstOrNull()
                        toast(if (pick == null) "No prop selected" else "Added ${pick.player} ${pick.propLabel} ${pick.line}")
                    },
                    modifier = Modifier.weight(1.1f),
                    filled = true,
                    leading = { Icon(Icons.Outlined.Add, null, tint = Color(0xFF052E16), modifier = Modifier.size(16.dp)) },
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ranked by win probability edge (model vs implied).", color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilterPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardFill)
            .border(1.dp, CardStroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = TextPrimary, fontSize = 12.sp)
    }
}

@Composable
private fun PropRow(prop: UnderdogProp, selected: Boolean, onClick: () -> Unit) {
    val edgePositive = prop.edgePct >= 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NavySurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(AccentGreen),
            contentAlignment = Alignment.Center,
        ) {
            Text(prop.rank.toString(), color = Color(0xFF052E16), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        PlayerAvatar(prop.player, prop.team, size = 34.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1.15f)) {
            Text(prop.player, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prop.propLabel, color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .border(1.dp, CardStroke, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(prop.line, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(6.dp))
                Text(formatOdds(prop.odds), color = TextMuted, fontSize = 11.sp)
            }
        }
        Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
            Text(pct(prop.modelProb), color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(pct(prop.impliedProb), color = TextMuted, fontSize = 11.sp)
        }
        Text(
            (if (edgePositive) "+" else "") + String.format(Locale.US, "%.1f%%", prop.edgePct),
            color = if (edgePositive) AccentGreen else TextMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .width(58.dp)
                .padding(start = 4.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
            ConfidenceMeter(filled = confidenceBars(prop.confidence))
            Text(confidenceLabel(prop.confidence), color = TextMuted, fontSize = 9.sp)
        }
    }
}

private fun pct(value: Float): String = String.format(Locale.US, "%.1f%%", value)

private fun formatOdds(odds: Int): String = if (odds > 0) "+$odds" else odds.toString()

private fun confidenceBars(confidence: Confidence): Int = when (confidence) {
    Confidence.VERY_HIGH -> 6
    Confidence.HIGH -> 5
    Confidence.MEDIUM -> 3
    Confidence.LOW -> 1
}

private fun confidenceLabel(confidence: Confidence): String = when (confidence) {
    Confidence.VERY_HIGH -> "Very High"
    Confidence.HIGH -> "High"
    Confidence.MEDIUM -> "Medium"
    Confidence.LOW -> "Low"
}
