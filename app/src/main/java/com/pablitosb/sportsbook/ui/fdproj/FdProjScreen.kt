package com.pablitosb.sportsbook.ui.fdproj

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.dfs.SalarySource
import com.pablitosb.sportsbook.data.dfs.SampleSalaryCsv
import com.pablitosb.sportsbook.data.fdproj.FdPosFilter
import com.pablitosb.sportsbook.data.fdproj.FdProjRow
import com.pablitosb.sportsbook.data.fdproj.FdProjSort
import com.pablitosb.sportsbook.data.fdproj.FdProjSorter
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.AwayAtHomeLine
import com.pablitosb.sportsbook.ui.components.LiveBadge
import com.pablitosb.sportsbook.ui.components.SalaryActionLinks
import com.pablitosb.sportsbook.ui.components.SalaryImportDialog
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SectionRule
import com.pablitosb.sportsbook.ui.components.SlateDateNavBar
import com.pablitosb.sportsbook.ui.components.SlateLoading
import com.pablitosb.sportsbook.ui.components.SlateMessage
import com.pablitosb.sportsbook.ui.components.StubButton
import com.pablitosb.sportsbook.data.starters.StartersRepository
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdProjScreen(
    onBack: () -> Unit,
    viewModel: FdProjViewModel = viewModel(),
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var showSlates by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf("") }

    fun shareSample() {
        SampleSalaryCsv.share(context)?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            ?: Toast.makeText(
                context,
                "Sample CSV ready — save or open it to see the import format.",
                Toast.LENGTH_LONG,
            ).show()
    }

    val slate = when (val state = viewModel.ui) {
        is FdProjUiState.Ready -> state.board.slateDate
        is FdProjUiState.Empty -> state.slateDate
        is FdProjUiState.Error -> state.slateDate
        is FdProjUiState.Loading -> state.slateDate
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
        val slateBoard = when (val state = viewModel.ui) {
            is FdProjUiState.Ready -> state.board
            is FdProjUiState.Empty -> state.board
            else -> null
        }
        if (showSlates && slateBoard != null) {
            AlertDialog(
                onDismissRequest = { showSlates = false },
                title = { Text("Choose slate") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(slateBoard.fdApiNote, color = TextMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        slateBoard.slates.forEach { option ->
                            val on = option.id == viewModel.selectedSlateId
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) AccentGreen.copy(alpha = 0.16f) else CardFill)
                                    .border(1.dp, if (on) AccentGreen else CardStroke, RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.selectSlate(option.id)
                                        showSlates = false
                                    }
                                    .padding(10.dp),
                            ) {
                                Text(option.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(option.subtitle, color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSlates = false }) { Text("Close", color = AccentGreen) }
                },
            )
        }
        if (showImport) {
            SalaryImportDialog(
                title = "Import FanDuel salaries",
                paste = paste,
                onPasteChange = { paste = it },
                onDismiss = { showImport = false },
                onLoadPaste = {
                    if (paste.isNotBlank()) viewModel.applyImport(paste)
                    showImport = false
                },
                onLoadExample = {
                    viewModel.loadExampleFile()
                    showImport = false
                },
                onShareSample = { shareSample() },
                pasteConfirmLabel = "Import",
            )
        }
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is FdProjUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is FdProjUiState.Loading -> SlateLoading(state.slateDate)
                is FdProjUiState.Error -> SlateMessage(
                    title = "Projections unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    extra = {
                        SalaryActionLinks(
                            onImport = { showImport = true },
                            onSample = { shareSample() },
                        )
                    },
                )
                is FdProjUiState.Empty -> SlateMessage(
                    title = "No slate projections",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                    extra = {
                        SalaryActionLinks(
                            onImport = { showImport = true },
                            onSample = { shareSample() },
                        )
                    },
                )
                is FdProjUiState.Ready -> ReadyBoard(
                    state = state,
                    viewModel = viewModel,
                    onChooseSlate = { showSlates = true },
                    onImport = { showImport = true },
                    onShareSample = { shareSample() },
                )
            }
        }
    }
}

@Composable
private fun ReadyBoard(
    state: FdProjUiState.Ready,
    viewModel: FdProjViewModel,
    onChooseSlate: () -> Unit,
    onImport: () -> Unit,
    onShareSample: () -> Unit,
) {
    val board = state.board
    val visible = remember(board.rows, viewModel.sortKey, viewModel.sortAscending, viewModel.posFilter) {
        val filtered = FdProjSorter.filter(board.rows, viewModel.posFilter)
        FdProjSorter.sort(filtered, viewModel.sortKey, viewModel.sortAscending)
    }
    val slateTitle = board.slates.firstOrNull { it.id == board.selectedSlateId }?.title ?: "Main"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveBadge(board.sourceLabel)
                Spacer(Modifier.width(8.dp))
                LiveBadge(
                    when (board.salarySource) {
                        SalarySource.IMPORTED -> "Imported $"
                        SalarySource.EXAMPLE_FILE -> "EXAMPLE file $"
                        SalarySource.EXAMPLE_FORMULA -> "EXAMPLE $"
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Updated " + board.fetchedAt.atZone(StartersRepository.SLATE_ZONE)
                        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a z", Locale.US)),
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("FD DFS Projections", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "FanDuel classic points from the live MLB projection pipeline · $slateTitle",
                color = TextMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StubButton(label = "Choose slate", onClick = onChooseSlate, leading = {
                    Icon(Icons.Outlined.Layers, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                })
                StubButton(label = "Import $", onClick = onImport, leading = {
                    Icon(Icons.Outlined.FileUpload, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                })
                StubButton(label = "Sample CSV", onClick = onShareSample, leading = {
                    Icon(Icons.Outlined.Download, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                })
                if (viewModel.importedText != null || viewModel.useExampleFile) {
                    StubButton(label = "Clear $", onClick = { viewModel.clearImport() })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(SampleSalaryCsv.HINT, color = TextMuted, fontSize = 11.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(board.salaryNote, color = AccentGreen, fontSize = 11.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(10.dp))
            ChipRow(
                items = listOf(
                    FdProjSort.PROJ to "Proj pts",
                    FdProjSort.VALUE to "Value",
                    FdProjSort.SALARY to "Salary",
                    FdProjSort.POS to "Pos",
                ),
                selected = viewModel.sortKey,
                selectedLabel = { key, label ->
                    val arrow = if (viewModel.sortAscending) "↑" else "↓"
                    if (key == viewModel.sortKey) "$label $arrow" else label
                },
                onClick = { viewModel.selectSort(it) },
            )
            Spacer(Modifier.height(6.dp))
            ChipRow(
                items = listOf(
                    FdPosFilter.ALL to "All",
                    FdPosFilter.P to "P",
                    FdPosFilter.C to "C",
                    FdPosFilter.B1 to "1B",
                    FdPosFilter.B2 to "2B",
                    FdPosFilter.B3 to "3B",
                    FdPosFilter.SS to "SS",
                    FdPosFilter.OF to "OF",
                    FdPosFilter.DH to "DH",
                ),
                selected = viewModel.posFilter,
                selectedLabel = { _, label -> label },
                onClick = { viewModel.selectPos(it) },
            )
            Spacer(Modifier.height(8.dp))
            Text("${visible.size} players", color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
        }
        itemsIndexed(visible, key = { _, row -> "${row.mlbId}-${row.pos}" }) { index, row ->
            ProjRow(index + 1, row)
            SectionRule()
        }
        item {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Proj FD pts are the same FanDuel scoring the lineup optimizer uses " +
                        "(hitters: 1B/2B/3B/HR/RBI/R/BB/SB; pitchers: W/QS/ER/SO/IP). " +
                        "Value = pts per \$1k. Own is a placeholder (—) until a real ownership feed exists. " +
                        "FanDuel fixture-lists stay 401 without login.",
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
private fun <T> ChipRow(
    items: List<Pair<T, String>>,
    selected: T,
    selectedLabel: (T, String) -> String,
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
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    selectedLabel(key, label),
                    color = if (on) AccentGreen else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun ProjRow(rank: Int, row: FdProjRow) {
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
                Text(row.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                Row {
                    Text(row.pos, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("  ", color = TextMuted, fontSize = 11.sp)
                    AwayAtHomeLine(
                        team = row.team,
                        opponent = row.opponent,
                        homeAway = row.homeAway,
                        awayAbbr = row.awayAbbr,
                        homeAbbr = row.homeAbbr,
                        time = row.gameTimeLabel,
                        fontSize = 11.sp,
                    )
                }
                if (row.driver.isNotBlank()) {
                    Text(row.driver, color = TextMuted, fontSize = 10.sp)
                }
                if (!row.inPostedLineup && !row.isPitcher) {
                    Text("Roster (lineup not posted)", color = TextMuted, fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.US, "%.1f", row.proj),
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text("PROJ FD", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Meta("SAL", "$${row.salary}")
            Meta("VAL", row.value?.let { String.format(Locale.US, "%.2f", it) } ?: "—")
            Meta("CEIL", String.format(Locale.US, "%.1f", row.ceiling))
            Meta("TB", row.projTb?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
            Meta("OWN", row.ownPlaceholder)
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
