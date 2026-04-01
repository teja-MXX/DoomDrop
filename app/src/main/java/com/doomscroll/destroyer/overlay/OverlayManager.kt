package com.doomscroll.destroyer.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.doomscroll.destroyer.R
import com.doomscroll.destroyer.utils.BlockerState
import com.doomscroll.destroyer.utils.RoastContent

/**
 * OverlayManager
 *
 * Uses SYSTEM_ALERT_WINDOW permission to draw a full-screen view
 * directly over Instagram (or any app). The overlay:
 *  - Covers the entire screen including status bar area
 *  - Cannot be dismissed by the user (no back button escape)
 *  - Shows the roast message + countdown
 *  - Updates the countdown every second
 */
object OverlayManager {

    private var overlayView: View? = null
    private var countdownHandler: Handler? = null
    private var countdownRunnable: Runnable? = null

    fun showBlockOverlay(context: Context) {
        // If already blocked and overlay is showing, just refresh the message
        if (overlayView != null) {
            refreshMessage(context)
            return
        }
        when {
            BlockerState.isDailyExhausted(context) -> showDailyExhaustedOverlay(context)
            BlockerState.isLockedOut(context) -> showLockoutOverlay(context)
        }
    }

    fun showLockoutOverlay(context: Context) {
        dismiss(context)
        showOverlay(
            context = context,
            roastText = RoastContent.randomLockoutRoast(),
            faceEmoji = RoastContent.randomFace(),
            mode = OverlayMode.LOCKOUT
        )
    }

    fun showDailyExhaustedOverlay(context: Context) {
        dismiss(context)
        showOverlay(
            context = context,
            roastText = RoastContent.randomDailyRoast(),
            faceEmoji = RoastContent.randomFace(),
            mode = OverlayMode.DAILY_EXHAUSTED
        )
    }

    private fun showOverlay(
        context: Context,
        roastText: String,
        faceEmoji: String,
        mode: OverlayMode
    ) {
        val appContext = context.applicationContext
        if (!android.provider.Settings.canDrawOverlays(appContext)) return
        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE removed so back-press doesn't bypass
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val inflater = LayoutInflater.from(appContext)
        val view = inflater.inflate(R.layout.overlay_roast, null)

        // Populate content
        view.findViewById<TextView>(R.id.tv_roast_message).text = roastText
        view.findViewById<TextView>(R.id.tv_doodle_face).text = faceEmoji

        val tvHeadline = view.findViewById<TextView>(R.id.tv_headline)
        val tvCountdownLabel = view.findViewById<TextView>(R.id.tv_countdown_label)
        val tvCountdown = view.findViewById<TextView>(R.id.tv_countdown)

        when (mode) {
            OverlayMode.LOCKOUT -> {
                tvHeadline.text = "YOU\nLITERALLY\nCANNOT."
                tvCountdownLabel.text = "INSTAGRAM RETURNS IN"
                startCountdown(tvCountdown, mode, appContext)
            }
            OverlayMode.DAILY_EXHAUSTED -> {
                tvHeadline.text = "20 MINS.\nGONE.\nBYE."
                tvCountdownLabel.text = "INSTAGRAM UNLOCKS AT MIDNIGHT"
                startCountdown(tvCountdown, mode, appContext)
            }
        }

        wm.addView(view, params)
        overlayView = view
    }

    private fun startCountdown(tvCountdown: TextView, mode: OverlayMode, context: Context) {
        val handler = Handler(Looper.getMainLooper())
        countdownHandler = handler

        val runnable = object : Runnable {
            override fun run() {
                val text = when (mode) {
                    OverlayMode.LOCKOUT -> {
                        val secs = BlockerState.getLockoutRemainingSeconds(context)
                        if (secs <= 0) {
                            // Lockout ended — dismiss overlay
                            dismiss(context)
                            return
                        }
                        "${secs / 60}:${String.format("%02d", secs % 60)}"
                    }
                    OverlayMode.DAILY_EXHAUSTED -> {
                        // Show time until midnight
                        val now = System.currentTimeMillis()
                        val cal = java.util.Calendar.getInstance().apply {
                            add(java.util.Calendar.DAY_OF_MONTH, 1)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        val secs = (cal.timeInMillis - now) / 1000
                        val h = secs / 3600
                        val m = (secs % 3600) / 60
                        val s = secs % 60
                        "$h:${String.format("%02d", m)}:${String.format("%02d", s)}"
                    }
                }
                tvCountdown.text = text
                handler.postDelayed(this, 1000)
            }
        }
        countdownRunnable = runnable
        handler.post(runnable)
    }

    private fun refreshMessage(context: Context) {
        val view = overlayView ?: return
        view.findViewById<TextView>(R.id.tv_roast_message).text =
            if (BlockerState.isDailyExhausted(context))
                RoastContent.randomDailyRoast()
            else
                RoastContent.randomLockoutRoast()
        view.findViewById<TextView>(R.id.tv_doodle_face).text = RoastContent.randomFace()
    }

    fun dismiss(context: Context) {
        countdownRunnable?.let { countdownHandler?.removeCallbacks(it) }
        countdownHandler = null
        countdownRunnable = null

        val view = overlayView ?: return
        try {
            val wm = context.applicationContext
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
        } catch (_: Exception) {}
        overlayView = null
    }

    enum class OverlayMode { LOCKOUT, DAILY_EXHAUSTED }
}
