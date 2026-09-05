package com.pablitosb.sportsbook.ui.starters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.starters.SlateMode
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.HrWeatherOrange
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
            Spacer(Modifier.height(10.dp))
            OutlookDots()
            Spacer(Modifier.height(12.dp))
        }
        items(board.starters, key = { "${it.mlbId}-${it.rank}-${it.homeAway}" }) { starter ->
            StarterRow(starter, results)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(14.dp))
            WeatherLegend()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "xwOBA · Statcast: Expected wOBA against via Statcast batted ball data " +
                        "(lower is better for pitchers). Missing Savant row shows —.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (results) {
                    "Predictions are reconstructed with OutlookCalculator using only game logs before this date. " +
                        "Actual Ks / K% come from the boxscore starter (SO/BF). Δ = actual − predicted."
                } else {
                    "Outlook = quality (proj K% vs 22.5% lg) + last-5-GS vs season K% trajectory. " +
                        "Proj K% blends recent K%, season K%, and strike%. Proj Ks ≈ proj K% × expected BF. " +
                        "Wind/temp/precip are Open-Meteo at the park lat/long for first pitch, rotated by " +
                        "MLB CF azimuth. Tags also use the same multi-year HR park factor as the Daily HR board " +
                        "(Coors 1.28, Petco 0.90). Rain risk ignores PF. Domes / closed roofs ignore outdoor wind. " +
                        "Fetch failure is Neutral / —. Use ◀ ▶ or the date chip to jump days."
                },
                color = AccentGreen,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OutlookDots() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Ranked by progression → regression",
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
        )
        DotLab("PROG", AccentGreen)
        DotLab("STABLE", StableSlate)
        DotLab("REG", RegRed)
    }
}

@Composable
private fun DotLab(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, CircleShape),
        )
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeatherLegend() {
    Column {
        Text("WEATHER + PARK FACTOR", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendSwatch(RegRed, "RAIN RISK")
            LegendSwatch(HrWeatherOrange, "HR WEATHER")
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LegendSwatch(AccentGreen, "PITCHER WX")
            LegendSwatch(StableSlate, "NEUTRAL WX")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Rain stays weather-only. HR / pitcher chips blend Open-Meteo with the multi-year HR park factor (PF 1.00 = average).",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
private fun StarterRow(starter: Starter, results: Boolean) {
    val accent = when (starter.outlook) {
        Outlook.PROG -> AccentGreen
        Outlook.STABLE -> StableSlate
        Outlook.REG -> RegRed
    }
    val scoreColor = when {
        starter.outlookScore > 3 -> AccentGreen
        starter.outlookScore < -3 -> RegRed
        else -> StableSlate
    }
    val trendColor = when (starter.outlook) {
        Outlook.REG -> RegRed
        Outlook.PROG -> AccentGreen
        Outlook.STABLE -> StableSlate
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 10.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = starter.rank.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.width(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(name = starter.name, team = starter.team, size = 38.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        starter.name,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                    Row {
                        Text(starter.team, color = TextPrimary, fontSize = 11.sp)
                        Text(" vs ", color = TextMuted, fontSize = 11.sp)
                        Text(starter.opponent, color = OpponentRed, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (starter.gameTimeLabel.isNotBlank()) {
                            Icon(Icons.Outlined.Schedule, null, tint = TextMuted, modifier = Modifier.size(11.dp))
                            Text(" ${starter.gameTimeLabel}", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    if (starter.resultNote.isNotBlank()) {
                        Text(starter.resultNote, color = TextMuted, fontSize = 10.sp)
                    }
                }
                if (starter.ace) AceChip()
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column {
                    OutlookChip(starter.outlook)
                    Text(
                        text = (if (starter.outlookScore > 0) "+" else "") + starter.outlookScore,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                if (starter.trend.size >= 2) {
                    Sparkline(
                        values = starter.trend,
                        color = trendColor,
                        modifier = Modifier
                            .width(48.dp)
                            .height(22.dp),
                    )
                }
                StatCell(
                    value = String.format(Locale.US, "%.1f%%", starter.projKPct),
                    label = "PROJ K%",
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (results) {
                    ResultsCell(starter, modifier = Modifier.weight(1f))
                } else {
                    StatCell(
                        value = String.format(Locale.US, "%.1f", starter.nextStartKs),
                        label = "PROJ KS",
                        color = AccentGreen,
                        modifier = Modifier.weight(1f),
                    )
                }
                XwobaCell(starter.xwoba, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            WeatherCard(starter)
        }
    }
}

@Composable
private fun AceChip() {
    Box(
        modifier = Modifier
            .background(AccentGreen.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text("ACE", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun StatCell(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultsCell(starter: Starter, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        if (starter.actualKs != null && starter.actualKPct != null) {
            Text(
                String.format(Locale.US, "%.0f K", starter.actualKs.toFloat()),
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            val dKs = starter.ksDelta
            Text(
                if (dKs != null) String.format(Locale.US, "Δ %+.1f", dKs) else "ACT / Δ",
                color = if (dKs != null && dKs >= 0) AccentGreen else RegRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Text("—", color = TextMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("ACT / Δ", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun XwobaCell(xwoba: Float?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = xwoba?.let { String.format(Locale.US, ".%03d", (it * 1000).toInt()) } ?: "—",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Text("XWOBA · STATCAST", color = TextMuted, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeatherCard(starter: Starter) {
    val (tagLabel, tagColor) = when (starter.wxTag) {
        WxTag.RAIN_RISK -> "RAIN RISK" to RegRed
        WxTag.HR_WEATHER -> "HR WEATHER" to HrWeatherOrange
        WxTag.PITCHER_WX -> "PITCHER WX" to AccentGreen
        WxTag.NEUTRAL -> "NEUTRAL WX" to StableSlate
    }
    val wxIcon = when (starter.weather) {
        Weather.SUN -> Icons.Outlined.WbSunny
        Weather.RAIN -> Icons.Outlined.Umbrella
        Weather.CLOUD -> Icons.Outlined.Cloud
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
            .background(NavySurface, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ParkDiamond(rel = starter.windRel, tag = starter.wxTag)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                starter.windLabel.ifBlank { "Wind n/a" },
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(wxIcon, null, tint = tagColor, modifier = Modifier.size(12.dp))
                if (starter.tempF > 0) {
                    Text(" ${starter.tempF}°", color = TextMuted, fontSize = 11.sp)
                } else {
                    Text(" Temp n/a", color = TextMuted, fontSize = 11.sp)
                }
                if (starter.precipPct != null) {
                    Text(
                        "  ${starter.precipPct}% chance",
                        color = RegRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (starter.parkHint.isNotBlank()) {
                Text(
                    starter.parkHint,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(4.dp))
            WxTagChip(tagLabel, tagColor)
        }
    }
}

@Composable
private fun WxTagChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun ParkDiamond(rel: WindRel, tag: WxTag) {
    val arrow = when {
        rel == WindRel.IN_CF || rel == WindRel.IN_LF || rel == WindRel.IN_RF -> AccentGreen
        rel == WindRel.OUT_CF || rel == WindRel.OUT_LF || rel == WindRel.OUT_RF -> HrWeatherOrange
        rel == WindRel.CROSS_LR || rel == WindRel.CROSS_RL -> StableSlate
        tag == WxTag.RAIN_RISK -> RegRed
        else -> TextMuted
    }
    Canvas(Modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        val home = Offset(w * 0.50f, h * 0.88f)
        val first = Offset(w * 0.88f, h * 0.50f)
        val second = Offset(w * 0.50f, h * 0.12f)
        val third = Offset(w * 0.12f, h * 0.50f)
        val lf = Offset(w * 0.22f, h * 0.22f)
        val rf = Offset(w * 0.78f, h * 0.22f)
        val diamond = Path().apply {
            moveTo(home.x, home.y)
            lineTo(first.x, first.y)
            lineTo(second.x, second.y)
            lineTo(third.x, third.y)
            close()
        }
        drawPath(diamond, AccentGreen.copy(alpha = 0.08f))
        drawPath(diamond, CardStroke, style = Stroke(width = 1.6.dp.toPx()))
        val pair = when (rel) {
            WindRel.IN_CF -> second to home
            WindRel.OUT_CF -> home to second
            WindRel.IN_LF -> lf to home
            WindRel.OUT_LF -> home to lf
            WindRel.IN_RF -> rf to home
            WindRel.OUT_RF -> home to rf
            WindRel.CROSS_LR -> third to first
            WindRel.CROSS_RL -> first to third
            else -> null
        }
        if (pair != null) {
            val (from, to) = pair
            drawLine(arrow, from, to, strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
            val angle = atan2(to.y - from.y, to.x - from.x)
            val head = 6.dp.toPx()
            val left = Offset(
                to.x - head * cos(angle - 0.45f),
                to.y - head * sin(angle - 0.45f),
            )
            val right = Offset(
                to.x - head * cos(angle + 0.45f),
                to.y - head * sin(angle + 0.45f),
            )
            val tip = Path().apply {
                moveTo(to.x, to.y)
                lineTo(left.x, left.y)
                lineTo(right.x, right.y)
                close()
            }
            drawPath(tip, arrow)
        }
    }
}

private fun updatedLabel(instant: Instant): String {
    val local = instant.atZone(StartersRepository.SLATE_ZONE)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}
