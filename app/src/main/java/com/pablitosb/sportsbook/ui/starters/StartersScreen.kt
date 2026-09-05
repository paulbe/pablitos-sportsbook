package com.pablitosb.sportsbook.ui.starters

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.starters.SlateMode
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.OpponentRed
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.StableSlate
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.OutlookChip
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.Sparkline
import com.pablitosb.sportsbook.ui.components.StubButton
import com.pablitosb.sportsbook.ui.components.slateDateLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartersScreen(
    onBack: () -> Unit,
    viewModel: StartersViewModel = viewModel(),
) {
    var showPicker by remember { mutableStateOf(false) }
    val slate = when (val state = viewModel.ui) {
        is StartersUiState.Ready -> state.board.slateDate
        is StartersUiState.Empty -> state.slateDate
        is StartersUiState.Error -> state.slateDate
        is StartersUiState.Loading -> state.slateDate
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
        DateNavBar(
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
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { millis ->
                                val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                viewModel.goTo(picked)
                            }
                            showPicker = false
                        },
                    ) { Text("Go", color = AccentGreen) }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Cancel", color = TextMuted) }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is StartersUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is StartersUiState.Loading -> LoadingBody(state.slateDate)
                is StartersUiState.Error -> MessageBody(
                    title = "Slate unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is StartersUiState.Empty -> MessageBody(
                    title = if (state.slateDate.isAfter(viewModel.today)) "Probables not posted" else "No starts",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    badge = state.sourceLabel,
                )
                is StartersUiState.Ready -> ReadyList(state)
            }
        }
    }
}

@Composable
private fun DateNavBar(
    date: LocalDate,
    isToday: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, enabled = canPrev) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = "Previous day",
                tint = if (canPrev) AccentGreen else TextMuted,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, AccentGreen.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .clickable(onClick = onPick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                slateDateLabel(date) + if (isToday) "  ·  Today" else "",
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next day",
                tint = if (canNext) AccentGreen else TextMuted,
            )
        }
        if (!isToday) {
            TextButton(onClick = onToday) {
                Text("Today", color = AccentGreen, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReadyList(state: StartersUiState.Ready) {
    val board = state.board
    val results = board.mode == SlateMode.RESULTS
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LiveChip(board.sourceLabel)
                if (board.reconstructed) {
                    Spacer(Modifier.width(8.dp))
                    LiveChip("Reconstructed")
                }
                Spacer(Modifier.weight(1f))
                Text(updatedLabel(board.fetchedAt), color = TextMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (results) "Predicted vs actual" else "Projected Starters",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (results) {
                    "Outlook reconstructed as of slate morning · ranked by pred score"
                } else {
                    "Ranked by progression → regression outlook"
                },
                color = TextMuted,
                fontSize = 13.sp,
            )
            if (board.postponedCount > 0) {
                Text(
                    "${board.postponedCount} game(s) postponed/canceled — omitted.",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            LegendCard()
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("SP / MATCHUP", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.3f))
                Text("OUTLOOK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.55f))
                Text(if (results) "PRED" else "PROJ K%", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.5f))
                Text(if (results) "ACT / Δ" else "NEXT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.65f))
            }
            Spacer(Modifier.height(6.dp))
            SectionRule()
        }
        items(board.starters, key = { "${it.mlbId}-${it.rank}-${it.homeAway}" }) { starter ->
            StarterRow(starter, results)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (results) {
                        "Predictions are reconstructed with OutlookCalculator using only game logs before this date (we don’t persist live cards). " +
                            "Actual Ks / K% come from the boxscore starter (SO/BF). Δ = actual − predicted."
                    } else {
                        "Outlook = quality (proj K% vs 22.5% lg) + last-5-GS vs season K% trajectory. " +
                            "Proj K% blends recent K%, season K%, and strike%. Next start Ks ≈ proj K% × expected BF. " +
                            "Use ◀ ▶ or the date chip to jump days."
                    },
                    color = AccentGreen,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveChip(label: String) {
    Box(
        modifier = Modifier
            .background(AccentGreen.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingBody(date: LocalDate) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text("Loading MLB slate for ${slateDateLabel(date)}…", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MessageBody(
    title: String,
    body: String,
    onRetry: () -> Unit,
    fetchedAt: Instant? = null,
    badge: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) LiveChip(badge)
        Spacer(Modifier.height(16.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextMuted, fontSize = 14.sp)
        if (fetchedAt != null) {
            Spacer(Modifier.height(6.dp))
            Text(updatedLabel(fetchedAt), color = TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        StubButton(label = "Retry", onClick = onRetry, filled = true)
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
            Text("score ≥ +5 · improving K% / stuff vs season", color = TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlookChip(Outlook.REG)
            Spacer(Modifier.width(8.dp))
            Text("score ≤ −5 · regressing toward mean / declining K%", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StarterRow(starter: Starter, results: Boolean) {
    val scoreColor = when {
        starter.outlookScore > 3 -> AccentGreen
        starter.outlookScore < -3 -> RegRed
        else -> StableSlate
    }
    val trendColor = when (starter.outlook) {
        Outlook.REG -> RegRed
        else -> AccentGreen
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (starter.homeAway.isNotBlank()) {
                        Text(" · ${starter.homeAway}", color = TextMuted, fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Stadium, null, tint = TextMuted, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(starter.venue, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (starter.gameTimeLabel.isNotBlank()) {
                        Icon(Icons.Outlined.Schedule, null, tint = TextMuted, modifier = Modifier.size(11.dp))
                        Text(" ${starter.gameTimeLabel}", color = TextMuted, fontSize = 10.sp)
                        Spacer(Modifier.width(6.dp))
                    }
                    if (starter.tempF > 0) {
                        Icon(
                            if (starter.weather == Weather.SUN) Icons.Outlined.WbSunny else Icons.Outlined.Cloud,
                            null,
                            tint = AccentGreen,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(" ${starter.tempF}°", color = TextMuted, fontSize = 10.sp)
                    }
                }
                if (starter.resultNote.isNotBlank()) {
                    Text(starter.resultNote, color = TextMuted, fontSize = 10.sp)
                }
            }
            Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.Start) {
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
                Text(
                    text = String.format(Locale.US, "%.1f Ks", starter.nextStartKs),
                    color = AccentGreen,
                    fontSize = 11.sp,
                )
            }
            Column(Modifier.weight(0.65f), horizontalAlignment = Alignment.End) {
                if (results) {
                    if (starter.actualKs != null && starter.actualKPct != null) {
                        Text(
                            String.format(Locale.US, "%.0f K · %.1f%%", starter.actualKs.toFloat(), starter.actualKPct),
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        val dKs = starter.ksDelta
                        val dPct = starter.kPctDelta
                        if (dKs != null && dPct != null) {
                            Text(
                                String.format(Locale.US, "Δ %+.1f K  %+.1f%%", dKs, dPct),
                                color = if (dKs >= 0) AccentGreen else RegRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        if (starter.actualBf != null) {
                            Text("${starter.actualBf} BF", color = TextMuted, fontSize = 10.sp)
                        }
                    } else {
                        Text("No box yet", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        text = String.format(Locale.US, "%.1f Ks", starter.nextStartKs),
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    if (starter.trend.size >= 2) {
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
        }
    }
}

private fun updatedLabel(instant: Instant): String {
    val local = instant.atZone(StartersRepository.SLATE_ZONE)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}
