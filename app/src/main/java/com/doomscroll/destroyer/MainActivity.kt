package com.doomscroll.destroyer

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.doomscroll.destroyer.receiver.scheduleMidnightReset
import com.doomscroll.destroyer.service.BlockerForegroundService
import com.doomscroll.destroyer.utils.BlockerState

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BlockerState.checkAndResetIfNewDay(this)
        scheduleMidnightReset(this)

        // Start the foreground service only if overlay permission is granted
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, BlockerForegroundService::class.java))
        }

        setupButtons()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        BlockerState.checkAndResetIfNewDay(this)
        if (Settings.canDrawOverlays(this)) {
            startForegroundService(Intent(this, BlockerForegroundService::class.java))
        }
        handler.post(uiUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(uiUpdateRunnable)
    }

    private fun updateUI() {
        val remaining = BlockerState.getRemainingSeconds(this)
        val sessionSec = BlockerState.getSessionSeconds(this)
        val tripSec = BlockerState.getTripSeconds(this)
        val sessionLeft = tripSec - sessionSec
        val pct = ((BlockerState.getDailyLimitSeconds(this) - remaining).toFloat() /
                BlockerState.getDailyLimitSeconds(this) * 100).toInt()
        val lockouts = BlockerState.getLockoutCount(this)

        // Format remaining daily time
        val rm = remaining / 60
        val rs = remaining % 60
        findViewById<TextView>(R.id.tv_daily_remaining)
            .text = "${rm}m ${String.format("%02d", rs)}s"

        // Format session trip wire countdown
        val sm = sessionLeft / 60
        val ss = sessionLeft % 60
        findViewById<TextView>(R.id.tv_session_remaining)
            .text = "${sm}m ${String.format("%02d", ss)}s"

        // Progress bar
        val progressBar = findViewById<View>(R.id.view_progress)
        val parentWidth = progressBar.parent?.let { (it as View).width } ?: 0
        if (parentWidth > 0) {
            progressBar.layoutParams = progressBar.layoutParams.apply {
                width = (parentWidth * pct / 100).coerceAtLeast(0)
            }
        }

        // Status
        val tvStatus = findViewById<TextView>(R.id.tv_status)
        when {
            BlockerState.isDailyExhausted(this) -> {
                tvStatus.text = "☠ INSTAGRAM: BLOCKED (daily limit)"
            }
            BlockerState.isLockedOut(this) -> {
                val secs = BlockerState.getLockoutRemainingSeconds(this)
                tvStatus.text = "🔒 LOCKED — ${secs / 60}m ${secs % 60}s remaining"
            }
            else -> tvStatus.text = "● INSTAGRAM: FREE TO USE"
        }

        // Stats
        findViewById<TextView>(R.id.tv_lockout_count).text = lockouts.toString()
        val used = BlockerState.getUsedSeconds(this)
        val um = used / 60
        val us = used % 60
        findViewById<TextView>(R.id.tv_used_time).text = "${um}m ${String.format("%02d", us)}s"
    }

    private fun setupButtons() {
        // Navigate to accessibility settings
        findViewById<View>(R.id.btn_enable_accessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        // Overlay permission
        findViewById<View>(R.id.btn_enable_overlay).setOnClickListener {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        // Usage stats permission
        findViewById<View>(R.id.btn_enable_usage_stats).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun checkPermissions() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasUsageStats = hasUsageStatsPermission()
        // Accessibility is harder to check directly — just show the button

        findViewById<View>(R.id.btn_enable_overlay)
            .alpha = if (hasOverlay) 0.4f else 1f
        findViewById<View>(R.id.btn_enable_usage_stats)
            .alpha = if (hasUsageStats) 0.4f else 1f
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
