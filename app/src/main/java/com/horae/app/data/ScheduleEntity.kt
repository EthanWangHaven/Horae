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
    /** -1 不提醒；0 准时；>0 提前分钟数 */
    val remindMinutes: Int = -1,
    val note: String? = null,
    val colorIndex: Int = 0,
)
