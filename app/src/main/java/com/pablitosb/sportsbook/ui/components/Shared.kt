package com.pablitosb.sportsbook.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pablitosb.sportsbook.data.model.Outlook
import com.pablitosb.sportsbook.theme.AccentGreen
import com.pablitosb.sportsbook.theme.CardFill
import com.pablitosb.sportsbook.theme.CardStroke
import com.pablitosb.sportsbook.theme.ChipFill
import com.pablitosb.sportsbook.theme.NavySurface
import com.pablitosb.sportsbook.theme.RegRed
import com.pablitosb.sportsbook.theme.ScriptFamily
import com.pablitosb.sportsbook.theme.StableSlate
import com.pablitosb.sportsbook.theme.TextMuted
import com.pablitosb.sportsbook.theme.TextPrimary
import java.util.Locale

fun slateDateLabel(date: LocalDate = LocalDate.now()): String {
    return date.format(DateTimeFormatter.ofPattern("EEE MMM d", Locale.US))
}

fun updatedLabel(instant: Instant, zone: ZoneId): String {
    val local = instant.atZone(zone)
    return "Updated " + local.format(DateTimeFormatter.ofPattern("h:mm a z", Locale.US))
}

fun initialsFor(name: String): String {
    val parts = name.split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.isNotEmpty() -> parts.first().take(2)
        else -> "?"
    }.uppercase(Locale.US)
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 34.dp else 44.dp)
                .border(1.5.dp, AccentGreen, RoundedCornerShape(8.dp))
                .background(NavySurface, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "PS",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 13.sp else 16.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "Pablito's",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 14.sp else 16.sp,
            )
            Text(
                text = "Sportsbook",
                color = AccentGreen,
                fontFamily = ScriptFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = if (compact) 18.sp else 22.sp,
                lineHeight = if (compact) 18.sp else 22.sp,
            )
        }
    }
}

@Composable
fun DateChip(
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now(),
) {
    Row(
        modifier = modifier
            .border(1.dp, AccentGreen.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = slateDateLabel(date),
            color = AccentGreenSoftText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private val AccentGreenSoftText = Color(0xFF86EFAC)

@Composable
fun ScreenTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ShowChart,
            contentDescription = null,
            tint = AccentGreen,
            modifier = Modifier.size(22.dp),
        )
    },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = AccentGreen,
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            BrandMark(compact = true)
        }
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(content = trailing)
        }
    }
}

@Composable
fun PlayerAvatar(
    name: String,
    team: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(modifier = modifier.size(size + 4.dp)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(ChipFill)
                .border(1.dp, AccentGreen.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsFor(name),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.32f).sp,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(size * 0.42f)
                .clip(CircleShape)
                .background(NavySurface)
                .border(1.dp, AccentGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = team.take(3),
                color = AccentGreen,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun OutlookChip(outlook: Outlook) {
    val (label, color) = when (outlook) {
        Outlook.PROG -> "PROG" to AccentGreen
        Outlook.STABLE -> "STABLE" to StableSlate
        Outlook.REG -> "REG" to RegRed
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.lastIndex)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range) * size.height * 0.85f - size.height * 0.08f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )
        val last = values.last()
        val lastX = size.width
        val lastY = size.height - ((last - min) / range) * size.height * 0.85f - size.height * 0.08f
        drawCircle(color, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Composable
fun ConfidenceMeter(filled: Int, total: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        repeat(total) { index ->
            val on = index < filled
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((8 + index * 2).dp)
                    .background(
                        if (on) AccentGreen else CardStroke,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
fun StubButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (filled) AccentGreen else CardFill)
            .border(1.dp, if (filled) AccentGreen else CardStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            color = if (filled) Color(0xFF052E16) else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun SectionRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardStroke),
    )
}

@Composable
fun LiveBadge(label: String) {
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
fun SlateDateNavBar(
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
fun SlateLoading(date: LocalDate) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text("Loading MLB slate for ${slateDateLabel(date)}…", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
fun SlateMessage(
    title: String,
    body: String,
    onRetry: () -> Unit,
    fetchedAt: Instant? = null,
    zone: ZoneId? = null,
    badge: String? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) LiveBadge(badge)
        Spacer(Modifier.height(16.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(body, color = TextMuted, fontSize = 14.sp)
        if (fetchedAt != null && zone != null) {
            Spacer(Modifier.height(6.dp))
            Text(updatedLabel(fetchedAt, zone), color = TextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        StubButton(label = "Retry", onClick = onRetry, filled = true)
        if (extra != null) {
            Spacer(Modifier.height(10.dp))
            extra()
        }
    }
}
