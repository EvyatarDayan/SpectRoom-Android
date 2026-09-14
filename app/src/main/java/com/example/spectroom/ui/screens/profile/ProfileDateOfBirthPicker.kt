package com.example.spectroom.ui.screens.profile

import android.text.format.DateFormat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spectroom.ui.theme.LocalSpectRoomColors
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val WheelHeight = 132.dp
private val WheelItemHeight = 44.dp

fun maxDateOfBirthMillis(): Long =
    Calendar.getInstance().apply {
        add(Calendar.YEAR, -18)
        startOfDay()
    }.timeInMillis

fun minDateOfBirthMillis(): Long =
    Calendar.getInstance().apply {
        add(Calendar.YEAR, -100)
        startOfDay()
    }.timeInMillis

@Composable
fun ProfileDateOfBirthWheel(
    date: Date,
    onDateChange: (Date) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val context = LocalContext.current
    val minCal = remember {
        Calendar.getInstance().apply {
            timeInMillis = minDateOfBirthMillis()
            startOfDay()
        }
    }
    val maxCal = remember {
        Calendar.getInstance().apply {
            timeInMillis = maxDateOfBirthMillis()
            startOfDay()
        }
    }
    val selected = remember(date.time) {
        Calendar.getInstance().apply {
            time = date
            startOfDay()
        }
    }
    val year = selected.get(Calendar.YEAR)
    val month = selected.get(Calendar.MONTH)
    val day = selected.get(Calendar.DAY_OF_MONTH)

    val years = remember(minCal, maxCal) {
        (minCal.get(Calendar.YEAR)..maxCal.get(Calendar.YEAR)).toList()
    }
    val months = remember(year, minCal, maxCal) {
        val start = if (year == minCal.get(Calendar.YEAR)) minCal.get(Calendar.MONTH) else 0
        val end = if (year == maxCal.get(Calendar.YEAR)) maxCal.get(Calendar.MONTH) else 11
        (start..end).toList()
    }
    val days = remember(year, month, minCal, maxCal) {
        val probe = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            startOfDay()
        }
        val last = probe.getActualMaximum(Calendar.DAY_OF_MONTH)
        val start = if (year == minCal.get(Calendar.YEAR) && month == minCal.get(Calendar.MONTH)) {
            minCal.get(Calendar.DAY_OF_MONTH)
        } else {
            1
        }
        val end = if (year == maxCal.get(Calendar.YEAR) && month == maxCal.get(Calendar.MONTH)) {
            minOf(last, maxCal.get(Calendar.DAY_OF_MONTH))
        } else {
            last
        }
        (start..end).toList()
    }
    val monthNames = remember {
        DateFormatSymbols.getInstance().months.take(12)
    }
    val order = remember(context) {
        DateFormat.getDateFormatOrder(context).toList()
    }

    fun emit(nextYear: Int, nextMonth: Int, nextDay: Int) {
        onDateChange(clampedBirthDate(nextYear, nextMonth, nextDay, minCal, maxCal).time)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(WheelHeight)
            .clip(RoundedCornerShape(ProfileFieldStyle.fieldCorner))
            .background(colors.card)
            .border(1.dp, colors.text.copy(alpha = 0.08f), RoundedCornerShape(ProfileFieldStyle.fieldCorner)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(WheelItemHeight)
                .background(colors.text.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            order.forEach { part ->
                when (part) {
                    'M' -> WheelColumn(
                        items = months.map { monthNames[it] },
                        selectedIndex = months.indexOf(month).coerceAtLeast(0),
                        onSelectedIndexChange = { emit(year, months[it], day) },
                        modifier = Modifier.weight(1.35f),
                    )
                    'd' -> WheelColumn(
                        items = days.map { it.toString() },
                        selectedIndex = days.indexOf(day).coerceAtLeast(0),
                        onSelectedIndexChange = { emit(year, month, days[it]) },
                        modifier = Modifier.weight(0.8f),
                    )
                    'y' -> WheelColumn(
                        items = years.map { it.toString() },
                        selectedIndex = years.indexOf(year).coerceAtLeast(0),
                        onSelectedIndexChange = { emit(years[it], month, day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalSpectRoomColors.current
    val itemHeightPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    val lastIndex = (items.size - 1).coerceAtLeast(0)
    var offset by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    val scope = rememberCoroutineScope()
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val onSelectedIndexChangeState = rememberUpdatedState(onSelectedIndexChange)

    LaunchedEffect(selectedIndex, items.size) {
        val clamped = selectedIndex.coerceIn(0, lastIndex).toFloat()
        if (abs(offset - clamped) >= 0.01f) {
            offset = clamped
        }
    }

    Box(
        modifier
            .fillMaxHeight()
            .clipToBounds()
            .pointerInput(items, itemHeightPx, lastIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var current = offset
                    drag(down.id) { change ->
                        val dy = change.positionChange().y
                        if (dy == 0f) return@drag
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        current = (current - dy / itemHeightPx).coerceIn(0f, lastIndex.toFloat())
                        offset = current
                    }
                    val projected = current - (tracker.calculateVelocity().y / itemHeightPx) * 0.12f
                    val target = projected.roundToInt().coerceIn(0, lastIndex)
                    scope.launch {
                        animate(
                            initialValue = current,
                            targetValue = target.toFloat(),
                            animationSpec = spring(
                                dampingRatio = 0.92f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ) { value, _ ->
                            offset = value
                        }
                        if (target != selectedIndexState.value) {
                            onSelectedIndexChangeState.value(target)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (items.isEmpty()) return@Box
        val center = offset
        val first = (center.toInt() - 2).coerceAtLeast(0)
        val last = (center.toInt() + 3).coerceAtMost(items.lastIndex)
        for (index in first..last) {
            val distance = index - center
            val selected = abs(distance) < 0.5f
            Text(
                text = items[index],
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .graphicsLayer { translationY = distance * itemHeightPx },
                textAlign = TextAlign.Center,
                fontSize = if (selected) 18.sp else 16.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = colors.text.copy(alpha = (1f - abs(distance) * 0.42f).coerceIn(0.28f, 1f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Calendar.startOfDay() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

private fun clampedBirthDate(
    year: Int,
    month: Int,
    day: Int,
    minCal: Calendar,
    maxCal: Calendar,
): Calendar {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        startOfDay()
        val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
    }
    return when {
        cal.before(minCal) -> minCal.clone() as Calendar
        cal.after(maxCal) -> maxCal.clone() as Calendar
        else -> cal
    }
}
