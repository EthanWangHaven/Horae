package com.horae.app.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.horae.app.R
import com.horae.app.data.AppSettings
import com.horae.app.logic.ScheduleLogic
import com.horae.app.ui.common.appStrings
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

const val CHANNEL_ID = "horae_reminders"
const val EXTRA_SCHEDULE_ID = "schedule_id"

object ReminderScheduler {

    fun ensureChannel(context: Context) {
        // 通知可能由广播接收器触发（进程冷启动），先确保设置已加载
        AppSettings.ensureLoaded(context)
        val s = appStrings(AppSettings.languageIndex)
        val nm = context.getSystemService(NotificationManager::class.java)
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        // 语言变化后重建通知渠道以更新名称
        if (existing != null && existing.name.toString() != s.notifChannelName) {
            nm.deleteNotificationChannel(CHANNEL_ID)
        }
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, s.notifChannelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = s.notifChannelDesc
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun pendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_SCHEDULE_ID, scheduleId)
        return PendingIntent.getBroadcast(
            context, scheduleId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(context: Context, scheduleId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        am.cancel(pendingIntent(context, scheduleId))
    }

    /** 保存/更新后调用：重设该日程的提醒闹钟 */
    fun schedule(context: Context, scheduleId: Long, startTime: Long, remindMinutes: Int, allDay: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java)
        cancel(context, scheduleId)
        if (remindMinutes < 0 || allDay) return
        val triggerAt = startTime - remindMinutes * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return
        val pi = pendingIntent(context, scheduleId)
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 60_000L, pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1)
        if (id == -1L) return
        ReminderScheduler.ensureChannel(context)
        // 提醒触发时同步查库；为避免阻塞，简单用 goAsync
        val pending = goAsync()
        Thread {
            try {
                val schedule = runBlocking {
                    com.horae.app.data.AppDatabase.get(context).scheduleDao().getById(id)
                }
                if (schedule != null) {
                    val s = appStrings(AppSettings.languageIndex)
                    val start = ScheduleLogic.toLocal(schedule.startTime)
                    val text = buildString {
                        append(s.notifStartFmt(ScheduleLogic.hm(start)))
                        schedule.location?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                    }
                    val nm = context.getSystemService(NotificationManager::class.java)
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_clock)
                        .setContentTitle(schedule.title.ifBlank { s.notifFallbackTitle })
                        .setContentText(text)
                        .setAutoCancel(true)
                        .build()
                    nm.notify(id.toInt(), notification)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
