package com.horae.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horae.app.data.AppDatabase
import com.horae.app.logic.DayOccurrences
import com.horae.app.logic.Occurrence
import com.horae.app.logic.ScheduleLogic
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.ScheduleColors
import com.horae.app.ui.theme.SubText
import java.time.LocalDate

/** 日程列表页（图1）：按天展示，卡片含时间/标题/位置/重复标记，底部添加按钮 */
@Composable
fun ScheduleListScreen(
    initialDayMillis: Long,
    onBack: () -> Unit,
    onAdd: (dayMillis: Long) -> Unit,
    onEdit: (scheduleId: Long, dayMillis: Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val schedules by AppDatabase.get(context).scheduleDao()
        .observeAll().collectAsState(initial = emptyList())

    var selectedDay by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialDayMillis).toLocalDate())
    }

    val dayOcc = remember(selectedDay, schedules) {
        ScheduleLogic.expand(schedules, selectedDay, selectedDay).firstOrNull()
            ?: DayOccurrences(selectedDay, emptyList(), emptyList())
    }
    val items: List<Occurrence> = dayOcc.allDay + dayOcc.timed

    // 左右滑动切换上一天/下一天
    var dragAccum by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val threshold = 90.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onDragEnd = {
                        when {
                            dragAccum <= -threshold -> selectedDay = selectedDay.plusDays(1) // 左滑 → 下一天
                            dragAccum >= threshold -> selectedDay = selectedDay.minusDays(1) // 右滑 → 上一天
                        }
                    },
                ) { change, dragAmount ->
                    dragAccum += dragAmount
                    change.consume()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            ListHeader(
                day = selectedDay,
                onBack = onBack,
                onPrev = { selectedDay = selectedDay.minusDays(1) },
                onNext = { selectedDay = selectedDay.plusDays(1) },
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (items.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    items(items, key = { "${it.schedule.id}_${it.start}" }) { occ ->
                        ScheduleCard(
                            occ = occ,
                            onClick = { onEdit(occ.schedule.id, ScheduleLogic.toMillis(selectedDay.atStartOfDay())) },
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }

        // 底部添加按钮
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .liquidGlass(shape = RoundedCornerShape(50), tintAlpha = 0.75f, blurRadius = 20.dp)
                .clickableNoRipple { onAdd(ScheduleLogic.toMillis(selectedDay.atStartOfDay())) }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.padding(0.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "添加日程",
                color = AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ListHeader(
    day: LocalDate,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), tintAlpha = 0.6f, blurRadius = 20.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = AccentBlue,
            modifier = Modifier.clickableNoRipple(onBack),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ScheduleLogic.listTitle(day),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "${ScheduleLogic.weekdayHan(day)} · 第 ${ScheduleLogic.weekNumber(day)} 周",
                fontSize = 12.sp,
                color = SubText,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        DaySwitchText(text = "‹", onClick = onPrev)
        Spacer(Modifier.width(14.dp))
        DaySwitchText(text = "›", onClick = onNext)
    }
}

@Composable
private fun DaySwitchText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 26.sp,
        color = AccentBlue,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp) // 扩大点击热区，加大两按钮间距，避免误触
            .clickableNoRipple(onClick),
    )
}

/** 单条日程卡片（图1）：时间列 + 色条 + 标题/位置/重复 */
@Composable
private fun ScheduleCard(occ: Occurrence, onClick: () -> Unit) {
    val color = ScheduleColors[occ.schedule.colorIndex % ScheduleColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            if (occ.schedule.allDay) {
                Text(text = "全天", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            } else {
                Text(
                    text = ScheduleLogic.hm(occ.start),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                )
                val endText = if (occ.end.toLocalDate().isAfter(occ.start.toLocalDate()))
                    "次日 ${ScheduleLogic.hm(occ.end.toLocalTime())}" else ScheduleLogic.hm(occ.end)
                Text(text = endText, fontSize = 12.sp, color = SubText, modifier = Modifier.padding(top = 2.dp))
            }
        }

        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .background(color.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = occ.schedule.title.ifBlank { "日程" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            occ.schedule.location?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, fontSize = 13.sp, color = SubText, modifier = Modifier.padding(top = 2.dp))
            }
            if (occ.schedule.repeatType != com.horae.app.data.RepeatType.NONE) {
                Text(
                    text = ScheduleLogic.repeatLabel(occ.schedule.repeatType),
                    fontSize = 12.sp,
                    color = SubText,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** 空状态占位卡（图1 中部的占位） */
@Composable
private fun EmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(24.dp), tintAlpha = 0.5f, blurRadius = 22.dp)
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "◦ ◦ ◦", fontSize = 22.sp, color = SubText)
        Text(
            text = "这一天还没有日程",
            fontSize = 15.sp,
            color = Ink,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = "点击下方「添加日程」开始安排",
            fontSize = 12.sp,
            color = SubText,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
