package com.doomscroll.destroyer.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doomscroll.destroyer.service.BlockerForegroundService
import com.doomscroll.destroyer.utils.BlockerState
import java.util.Calendar

/**
 * BootReceiver — restarts the foreground service after device reboot.
 * Also fires when our app is updated (MY_PACKAGE_REPLACED).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            BlockerState.checkAndResetIfNewDay(context)
            // Restart the foreground service — the accessibility service
            // will reconnect automatically as it's still enabled in settings
            val serviceIntent = Intent(context, BlockerForegroundService::class.java)
            context.startForegroundService(serviceIntent)
            scheduleMidnightReset(context)
        }
    }
}

/**
 * MidnightResetReceiver — fired at 12:00 AM to reset daily quotas.
 * Re-schedules itself for the next midnight.
 */
class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BlockerState.checkAndResetIfNewDay(context)
        scheduleMidnightReset(context)
    }
}

fun scheduleMidnightReset(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MidnightResetReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Next midnight
    val midnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Use setExactAndAllowWhileIdle so it fires even in doze mode
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        midnight.timeInMillis,
        pendingIntent
    )
}
