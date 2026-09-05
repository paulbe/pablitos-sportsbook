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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.model.Confidence
import com.pablitosb.sportsbook.data.model.PropLineSource
import com.pablitosb.sportsbook.data.model.UnderdogProp
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.ConfidenceMeter
import com.pablitosb.sportsbook.ui.components.LiveBadge
import com.pablitosb.sportsbook.ui.components.PlayerAvatar
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.SlateDateNavBar
import com.pablitosb.sportsbook.ui.components.SlateLoading
import com.pablitosb.sportsbook.ui.components.SlateMessage
import com.pablitosb.sportsbook.ui.components.StubButton
import com.pablitosb.sportsbook.ui.components.updatedLabel
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnderdogPropsScreen(
    onBack: () -> Unit,
    viewModel: PropsViewModel = viewModel(),
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<Int?>(1) }
    var showPicker by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf("") }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val slate = when (val state = viewModel.ui) {
        is PropsUiState.Ready -> state.board.slate.slateDate
        is PropsUiState.Empty -> state.slateDate
        is PropsUiState.Error -> state.slateDate
        is PropsUiState.Loading -> state.slateDate
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
        if (showImport) {
            AlertDialog(
                onDismissRequest = { showImport = false },
                title = { Text("Import Underdog lines") },
                text = {
                    Column {
                        Text(
                            "CSV: player,market,line,side,odds  — market is pitcher_ks / batter_hr / batter_hits. We never invent live Underdog prices.",
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = paste,
                            onValueChange = { paste = it },
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            placeholder = { Text("Tarik Skubal,pitcher_ks,7.5,higher,-120") },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.applyImport(paste)
                        showImport = false
                        toast("Applied imported lines to matching model props.")
                    }) { Text("Load paste", color = AccentGreen) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.loadExampleFile()
                            showImport = false
                            toast("Loaded EXAMPLE lines — labeled, not live Underdog.")
                        }) { Text("EXAMPLE file", color = AccentGreen) }
                        TextButton(onClick = { showImport = false }) { Text("Cancel", color = TextMuted) }
                    }
                },
            )
        }
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is PropsUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            when (val state = viewModel.ui) {
                is PropsUiState.Loading -> SlateLoading(state.slateDate)
                is PropsUiState.Error -> SlateMessage("Props board unavailable", state.message, { viewModel.refresh() })
                is PropsUiState.Empty -> SlateMessage(
                    title = "No props",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                )
                is PropsUiState.Ready -> ReadyProps(
                    board = state.board,
                    minEdge = viewModel.minEdge,
                    selected = selected,
                    onSelect = { selected = it },
                    onCycleEdge = { viewModel.cycleMinEdge() },
                    onImport = { showImport = true },
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StubButton(
                    label = "Refresh model",
                    onClick = {
                        viewModel.refresh()
                        toast("Re-pulled MLB projections. Book lines only change if you import.")
                    },
                    modifier = Modifier.weight(1f),
                    leading = { Icon(Icons.Outlined.Refresh, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
                )
                StubButton(
                    label = "Sync Underdog",
                    onClick = {
                        toast("No Underdog API in this build. Import CSV lines (player,market,line,side,odds) or stay on the model board.")
                    },
                    modifier = Modifier.weight(1f),
                )
                StubButton(
                    label = "Add to slip",
                    onClick = {
                        val ready = viewModel.ui as? PropsUiState.Ready
                        val rows = ready?.board?.props.orEmpty()
                        val pick = rows.firstOrNull { it.rank == selected } ?: rows.firstOrNull()
                        toast(if (pick == null) "No prop selected" else "Noted ${pick.player} ${pick.propLabel} ${pick.line} (local slip only)")
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
                Text(
                    "Model board until you import lines. Edge = model − implied. No live Underdog odds.",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ReadyProps(
    board: com.pablitosb.sportsbook.data.props.PropsBoard,
    minEdge: Float,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onCycleEdge: () -> Unit,
    onImport: () -> Unit,
) {
    val rows = board.props.filter { (it.edgePct ?: 0f) >= minEdge || it.impliedProb == null && minEdge == 0f }
    val plusEv = board.props.count { (it.edgePct ?: 0f) > 0f && it.source == PropLineSource.IMPORTED }
    val avg = board.props.mapNotNull { it.edgePct }.filter { it > 0f }.average().toFloat().takeIf { !it.isNaN() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Underdog Props", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Model vs imported line — never invented book odds", color = TextMuted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(board.sourceLabel)
                Spacer(Modifier.weight(1f))
                Text(updatedLabel(board.slate.fetchedAt, StartersRepository.SLATE_ZONE), color = TextMuted, fontSize = 11.sp)
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
                Text(
                    if (board.importedCount > 0) "$plusEv +EV vs imported" else "Model board · import lines for edge",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                if (avg != null) {
                    Text("Avg edge ", color = TextMuted, fontSize = 13.sp)
                    Text(String.format(Locale.US, "%.1f%%", avg), color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterPill(Icons.Outlined.SportsBaseball, "Baseball")
                FilterPill(null, "Ks / HR / Hits")
                FilterPill(null, if (minEdge <= 0f) "All leans" else "Min edge ${minEdge.toInt()}%", onCycleEdge)
                FilterPill(null, "Import lines", onImport)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardFill)
                        .border(1.dp, CardStroke, RoundedCornerShape(10.dp)),
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
        items(rows, key = { "${it.rank}-${it.player}-${it.propLabel}" }) { prop ->
            PropRow(prop, selected == prop.rank) { onSelect(prop.rank) }
            SectionRule()
        }
        item { Spacer(Modifier.height(8.dp)) }
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
    val edge = prop.edgePct
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NavySurface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(AccentGreen),
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
                Box(Modifier.border(1.dp, CardStroke, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                    Text(prop.line, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    prop.odds?.let { if (it > 0) "+$it" else it.toString() } ?: "no book",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
        }
        Column(Modifier.weight(0.5f), horizontalAlignment = Alignment.End) {
            Text(pct(prop.modelProb), color = AccentGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(prop.impliedProb?.let { pct(it) } ?: "—", color = TextMuted, fontSize = 11.sp)
        }
        Text(
            edge?.let { (if (it >= 0) "+" else "") + String.format(Locale.US, "%.1f%%", it) } ?: "—",
            color = if (edge != null && edge >= 0) AccentGreen else TextMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(58.dp).padding(start = 4.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
            ConfidenceMeter(filled = confidenceBars(prop.confidence))
            Text(confidenceLabel(prop.confidence), color = TextMuted, fontSize = 9.sp)
        }
    }
}

private fun pct(value: Float): String = String.format(Locale.US, "%.1f%%", value)

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
