package com.pablitosb.sportsbook.ui.tb

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.tb.TbBatter
import com.pablitosb.sportsbook.data.tb.TbSort
import com.pablitosb.sportsbook.data.tb.TbSorter
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.HrWeatherOrange
import com.pablitosb.sportsbook.theme.MatchupBlue
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.OpponentRed
import com.pablitosb.sportsbook.theme.ParkPurple
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.LiveBadge
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
fun TbScreen(
    onBack: () -> Unit,
    viewModel: TbViewModel = viewModel(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val slate = when (val state = viewModel.ui) {
        is TbUiState.Ready -> state.board.slateDate
        is TbUiState.Empty -> state.slateDate
        is TbUiState.Error -> state.slateDate
        is TbUiState.Loading -> state.slateDate
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
            isRefreshing = viewModel.refreshing && viewModel.ui is TbUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is TbUiState.Loading -> SlateLoading(state.slateDate)
                is TbUiState.Error -> SlateMessage(
                    title = "Total bases unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is TbUiState.Empty -> SlateMessage(
                    title = "No hitters posted",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                )
                is TbUiState.Ready -> ReadyTb(state, viewModel)
            }
        }
    }
}

@Composable
private fun ReadyTb(state: TbUiState.Ready, viewModel: TbViewModel) {
    val board = state.board
    val visible = remember(board.batters, viewModel.sortKey, viewModel.sortAscending) {
        TbSorter.sort(board.batters, viewModel.sortKey, viewModel.sortAscending)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(board.sourceLabel)
                Spacer(Modifier.weight(1f))
                Text(updatedLabel(board.fetchedAt, StartersRepository.SLATE_ZONE), color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Total Bases", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                "Expected TB from season 1B/2B/3B/HR rates × PA, with park / pitcher / weather",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    TbSort.PROJ_TB to "Proj TB",
                    TbSort.TB_PA to "TB/PA",
                    TbSort.SLG to "SLG",
                ).forEach { (key, label) ->
                    val on = key == viewModel.sortKey
                    val arrow = if (on) if (viewModel.sortAscending) " ↑" else " ↓" else ""
                    Box(
                        modifier = Modifier
                            .background(if (on) AccentGreen.copy(alpha = 0.16f) else NavySurface, RoundedCornerShape(16.dp))
                            .border(1.dp, if (on) AccentGreen.copy(alpha = 0.7f) else com.pablitosb.sportsbook.theme.CardStroke, RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectSort(key) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            label + arrow,
                            color = if (on) AccentGreen else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${visible.size} hitters", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
        }
        itemsIndexed(visible, key = { _, row -> "${row.mlbId}-${row.rank}" }) { index, batter ->
            TbRow(index + 1, batter)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "TB ≈ PA × (1·1B + 2·2B + 3·3B + 4·HR) per PA. Season rates from MLB Stats API, " +
                        "shrunk toward league priors (80 PA). HR uses the Daily HR p_PA " +
                        "(park × weather × pitcher HR/9 × platoon). Doubles/triples get a muted tilt; " +
                        "singles stay nearly park-neutral. SLG proxy = (TB/PA) / (AB/PA). Not Statcast xTB.",
                    color = AccentGreen,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TbRow(rank: Int, row: TbBatter) {
    Column(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rank.toString(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(24.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(row.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Row {
                    if (row.pos.isNotBlank()) {
                        Text(row.pos, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("  ", fontSize = 11.sp)
                    }
                    Text(row.team, color = TextPrimary, fontSize = 11.sp)
                    Text(" vs ", color = TextMuted, fontSize = 11.sp)
                    Text(row.opponent, color = OpponentRed, fontSize = 11.sp)
                    Text("  ${row.pitcherHand}", color = MatchupBlue, fontSize = 11.sp)
                }
                val extra = buildList {
                    if (row.battingOrder != null) add("#${row.battingOrder}")
                    add(if (row.inPostedLineup) "Lineup" else "Roster")
                    if (row.pitcherName.isNotBlank()) add("vs ${row.pitcherName}")
                }.joinToString(" · ")
                Text(extra, color = TextMuted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.US, "%.2f", row.projTb),
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                Text("PROJ TB", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meta("TB/PA", String.format(Locale.US, "%.3f", row.tbPerPa))
            Meta("SLG", String.format(Locale.US, ".%03d", kotlin.math.round(row.slgProxy * 1000f).toInt().coerceIn(0, 999)))
            Meta("PA", String.format(Locale.US, "%.1f", row.expectedPa))
            Meta("HR%", String.format(Locale.US, "%.1f", row.gameHrPct))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (row.parkAdjPct != 0) {
                Chip(
                    String.format(Locale.US, "Park %+d%%", row.parkAdjPct),
                    ParkPurple,
                    Icons.Outlined.Park,
                )
            }
            Chip(
                if (row.weather == Weather.SUN) "${row.tempF}° sun" else "${row.tempF}°",
                if (row.weather == Weather.SUN) HrWeatherOrange else TextMuted,
                if (row.weather == Weather.SUN) Icons.Outlined.WbSunny else Icons.Outlined.Cloud,
            )
            if (row.pitcherAdjPct != 0) {
                Chip(
                    String.format(Locale.US, "Arm %+d%%", row.pitcherAdjPct),
                    MatchupBlue,
                    null,
                )
            }
        }
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Column {
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Chip(label: String, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
