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
    private val listTitleFmt = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.CHINA)
    private val hmFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun boardTitle(day: LocalDate): String = day.format(boardTitleFmt)
    fun listTitle(day: LocalDate): String = day.format(listTitleFmt)
    fun hm(t: LocalTime): String = t.format(hmFmt)
    fun hm(dt: LocalDateTime): String = dt.format(hmFmt)

    fun weekdayHan(day: LocalDate): String =
        when (day.dayOfWeek) {
            DayOfWeek.MONDAY -> "周一"; DayOfWeek.TUESDAY -> "周二"
            DayOfWeek.WEDNESDAY -> "周三"; DayOfWeek.THURSDAY -> "周四"
            DayOfWeek.FRIDAY -> "周五"; DayOfWeek.SATURDAY -> "周六"
            else -> "周日"
        }

    fun weekNumber(day: LocalDate): Int =
        day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    fun weekLabel(day: LocalDate): String = "第 ${weekNumber(day)} 周 ${weekdayHan(day)}"

    fun repeatLabel(type: Int): String = when (type) {
        RepeatType.NONE -> "永不"
        RepeatType.DAILY -> "每天"
        RepeatType.WEEKLY -> "每周"
        RepeatType.WEEKDAYS -> "工作日"
        RepeatType.MONTHLY -> "每月"
        RepeatType.CUSTOM -> "自定义"
        else -> "永不"
    }

    /** 自定义重复的显示文案：每 N 天/周/月/年 */
    fun customRepeatLabel(freq: Int, interval: Int): String {
        val unit = freqUnit(freq)
        return if (interval <= 1) "每$unit" else "每 $interval $unit"
    }

    fun freqUnit(freq: Int): String = when (freq) {
        RepeatFreq.DAY -> "天"
        RepeatFreq.WEEK -> "周"
        RepeatFreq.MONTH -> "月"
        RepeatFreq.YEAR -> "年"
        else -> "周"
    }

    fun remindLabel(minutes: Int): String = when (minutes) {
        -1 -> "无"
        0 -> "准时"
        5 -> "提前 5 分钟"
        15 -> "提前 15 分钟"
        30 -> "提前 30 分钟"
        60 -> "提前 1 小时"
        else -> "无"
    }

    val remindOptions = listOf(-1, 0, 5, 15, 30, 60)
    val repeatOptions = listOf(
        RepeatType.NONE, RepeatType.DAILY, RepeatType.WEEKLY, RepeatType.WEEKDAYS, RepeatType.MONTHLY, RepeatType.CUSTOM
    )
}
