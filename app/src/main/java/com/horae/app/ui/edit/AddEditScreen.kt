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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import com.horae.app.alarm.ReminderScheduler
import com.horae.app.data.AppDatabase
import com.horae.app.data.AppSettings
import com.horae.app.data.RepeatEnd
import com.horae.app.data.RepeatFreq
import com.horae.app.data.RepeatType
import com.horae.app.data.ScheduleEntity
import com.horae.app.logic.ScheduleLogic
import com.horae.app.ui.common.SettingsRow
import com.horae.app.ui.common.GlassScreenRoot
import com.horae.app.ui.common.GlassDialog
import com.horae.app.ui.common.DialogActions
import com.horae.app.ui.common.IOSSwitch
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.common.strings
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private enum class Picker { StartDate, StartTime, EndDate, EndTime, Repeat, CustomRepeat, RepeatEnd, RepeatEndDate, RepeatCount, Remind, Delete }

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
    val s = strings()
    val lang = com.horae.app.data.AppSettings.languageIndex
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
    var repeatFreq by remember { mutableIntStateOf(RepeatFreq.WEEK) }
    var repeatInterval by remember { mutableIntStateOf(1) }
    var repeatEndType by remember { mutableIntStateOf(RepeatEnd.NEVER) }
    var repeatEndDate by remember {
        mutableStateOf(ScheduleLogic.toLocal(initialMillis).toLocalDate())
    }
    var repeatCount by remember { mutableIntStateOf(10) }
    var remindMinutes by remember { mutableStateOf(-1) }
    var colorIndex by remember { mutableStateOf(0) }
    // 看板条显示开关
    var showStartTime by remember { mutableStateOf(true) }
    var showEndTime by remember { mutableStateOf(false) }
    var showLocation by remember { mutableStateOf(false) }

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
                repeatFreq = s.repeatFreq
                repeatInterval = s.repeatInterval
                repeatEndType = s.repeatEndType
                if (s.repeatEndDate > 0L) repeatEndDate = ScheduleLogic.toLocal(s.repeatEndDate).toLocalDate()
                repeatCount = s.repeatCount
                remindMinutes = s.remindMinutes
                colorIndex = s.colorIndex
                showStartTime = s.showStartTime
                showEndTime = s.showEndTime
                showLocation = s.showLocation
            }
            initDone = true
        }
    }

    var picker by remember { mutableStateOf<Picker?>(null) }
    // f2: 时间校验错误提示
    var timeError by remember { mutableStateOf(false) }
    // 标题必填校验错误提示
    var titleError by remember { mutableStateOf(false) }
    // 提醒权限引导（保存带提醒的日程后，若系统权限缺失则弹出）
    var showReminderGuide by remember { mutableStateOf(false) }
    var pendingDone by remember { mutableStateOf(false) }
    // 看板条显示溢出提示（保存时判断，选择结果记入 AppSettings.boardOverflowMode）
    var showOverflowSaveDialog by remember { mutableStateOf(false) }
    // 时间段重合提示（与其他日程区间重叠时拦截保存）
    var overlapError by remember { mutableStateOf<String?>(null) }

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
        val newStart = ScheduleLogic.toMillis(startDt)
        val newEnd = ScheduleLogic.toMillis(endDt)
        scope.launch {
            // 时间段重合校验：同一时间只能做一件事，与其他日程区间重叠则拦截
            val selfId = loaded?.id ?: 0L
            val rangeStart = ScheduleLogic.toLocal(newStart).toLocalDate().minusDays(1)
            val rangeEnd = ScheduleLogic.toLocal(newEnd).toLocalDate().plusDays(1)
            val conflict = ScheduleLogic.expand(dao.getAll(), rangeStart, rangeEnd)
                .flatMap { it.allDay + it.timed }
                .filter { it.schedule.id != selfId }
                .firstOrNull {
                    ScheduleLogic.toMillis(it.start) < newEnd &&
                        ScheduleLogic.toMillis(it.end) > newStart
                }
            if (conflict != null) {
                overlapError = s.overlapBefore + conflict.schedule.title.ifBlank { s.scheduleFallback } +
                    s.overlapMid + ScheduleLogic.hm(conflict.start) + "–" + ScheduleLogic.hm(conflict.end) +
                    s.overlapAfter
                return@launch
            }
            val entity = ScheduleEntity(
                id = loaded?.id ?: 0,
                title = title.trim(),
                location = location.trim().takeIf { it.isNotEmpty() },
                allDay = allDay,
                startTime = newStart,
                endTime = newEnd,
                repeatType = repeatType,
                repeatFreq = repeatFreq,
                repeatInterval = repeatInterval,
                repeatEndType = repeatEndType,
                repeatEndDate = if (repeatEndType == RepeatEnd.UNTIL)
                    ScheduleLogic.toMillis(repeatEndDate.atTime(23, 59, 59)) else 0L,
                repeatCount = repeatCount,
                remindMinutes = remindMinutes,
                note = note.trim().takeIf { it.isNotEmpty() },
                colorIndex = colorIndex,
                showStartTime = showStartTime,
                showEndTime = showEndTime,
                showLocation = showLocation,
            )
            val id = dao.insert(entity)
            val useId = if (entity.id != 0L) entity.id else id
            ReminderScheduler.schedule(context, useId, entity.startTime, entity.remindMinutes, entity.allDay)
            // 保存时估算看板条空间：勾选的显示内容放不下则提示（自动适配 / 仍然显示）
            val dm = context.resources.displayMetrics
            val hourHeightPx = (dm.widthPixels - 52 * dm.density) / 7f
            val blockH = ((newEnd - newStart) / 3600000f).coerceAtLeast(0.5f) * hourHeightPx
            val extraCount = listOf(showStartTime, showEndTime, showLocation && location.isNotBlank()).count { it }
            val overflow = !allDay && extraCount > 0 && blockH <= 90f + extraCount * 34f
            // 带提醒的日程：保存后检查系统权限，缺失则引导开启（MIUI 等厂商 ROM 尤其需要）
            if (entity.remindMinutes >= 0 && !entity.allDay && reminderGuideItems(context, s).isNotEmpty()) {
                pendingDone = true
                showReminderGuide = true
            } else if (overflow && AppSettings.boardOverflowMode != 2) {
                pendingDone = true
                showOverflowSaveDialog = true
            } else {
                onDone()
            }
        }
    }

    if (!initDone) {
        // 编辑加载中：空玻璃屏
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) { Text(text = s.loading, color = SubText) }
        return
    }

    GlassScreenRoot {
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
                text = s.cancel,
                color = AccentBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickableNoRipple(onCancel)
                    .padding(8.dp),
            )
            Text(
                text = if (loaded != null) s.editSchedule else s.addSchedule,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text = s.save,
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
                            Text(s.titlePlaceholder, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = SubText.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )
            // 标题必填错误提示
            if (titleError) {
                Text(
                    text = s.titleRequired,
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
                            Text(s.locationPlaceholder, fontSize = 16.sp, color = SubText.copy(alpha = 0.6f))
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SettingsRow(
                    icon = null,
                    label = s.allDay,
                    trailing = {
                        IOSSwitch(
                            checked = allDay,
                            onCheckedChange = {
                                allDay = it
                                timeError = false // f2: 全天无需校验，清除错误
                            },
                        )
                    },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.start,
                    value = if (allDay) ScheduleLogic.listTitle(startDate, lang)
                    else "${ScheduleLogic.listTitle(startDate, lang)} ${ScheduleLogic.hm(startTime)}",
                    onClick = { picker = Picker.StartDate },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.end,
                    value = if (allDay) ScheduleLogic.listTitle(endDate, lang)
                    else "${ScheduleLogic.listTitle(endDate, lang)} ${ScheduleLogic.hm(endTime)}",
                    onClick = { picker = Picker.EndDate },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.repeat,
                    value = if (repeatType == RepeatType.CUSTOM)
                        ScheduleLogic.customRepeatLabel(repeatFreq, repeatInterval, lang)
                    else ScheduleLogic.repeatLabel(repeatType, lang),
                    onClick = { picker = Picker.Repeat },
                )
                if (repeatType != RepeatType.NONE) {
                    FieldDivider()
                    SettingsRow(
                        icon = null,
                        label = s.repeatEnd,
                        value = when (repeatEndType) {
                            RepeatEnd.UNTIL -> ScheduleLogic.listTitle(repeatEndDate, lang)
                            RepeatEnd.COUNT -> s.timesFmt(repeatCount)
                            else -> s.never
                        },
                        onClick = { picker = Picker.RepeatEnd },
                    )
                }
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.remind,
                    value = ScheduleLogic.remindLabel(remindMinutes, lang),
                    onClick = { picker = Picker.Remind },
                )
            }

            // f2: 时间校验错误提示
            if (timeError) {
                Text(
                    text = s.endAfterStart,
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            // 时间段与其他日程重合提示
            overlapError?.let {
                Text(
                    text = it,
                    color = Color(0xFFFF3B30),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
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
                    .padding(top = 12.dp)
                    .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    Box(modifier = Modifier.height(64.dp)) {
                        if (note.isEmpty()) {
                            Text(s.notePlaceholder, fontSize = 15.sp, color = SubText.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                },
            )

            // ---------- 颜色（设置页卡片样式） ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(text = s.color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(12.dp))
                // 8 个颜色均匀排满整行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
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
                    }
                }
            }

            // ---------- 看板显示 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SettingsRow(
                    icon = null,
                    label = s.boardDisplay,
                    value = null,
                    valuePlaceholder = null,
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.showStartTimeLabel,
                    trailing = {
                        IOSSwitch(
                            checked = showStartTime,
                            onCheckedChange = { showStartTime = it },
                        )
                    },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.showEndTimeLabel,
                    trailing = {
                        IOSSwitch(
                            checked = showEndTime,
                            onCheckedChange = { showEndTime = it },
                        )
                    },
                )
                FieldDivider()
                SettingsRow(
                    icon = null,
                    label = s.locationLabel,
                    trailing = {
                        IOSSwitch(
                            checked = showLocation,
                            onCheckedChange = { showLocation = it },
                        )
                    },
                )
            }

            // ---------- 删除（编辑模式） ----------
            if (loaded != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .liquidGlass(shape = RoundedCornerShape(20.dp), tintAlpha = 0.62f, blurRadius = 20.dp)
                        .clickableNoRipple { picker = Picker.Delete }
                        .padding(vertical = 14.dp),
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
                    Text(text = s.deleteSchedule, color = Color(0xFFFF3B30), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
    }

    // ---------- 看板条显示溢出提示（保存时） ----------
    if (showOverflowSaveDialog) {
        GlassDialog(
            onDismiss = {
                showOverflowSaveDialog = false
                if (pendingDone) {
                    pendingDone = false
                    if (AppSettings.boardOverflowMode == 0) AppSettings.boardOverflowMode = 1
                    onDone()
                }
            },
            compact = true,
        ) {
            Text(text = s.overflowTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(text = s.overflowMsg, fontSize = 14.sp, color = SubText, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = s.autoFit,
                    color = Ink,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickableNoRipple {
                            AppSettings.boardOverflowMode = 1
                            showOverflowSaveDialog = false
                            if (pendingDone) {
                                pendingDone = false
                                onDone()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue)
                        .clickableNoRipple {
                            AppSettings.boardOverflowMode = 2
                            showOverflowSaveDialog = false
                            if (pendingDone) {
                                pendingDone = false
                                onDone()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = s.showAnyway,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    // ---------- 提醒权限引导 ----------
    if (showReminderGuide) {
        val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        val items = reminderGuideItems(context, s)
        GlassDialog(onDismiss = { showReminderGuide = false; if (pendingDone) onDone() }) {
            Text(text = s.permTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(10.dp))
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(text = item.desc, fontSize = 12.sp, color = SubText)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue)
                            .clickableNoRipple {
                                when (item.type) {
                                    ReminderGuideType.NOTIFICATION ->
                                        permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    ReminderGuideType.AUTOSTART -> runCatching {
                                        context.startActivity(
                                            Intent()
                                                .setComponent(
                                                    ComponentName(
                                                        "com.miui.securitycenter",
                                                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                                    )
                                                )
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(text = s.goEnable, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = s.permFooter,
                fontSize = 12.sp,
                color = SubText,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(12.dp))
            DialogActions(
                onCancel = { showReminderGuide = false; if (pendingDone) onDone() },
                confirmText = s.done,
                onConfirm = { showReminderGuide = false; if (pendingDone) onDone() },
            )
        }
    }

    // ---------- 弹层 ----------
    when (picker) {
        Picker.StartDate -> DateDialog(
            initial = startDate,
            lang = lang,
            s = s,
            onConfirm = { d ->
                overlapError = null
                startDate = d
                if (endDate.isBefore(d)) endDate = d
                picker = if (allDay) null else Picker.StartTime
            },
            onDismiss = { picker = null },
        )
        Picker.StartTime -> TimeDialog(
            initial = startTime,
            lang = lang,
            s = s,
            onConfirm = { t ->
                overlapError = null
                startTime = t
                picker = null
                // f2: 实时校验（不自动改值）
                timeError = !allDay && !endDate.atTime(endTime).isAfter(startDate.atTime(t))
            },
            onDismiss = { picker = null },
        )
        Picker.EndDate -> DateDialog(
            initial = endDate,
            lang = lang,
            s = s,
            onConfirm = { d ->
                overlapError = null
                endDate = d
                picker = if (allDay) null else Picker.EndTime
            },
            onDismiss = { picker = null },
        )
        Picker.EndTime -> TimeDialog(
            initial = endTime,
            lang = lang,
            s = s,
            onConfirm = { t ->
                overlapError = null
                endTime = t
                picker = null
                // f2: 实时校验（不自动改值）
                timeError = !allDay && !endDate.atTime(t).isAfter(startDate.atTime(startTime))
            },
            onDismiss = { picker = null },
        )
        Picker.Repeat -> OptionsDialog(
            title = s.repeat,
            options = ScheduleLogic.repeatOptions.map { ScheduleLogic.repeatLabel(it, lang) },
            selectedIndex = ScheduleLogic.repeatOptions.indexOf(repeatType),
            onSelect = { idx ->
                val t = ScheduleLogic.repeatOptions[idx]
                if (t == RepeatType.CUSTOM) { picker = Picker.CustomRepeat } else { repeatType = t; picker = null }
            },
            onDismiss = { picker = null },
        )
        Picker.CustomRepeat -> CustomRepeatDialog(
            initFreq = repeatFreq,
            initInterval = repeatInterval,
            lang = lang,
            s = s,
            onConfirm = { f, n ->
                repeatFreq = f
                repeatInterval = n
                repeatType = RepeatType.CUSTOM
                picker = null
            },
            onDismiss = { picker = null },
        )
        Picker.RepeatEnd -> OptionsDialog(
            title = s.repeatEnd,
            options = listOf(s.never, s.untilDate, s.untilCount),
            selectedIndex = when (repeatEndType) {
                RepeatEnd.UNTIL -> 1
                RepeatEnd.COUNT -> 2
                else -> 0
            },
            onSelect = { idx ->
                when (idx) {
                    1 -> { repeatEndType = RepeatEnd.UNTIL; picker = Picker.RepeatEndDate }
                    2 -> { repeatEndType = RepeatEnd.COUNT; picker = Picker.RepeatCount }
                    else -> { repeatEndType = RepeatEnd.NEVER; picker = null }
                }
            },
            onDismiss = { picker = null },
        )
        Picker.RepeatEndDate -> DateDialog(
            initial = repeatEndDate,
            lang = lang,
            s = s,
            onConfirm = { d ->
                repeatEndDate = d
                // 结束日期不能早于开始日期
                if (d.isBefore(startDate)) repeatEndDate = startDate
                picker = null
            },
            onDismiss = { picker = null },
        )
        Picker.RepeatCount -> CountDialog(
            init = repeatCount,
            s = s,
            onConfirm = { repeatCount = it; picker = null },
            onDismiss = { picker = null },
        )
        Picker.Remind -> OptionsDialog(
            title = s.remind,
            options = ScheduleLogic.remindOptions.map { ScheduleLogic.remindLabel(it, lang) },
            selectedIndex = ScheduleLogic.remindOptions.indexOf(remindMinutes),
            onSelect = { idx -> remindMinutes = ScheduleLogic.remindOptions[idx]; picker = null },
            onDismiss = { picker = null },
        )
        Picker.Delete -> GlassDialog(
            onDismiss = { picker = null },
            compact = true,
        ) {
            Text(text = s.deleteTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(text = s.deleteMessage, fontSize = 14.sp, color = SubText, lineHeight = 20.sp)
            Spacer(Modifier.height(10.dp))
            DialogActions(
                onCancel = { picker = null },
                confirmText = s.delete,
                onConfirm = {
                    val sc = loaded
                    if (sc != null) {
                        scope.launch {
                            dao.delete(sc)
                            ReminderScheduler.cancel(context, sc.id)
                            onDone()
                        }
                    }
                    picker = null
                },
                confirmColor = Color(0xFFFF3B30),
            )
        }
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
    lang: Int,
    s: com.horae.app.ui.common.AppStrings,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var viewMonth by remember { mutableStateOf(YearMonth.from(initial)) }
    var selected by remember { mutableStateOf(initial) }

    GlassDialog(onDismiss = onDismiss) {
        // 月份切换
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = s.monthTitle(viewMonth),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            MonthArrow(text = "‹") { viewMonth = viewMonth.minusMonths(1) }
            Spacer(Modifier.width(6.dp))
            MonthArrow(text = "›") { viewMonth = viewMonth.plusMonths(1) }
        }
        Spacer(Modifier.height(10.dp))
        // 星期表头（周一起始）
        Row {
            ScheduleLogic.weekdayLetters(lang).forEach { wd ->
                Text(
                    text = wd,
                    fontSize = 12.sp,
                    color = SubText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 日网格
        val firstOffset = viewMonth.atDay(1).dayOfWeek.value - 1
        val cells: List<LocalDate?> = List(firstOffset) { null } +
            (1..viewMonth.lengthOfMonth()).map { viewMonth.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                week.forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val sel = d == selected
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (sel) AccentBlue else Color.Transparent)
                                    .clickableNoRipple { selected = d },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${d.dayOfMonth}",
                                    fontSize = 13.sp,
                                    color = if (sel) Color.White else Ink,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(14.dp))
        DialogActions(onCancel = onDismiss, confirmText = s.nextStep, onConfirm = { onConfirm(selected) })
    }
}

/** 月份切换小箭头 */
@Composable
private fun MonthArrow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.55f))
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 17.sp, color = AccentBlue)
    }
}

/** f4: 时间选择——分钟只有整五整十可选（00/05/.../55），初始分钟自动吸附 */
@Composable
private fun TimeDialog(
    initial: LocalTime,
    lang: Int,
    s: com.horae.app.ui.common.AppStrings,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableStateOf(initial.hour) }
    var minute by remember { mutableStateOf((initial.minute / 5) * 5) }
    GlassDialog(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                Text(
                    text = s.hour,
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
                    text = s.minute,
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
        Spacer(Modifier.height(12.dp))
        DialogActions(onCancel = onDismiss, confirmText = s.confirm, onConfirm = { onConfirm(LocalTime.of(hour, minute)) })
    }
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
    GlassDialog(onDismiss = onDismiss, compact = true) {
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(6.dp))
        options.forEachIndexed { idx, label ->
            Text(
                text = if (idx == selectedIndex) "✓ $label" else label,
                color = if (idx == selectedIndex) AccentBlue else Ink,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickableNoRipple { onSelect(idx) }
                    .padding(vertical = 8.dp),
            )
        }
        DialogActions(onCancel = onDismiss)
    }
}

/** 自定义重复弹窗：频率（天/周/月/年）+ 每 N 个单位 */
@Composable
private fun CustomRepeatDialog(
    initFreq: Int,
    initInterval: Int,
    lang: Int,
    s: com.horae.app.ui.common.AppStrings,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var freq by remember { mutableStateOf(initFreq) }
    var interval by remember { mutableIntStateOf(initInterval) }
    GlassDialog(onDismiss = onDismiss) {
        Text(text = s.customRepeat, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(12.dp))
        Text(text = s.freqLabel, fontSize = 13.sp, color = SubText)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                RepeatFreq.DAY,
                RepeatFreq.WEEK,
                RepeatFreq.MONTH,
                RepeatFreq.YEAR,
            ).forEach { f ->
                val sel = freq == f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (sel) AccentBlue else Color.White.copy(alpha = 0.65f))
                        .clickableNoRipple { freq = f }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ScheduleLogic.freqUnit(f, lang),
                        color = if (sel) Color.White else Ink,
                        fontSize = 14.sp,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        StepperRow(
            label = s.interval,
            value = interval,
            onMinus = { if (interval > 1) interval-- },
            onPlus = { if (interval < 99) interval++ },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = s.cyclePrefix + ScheduleLogic.customRepeatLabel(freq, interval.coerceIn(1, 99), lang),
            fontSize = 12.sp,
            color = SubText,
        )
        Spacer(Modifier.height(12.dp))
        DialogActions(
            onCancel = onDismiss,
            confirmText = s.confirm,
            onConfirm = { onConfirm(freq, interval.coerceIn(1, 99)) },
        )
    }
}

/** 重复次数弹窗：1..99 次步进 */
@Composable
private fun CountDialog(
    init: Int,
    s: com.horae.app.ui.common.AppStrings,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var count by remember { mutableIntStateOf(init) }
    GlassDialog(onDismiss = onDismiss) {
        Text(text = s.repeatCountTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(14.dp))
        StepperRow(
            label = s.countLabel,
            value = count,
            onMinus = { if (count > 1) count-- },
            onPlus = { if (count < 99) count++ },
        )
        Spacer(Modifier.height(14.dp))
        DialogActions(
            onCancel = onDismiss,
            confirmText = s.confirm,
            onConfirm = { onConfirm(count.coerceIn(1, 99)) },
        )
    }
}

/** 步进行：标签 − 数值 + */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = label, fontSize = 14.sp, color = Ink)
        Spacer(Modifier.weight(1f))
        StepperButton(text = "−", enabled = value > 1, onClick = onMinus)
        Text(
            text = "$value",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.widthIn(min = 44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(text = "+", enabled = value < 99, onClick = onPlus)
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.3f))
            .then(if (enabled) Modifier.clickableNoRipple(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 17.sp, color = if (enabled) AccentBlue else SubText)
    }
}

/** 提醒引导项 */
private enum class ReminderGuideType { NOTIFICATION, AUTOSTART }

private data class ReminderGuideItem(val title: String, val desc: String, val type: ReminderGuideType)

/**
 * 检查提醒所需权限：
 * - 通知权限缺失 → 引导授权
 * - 小米/红米（MIUI/HyperOS）→ 始终引导开启自启动（系统无 API 检测，需用户确认）
 */
private fun reminderGuideItems(
    context: android.content.Context,
    s: com.horae.app.ui.common.AppStrings,
): List<ReminderGuideItem> {
    val manufacturer = android.os.Build.MANUFACTURER
    return buildList {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) add(ReminderGuideItem(s.permNotifTitle, s.permNotifDesc, ReminderGuideType.NOTIFICATION))
        if (manufacturer.equals("xiaomi", true) || manufacturer.equals("redmi", true))
            add(ReminderGuideItem(s.permAutoTitle, s.permAutoDesc, ReminderGuideType.AUTOSTART))
    }
}
