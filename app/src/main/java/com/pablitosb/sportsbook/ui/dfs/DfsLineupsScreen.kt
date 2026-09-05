package com.pablitosb.sportsbook.ui.dfs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.dfs.SalarySource
import com.pablitosb.sportsbook.data.model.ContestType
import com.pablitosb.sportsbook.data.model.DfsLineup
import com.pablitosb.sportsbook.data.model.LineupKind
import com.pablitosb.sportsbook.data.starters.StartersRepository
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.LiveBadge
import com.pablitosb.sportsbook.ui.components.ScreenTopBar
import com.pablitosb.sportsbook.ui.components.SlateDateNavBar
import com.pablitosb.sportsbook.ui.components.SlateLoading
import com.pablitosb.sportsbook.ui.components.SlateMessage
import com.pablitosb.sportsbook.ui.components.StubButton
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DfsLineupsScreen(
    onBack: () -> Unit,
    viewModel: DfsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showSlates by remember { mutableStateOf(false) }
    var paste by remember { mutableStateOf("") }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val slate = when (val state = viewModel.ui) {
        is DfsUiState.Ready -> state.board.slate.slateDate
        is DfsUiState.Empty -> state.slateDate
        is DfsUiState.Error -> state.slateDate
        is DfsUiState.Loading -> state.slateDate
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
        val slateOptions = (viewModel.ui as? DfsUiState.Ready)?.board?.slates.orEmpty()
        if (showSlates) {
            AlertDialog(
                onDismissRequest = { showSlates = false },
                title = { Text("Choose slate") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            (viewModel.ui as? DfsUiState.Ready)?.board?.fdApiNote
                                ?: "Pick Main, Early, Late, or a single-game Showdown pool.",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        slateOptions.forEach { option ->
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
                                        toast("Loaded ${option.title}")
                                    }
                                    .padding(10.dp),
                            ) {
                                Text(option.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(option.subtitle, color = TextMuted, fontSize = 12.sp)
                            }
                        }
                        if (slateOptions.isEmpty()) {
                            Text("No slates yet — wait for MLB games or import a CSV.", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSlates = false }) { Text("Close", color = AccentGreen) }
                },
            )
        }
        if (showImport) {
            AlertDialog(
                onDismissRequest = { showImport = false },
                title = { Text("Import slate / salaries") },
                text = {
                    Column {
                        Text(
                            "Paste CSV: name,team,pos,salary[,proj][,mlbId]. These replace EXAMPLE prices. Not a live FanDuel feed.",
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = paste,
                            onValueChange = { paste = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            placeholder = { Text("Aaron Judge,NYY,OF,4500") },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (paste.isBlank()) {
                            toast("Paste a CSV first, or load the EXAMPLE file.")
                        } else {
                            viewModel.applyImport(paste)
                            showImport = false
                            toast("Imported salaries — optimizer will use this slate.")
                        }
                    }) { Text("Load paste", color = AccentGreen) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.loadExampleFile()
                            showImport = false
                            toast("Loaded EXAMPLE file salaries — not live FanDuel.")
                        }) { Text("EXAMPLE file", color = AccentGreen) }
                        TextButton(onClick = { showImport = false }) { Text("Cancel", color = TextMuted) }
                    }
                },
            )
        }
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is DfsUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            when (val state = viewModel.ui) {
                is DfsUiState.Loading -> SlateLoading(state.slateDate)
                is DfsUiState.Error -> SlateMessage(
                    title = "DFS slate unavailable",
                    body = state.message + " You can still import a salary CSV.",
                    onRetry = { viewModel.refresh() },
                )
                is DfsUiState.Empty -> SlateMessage(
                    title = "No lineups",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    zone = StartersRepository.SLATE_ZONE,
                    badge = state.sourceLabel,
                )
                is DfsUiState.Ready -> ReadyDfs(
                    state = state,
                    contest = viewModel.contest,
                    stackDots = viewModel.stackDots,
                    ownDots = viewModel.ownDots,
                    onContest = viewModel::selectContest,
                    onStack = viewModel::cycleStack,
                    onOwn = viewModel::cycleOwn,
                    onPage = { viewModel.currentLineupIndex = it },
                    onImport = { showImport = true },
                    onChooseSlate = { showSlates = true },
                    onClearImport = {
                        viewModel.clearImport()
                        toast("Back to EXAMPLE formula salaries.")
                    },
                )
            }
        }
        val ready = viewModel.ui as? DfsUiState.Ready
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StubButton(
                label = "Regenerate 5",
                onClick = {
                    viewModel.regenerate()
                    toast("Re-ran optimizer (${viewModel.contest.name.lowercase()})")
                },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.Refresh, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
            StubButton(
                label = "Export all",
                onClick = {
                    val csv = viewModel.exportCsv()
                    if (csv.isBlank()) {
                        toast("Nothing to export yet.")
                    } else {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_TEXT, csv)
                            putExtra(Intent.EXTRA_SUBJECT, "Pablito DFS lineups")
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Export lineups")) }
                            .onFailure { toast("Share sheet unavailable — copied CSV instead.") }
                        copyText(context, csv)
                    }
                },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.FileUpload, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
            StubButton(
                label = "Copy this",
                onClick = {
                    val text = viewModel.copyCurrent(viewModel.currentLineupIndex)
                    if (text.isBlank()) toast("No lineup to copy") else {
                        copyText(context, text)
                        toast("Copied lineup")
                    }
                },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Outlined.ContentCopy, null, tint = TextPrimary, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun ReadyDfs(
    state: DfsUiState.Ready,
    contest: ContestType,
    stackDots: Int,
    ownDots: Int,
    onContest: (ContestType) -> Unit,
    onStack: () -> Unit,
    onOwn: () -> Unit,
    onPage: (Int) -> Unit,
    onImport: () -> Unit,
    onChooseSlate: () -> Unit,
    onClearImport: () -> Unit,
) {
    val board = state.board
    val lineups = board.lineups
    val safeCount = lineups.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { safeCount })
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(lineups.size) {
        if (pagerState.currentPage >= lineups.size && lineups.isNotEmpty()) {
            runCatching { pagerState.scrollToPage(0) }
        }
        onPage(pagerState.currentPage.coerceIn(0, (lineups.size - 1).coerceAtLeast(0)))
    }
    androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
        onPage(pagerState.currentPage.coerceIn(0, (lineups.size - 1).coerceAtLeast(0)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("DFS Lineups", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("FanDuel classic • $35k • our projections", color = AccentGreen, fontSize = 13.sp)
            }
            ContestToggle(contest, onContest)
        }
        Spacer(Modifier.height(8.dp))
        LiveBadge(
            when (board.salarySource) {
                SalarySource.IMPORTED -> "Imported slate"
                SalarySource.EXAMPLE_FILE -> "EXAMPLE file"
                SalarySource.EXAMPLE_FORMULA -> "EXAMPLE salaries"
            },
        )
        Spacer(Modifier.height(6.dp))
        val selected = board.slates.firstOrNull { it.id == board.selectedSlateId }
        StubButton(
            label = "Choose slate · ${selected?.title ?: "Main"}",
            onClick = onChooseSlate,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(board.fdApiNote, color = TextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(board.salaryNote, color = TextMuted, fontSize = 12.sp)
        if (board.optimizeError != null && board.lineups.isEmpty()) {
            Text(board.optimizeError, color = TextMuted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Import slate", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onImport))
            if (board.salarySource != SalarySource.EXAMPLE_FORMULA) {
                Text("Use formula", color = TextMuted, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onClearImport))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LeverCard(
                title = "STACK LEVER",
                value = "Stack $stackDots",
                icon = Icons.Outlined.Layers,
                filled = stackDots,
                onClick = onStack,
                modifier = Modifier.weight(1f),
            )
            LeverCard(
                title = "OWN LEVER",
                value = ownLabel(ownDots),
                icon = Icons.Outlined.Person,
                filled = ownDots,
                onClick = onOwn,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(14.dp))
        if (lineups.isEmpty()) {
            Text("Optimizer produced no legal lineups.", color = TextMuted)
        } else {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
            ) { page ->
                val lineup = lineups.getOrNull(page)
                if (lineup != null) {
                    LineupCard(lineup = lineup, selected = pagerState.currentPage == page)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous", tint = TextPrimary)
                }
                Text("Lineup ${pagerState.currentPage + 1} of ${lineups.size}", color = TextPrimary, fontWeight = FontWeight.Medium)
                IconButton(onClick = {
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(lineups.lastIndex)) }
                }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next", tint = TextPrimary)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                lineups.forEachIndexed { index, lineup ->
                    LineupShortcut(lineup, index == pagerState.currentPage) {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Points from our SP K outlook + batter HR/counting model, mapped to FanDuel MLB scoring. " +
                "Copy uses the first visible lineup unless you export all.",
            color = TextMuted,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
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
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < filled) AccentGreen else CardStroke))
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
            "Proj ${String.format(Locale.US, "%.1f", lineup.proj)}  •  Ceiling ${lineup.ceiling}  •  ${lineup.stackNote}",
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(player.pos, color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
                Text(player.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
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
        LineupKind.STACK_A -> null to lineup.stackNote.take(3)
        LineupKind.STACK_B -> null to lineup.stackNote.take(3)
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
                LineupKind.STACK_A -> "Stk"
                LineupKind.STACK_B -> "Stk"
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

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("pablitos-dfs", text))
}
