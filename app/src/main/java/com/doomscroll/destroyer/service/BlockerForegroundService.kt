package com.doomscroll.destroyer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.doomscroll.destroyer.MainActivity
import com.doomscroll.destroyer.R
import com.doomscroll.destroyer.overlay.OverlayManager
import com.doomscroll.destroyer.utils.BlockerState

/**
 * BlockerForegroundService
 *
 * Keeps a persistent notification so Android doesn't kill our process.
 * When ACTION_START_TICKING is received, starts a 1-second timer that
 * calls BlockerState.tickWhileOpen() and reacts to the result.
 *
 * This is what enforces the "1 cumulative minute = 30 min lockout" rule
 * even across multiple short opens (15s + 30s + 16s = lockout).
 */
class BlockerForegroundService : Service() {

    companion object {
        const val ACTION_START_TICKING = "start_ticking"
        const val ACTION_PAUSE_TICKING = "pause_ticking"
        private const val NOTIF_CHANNEL_ID = "doomscroll_channel"
        private const val NOTIF_ID = 1001
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isTicking = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isTicking) return

            val result = BlockerState.tickWhileOpen(this@BlockerForegroundService)

            when (result) {
                BlockerState.TickResult.LOCKOUT_TRIGGERED -> {
                    isTicking = false
                    onLockoutTriggered()
                }
                BlockerState.TickResult.DAILY_EXHAUSTED -> {
                    isTicking = false
                    onDailyExhausted()
                }
                BlockerState.TickResult.CONTINUE -> {
                    // Update notification with remaining time
                    val remaining = BlockerState.getRemainingSeconds(this@BlockerForegroundService)
                    val sessionLeft = BlockerState.getTripSeconds(this@BlockerForegroundService) -
                            BlockerState.getSessionSeconds(this@BlockerForegroundService)
                    updateNotification("$remaining seconds left", "$sessionLeft sec session left")
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Watching... 👁", ""))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TICKING -> startTicking()
            ACTION_PAUSE_TICKING -> pauseTicking()
        }
        return START_STICKY // restart if killed
    }

    private fun startTicking() {
        if (!isTicking) {
            isTicking = true
            handler.post(tickRunnable)
        }
    }

    private fun pauseTicking() {
        isTicking = false
    }

    private fun onLockoutTriggered() {
        vibrate(longArrayOf(0, 200, 100, 200, 100, 400))
        OverlayManager.showLockoutOverlay(this)
        updateNotification("LOCKED 🔒", "Come back in 30 minutes.")
    }

    private fun onDailyExhausted() {
        vibrate(longArrayOf(0, 300, 100, 300, 100, 300, 100, 600))
        OverlayManager.showDailyExhaustedOverlay(this)
        updateNotification("DAILY LIMIT REACHED ☠", "Instagram is DONE for today.")
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun updateNotification(title: String, body: String) {
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIF_ID, buildNotification(title, body))
    }

    private fun buildNotification(title: String, body: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body.ifEmpty {
                val rem = BlockerState.getRemainingSeconds(this)
                "${rem / 60}m ${rem % 60}s left today"
            })
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID,
            "Doomscroll Destroyer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the blocker alive"
        }
        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
