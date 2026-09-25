package com.horae.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 重复类型 */
object RepeatType {
    const val NONE = 0
    const val DAILY = 1
    const val WEEKLY = 2
    const val WEEKDAYS = 3
    const val MONTHLY = 4
    const val CUSTOM = 5
}

/** 自定义重复的频率单位 */
object RepeatFreq {
    const val DAY = 0
    const val WEEK = 1
    const val MONTH = 2
    const val YEAR = 3
}

/** 重复结束方式 */
object RepeatEnd {
    const val NEVER = 0
    const val UNTIL = 1
    const val COUNT = 2
}

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String? = null,
    val allDay: Boolean = false,
    val startTime: Long,
    val endTime: Long,
    val repeatType: Int = RepeatType.NONE,
    /** 自定义重复：频率单位（RepeatFreq） */
    val repeatFreq: Int = RepeatFreq.WEEK,
    /** 自定义重复：每多少个频率单位 */
    val repeatInterval: Int = 1,
    /** 重复结束方式（RepeatEnd） */
    val repeatEndType: Int = RepeatEnd.NEVER,
    /** 按日期结束时的结束时刻（epochMillis，0=未设置） */
    val repeatEndDate: Long = 0L,
    /** 按次数结束时的最大次数 */
    val repeatCount: Int = 0,
    /** -1 不提醒；0 准时；>0 提前分钟数 */
    val remindMinutes: Int = -1,
    val note: String? = null,
    val colorIndex: Int = 0,
    /** 看板条上显示开始时间 */
    val showStartTime: Boolean = true,
    /** 看板条上显示结束时间（显示在开始时间下方） */
    val showEndTime: Boolean = false,
    /** 看板条上显示地点 */
    val showLocation: Boolean = false,
)
