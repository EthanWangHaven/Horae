package com.horae.app.ui.board

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horae.app.data.AppDatabase
import com.horae.app.data.AppSettings
import com.horae.app.logic.DayOccurrences
import com.horae.app.logic.Occurrence
import com.horae.app.logic.ScheduleLogic
import com.horae.app.ui.common.DialogActions
import com.horae.app.ui.common.GlassDialog
import com.horae.app.ui.common.GlassIconButton
import com.horae.app.ui.common.GlassScreenRoot
import com.horae.app.ui.common.clickableNoRipple
import com.horae.app.ui.common.strings
import com.horae.app.ui.glass.liquidGlass
import com.horae.app.ui.theme.AccentBlue
import com.horae.app.ui.theme.Hairline
import com.horae.app.ui.theme.Ink
import com.horae.app.ui.theme.ScheduleColors
import com.horae.app.ui.theme.SubText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TOTAL_PAGES = 4001
private const val BASE_PAGE = 2000
private const val AXIS_WIDTH_DP = 52
private const val DAY_COLUMN_COUNT = 7

private val detailTimeFmt = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun BoardScreen(
    onOpenList: () -> Unit,
    onAdd: (dayStartMillis: Long) -> Unit,
    onEdit: (scheduleId: Long, dayStartMillis: Long) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val s = strings()
    val schedules by AppDatabase.get(context).scheduleDao()
        .observeAll().collectAsState(initial = emptyList())

    val today = remember { LocalDate.now() }
    val thisMonday = remember {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    fun weekStartOf(page: Int): LocalDate =
        thisMonday.plusWeeks((page - BASE_PAGE).toLong())

    val pagerState = rememberPagerState(initialPage = BASE_PAGE) { TOTAL_PAGES }
    val currentWeekStart = weekStartOf(pagerState.currentPage)

    val density = LocalDensity.current
    val axisWidth = with(density) { AXIS_WIDTH_DP.dp.toPx() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val flingDecay = exponentialDecay<Float>()
    // 竖滑灵敏度乘数（设置页五挡）
    val sensFactor = AppSettings.sensitivityFactor

    // 「回到今天」菜单状态（f9）
    var menuOpen by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Rect.Zero) }

    // 日程详情弹窗状态（f1）
    var detailOcc by remember { mutableStateOf<Occurrence?>(null) }
    var detailDayMillis by remember { mutableStateOf(0L) }

    // 顶部标题栏折叠状态（记住上次状态，持久化到 AppSettings）
    var headerCollapsed by remember { mutableStateOf(AppSettings.boardHeaderCollapsed) }

    // 日程条显示溢出提示（null=未选择 0=自动适配 1=仍然显示；进程内记住，重启后重新询问）
    var overflowMode by rememberSaveable { mutableStateOf<Int?>(null) }
    var showOverflowDialog by rememberSaveable { mutableStateOf(false) }

    // 默认竖直滚动位置（7:00 横线贴顶，8:00 字样刚好露出）
    var defaultScrollPx by remember { mutableStateOf(0f) }

    fun openDetail(occ: Occurrence, day: LocalDate) {
        detailDayMillis = day.atStartOfDay()
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        detailOcc = occ
    }

    GlassScreenRoot {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ---------- 顶部标题栏（可折叠收纳到纵轴交点）：垂直卷起/展开 + 淡化 ----------
            AnimatedVisibility(
                visible = !headerCollapsed,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(196, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(196)),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(196, easing = FastOutSlowInEasing),
                ) + fadeOut(animationSpec = tween(126)),
            ) {
                Column {
                    BoardHeader(
                        today = today,
                        onOpenList = onOpenList,
                        onAdd = {
                            val nextHour = java.time.LocalDateTime.now()
                                .plusHours(1).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                            onAdd(
                                nextHour.atZone(java.time.ZoneId.systemDefault())
                                    .toInstant().toEpochMilli()
                            )
                        },
                        onMenuClick = { menuOpen = true },
                        onMenuAnchorChange = { menuAnchor = it },
                        onCollapse = {
                            headerCollapsed = true
                            AppSettings.updateBoardHeaderCollapsed(true)
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ---------- 星期表头：一 二 三 ... + 日期（折叠时交点处显示展开按钮） ----------
            WeekHeaderRow(
                weekStart = currentWeekStart,
                today = today,
                axisWidth = axisWidth,
                collapsed = headerCollapsed,
                onExpand = {
                    headerCollapsed = false
                    AppSettings.updateBoardHeaderCollapsed(false)
                },
            )

            // ---------- 全天日程条 ----------
            val weekOccurrences = remember(currentWeekStart, schedules) {
                ScheduleLogic.expand(schedules, currentWeekStart, currentWeekStart.plusDays(6))
            }
            AllDayRow(
                weekOccurrences = weekOccurrences,
                weekStart = currentWeekStart,
                axisWidth = axisWidth,
            ) { occ, day ->
                openDetail(occ, day) // f1: 点击全天条 → 详情弹窗
            }

            Spacer(Modifier.height(4.dp))

            // ---------- 时间轴 + 周网格（可上下滚动，横向翻周） ----------
            // f6: 格子高度 = 列宽，即正方形格子
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val colWidthDp = (maxWidth - AXIS_WIDTH_DP.dp) / DAY_COLUMN_COUNT
                val hourHeight = with(density) { colWidthDp.toPx() }
                val startHour = AppSettings.axisStartHour
                val endHour = AppSettings.axisEndHour
                val gridHeight = colWidthDp * (endHour - startHour + 1)
                LaunchedEffect(startHour, endHour) {
                    defaultScrollPx = 0f
                    scrollState.scrollTo(0) // 初始滚到起始小时（顶部）
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    TimeAxis(hourHeight = hourHeight, gridHeight = gridHeight, startHour = startHour, endHour = endHour)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScrollGuard(scrollState, scope, flingDecay, sensFactor)
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            val weekStart = weekStartOf(page)
                            val days = remember(weekStart, schedules) {
                                ScheduleLogic.expand(schedules, weekStart, weekStart.plusDays(6))
                            }
                            WeekGridPage(
                                weekStart = weekStart,
                                today = today,
                                days = days,
                                hourHeight = hourHeight,
                                startHour = startHour,
                                endHour = endHour,
                                overflowShowAll = overflowMode == 1,
                                onOverflowDetected = {
                                    if (overflowMode == null && !showOverflowDialog) showOverflowDialog = true
                                },
                                onAddAt = { day, hour ->
                                    val millis = day.atTime(hour, 0)
                                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    onAdd(millis)
                                },
                                onOpenDetail = { occ, day -> openDetail(occ, day) },
                            )
                        }
                    }
                }
            }
        }

        // ---------- 「回到今天」液态玻璃菜单（f9） ----------
        if (menuOpen) {
            // 全屏透明遮罩：点击外部关闭
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickableNoRipple { menuOpen = false }
            )
            // 大圆角玻璃面板，定位在「更多」按钮下方
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (menuAnchor.right - 150.dp.toPx()).roundToInt().coerceAtLeast(0),
                            (menuAnchor.bottom + 6.dp.toPx()).roundToInt(),
                        )
                    }
                    .width(150.dp)
                    .liquidGlass(shape = RoundedCornerShape(22.dp), tintAlpha = 0.72f, blurRadius = 24.dp)
                    .padding(vertical = 8.dp),
            ) {
                Column {
                Text(
                    text = s.backToToday,
                    fontSize = 15.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple {
                            menuOpen = false
                            scope.launch {
                                pagerState.scrollToPage(BASE_PAGE)
                                scrollState.scrollTo(defaultScrollPx.roundToInt()) // 竖直方向也回到默认位置
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                Text(
                    text = s.search,
                    fontSize = 15.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple {
                            menuOpen = false
                            onSearch()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                Text(
                    text = s.settings,
                    fontSize = 15.sp,
                    color = Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableNoRipple {
                            menuOpen = false
                            onSettings()
                        }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
                }
            }
        }

        // ---------- 日程条显示溢出提示 ----------
        if (showOverflowDialog) {
            GlassDialog(
                onDismiss = {
                    showOverflowDialog = false
                    if (overflowMode == null) overflowMode = 0
                },
            ) {
                Text(text = s.overflowTitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.height(10.dp))
                Text(text = s.overflowMsg, fontSize = 14.sp, color = SubText, lineHeight = 20.sp)
                Spacer(Modifier.height(14.dp))
                DialogActions(
                    onCancel = {
                        showOverflowDialog = false
                        overflowMode = 0
                    },
                    confirmText = s.showAnyway,
                    onConfirm = {
                        showOverflowDialog = false
                        overflowMode = 1
                    },
                )
            }
        }

        // ---------- 日程详情弹窗（f1） ----------
        detailOcc?.let { occ ->
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickableNoRipple { detailOcc = null },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(0.9f) // 宽度减小 10%
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(22.dp)) // 统一半透明底，不用高斯模糊
                        .clickableNoRipple { } // 吞掉面板内点击，避免误关闭
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = occ.schedule.title.ifBlank { s.scheduleFallback },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        // 右上角铅笔图标：进入编辑
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .liquidGlass(shape = CircleShape, tintAlpha = 0.5f, blurRadius = 12.dp)
                                .clickableNoRipple {
                                    detailOcc = null
                                    onEdit(occ.schedule.id, detailDayMillis)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = s.edit,
                                tint = AccentBlue,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                    DetailField(label = s.timeLabel, value = detailTimeString(occ, AppSettings.languageIndex, s.allDay))
                    occ.schedule.location?.takeIf { it.isNotBlank() }?.let {
                        DetailField(label = s.locationLabel, value = it)
                    }
                    occ.schedule.note?.takeIf { it.isNotBlank() }?.let {
                        DetailField(label = s.noteLabel, value = it)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardHeader(
    today: LocalDate,
    onOpenList: () -> Unit,
    onAdd: () -> Unit,
    onMenuClick: () -> Unit,
    onMenuAnchorChange: (Rect) -> Unit,
    onCollapse: () -> Unit,
) {
    val s = strings()
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .liquidGlass(shape = RoundedCornerShape(24.dp), tintAlpha = 0.6f, blurRadius = 20.dp)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ScheduleLogic.boardTitle(today, AppSettings.languageIndex),
            // 英文日期较长，字号自适应缩小避免换行
            fontSize = if (AppSettings.languageIndex == 1) 21.sp else 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        GlassIconButton(
            icon = Icons.Default.Add,
            contentDescription = s.addSchedule,
            onClick = onAdd,
            tint = AccentBlue,
        )
        Spacer(Modifier.width(7.dp))
        GlassIconButton(
            icon = Icons.Default.Menu,
            contentDescription = s.scheduleList,
            onClick = onOpenList,
        )
        Spacer(Modifier.width(7.dp))
        Box(
            modifier = Modifier.onGloballyPositioned { onMenuAnchorChange(it.boundsInRoot()) },
        ) {
            GlassIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = s.more,
                onClick = onMenuClick,
            )
        }
        Spacer(Modifier.width(7.dp))
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = s.collapseHeader,
            onClick = onCollapse,
        )
    }
}

@Composable
private fun WeekHeaderRow(
    weekStart: LocalDate,
    today: LocalDate,
    axisWidth: Float,
    collapsed: Boolean = false,
    onExpand: () -> Unit = {},
) {
    val s = strings()
    val lang = AppSettings.languageIndex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        // 纵轴与星期表头交点：折叠时淡入圆形玻璃展开按钮（居中于交点）
        Box(
            modifier = Modifier
                .width(with(LocalDensity.current) { axisWidth.toDp() })
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = collapsed,
                enter = fadeIn(animationSpec = tween(154)) +
                    scaleIn(initialScale = 0.6f, animationSpec = tween(154)),
                exit = fadeOut(animationSpec = tween(112)) +
                    scaleOut(targetScale = 0.6f, animationSpec = tween(112)),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .liquidGlass(shape = CircleShape, tintAlpha = 0.8f, blurRadius = 10.dp)
                        .clickableNoRipple(onExpand),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = s.expandHeader,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        repeat(DAY_COLUMN_COUNT) { idx ->
            val day = weekStart.plusDays(idx.toLong())
            val isToday = day == today
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = ScheduleLogic.weekdayLetters(lang)[idx],
                    fontSize = 12.sp,
                    color = SubText,
                )
                // f8: 去掉顶部间距，星期与日期数字更紧凑
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (isToday) Modifier
                                .liquidGlass(
                                    shape = CircleShape,
                                    tintAlpha = 0.8f,
                                    blurRadius = 10.dp,
                                )
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.dayOfMonth.toString(),
                        fontSize = 15.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) AccentBlue else Ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayRow(
    weekOccurrences: List<DayOccurrences>,
    weekStart: LocalDate,
    axisWidth: Float,
    onClick: (Occurrence, LocalDate) -> Unit,
) {
    val s = strings()
    val hasAllDay = weekOccurrences.any { it.allDay.isNotEmpty() }
    if (!hasAllDay) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Spacer(Modifier.width(with(LocalDensity.current) { axisWidth.toDp() }))
        repeat(DAY_COLUMN_COUNT) { idx ->
            val day = weekStart.plusDays(idx.toLong())
            val items = weekOccurrences.firstOrNull { it.day == day }?.allDay ?: emptyList()
            Column(modifier = Modifier.weight(1f).padding(horizontal = 1.dp)) {
                items.forEach { occ ->
                    val color = ScheduleColors[occ.schedule.colorIndex % ScheduleColors.size]
                    Text(
                        text = occ.schedule.title.ifBlank { s.allDay },
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp)
                            .liquidGlass(
                                shape = RoundedCornerShape(6.dp),
                                tintAlpha = 0f,
                                blurRadius = 10.dp,
                                highlight = false,
                            )
                            .background(color.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .clickableNoRipple { onClick(occ, day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAxis(hourHeight: Float, gridHeight: Dp, startHour: Int, endHour: Int) {
    val hourHeightDp = with(LocalDensity.current) { hourHeight.toDp() }
    Column(
        modifier = Modifier
            .width(AXIS_WIDTH_DP.dp)
            .height(gridHeight),
    ) {
        repeat(endHour - startHour + 1) { i ->
            val h = startHour + i
            Box(modifier = Modifier.height(hourHeightDp)) {
                Text(
                    text = "%02d:00".format(h),
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = SubText,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        // f7: 时间文字垂直居中对齐整点横线（首行放线下方避免顶部裁切）
                        .offset(y = if (i == 0) 2.dp else (-5).dp)
                        .padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekGridPage(
    weekStart: LocalDate,
    today: LocalDate,
    days: List<DayOccurrences>,
    hourHeight: Float,
    startHour: Int,
    endHour: Int,
    overflowShowAll: Boolean,
    onOverflowDetected: () -> Unit,
    onAddAt: (LocalDate, Int) -> Unit,
    onOpenDetail: (Occurrence, LocalDate) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val gridWidth = maxWidth
        val colWidth = with(LocalDensity.current) { (gridWidth / DAY_COLUMN_COUNT).toPx() }
        val gridHeightDp = with(LocalDensity.current) { hourHeight.toDp() }

        // 网格线画布 + 空白点击
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .height(gridHeightDp * (endHour - startHour + 1))
                .pointerInput(weekStart, colWidth, hourHeight, startHour, endHour) {
                    detectTapGestures { pos ->
                        val dayIdx = (pos.x / colWidth).toInt().coerceIn(0, DAY_COLUMN_COUNT - 1)
                        val hour = startHour + (pos.y / hourHeight).toInt().coerceIn(0, endHour - startHour)
                        onAddAt(weekStart.plusDays(dayIdx.toLong()), hour)
                    }
                }
        ) {
            val totalH = (endHour - startHour + 1) * hourHeight
            val totalW = colWidth * DAY_COLUMN_COUNT
            // 今天列淡高亮
            val todayIdx = java.time.temporal.ChronoUnit.DAYS.between(weekStart, today).toInt()
            if (todayIdx in 0 until DAY_COLUMN_COUNT) {
                drawRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(todayIdx * colWidth, 0f),
                    size = Size(colWidth, totalH),
                )
            }
            // 横向整点线
            var y = 0f
            while (y <= totalH + 0.5f) {
                drawLine(Hairline, Offset(0f, y), Offset(totalW, y), strokeWidth = 1f)
                y += hourHeight
            }
            // 纵向日期分隔线
            var x = 0f
            while (x <= totalW + 0.5f) {
                drawLine(Hairline, Offset(x, 0f), Offset(x, totalH), strokeWidth = 1f)
                x += colWidth
            }
        }

        // 日程块（按天分列，重叠均分车道）
        days.forEach { dayOcc ->
            val dayIdx = java.time.temporal.ChronoUnit.DAYS.between(weekStart, dayOcc.day).toInt()
            if (dayIdx !in 0 until DAY_COLUMN_COUNT) return@forEach
            val lanes = assignLanes(dayOcc.timed)
            lanes.forEach { (laneIdx, laneCount, occ) ->
                val startMin = occ.start.hour * 60 + occ.start.minute
                val durMin = ((occ.end.hour * 60 + occ.end.minute) - startMin)
                    .coerceAtLeast(30)
                // 相对纵轴起点定位（轴外日程钳到轴顶，避免盖住表头）
                val top = (startMin / 60f - startHour).coerceAtLeast(0f) * hourHeight
                val blockHeight = (durMin / 60f + (startMin / 60f - startHour).coerceAtMost(0f)) * hourHeight
                val laneW = colWidth / laneCount
                val x = dayIdx * colWidth + laneIdx * laneW
                ScheduleBlock(
                    occ = occ,
                    x = x,
                    y = top,
                    width = laneW,
                    height = blockHeight,
                    overflowShowAll = overflowShowAll,
                    onOverflowDetected = onOverflowDetected,
                    onClick = { onOpenDetail(occ, dayOcc.day) }, // f1: 点击 → 详情弹窗
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlock(
    occ: Occurrence,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    overflowShowAll: Boolean,
    onOverflowDetected: () -> Unit,
    onClick: () -> Unit,
) {
    val density = LocalDensity.current
    val color = ScheduleColors[occ.schedule.colorIndex % ScheduleColors.size]
    val sched = occ.schedule
    val hasLocation = !sched.location.isNullOrBlank()
    val extraCount = listOf(sched.showStartTime, sched.showEndTime, sched.showLocation && hasLocation).count { it }
    // 粗略估算：额外行每行约 34px，基础（两行标题+内边距）约 90px
    val canFit = height > 90f + extraCount * 34f
    val renderStart = sched.showStartTime && (overflowShowAll || canFit)
    val renderEnd = sched.showEndTime && (overflowShowAll || canFit)
    val renderLocation = sched.showLocation && hasLocation && (overflowShowAll || canFit)
    val hasExtras = renderStart || renderEnd || renderLocation
    // 标题行数：显示附加行时按剩余高度自适应（每行标题约 35px、附加行约 28px），超出用 … 截断
    val shownExtraLines = listOf(renderStart, renderEnd, renderLocation).count { it }
    val titleMaxLines = when {
        !hasExtras -> if (height > 80f) 3 else 2
        else -> (((height - 22f) - shownExtraLines * 33f) / 36f).toInt().coerceIn(1, 2)
    }
    if (extraCount > 0 && !canFit) {
        // 空间不足：提醒用户（每次会话仅提示一次，由调用方去重）
        SideEffect { onOverflowDetected() }
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(with(density) { (width - 2.dp.toPx()).coerceAtLeast(0f).toDp() })
            .height(with(density) { (height - 2.dp.toPx()).coerceAtLeast(0f).toDp() })
            .padding(1.dp)
            .liquidGlass(
                shape = RoundedCornerShape(10.dp),
                tintAlpha = 0f,
                blurRadius = 12.dp,
            )
            .background(color.copy(alpha = 0.78f), RoundedCornerShape(10.dp))
            .clickableNoRipple(onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center, // f5: 文字竖直方向居中
    ) {
        Column {
            Text(
                text = sched.title.ifBlank { "日程" },
                fontSize = 11.sp,
                lineHeight = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
            if (renderStart) {
                Text(
                    text = ScheduleLogic.hm(occ.start),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // 结束时间显示在开始时间下方（不加 - 连接）
            if (renderEnd) {
                Text(
                    text = ScheduleLogic.hm(occ.end),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (renderStart) 1.dp else 2.dp),
                )
            }
            if (renderLocation) {
                Text(
                    text = sched.location.orEmpty(),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = SubText)
        Text(
            text = value,
            fontSize = 15.sp,
            color = Ink,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** 详情弹窗时间文案 */
private fun detailTimeString(occ: Occurrence, lang: Int, allDayText: String): String {
    val s = occ.start
    val e = occ.end
    return if (occ.schedule.allDay) {
        if (s.toLocalDate() == e.toLocalDate())
            "${ScheduleLogic.detailDate(s.toLocalDate(), lang)} $allDayText"
        else "${ScheduleLogic.detailDate(s.toLocalDate(), lang)} – ${ScheduleLogic.detailDate(e.toLocalDate(), lang)} $allDayText"
    } else {
        if (s.toLocalDate() == e.toLocalDate())
            "${ScheduleLogic.detailDate(s.toLocalDate(), lang)} ${s.format(detailTimeFmt)} – ${e.format(detailTimeFmt)}"
        else
            "${ScheduleLogic.detailDate(s.toLocalDate(), lang)} ${s.format(detailTimeFmt)} – ${ScheduleLogic.detailDate(e.toLocalDate(), lang)} ${e.format(detailTimeFmt)}"
    }
}

/** 简单重叠分组：返回 (laneIndex, laneCount, occurrence) 列表 */
private fun assignLanes(occurrences: List<Occurrence>): List<Triple<Int, Int, Occurrence>> {
    if (occurrences.isEmpty()) return emptyList()
    val sorted = occurrences.sortedBy { it.start }
    data class Group(val lanes: MutableList<Occurrence> = mutableListOf()) {
        var end = java.time.LocalDateTime.MIN
    }
    val groups = mutableListOf<Group>()
    for (occ in sorted) {
        val group = groups.lastOrNull { occ.start < it.end }
        if (group == null) {
            val g = Group()
            g.lanes.add(occ)
            g.end = occ.end
            groups.add(g)
        } else {
            group.lanes.add(occ)
            if (occ.end > group.end) group.end = occ.end
        }
    }
    val result = mutableListOf<Triple<Int, Int, Occurrence>>()
    for (g in groups) {
        g.lanes.forEachIndexed { idx, occ ->
            result.add(Triple(idx, g.lanes.size, occ))
        }
    }
    return result
}

/**
 * pager 区域的竖滑仲裁（Initial pass，先于 pager 的 Main pass 处理）：
 * - 竖直为主的滑动：驱动 scrollState 并消费事件（pager 看到已消费即放弃），
 *   结束后按速度做惯性衰减；
 * - 水平为主的滑动：不消费，放行给 HorizontalPager 翻周。
 */
private fun Modifier.verticalScrollGuard(
    scrollState: ScrollState,
    scope: CoroutineScope,
    decay: DecayAnimationSpec<Float>,
    factor: Float,
): Modifier = pointerInput(scrollState, factor) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var totalX = 0f
        var totalY = 0f
        var decided = false
        var vertical = false
        val tracker = VelocityTracker()
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val main = event.changes.firstOrNull { it.id == down.id } ?: break
            if (main.changedToUp()) break
            val change = main.positionChange()
            tracker.addPosition(main.uptimeMillis, main.position)
            if (!decided) {
                totalX += change.x
                totalY += change.y
                val slop = viewConfiguration.touchSlop
                if (abs(totalX) > slop || abs(totalY) > slop) {
                    decided = true
                    vertical = abs(totalY) >= abs(totalX)
                    if (vertical) scrollState.dispatchRawDelta(-totalY * factor)
                }
            } else if (vertical) {
                scrollState.dispatchRawDelta(-change.y * factor)
            }
            if (decided && vertical) {
                main.consume() // 拦截 pager 与外层 verticalScroll 对该手指的处理
            }
        }
        if (decided && vertical) {
            val velocityY = tracker.calculateVelocity().y
            scope.launch {
                val state = AnimationState(initialValue = -velocityY * factor)
                var last = state.value
                state.animateDecay(decay) {
                    val delta = value - last
                    last = value
                    scrollState.dispatchRawDelta(delta)
                }
            }
        }
    }
}
