package com.horae.app.logic

import com.horae.app.data.RepeatEnd
import com.horae.app.data.RepeatFreq
import com.horae.app.data.RepeatType
import com.horae.app.data.ScheduleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Locale

/** 某天中一个日程的具体发生（含起止时刻） */
data class Occurrence(
    val schedule: ScheduleEntity,
    val start: LocalDateTime,
    val end: LocalDateTime,
) {
    val allDay: Boolean get() = schedule.allDay
}

/** 单日聚合结果 */
data class DayOccurrences(
    val day: LocalDate,
    val allDay: List<Occurrence>,
    val timed: List<Occurrence>,
)

object ScheduleLogic {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun toLocal(millis: Long): LocalDateTime =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

    fun toMillis(dt: LocalDateTime): Long =
        dt.atZone(zone).toInstant().toEpochMilli()

    fun startDayOf(s: ScheduleEntity): LocalDate = toLocal(s.startTime).toLocalDate()

    fun endDayOf(s: ScheduleEntity): LocalDate = toLocal(s.endTime).toLocalDate()

    /** 判断某日程在 [day] 这一天是否发生 */
    fun occursOn(s: ScheduleEntity, day: LocalDate): Boolean {
        val startDay = startDayOf(s)
        return when (s.repeatType) {
            RepeatType.NONE -> day == startDay
            RepeatType.DAILY -> !day.isBefore(startDay)
            RepeatType.WEEKLY -> !day.isBefore(startDay) && day.dayOfWeek == startDay.dayOfWeek
            RepeatType.WEEKDAYS -> !day.isBefore(startDay) &&
                day.dayOfWeek != DayOfWeek.SATURDAY && day.dayOfWeek != DayOfWeek.SUNDAY
            RepeatType.MONTHLY -> !day.isBefore(startDay) && day.dayOfMonth == startDay.dayOfMonth
            RepeatType.CUSTOM -> customOccursOn(s, day)
            else -> day == startDay
        }
    }

    /** 自定义重复判定：频率 + 间隔 + 结束方式（日期/次数） */
    private fun customOccursOn(s: ScheduleEntity, day: LocalDate): Boolean {
        val startDay = startDayOf(s)
        if (day.isBefore(startDay)) return false
        val interval = s.repeatInterval.coerceAtLeast(1)
        val period: Long = when (s.repeatFreq) {
            RepeatFreq.DAY -> java.time.temporal.ChronoUnit.DAYS.between(startDay, day)
            RepeatFreq.WEEK -> {
                if (day.dayOfWeek != startDay.dayOfWeek) return false
                java.time.temporal.ChronoUnit.WEEKS.between(startDay, day)
            }
            RepeatFreq.MONTH -> {
                if (day.dayOfMonth != startDay.dayOfMonth) return false
                java.time.temporal.ChronoUnit.MONTHS.between(startDay, day)
            }
            RepeatFreq.YEAR -> {
                if (day.month != startDay.month || day.dayOfMonth != startDay.dayOfMonth) return false
                java.time.temporal.ChronoUnit.YEARS.between(startDay, day)
            }
            else -> return false
        }
        if (period < 0 || period % interval != 0L) return false
        when (s.repeatEndType) {
            RepeatEnd.UNTIL -> {
                val endDay = toLocal(s.repeatEndDate).toLocalDate()
                if (day.isAfter(endDay)) return false
            }
            RepeatEnd.COUNT -> if (period / interval + 1 > s.repeatCount) return false
        }
        return true
    }

    /**
     * 展开给定日期范围内的所有发生。
     * 全天日程：从起始日到结束日每天都发生；
     * 非全天：按重复规则每天生成一个保持原时长/时刻的发生。
     */
    fun expand(
        schedules: List<ScheduleEntity>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<DayOccurrences> {
        val byDay = linkedMapOf<LocalDate, MutableList<Occurrence>>()
        fun bucket(day: LocalDate) = byDay.getOrPut(day) { mutableListOf() }

        for (s in schedules) {
            val sStart = toLocal(s.startTime)
            val sEnd = toLocal(s.endTime)
            if (s.allDay) {
                val sd = sStart.toLocalDate()
                val ed = sEnd.toLocalDate()
                var d = maxOf(sd, rangeStart)
                val last = minOf(ed, rangeEnd)
                while (!d.isAfter(last)) {
                    if (occursOn(s, d) || (!d.isBefore(sd) && !d.isAfter(ed))) {
                        bucket(d).add(
                            Occurrence(s, d.atStartOfDay(), d.plusDays(1).atStartOfDay().minusNanos(1))
                        )
                    }
                    d = d.plusDays(1)
                }
            } else {
                val duration = java.time.Duration.between(sStart, sEnd)
                var d = maxOf(sStart.toLocalDate(), rangeStart)
                val last = rangeEnd
                while (!d.isAfter(last)) {
                    if (occursOn(s, d)) {
                        val start = d.atTime(sStart.toLocalTime())
                        bucket(d).add(Occurrence(s, start, start.plus(duration)))
                    }
                    d = d.plusDays(1)
                }
            }
        }

        return byDay.entries
            .sortedBy { it.key }
            .map { (day, list) ->
                val (allDay, timed) = list.partition { it.schedule.allDay }
                DayOccurrences(
                    day = day,
                    allDay = allDay.sortedBy { it.schedule.startTime },
                    timed = timed.sortedBy { it.start },
                )
            }
    }

    fun occurrenceForDay(s: ScheduleEntity, day: LocalDate): Occurrence? {
        val sStart = toLocal(s.startTime)
        val sEnd = toLocal(s.endTime)
        return if (s.allDay) {
            val sd = sStart.toLocalDate()
            val ed = sEnd.toLocalDate()
            if (!day.isBefore(sd) && !day.isAfter(ed)) {
                Occurrence(s, day.atStartOfDay(), day.plusDays(1).atStartOfDay().minusNanos(1))
            } else null
        } else {
            if (!occursOn(s, day)) null
            else {
                val start = day.atTime(sStart.toLocalTime())
                Occurrence(s, start, start.plus(java.time.Duration.between(sStart, sEnd)))
            }
        }
    }

    // ---------- 展示格式化 ----------

    private val boardTitleFmt = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)
    private val boardTitleFmtEn = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val listTitleFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA)
    private val listTitleFmtEn = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val detailDateFmt = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)
    private val detailDateFmtEn = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
    private val hmFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun detailDate(day: java.time.LocalDate, lang: Int = 0): String =
        day.format(if (lang == 1) detailDateFmtEn else detailDateFmt)

    fun boardTitle(day: LocalDate, lang: Int = 0): String =
        day.format(if (lang == 1) boardTitleFmtEn else boardTitleFmt)
    fun listTitle(day: LocalDate, lang: Int = 0): String =
        day.format(if (lang == 1) listTitleFmtEn else listTitleFmt)
    fun hm(t: LocalTime): String = t.format(hmFmt)
    fun hm(dt: LocalDateTime): String = dt.format(hmFmt)

    /** 星期短名：周一..周日 / Mon..Sun */
    fun weekdayHan(day: LocalDate, lang: Int = 0): String = when (lang) {
        1 -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[day.dayOfWeek.value - 1]
        else -> when (day.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"; DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
            else -> "周日"
        }
    }

    /** 看板星期表头单字：一..日 / M..S */
    fun weekdayLetters(lang: Int = 0): List<String> =
        if (lang == 1) listOf("M", "T", "W", "T", "F", "S", "S")
        else listOf("一", "二", "三", "四", "五", "六", "日")

    fun weekNumber(day: LocalDate): Int =
        day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    fun weekLabel(day: LocalDate, lang: Int = 0): String = when (lang) {
        1 -> "${weekdayHan(day, 1)} · Week ${weekNumber(day)}"
        else -> "第 ${weekNumber(day)} 周 ${weekdayHan(day, 0)}"
    }

    /** 列表页头部副标题：周五 · 第 39 周 / Fri · Week 39 */
    fun weekSubLabel(day: LocalDate, lang: Int = 0): String = weekLabel(day, lang)

    fun repeatLabel(type: Int, lang: Int = 0): String = when (type) {
        RepeatType.NONE -> if (lang == 1) "Never" else "永不"
        RepeatType.DAILY -> if (lang == 1) "Every day" else "每天"
        RepeatType.WEEKLY -> if (lang == 1) "Every week" else "每周"
        RepeatType.WEEKDAYS -> if (lang == 1) "Weekdays" else "工作日"
        RepeatType.MONTHLY -> if (lang == 1) "Every month" else "每月"
        RepeatType.CUSTOM -> if (lang == 1) "Custom" else "自定义"
        else -> if (lang == 1) "Never" else "永不"
    }

    /** 自定义重复的显示文案：每 N 天/周/月/年 / Every N day(s) */
    fun customRepeatLabel(freq: Int, interval: Int, lang: Int = 0): String {
        if (lang == 1) {
            val unit = when (freq) {
                RepeatFreq.DAY -> "day"
                RepeatFreq.WEEK -> "week"
                RepeatFreq.MONTH -> "month"
                RepeatFreq.YEAR -> "year"
                else -> "week"
            }
            return if (interval <= 1) "Every $unit" else "Every $interval ${unit}s"
        }
        val unit = freqUnit(freq, 0)
        return if (interval <= 1) "每$unit" else "每 $interval $unit"
    }

    /** 频率单位：天/周/月/年 / Day/Week/Month/Year */
    fun freqUnit(freq: Int, lang: Int = 0): String = when (freq) {
        RepeatFreq.DAY -> if (lang == 1) "Day" else "天"
        RepeatFreq.WEEK -> if (lang == 1) "Week" else "周"
        RepeatFreq.MONTH -> if (lang == 1) "Month" else "月"
        RepeatFreq.YEAR -> if (lang == 1) "Year" else "年"
        else -> if (lang == 1) "Week" else "周"
    }

    fun remindLabel(minutes: Int, lang: Int = 0): String = when (minutes) {
        -1 -> if (lang == 1) "None" else "无"
        0 -> if (lang == 1) "On time" else "准时"
        5 -> if (lang == 1) "5 min before" else "提前 5 分钟"
        15 -> if (lang == 1) "15 min before" else "提前 15 分钟"
        30 -> if (lang == 1) "30 min before" else "提前 30 分钟"
        60 -> if (lang == 1) "1 hr before" else "提前 1 小时"
        else -> if (lang == 1) "None" else "无"
    }

    val remindOptions = listOf(-1, 0, 5, 15, 30, 60)
    val repeatOptions = listOf(
        RepeatType.NONE, RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.WEEKDAYS, RepeatType.MONTHLY, RepeatType.CUSTOM
    )
}
