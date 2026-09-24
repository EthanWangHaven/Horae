@file:OptIn(ExperimentalMaterial3Api::class)

package com.horae.app.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horae.app.alarm.ReminderScheduler
import com.horae.app.data.AppDatabase
import com.horae.app.data.RepeatType
import com.horae.app.data.ScheduleEntity
import com.horae.app.logic.ScheduleLogic
import com.horae.app.ui.common.SettingsRow
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Hairline
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.ScheduleColors
import com.horae.app.ui.theme.SubText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Picker { StartDate, StartTime, EndDate, EndTime, Repeat, Remind, Delete }

/** 添加/编辑日程页：标题/位置/全天/开始/结束/重复/提醒/备注/颜色 */
@Composable
fun AddEditScreen(
    scheduleId: Long?,
    initialMillis: Long,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = AppDatabase.get(context).scheduleDao()

    // ---------- 表单状态 ----------
    var initDone by remember { mutableStateOf(scheduleId == null) }
    var loaded by remember { mutableStateOf<ScheduleEntity?>(null) }

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var allDay by remember { mutableStateOf(false) }
    var startDate by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialMillis).toLocalDate())
    }
    var startTime by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialMillis).toLocalTime())
    }
    var endDate by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialMillis).toLocalDate())
    }
    var endTime by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialMillis).toLocalTime().plusHours(1))
    }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var remindMinutes by remember { mutableStateOf(-1) }
    var colorIndex by remember { mutableStateOf(0) }

    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            val s = dao.getById(scheduleId)
            if (s != null) {
                loaded = s
                title = s.title
                location = s.location.orEmpty()
                note = s.note.orEmpty()
                allDay = s.allDay
                val st = ScheduleLogic.toLocal(s.startTime)
                val et = ScheduleLogic.toLocal(s.endTime)
                startDate = st.toLocalDate(); startTime = st.toLocalTime()
                endDate = et.toLocalDate(); endTime = et.toLocalTime()
                repeatType = s.repeatType
                remindMinutes = s.remindMinutes
                colorIndex = s.colorIndex
            }
            initDone = true
        }
    }

    var picker by remember { mutableStateOf<Picker?>(null) }
    // f2: 时间校验错误提示
    var timeError by remember { mutableStateOf(false) }
    // 标题必填校验错误提示
    var titleError by remember { mutableStateOf(false) }

    // ---------- 保存 ----------
    fun save() {
        // 标题必填：不填则拦截保存并提示
        if (title.isBlank()) {
            titleError = true
            return
        }
        val startDt = if (allDay) startDate.atStartOfDay() else startDate.atTime(startTime)
        // f2: 校验——非全天时结束时间必须晚于开始时间，否则拦截保存
        if (!allDay && !endDate.atTime(endTime).isAfter(startDt)) {
            timeError = true
            return
        }
        val endDt = if (allDay) endDate.atTime(23, 59, 59) else endDate.atTime(endTime)
        val entity = ScheduleEntity(
            id = loaded?.id ?: 0,
            title = title.trim(),
            location = location.trim().takeIf { it.isNotEmpty() },
            allDay = allDay,
            startTime = ScheduleLogic.toMillis(startDt),
            endTime = ScheduleLogic.toMillis(endDt),
            repeatType = repeatType,
            remindMinutes = remindMinutes,
            note = note.trim().takeIf { it.isNotEmpty() },
            colorIndex = colorIndex,
        )
        scope.launch {
            val id = dao.insert(entity)
            val useId = if (entity.id != 0L) entity.id else id
            ReminderScheduler.schedule(context, useId, entity.startTime, entity.remindMinutes, entity.allDay)
            onDone()
        }
    }

    if (!initDone) {
        // 编辑加载中：空玻璃屏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) { Text(text = "加载中…", color = SubText) }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        // ---------- 顶栏：取消 | 标题 | 保存 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "取消",
                color = AccentBlue,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickableNoRipple(onCancel)
                    .padding(8.dp),
            )
            Text(
                text = if (loaded != null) "编辑日程" else "添加日程",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = "保存",
                color = AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickableNoRipple { save() }
                    .padding(8.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- 标题 ----------
            BasicTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 10.dp),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) {
                            Text("标题", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = SubText.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )
            // 标题必填错误提示
            if (titleError) {
                Text(
                    text = "请填写标题",
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp),
                )
            }

            // ---------- 位置 ----------
            BasicTextField(
                value = location,
                onValueChange = { location = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                decorationBox = { inner ->
                    Box {
                        if (location.isEmpty()) {
                            Text("位置", fontSize = 16.sp, color = SubText.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )

            // ---------- 分组卡：全天/开始/结束/重复/提醒 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
                    .padding(horizontal = 16.dp, vertical = 2.dp),
            ) {
                SettingsRow(
                    icon = null,
                    label = "全天",
                    trailing = {
                        Switch(
                            checked = allDay,
                            onCheckedChange = {
                                allDay = it
                                timeError = false // f2: 全天无需校验，清除错误
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue),
                        )
                    },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = "开始",
                    value = if (allDay) ScheduleLogic.listTitle(startDate)
                    else "${ScheduleLogic.listTitle(startDate)} ${ScheduleLogic.hm(startTime)}",
                    onClick = { picker = Picker.StartDate },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = "结束",
                    value = if (allDay) ScheduleLogic.listTitle(endDate)
                    else "${ScheduleLogic.listTitle(endDate)} ${ScheduleLogic.hm(endTime)}",
                    onClick = { picker = Picker.EndDate },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = "重复",
                    value = ScheduleLogic.repeatLabel(repeatType),
                    onClick = { picker = Picker.Repeat },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = "提醒",
                    value = ScheduleLogic.remindLabel(remindMinutes),
                    onClick = { picker = Picker.Remind },
                )
            }

            // f2: 时间校验错误提示
            if (timeError) {
                Text(
                    text = "结束时间必须晚于开始时间",
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            // ---------- 备注 ----------
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = TextStyle(fontSize = 15.sp, color = Ink, lineHeight = 22.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .liquidGlass(shape = RoundedCornerShape(16.dp), tintAlpha = 0.55f, blurRadius = 18.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box(modifier = Modifier.height(64.dp)) {
                        if (note.isEmpty()) {
                            Text("备注", fontSize = 15.sp, color = SubText.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )

            // ---------- 颜色 ----------
            Text(
                text = "颜色",
                fontSize = 13.sp,
                color = SubText,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScheduleColors.forEachIndexed { idx, c ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (idx == colorIndex) Modifier.border(
                                    width = 2.dp,
                                    color = Ink,
                                    shape = CircleShape,
                                ) else Modifier
                            )
                            .clickableNoRipple { colorIndex = idx },
                    )
                    Spacer(Modifier.width(12.dp))
                }
            }

            // ---------- 删除（编辑模式） ----------
            if (loaded != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .liquidGlass(shape = RoundedCornerShape(16.dp), tintAlpha = 0.6f, blurRadius = 18.dp)
                        .clickableNoRipple { picker = Picker.Delete }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "删除日程", color = Color(0xFFFF3B30), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    // ---------- 弹层 ----------
    when (picker) {
        Picker.StartDate -> DateDialog(
            initial = startDate,
            onConfirm = { d ->
                startDate = d
                if (endDate.isBefore(d)) endDate = d
                picker = if (allDay) null else Picker.StartTime
            },
            onDismiss = { picker = null },
        )
        Picker.StartTime -> TimeDialog(
            initial = startTime,
            onConfirm = { t ->
                startTime = t
                picker = null
                // f2: 实时校验（不自动改值）
                timeError = !allDay && !endDate.atTime(endTime).isAfter(startDate.atTime(t))
            },
            onDismiss = { picker = null },
        )
        Picker.EndDate -> DateDialog(
            initial = endDate,
            onConfirm = { d ->
                endDate = d
                picker = if (allDay) null else Picker.EndTime
            },
            onDismiss = { picker = null },
        )
        Picker.EndTime -> TimeDialog(
            initial = endTime,
            onConfirm = { t ->
                endTime = t
                picker = null
                // f2: 实时校验（不自动改值）
                timeError = !allDay && !endDate.atTime(t).isAfter(startDate.atTime(startTime))
            },
            onDismiss = { picker = null },
        )
        Picker.Repeat -> OptionsDialog(
            title = "重复",
            options = ScheduleLogic.repeatOptions.map { ScheduleLogic.repeatLabel(it) },
            selectedIndex = ScheduleLogic.repeatOptions.indexOf(repeatType),
            onSelect = { idx -> repeatType = ScheduleLogic.repeatOptions[idx]; picker = null },
            onDismiss = { picker = null },
        )
        Picker.Remind -> OptionsDialog(
            title = "提醒",
            options = ScheduleLogic.remindOptions.map { ScheduleLogic.remindLabel(it) },
            selectedIndex = ScheduleLogic.remindOptions.indexOf(remindMinutes),
            onSelect = { idx -> remindMinutes = ScheduleLogic.remindOptions[idx]; picker = null },
            onDismiss = { picker = null },
        )
        Picker.Delete -> AlertDialog(
            onDismissRequest = { picker = null },
            title = { Text("删除日程？") },
            text = { Text("该日程及其提醒将被移除。") },
            confirmButton = {
                TextButton(onClick = {
                    val s = loaded
                    if (s != null) {
                        scope.launch {
                            dao.delete(s)
                            ReminderScheduler.cancel(context, s.id)
                            onDone()
                        }
                    }
                    picker = null
                }) { Text("删除", color = Color(0xFFFF3B30), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { picker = null }) { Text("取消", color = Ink) }
            },
        )
        null -> Unit
    }
}

@Composable
private fun FieldDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline)
    )
}

@Composable
private fun DateDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms != null) onConfirm(Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate())
            }) { Text("下一步", color = AccentBlue) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Ink) }
        },
    ) {
        DatePicker(
            state = state,
            // f3: 移除铅笔切换按钮；顶部日期改为小字号
            showModeToggle = false,
            headline = {
                val sel = state.selectedDateMillis
                Text(
                    text = sel?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
                    } ?: "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 8.dp),
                )
            },
        )
    }
}

/** f4: 时间选择——分钟只有整五整十可选（00/05/.../55），初始分钟自动吸附 */
@Composable
private fun TimeDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf((initial.minute / 5) * 5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(hour, minute)) }) {
                Text("确定", color = AccentBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Ink) }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Text(
                    text = "小时",
                    fontSize = 12.sp,
                    color = SubText,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 6.dp),
                )
                listOf(0..5, 6..11, 12..17, 18..23).forEach { range ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        range.forEach { h ->
                            TimeCell(text = "%02d".format(h), selected = hour == h) { hour = h }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = "分钟",
                    fontSize = 12.sp,
                    color = SubText,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 8.dp, bottom = 6.dp),
                )
                listOf(listOf(0, 5, 10, 15, 20, 25), listOf(30, 35, 40, 45, 50, 55)).forEach { list ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        list.forEach { m ->
                            TimeCell(text = "%02d".format(m), selected = minute == m) { minute = m }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
    )
}

@Composable
private fun TimeCell(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AccentBlue else Color.Transparent)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Ink,
        )
    }
}

@Composable
private fun OptionsDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Ink) }
        },
        text = {
            Column {
                options.forEachIndexed { idx, label ->
                    Text(
                        text = if (idx == selectedIndex) "✓ $label" else label,
                        color = if (idx == selectedIndex) AccentBlue else Ink,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoRipple { onSelect(idx) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
    )
}
