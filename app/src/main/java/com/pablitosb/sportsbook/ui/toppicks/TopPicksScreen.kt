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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.hr.BatterOppTint
import com.pablitosb.sportsbook.data.hr.HrSort
import com.pablitosb.sportsbook.data.mlb.OppKScale
import com.pablitosb.sportsbook.data.mlb.OppKTier
import com.pablitosb.sportsbook.data.model.HrBatter
import com.pablitosb.sportsbook.data.model.Starter
import com.pablitosb.sportsbook.data.starters.SlateMode
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.data.starters.StartersSort
import com.pablitosb.sportsbook.data.toppicks.TopPicksSelector
import com.pablitosb.sportsbook.data.toppicks.TopPicksSide
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.NavyBlack
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
    val liveLabel = when (val state = viewModel.ui) {
        is TopPicksUiState.Ready ->
            if (state.board.startersBoard?.mode == SlateMode.RESULTS) "Results · MLB" else "Live • MLB"
        is TopPicksUiState.Empty -> state.sourceLabel
        else -> "Live • MLB"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        PicksTopBar(onBack = onBack, liveLabel = liveLabel)
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
                                viewModel.goTo(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                            }
                            showPicker = false
                        },
                    ) { Text("Go", color = AccentGreen) }
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
                is TopPicksUiState.Loading -> LoadingBody(state.slateDate)
                is TopPicksUiState.Error -> MessageBody(
                    title = "Picks unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is TopPicksUiState.Empty -> MessageBody(
                    title = "No picks yet",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    badge = state.sourceLabel,
                )
                is TopPicksUiState.Ready -> ReadyPicks(state, viewModel)
            }
        }
    }
}

@Composable
private fun PicksTopBar(onBack: () -> Unit, liveLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AccentGreen)
        }
        Text(
            "Today’s Top Picks",
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
private fun ReadyPicks(state: TopPicksUiState.Ready, viewModel: TopPicksViewModel) {
    val board = state.board
    val side = viewModel.side
    val starters = board.startersBoard?.starters.orEmpty()
    val batters = board.hrBoard?.batters.orEmpty()
    val rankedPitchers = remember(starters, viewModel.pitcherSort, viewModel.pitcherAscending) {
        TopPicksSelector.pitchers(starters, viewModel.pitcherSort, viewModel.pitcherAscending)
    }
    val rankedBatters = remember(batters, viewModel.batterSort, viewModel.batterAscending) {
        TopPicksSelector.batters(batters, viewModel.batterSort, viewModel.batterAscending)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        item {
            SideSegment(selected = side, onSelect = { viewModel.selectSide(it) })
            Spacer(Modifier.height(4.dp))
            if (side == TopPicksSide.PITCHERS) {
                PitcherFilterTabs(selected = viewModel.pitcherSort, onSelect = { viewModel.selectPitcherSort(it) })
            } else {
                BatterFilterTabs(selected = viewModel.batterSort, onSelect = { viewModel.selectBatterSort(it) })
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (side == TopPicksSide.PITCHERS) {
                    board.startersBoard?.oppKScale?.legend()
                        ?: "Opp K% (team SO/PA): red = low · green = high."
                } else {
                    board.hrBoard?.oppKScale?.batterLegend()
                        ?: "Opp pitcher K% inverted: green = low-K / favorable."
                },
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Text(updatedLabel(board.fetchedAt), color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
        }
        if (side == TopPicksSide.PITCHERS) {
            if (rankedPitchers.isEmpty()) {
                item {
                    Text(
                        board.pitchersNote ?: "No pitcher picks for this slate.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                val scale = board.startersBoard?.oppKScale ?: OppKScale.fallback()
                val results = board.startersBoard?.mode == SlateMode.RESULTS
                itemsIndexed(rankedPitchers, key = { _, it -> "p-${it.mlbId}-${it.homeAway}-${it.gameTimeLabel}" }) { index, starter ->
                    PitcherRow(starter, viewModel.pitcherSort, index + 1, scale, results)
                    SectionRule()
                }
            }
        } else {
            if (rankedBatters.isEmpty()) {
                item {
                    Text(
                        board.battersNote ?: "No batter picks for this slate.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                val scale = board.hrBoard?.oppKScale ?: OppKScale.fallback()
                itemsIndexed(rankedBatters, key = { _, it -> "b-${it.mlbId}-${it.homeAway}-${it.gameTimeLabel}-${it.battingOrder}" }) { index, batter ->
                    BatterRow(batter, viewModel.batterSort, index + 1, scale)
                    SectionRule()
                }
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Top ${TopPicksSelector.LIMIT} by the active filter — same models as Projected Starters and Daily Batters. " +
                        "Pitchers: Prog · Proj Ks · xwOBA · Proj Outs · Proj FD. " +
                        "Batters: Game HR% · Proj FD · Proj TB · H+R+RBI. " +
                        "Only the selected metric shows. Weather boost stays on every row. " +
                        "Pitcher opponent tint = team K%. Batter opponent tint = opposing pitcher K% inverted. " +
                        "PROG chip only on the Prog filter.",
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
private fun SideSegment(selected: TopPicksSide, onSelect: (TopPicksSide) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(TopPicksSide.PITCHERS to "Pitchers", TopPicksSide.BATTERS to "Batters").forEach { (key, label) ->
            val on = key == selected
            Column(
                modifier = Modifier
                    .clickable { onSelect(key) }
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    color = if (on) TextPrimary else TextMuted,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(72.dp)
                        .height(3.dp)
                        .background(if (on) AccentGreen else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun PitcherFilterTabs(selected: StartersSort, onSelect: (StartersSort) -> Unit) {
    FilterTabRow(
        items = listOf(
            StartersSort.PROG to "Prog",
            StartersSort.PROJ_KS to "Proj Ks",
            StartersSort.XWOBA to "xwOBA",
            StartersSort.PROJ_OUTS to "Proj Outs",
            StartersSort.PROJ_FD to "Proj FD",
        ),
        selected = selected,
        onSelect = onSelect,
        underlineWidth = 48.dp,
    )
}

@Composable
private fun BatterFilterTabs(selected: HrSort, onSelect: (HrSort) -> Unit) {
    FilterTabRow(
        items = listOf(
            HrSort.GAME_HR to "Game HR%",
            HrSort.PROJ_FD to "Proj FD",
            HrSort.PROJ_TB to "Proj TB",
            HrSort.HRR to "H+R+RBI",
        ),
        selected = selected,
        onSelect = onSelect,
        underlineWidth = 56.dp,
    )
}

@Composable
private fun <T> FilterTabRow(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    underlineWidth: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEach { (key, label) ->
            val on = key == selected
            Column(
                modifier = Modifier
                    .clickable { onSelect(key) }
                    .padding(top = 6.dp, start = 8.dp, end = 8.dp),
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
                        .width(underlineWidth)
                        .height(3.dp)
                        .background(if (on) AccentGreen else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun PitcherRow(
    starter: Starter,
    sortKey: StartersSort,
    displayRank: Int,
    scale: OppKScale,
    results: Boolean,
) {
    val oppColor = when (scale.tier(starter.oppKRate)) {
        OppKTier.LOW -> OpponentRed
        OppKTier.HIGH -> AccentGreen
        OppKTier.MID, OppKTier.UNKNOWN -> StableSlate
    }
    val boostColor = when {
        starter.envBoostPct > 0 -> AccentGreen
        starter.envBoostPct < 0 -> RegRed
        else -> TextMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            displayRank.toString(),
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
                if (TopPicksSelector.showOutlookChip(sortKey)) {
                    Spacer(Modifier.width(6.dp))
                    OutlookChip(starter.outlook)
                }
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
        }
        PitcherStat(
            starter,
            sortKey,
            results,
            Modifier.weight(if (sortKey == StartersSort.PROJ_FD) 1.35f else 0.85f),
        )
        WeatherBoost(starter.envBoostPct, boostColor)
    }
}

@Composable
private fun BatterRow(batter: HrBatter, sortKey: HrSort, displayRank: Int, scale: OppKScale) {
    val oppColor = when (val tier = scale.tier(batter.oppPitcherK)) {
        OppKTier.UNKNOWN -> StableSlate
        else -> when {
            BatterOppTint.favorable(tier) -> AccentGreen
            BatterOppTint.tough(tier) -> OpponentRed
            else -> StableSlate
        }
    }
    val boostColor = when {
        batter.envBoostPct > 0 -> AccentGreen
        batter.envBoostPct < 0 -> RegRed
        else -> TextMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            displayRank.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.width(22.dp),
        )
        Column(Modifier.weight(1.15f)) {
            Text(
                batter.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AwayAtHomeLine(
                team = batter.team,
                opponent = batter.opponent,
                homeAway = batter.homeAway,
                awayAbbr = batter.awayAbbr,
                homeAbbr = batter.homeAbbr,
                time = batter.gameTimeLabel,
                opponentColor = oppColor,
                fontSize = 11.sp,
            )
        }
        BatterStat(
            batter,
            sortKey,
            Modifier.weight(if (sortKey == HrSort.PROJ_FD) 1.35f else 0.85f),
        )
        WeatherBoost(batter.envBoostPct, boostColor)
    }
}

@Composable
private fun WeatherBoost(pct: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(start = 4.dp),
    ) {
        Text("Weather boost", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        Text(
            if (pct > 0) "+$pct%" else "$pct%",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PitcherStat(
    starter: Starter,
    sortKey: StartersSort,
    results: Boolean,
    modifier: Modifier,
) {
    if (sortKey == StartersSort.PROJ_FD) {
        FdTriple(starter.fdFloor, starter.fdProj, starter.fdCeiling, modifier)
        return
    }
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
        StartersSort.PROJ_FD -> Triple("Proj FD", "", null)
    }
    SingleStat(label, value, sub, modifier)
}

@Composable
private fun BatterStat(batter: HrBatter, sortKey: HrSort, modifier: Modifier) {
    if (sortKey == HrSort.PROJ_FD) {
        FdTriple(batter.fdFloor, batter.fdProj, batter.fdCeiling, modifier)
        return
    }
    val (label, value, sub) = when (sortKey) {
        HrSort.GAME_HR -> Triple("Game HR%", String.format(Locale.US, "%.1f%%", batter.gameHrPct), null)
        HrSort.PROJ_TB -> Triple("Proj TB", String.format(Locale.US, "%.2f", batter.projTb), null)
        HrSort.HRR -> Triple(
            "H+R+RBI",
            String.format(Locale.US, "%.2f", batter.projHrr),
            String.format(Locale.US, "H %.1f · R %.1f · RBI %.1f", batter.projHits, batter.projRuns, batter.projRbi),
        )
        HrSort.PROJ_FD -> Triple("Proj FD", "", null)
    }
    SingleStat(label, value, sub, modifier)
}

@Composable
private fun SingleStat(label: String, value: String, sub: String?, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(label, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp, bottom = 2.dp))
            Text(value, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        }
        if (sub != null) {
            Text(sub, color = TextMuted, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun FdTriple(floor: Float, proj: Float, ceiling: Float, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FdCell("Floor", String.format(Locale.US, "%.1f", floor), TextPrimary, 13.sp)
        FdCell("Proj", String.format(Locale.US, "%.1f", proj), AccentGreen, 18.sp)
        FdCell("Ceiling", String.format(Locale.US, "%.1f", ceiling), TextPrimary, 13.sp)
    }
}

@Composable
private fun FdCell(label: String, value: String, color: Color, valueSize: androidx.compose.ui.unit.TextUnit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
        Text(value, color = color, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1)
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

private fun updatedLabel(instant: Instant): String {
    val local = instant.atZone(StartersRepository.SLATE_ZONE)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}
