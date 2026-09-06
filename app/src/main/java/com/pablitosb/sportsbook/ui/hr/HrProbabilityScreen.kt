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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.ChipFill
import com.pablitosb.sportsbook.theme.MatchupBlue
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.ParkPurple
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.AwayAtHomeLine
import com.pablitosb.sportsbook.ui.components.LiveBadge
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.SlateDateNavBar
import com.pablitosb.sportsbook.ui.components.SlateLoading
import com.pablitosb.sportsbook.ui.components.SlateMessage
import com.pablitosb.sportsbook.ui.components.updatedLabel
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HrProbabilityScreen(
    onBack: () -> Unit,
    viewModel: HrViewModel = viewModel(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val slate = when (val state = viewModel.ui) {
        is HrUiState.Ready -> state.board.slate.slateDate
        is HrUiState.Empty -> state.slateDate
        is HrUiState.Error -> state.slateDate
        is HrUiState.Loading -> state.slateDate
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ScreenTopBar(
            onBack = onBack,
            trailing = {
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = AccentGreen)
                }
            },
        )
        SlateDateNavBar(
            date = slate,
            isToday = slate == viewModel.today,
            canPrev = slate > viewModel.minDate,
            canNext = slate < viewModel.maxDate,
            onPrev = { viewModel.shiftDays(-1) },
            onNext = { viewModel.shiftDays(1) },
            onToday = { viewModel.goToday() },
            onPick = { showPicker = true },
        )
        if (showPicker) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = slate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                yearRange = IntRange(viewModel.minDate.year, viewModel.maxDate.year),
            )
            DatePickerDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            viewModel.goTo(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    }) { Text("Go", color = AccentGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Cancel", color = TextMuted) }
                },
            ) { DatePicker(state = pickerState) }
        }
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is HrUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is HrUiState.Loading -> SlateLoading(state.slateDate)
                is HrUiState.Error -> SlateMessage(
                    title = "HR board unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is HrUiState.Empty -> SlateMessage(
                    title = "No hitters posted",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                )
                is HrUiState.Ready -> ReadyList(state.board.slate.sourceLabel, state.board.slate.fetchedAt, state.board.batters)
            }
        }
    }
}

@Composable
private fun ReadyList(source: String, fetchedAt: java.time.Instant, batters: List<HrBatter>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(source)
                Spacer(Modifier.weight(1f))
                Text(updatedLabel(fetchedAt, StartersRepository.SLATE_ZONE), color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Daily HR Probability", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Ranked by game HR% • talent × park × weather × pitcher × platoon",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FactorChip("HR%/ISO/FB talent", Icons.Outlined.BarChart, AccentGreen)
                FactorChip("Matchup / platoon", Icons.Outlined.Balance, MatchupBlue)
                FactorChip("Park / weather", Icons.Outlined.Park, ParkPurple)
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
        items(batters, key = { "${it.mlbId}-${it.rank}" }) { batter ->
            HrRow(batter)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "p_PA = shrink(0.70·HR/PA + 0.20·ISO proxy + 0.10·FB proxy) × park × weather × pitcher HR/9 × platoon. " +
                        "Pr(HR) = 1 − (1 − p_PA)^PA. No Statcast barrels — ISO/FB from MLB Stats API. " +
                        "Lineups when posted; otherwise active roster hitters. Network failure shows Retry, not mock names.",
                    color = AccentGreen,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AwayAtHomeLine(
                        team = batter.team,
                        opponent = batter.opponent,
                        homeAway = batter.homeAway,
                        awayAbbr = batter.awayAbbr,
                        homeAbbr = batter.homeAbbr,
                        fontSize = 11.sp,
                    )
                    Text("  ${batter.pitcherHand}", color = MatchupBlue, fontSize = 11.sp)
                }
                val extra = buildList {
                    if (batter.battingOrder != null) add("#${batter.battingOrder}")
                    if (batter.sourceNote.isNotBlank()) add(batter.sourceNote)
                }.joinToString(" · ")
                if (extra.isNotBlank()) Text(extra, color = TextMuted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.US, "%.1f%%", batter.gameHrPct),
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Text(
                    String.format(Locale.US, "%.1f%% szn HR/PA", batter.seasonHrPct),
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
            MiniChip("talent ${batter.xHrPct}%", AccentGreen)
            MiniChip(signed(batter.parkAdjPct) + "  ${batter.parkName}", ParkPurple)
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
        Text(if (tempF > 0) "$tempF°" else "—", color = TextMuted, fontSize = 11.sp)
    }
}

private fun signed(value: Int): String = if (value > 0) "+$value%" else "$value%"
