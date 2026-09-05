package com.pablitosb.sportsbook.ui.toppicks

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
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
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.toppicks.TopPick
import com.pablitosb.sportsbook.data.toppicks.TopPicksBoard
import com.pablitosb.sportsbook.data.toppicks.TopPicksSection
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.OpponentRed
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.LiveBadge
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.SlateDateNavBar
import com.pablitosb.sportsbook.ui.components.SlateLoading
import com.pablitosb.sportsbook.ui.components.SlateMessage
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPicksScreen(
    onBack: () -> Unit,
    viewModel: TopPicksViewModel = viewModel(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val slate = when (val state = viewModel.ui) {
        is TopPicksUiState.Ready -> state.board.slateDate
        is TopPicksUiState.Empty -> state.slateDate
        is TopPicksUiState.Error -> state.slateDate
        is TopPicksUiState.Loading -> state.slateDate
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
            isRefreshing = viewModel.refreshing && viewModel.ui is TopPicksUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is TopPicksUiState.Loading -> SlateLoading(state.slateDate)
                is TopPicksUiState.Error -> SlateMessage(
                    title = "Picks unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is TopPicksUiState.Empty -> SlateMessage(
                    title = "No picks yet",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                )
                is TopPicksUiState.Ready -> ReadyPicks(state.board, viewModel)
            }
        }
    }
}

@Composable
private fun ReadyPicks(board: TopPicksBoard, viewModel: TopPicksViewModel) {
    val section = viewModel.section
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(board.sourceLabel)
                Spacer(Modifier.weight(1f))
                Text(
                    "Updated " + board.fetchedAt.atZone(StartersRepository.SLATE_ZONE)
                        .format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US)),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Today’s Top Picks", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Four live formulas: SP Ks · HR · TB · FD value.",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            ChipRow(
                items = listOf(
                    TopPicksSection.ALL to "All",
                    TopPicksSection.SP_K to "SP Ks",
                    TopPicksSection.HR to "HR",
                    TopPicksSection.TB to "Top TB",
                    TopPicksSection.FD_VALUE to "FD value",
                ),
                selected = section,
                onClick = { viewModel.selectSection(it) },
            )
            Spacer(Modifier.height(12.dp))
        }
        if (section == TopPicksSection.ALL || section == TopPicksSection.SP_K) {
            item {
                SectionCard(
                    title = "Top SP K spots",
                    subtitle = "Outlook + Proj Ks · rain last",
                    metricLabel = "PROJ KS",
                    picks = board.kSpots,
                    empty = board.kNote,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (section == TopPicksSection.ALL || section == TopPicksSection.HR) {
            item {
                SectionCard(
                    title = "Top HR spots",
                    subtitle = "Daily game HR probability",
                    metricLabel = "GAME HR",
                    picks = board.hrSpots,
                    empty = board.hrNote,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (section == TopPicksSection.ALL || section == TopPicksSection.TB) {
            item {
                SectionCard(
                    title = "Top TB spots",
                    subtitle = "TB ≈ PA × (1·1B + 2·2B + 3·3B + 4·HR) / PA",
                    metricLabel = "PROJ TB",
                    picks = board.tbSpots,
                    empty = board.tbNote,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        if (section == TopPicksSection.ALL || section == TopPicksSection.FD_VALUE) {
            item {
                SectionCard(
                    title = "Top FD value",
                    subtitle = if (board.fdRankedByValue) "Pts per \$1k (EXAMPLE salaries)" else "Proj FD pts — no salary yet",
                    metricLabel = if (board.fdRankedByValue) "VAL" else "PROJ",
                    picks = board.fdValue,
                    empty = board.fdNote,
                )
                if (board.fdSalaryNote.isNotBlank() && board.fdValue.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(board.fdSalaryNote, color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Option 6 formulas — SP Ks: Proj Ks (rain last) from live probables. " +
                        "HR: Daily HR game%. TB: PA × (1·1B + 2·2B + 3·3B + 4·HR) per PA, " +
                        "shrunk season rates with park / pitcher / weather. " +
                        "FD value: FanDuel pts per \$1k (EXAMPLE salaries unless imported). " +
                        "Timezone: America/Los_Angeles.",
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
private fun SectionCard(
    title: String,
    subtitle: String,
    metricLabel: String,
    picks: List<TopPick>,
    empty: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardStroke, RoundedCornerShape(14.dp))
            .background(CardFill, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(subtitle, color = AccentGreen, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        if (picks.isEmpty()) {
            Text(empty ?: "Nothing posted for this section.", color = TextMuted, fontSize = 13.sp)
        } else {
            picks.forEachIndexed { index, pick ->
                PickRow(index + 1, pick, metricLabel)
                if (index != picks.lastIndex) SectionRule()
            }
        }
    }
}

@Composable
private fun PickRow(rank: Int, pick: TopPick, metricLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            rank.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.width(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(pick.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
            Row {
                if (pick.pos.isNotBlank()) {
                    Text(pick.pos, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("  ", color = TextMuted, fontSize = 11.sp)
                }
                Text(pick.team, color = TextPrimary, fontSize = 11.sp)
                if (pick.opponent.isNotBlank()) {
                    Text(" vs ", color = TextMuted, fontSize = 11.sp)
                    Text(pick.opponent, color = OpponentRed, fontSize = 11.sp)
                }
                if (pick.gameTimeLabel.isNotBlank()) {
                    Text("  ${pick.gameTimeLabel}", color = TextMuted, fontSize = 10.sp)
                }
            }
            if (pick.why.isNotBlank()) {
                Text(pick.why, color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(pick.metric, color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(metricLabel, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun <T> ChipRow(
    items: List<Pair<T, String>>,
    selected: T,
    onClick: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (key, label) ->
            val on = key == selected
            Box(
                modifier = Modifier
                    .background(if (on) AccentGreen.copy(alpha = 0.16f) else NavySurface, RoundedCornerShape(16.dp))
                    .border(1.dp, if (on) AccentGreen.copy(alpha = 0.7f) else CardStroke, RoundedCornerShape(16.dp))
                    .clickable { onClick(key) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    label,
                    color = if (on) AccentGreen else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}
