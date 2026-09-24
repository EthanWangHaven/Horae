package com.horae.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horae.app.data.AppDatabase
import com.horae.app.data.ScheduleEntity
import com.horae.app.ui.common.GlassScreenRoot
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.ScheduleColors
import com.horae.app.ui.theme.SubText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("M/d")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

/** 日程搜索页：按标题/地点/备注匹配 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onEdit: (scheduleId: Long, startMillis: Long) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val schedules by AppDatabase.get(context).scheduleDao()
        .observeAll().collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }

    val results = remember(query, schedules) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else schedules.filter { s ->
            s.title.contains(q, true) ||
                (s.location?.contains(q, true) == true) ||
                (s.note?.contains(q, true) == true)
        }.sortedBy { it.startTime }
    }

    GlassScreenRoot {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // ---------- 顶部：返回 + 搜索框 ----------
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
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SubText,
                    modifier = Modifier.height(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(text = "搜索标题、地点、备注", fontSize = 15.sp, color = SubText)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = Ink),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---------- 结果列表 ----------
            when {
                query.trim().isEmpty() -> HintText("输入关键词搜索日程")
                results.isEmpty() -> HintText("无匹配日程")
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(results, key = { it.id }) { s ->
                        SearchResultCard(schedule = s, onClick = { onEdit(s.id, s.startTime) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = SubText,
        modifier = Modifier.padding(top = 32.dp).fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun SearchResultCard(schedule: ScheduleEntity, onClick: () -> Unit) {
    val color = ScheduleColors[schedule.colorIndex % ScheduleColors.size]
    val zone = ZoneId.systemDefault()
    val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(schedule.startTime), zone)
    val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(schedule.endTime), zone)
    val day: LocalDate = start.toLocalDate()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
            .clickableNoRipple(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧日期列
        Column(
            modifier = Modifier.width(52.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = day.format(dayFmt),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
            )
            val dow = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[day.dayOfWeek.value - 1]
            Text(text = dow, fontSize = 12.sp, color = SubText, modifier = Modifier.padding(top = 2.dp))
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
                text = schedule.title.ifBlank { "日程" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            val timeText = if (schedule.allDay) "全天"
            else "${start.format(timeFmt)} – ${end.format(timeFmt)}"
            Text(text = timeText, fontSize = 13.sp, color = SubText, modifier = Modifier.padding(top = 2.dp))
            schedule.location?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = SubText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}
