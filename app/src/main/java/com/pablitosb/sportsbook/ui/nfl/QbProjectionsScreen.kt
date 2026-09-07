package com.pablitosb.sportsbook.ui.nfl

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablitosb.sportsbook.data.nfl.QbProjection
import com.pablitosb.sportsbook.data.nfl.QbProjectionsRepository
import com.pablitosb.sportsbook.data.nfl.QbSort
import com.pablitosb.sportsbook.data.nfl.QbSorter
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.NavyBlack
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.StableSlate
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import com.pablitosb.sportsbook.ui.components.StubButton
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QbProjectionsScreen(
    onBack: () -> Unit,
    viewModel: QbProjectionsViewModel = viewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val liveLabel = when (val state = viewModel.ui) {
            is QbUiState.Ready -> state.board.sourceLabel
            is QbUiState.Empty -> state.sourceLabel
            else -> "EXAMPLE • ESPN"
        }
        QbTopBar(onBack = onBack, liveLabel = liveLabel)
        WeekNavBar(
            week = viewModel.week,
            canPrev = viewModel.week > viewModel.minWeek,
            canNext = viewModel.week < viewModel.maxWeek,
            onPrev = { viewModel.shiftWeek(-1) },
            onNext = { viewModel.shiftWeek(1) },
        )
        PullToRefreshBox(
            isRefreshing = viewModel.refreshing && viewModel.ui is QbUiState.Ready,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = viewModel.ui) {
                is QbUiState.Loading -> LoadingBody(viewModel.week)
                is QbUiState.Error -> MessageBody(
                    title = "Week unavailable",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                )
                is QbUiState.Empty -> MessageBody(
                    title = "No QBs posted",
                    body = state.message,
                    onRetry = { viewModel.refresh() },
                    fetchedAt = state.fetchedAt,
                    badge = state.sourceLabel,
                )
                is QbUiState.Ready -> ReadyList(state, viewModel)
            }
        }
    }
}

@Composable
private fun QbTopBar(onBack: () -> Unit, liveLabel: String) {
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
            "QB Projections Weekly",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
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
private fun WeekNavBar(
    week: Int,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
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
                contentDescription = "Previous week",
                tint = if (canPrev) AccentGreen else TextMuted,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Outlined.CalendarMonth, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Week $week",
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        IconButton(onClick = onNext, enabled = canNext) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Next week",
                tint = if (canNext) AccentGreen else TextMuted,
            )
        }
    }
}

@Composable
private fun ReadyList(state: QbUiState.Ready, viewModel: QbProjectionsViewModel) {
    val board = state.board
    val sorted = remember(board.qbs, viewModel.sortKey, viewModel.sortAscending) {
        QbSorter.sort(board.qbs, viewModel.sortKey, viewModel.sortAscending)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        item {
            FilterTabRow(selected = viewModel.sortKey, onSelect = { viewModel.selectSort(it) })
            Spacer(Modifier.height(8.dp))
            Text(
                "Matchup % = opponent pass YPA allowed vs league. IAY is a YPA-blend proxy (not NGS).",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp,
            )
            Text(updatedLabel(board.fetchedAt), color = TextMuted, fontSize = 10.sp)
            Spacer(Modifier.height(6.dp))
        }
        itemsIndexed(sorted, key = { _, it -> "${it.espnId}-${it.awayAbbr}-${it.homeAbbr}" }) { index, qb ->
            QbRow(qb, viewModel.sortKey, index + 1)
            Spacer(Modifier.height(8.dp))
        }
        item {
            Spacer(Modifier.height(8.dp))
            SideLegend()
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    board.note,
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
private fun FilterTabRow(selected: QbSort, onSelect: (QbSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(
            QbSort.PROJ_YDS to "Proj Yds",
            QbSort.PROJ_FD to "Proj FD",
            QbSort.COMP to "Comp%",
            QbSort.IAY to "IAY",
        ).forEach { (key, label) ->
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
                        .width(52.dp)
                        .height(3.dp)
                        .background(if (on) AccentGreen else Color.Transparent, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun QbRow(qb: QbProjection, sortKey: QbSort, displayRank: Int) {
    val matchColor = when {
        qb.matchupPct > 0.4f -> AccentGreen
        qb.matchupPct < -0.4f -> RegRed
        else -> TextMuted
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardFill, shape)
            .border(1.dp, CardStroke, shape)
            .padding(horizontal = 10.dp, vertical = 12.dp),
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
                qb.name,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AwayHomeTime(qb.awayAbbr, qb.homeAbbr, qb.gameTimeLabel)
        }
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(CardStroke),
        )
        SelectedStat(
            qb,
            sortKey,
            Modifier.weight(if (sortKey == QbSort.PROJ_FD) 1.35f else 0.85f),
        )
        Box(
            Modifier
                .width(1.dp)
                .height(36.dp)
                .background(CardStroke),
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text("Matchup", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Medium)
            Text(
                String.format(Locale.US, "%+.0f%%", qb.matchupPct),
                color = matchColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AwayHomeTime(away: String, home: String, time: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(away.ifBlank { "TBD" }, color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(" @ ", color = TextPrimary, fontSize = 11.sp)
        Text(home.ifBlank { "TBD" }, color = RegRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (time.isNotBlank()) {
            Text("  ·  $time", color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SideLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(AccentGreen, "Away")
        LegendDot(StableSlate, "Neutral")
        LegendDot(RegRed, "Home")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectedStat(qb: QbProjection, sortKey: QbSort, modifier: Modifier) {
    if (sortKey == QbSort.PROJ_FD) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FdCell("Floor", String.format(Locale.US, "%.1f", qb.fdFloor), TextPrimary, 13.sp)
            FdCell("Proj", String.format(Locale.US, "%.1f", qb.fdProj), AccentGreen, 18.sp)
            FdCell("Ceiling", String.format(Locale.US, "%.1f", qb.fdCeiling), TextPrimary, 13.sp)
        }
        return
    }
    val (label, value) = when (sortKey) {
        QbSort.PROJ_YDS -> "Proj Yds" to String.format(Locale.US, "%.0f", qb.projYds)
        QbSort.COMP -> "Comp%" to String.format(Locale.US, "%.1f%%", qb.compPct)
        QbSort.IAY -> "IAY" to String.format(Locale.US, "%.1f", qb.iay)
        QbSort.PROJ_FD -> "Proj FD" to ""
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            Text(label, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(end = 4.dp, bottom = 2.dp))
            Text(value, color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        }
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
            Text(label, color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun LoadingBody(week: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text("Loading week $week QBs…", color = TextMuted, fontSize = 13.sp)
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
    val local = instant.atZone(QbProjectionsRepository.ZONE)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}
