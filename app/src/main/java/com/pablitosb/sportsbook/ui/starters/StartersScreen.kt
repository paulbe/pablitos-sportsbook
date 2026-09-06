package com.pablitosb.sportsbook.ui.starters

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.mlb.OppKTier
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.model.Weather
import com.pablitosb.sportsbook.data.model.WindRel
import com.pablitosb.sportsbook.data.model.WxTag
import com.pablitosb.sportsbook.data.starters.SlateMode
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.starters.StartersSort
import com.pablitosb.sportsbook.data.starters.StartersSorter
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
import com.pablitosb.sportsbook.ui.components.AwayAtHomeLine
import com.pablitosb.sportsbook.ui.components.OutlookChip
import com.pablitosb.sportsbook.ui.components.SectionRule
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
    val liveLabel = when (val state = viewModel.ui) {
        is StartersUiState.Ready -> if (state.board.mode == SlateMode.RESULTS) "Results · MLB" else "Live • MLB"
        is StartersUiState.Empty -> state.sourceLabel
        else -> "Live • MLB"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        StartersTopBar(onBack = onBack, liveLabel = liveLabel)
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
                is StartersUiState.Ready -> ReadyList(state, viewModel)
            }
        }
    }
}

@Composable
private fun StartersTopBar(onBack: () -> Unit, liveLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = AccentGreen,
            )
        }
        Text(
            "Projected Starters",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LiveChip(liveLabel)
        Spacer(Modifier.width(8.dp))
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
                .clickable(onClick = onPick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
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
private fun ReadyList(state: StartersUiState.Ready, viewModel: StartersViewModel) {
    val board = state.board
    val results = board.mode == SlateMode.RESULTS
    val sorted = remember(board.starters, viewModel.sortKey, viewModel.sortAscending) {
        StartersSorter.sort(board.starters, viewModel.sortKey, viewModel.sortAscending)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        item {
            FilterTabRow(
                selected = viewModel.sortKey,
                onSelect = { viewModel.selectSort(it) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                board.oppKScale.legend(),
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            if (board.postponedCount > 0) {
                Text(
                    "${board.postponedCount} game(s) postponed/canceled — omitted.",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            Text(updatedLabel(board.fetchedAt), color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
        }
        itemsIndexed(sorted, key = { _, it -> "${it.mlbId}-${it.homeAway}-${it.gameTimeLabel}" }) { index, starter ->
            StarterRow(starter, results, viewModel.sortKey, index + 1, board.oppKScale)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(14.dp))
            OppKLegend(board.oppKScale)
            Spacer(Modifier.height(10.dp))
            WeatherLegend()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "xwOBA · Statcast season-to-date expected wOBA against (lower is better). Missing Savant row shows —.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (results) {
                    "Predictions are reconstructed with OutlookCalculator using only game logs before this date. " +
                        "Actual Ks come from the boxscore starter. Δ = actual − predicted."
                } else {
                    "Filters: Prog · Proj Ks · xwOBA · Proj Outs. Center shows only the selected stat. " +
                        "Outlook = quality (proj K% vs 22.5% lg) + last-5-GS vs season K% trajectory. " +
                        "Proj Ks ≈ proj K% × expected BF. Proj Outs = matchup-adjusted IP × 3 " +
                        "(shrink recent/season IP/GS, then opponent OPS, Weather boost, early exits). " +
                        "Weather boost % = HR park + park-relative wind + temp (+ = hitter-friendly). " +
                        "Pitcher’s team stays white; opponent tint is team K% tertiles. " +
                        "Tap an active filter to flip sort direction."
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
private fun FilterTabRow(
    selected: StartersSort,
    onSelect: (StartersSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        filterTabs().forEach { (key, label) ->
            val on = key == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(key) }
                    .padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    color = if (on) TextPrimary else TextMuted,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(if (on) AccentGreen else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

private fun filterTabs(): List<Pair<StartersSort, String>> = listOf(
    StartersSort.PROG to "Prog",
    StartersSort.PROJ_KS to "Proj Ks",
    StartersSort.XWOBA to "xwOBA",
    StartersSort.PROJ_OUTS to "Proj Outs",
)

@Composable
private fun OppKLegend(scale: OppKScale) {
    Column {
        Text(
            "OPPONENT K% COLOR",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendSwatch(RegRed, "Low K")
            LegendSwatch(StableSlate, "Mid")
            LegendSwatch(AccentGreen, "High K")
        }
        Spacer(Modifier.height(4.dp))
        Text(scale.legend(), color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
        Text(
            "Pitcher’s club is always white. Only the opponent abbreviation is colored.",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

@Composable
private fun WeatherLegend() {
    Column {
        Text(
            "WEATHER CHIP",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        )
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
            "Weather boost is a persistent park + wind + temp % (not a filter). Rain is weather-only.",
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(AccentGreen, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
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
private fun StarterRow(
    starter: Starter,
    results: Boolean,
    sortKey: StartersSort,
    displayRank: Int,
    scale: OppKScale,
) {
    val oppColor = when (scale.tier(starter.oppKRate)) {
        OppKTier.LOW -> OpponentRed
        OppKTier.HIGH -> AccentGreen
        OppKTier.MID, OppKTier.UNKNOWN -> StableSlate
    }
    val boostColor = when {
        starter.wxTag == WxTag.RAIN_RISK -> RegRed
        starter.envBoostPct > 3 -> AccentGreen
        starter.envBoostPct < -3 -> AccentGreen
        else -> AccentGreen
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = displayRank.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.width(22.dp),
        )
        Column(Modifier.weight(1.15f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    starter.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                OutlookChip(starter.outlook)
            }
            AwayAtHomeLine(
                team = starter.team,
                opponent = starter.opponent,
                homeAway = starter.homeAway,
                awayAbbr = starter.awayAbbr,
                homeAbbr = starter.homeAbbr,
                time = starter.gameTimeLabel,
                opponentColor = oppColor,
                fontSize = 11.sp,
            )
            if (starter.resultNote.isNotBlank()) {
                Text(starter.resultNote, color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
        SelectedStat(starter, sortKey, results, Modifier.weight(0.85f))
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Text("Weather boost", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            Text(
                signedPct(starter.envBoostPct),
                color = boostColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(6.dp))
        WeatherChip(starter)
    }
}

@Composable
private fun SelectedStat(
    starter: Starter,
    sortKey: StartersSort,
    results: Boolean,
    modifier: Modifier = Modifier,
) {
    val (label, value, sub) = when (sortKey) {
        StartersSort.PROG -> Triple(
            "Prog",
            (if (starter.outlookScore > 0) "+" else "") + starter.outlookScore,
            starter.outlook.name,
        )
        StartersSort.PROJ_KS -> Triple(
            "Proj Ks",
            String.format(Locale.US, "%.1f", starter.nextStartKs),
            if (results && starter.actualKs != null) {
                val d = starter.ksDelta
                val act = String.format(Locale.US, "%.0f act", starter.actualKs.toFloat())
                if (d != null) "$act · Δ ${String.format(Locale.US, "%+.1f", d)}" else act
            } else {
                null
            },
        )
        StartersSort.XWOBA -> Triple(
            "xwOBA",
            starter.xwoba?.let { String.format(Locale.US, ".%03d", (it * 1000).toInt()) } ?: "—",
            null,
        )
        StartersSort.PROJ_OUTS -> Triple(
            "Proj Outs",
            String.format(Locale.US, "%.1f", starter.projOuts),
            if (starter.projIp > 0f) String.format(Locale.US, "~%.1f IP", starter.projIp) else null,
        )
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(label, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp, bottom = 2.dp))
            Text(
                value,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
            )
        }
        if (sub != null) {
            Text(sub, color = TextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherChip(starter: Starter) {
    val wxIcon = when (starter.weather) {
        Weather.SUN -> Icons.Outlined.WbSunny
        Weather.RAIN -> Icons.Outlined.Umbrella
        Weather.CLOUD -> Icons.Outlined.Cloud
    }
    val tagColor = when (starter.wxTag) {
        WxTag.RAIN_RISK -> RegRed
        WxTag.HR_WEATHER -> HrWeatherOrange
        WxTag.PITCHER_WX -> AccentGreen
        WxTag.NEUTRAL -> StableSlate
    }
    Column(
        modifier = Modifier
            .widthIn(min = 78.dp)
            .background(NavySurface, RoundedCornerShape(10.dp))
            .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ParkDiamond(rel = starter.windRel, tag = starter.wxTag, diamondSize = 22.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                starter.windLabel.ifBlank { "Wind n/a" },
                color = TextPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                lineHeight = 10.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(wxIcon, null, tint = tagColor, modifier = Modifier.size(11.dp))
            Text(
                if (starter.tempF > 0) " ${starter.tempF}°" else " n/a",
                color = TextMuted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ParkDiamond(rel: WindRel, tag: WxTag, diamondSize: androidx.compose.ui.unit.Dp = 40.dp) {
    val arrow = when {
        rel == WindRel.IN_CF || rel == WindRel.IN_LF || rel == WindRel.IN_RF -> AccentGreen
        rel == WindRel.OUT_CF || rel == WindRel.OUT_LF || rel == WindRel.OUT_RF -> HrWeatherOrange
        rel == WindRel.CROSS_LR || rel == WindRel.CROSS_RL -> StableSlate
        tag == WxTag.RAIN_RISK -> RegRed
        else -> TextMuted
    }
    Canvas(Modifier.size(diamondSize)) {
        val w = this.size.width
        val h = this.size.height
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

private fun signedPct(pct: Int): String = if (pct > 0) "+$pct%" else "$pct%"

private fun updatedLabel(instant: Instant): String {
    val local = instant.atZone(StartersRepository.SLATE_ZONE)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}
