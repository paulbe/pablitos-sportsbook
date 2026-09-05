package com.pablitosb.sportsbook.ui.dfs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.mock.MockRepository
import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.model.LineupKind
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.StubButton
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DfsLineupsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lineups = MockRepository.lineups
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { lineups.size })
    val scope = rememberCoroutineScope()
    var contest by remember { mutableStateOf(ContestType.GPP) }
    var stackDots by remember { mutableIntStateOf(4) }
    var ownDots by remember { mutableIntStateOf(3) }

    fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenTopBar(onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("DFS Lineups", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("FanDuel MLB • Main slate", color = AccentGreen, fontSize = 13.sp)
                }
                ContestToggle(contest) { contest = it }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LeverCard(
                    title = "STACK LEVER",
                    value = if (stackDots >= 4) "Yankees 4" else "Stack $stackDots",
                    icon = Icons.Outlined.Layers,
                    filled = stackDots,
                    onClick = { stackDots = if (stackDots == 5) 1 else stackDots + 1 },
                    modifier = Modifier.weight(1f),
                )
                LeverCard(
                    title = "OWN LEVER",
                    value = ownLabel(ownDots),
                    icon = Icons.Outlined.Person,
                    filled = ownDots,
                    onClick = { ownDots = if (ownDots == 5) 1 else ownDots + 1 },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            ) { page ->
                LineupCard(
                    lineup = lineups[page],
                    selected = pagerState.currentPage == page,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                lineups.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (index == pagerState.currentPage) AccentGreen else CardStroke),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous", tint = TextPrimary)
                }
                Text(
                    "Lineup ${pagerState.currentPage + 1} of ${lineups.size}",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                )
                IconButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(lineups.lastIndex))
                    }
                }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next", tint = TextPrimary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                lineups.forEachIndexed { index, lineup ->
                    LineupShortcut(
                        lineup = lineup,
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StubButton(
                label = "Regenerate 5",
                onClick = { toast("Regenerated 5 ${contest.name.lowercase()} lineups (mock)") },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.Refresh, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
            StubButton(
                label = "Export all",
                onClick = { toast("Export stub — FanDuel CSV coming later") },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.FileUpload, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
            StubButton(
                label = "Copy this",
                onClick = { toast("Copied ${lineups[pagerState.currentPage].title}") },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.ContentCopy, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun ContestToggle(selected: ContestType, onSelect: (ContestType) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(NavySurface)
            .border(1.dp, CardStroke, RoundedCornerShape(20.dp))
            .padding(3.dp),
    ) {
        ContestType.entries.forEach { type ->
            val on = type == selected
            Text(
                text = type.name.lowercase().replaceFirstChar { it.titlecase(Locale.US) },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) AccentGreen else Color.Transparent)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (on) Color(0xFF052E16) else TextMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun LeverCard(
    title: String,
    value: String,
    icon: ImageVector,
    filled: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardFill)
            .border(1.dp, CardStroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(title, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i < filled) AccentGreen else CardStroke),
                )
            }
        }
    }
}

@Composable
private fun LineupCard(lineup: DfsLineup, selected: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(CardFill)
            .border(1.5.dp, if (selected) AccentGreen else CardStroke, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(lineup.title, color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Salary $${"%,d".format(Locale.US, lineup.salary)} / $${"%,d".format(Locale.US, lineup.salaryCap)}",
            color = TextPrimary,
            fontSize = 12.sp,
        )
        Text(
            "Proj ${String.format(Locale.US, "%.1f", lineup.proj)}  •  Ceiling ${lineup.ceiling}  •  Avg Own ${lineup.avgOwnPct}%",
            color = AccentGreen,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("POS", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
            Text("PLAYER", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("SALARY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
            Text("PROJ", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        }
        Spacer(Modifier.height(4.dp))
        lineup.players.forEach { player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(player.pos, color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
                Text(player.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(
                    "$" + "%,d".format(Locale.US, player.salary),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.width(64.dp),
                    textAlign = TextAlign.End,
                )
                Text(
                    String.format(Locale.US, "%.1f", player.proj),
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun LineupShortcut(lineup: DfsLineup, selected: Boolean, onClick: () -> Unit) {
    val (icon, code) = when (lineup.kind) {
        LineupKind.CASH_CORE -> Icons.Outlined.Shield to "L1"
        LineupKind.NYY_STACK -> null to "NYY"
        LineupKind.LAD_STACK -> null to "LAD"
        LineupKind.LEVERAGE -> Icons.Outlined.Bolt to "L4"
        LineupKind.CONTRARIAN -> Icons.Outlined.GpsFixed to "L5"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NavySurface)
                .border(1.5.dp, if (selected) AccentGreen else CardStroke, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, null, tint = AccentGreen, modifier = Modifier.size(20.dp))
            } else {
                Text(code, color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when (lineup.kind) {
                LineupKind.CASH_CORE -> "Cash"
                LineupKind.NYY_STACK -> "NYY"
                LineupKind.LAD_STACK -> "LAD"
                LineupKind.LEVERAGE -> "Lev"
                LineupKind.CONTRARIAN -> "Fade"
            },
            color = if (selected) AccentGreen else TextMuted,
            fontSize = 10.sp,
        )
    }
}

private fun ownLabel(dots: Int): String = when (dots) {
    1 -> "Fade"
    2 -> "Neutral"
    3 -> "Leverage"
    4 -> "Smash"
    else -> "Max lev"
}
