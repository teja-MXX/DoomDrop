package com.doomscroll.destroyer

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * PermissionSetupActivity
 *
 * Shown on first launch. Walks the user through the 3 required
 * permission grants in order. Each step deep-links to the relevant
 * Android settings screen. Once all 3 are granted, the user is
 * taken to MainActivity and this activity is never shown again.
 *
 * Permissions needed:
 *  1. Accessibility Service  → watches Instagram foreground state
 *  2. Display over other apps → draws the roast overlay on top
 *  3. Usage stats access     → (optional backup detection method)
 */
class PermissionSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_setup)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        // If all critical permissions are granted, go to main
        if (Settings.canDrawOverlays(this) && isAccessibilityEnabled()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupButtons() {
        findViewById<View>(R.id.btn_step1).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<View>(R.id.btn_step2).setOnClickListener {
            startActivity(Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ))
        }
        findViewById<View>(R.id.btn_step3).setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun updatePermissionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasAccessibility = isAccessibilityEnabled()
        val hasUsageStats = hasUsageStatsPermission()

        setStepDone(R.id.tv_step1_status, hasAccessibility)
        setStepDone(R.id.tv_step2_status, hasOverlay)
        setStepDone(R.id.tv_step3_status, hasUsageStats)

        findViewById<View>(R.id.btn_step1).alpha = if (hasAccessibility) 0.4f else 1f
        findViewById<View>(R.id.btn_step2).alpha = if (hasOverlay) 0.4f else 1f
        findViewById<View>(R.id.btn_step3).alpha = if (hasUsageStats) 0.4f else 1f
    }

    private fun setStepDone(tvId: Int, done: Boolean) {
        val tv = findViewById<TextView>(tvId)
        tv.text = if (done) "✓ GRANTED" else "✗ NOT YET"
        tv.setTextColor(
            if (done) getColor(R.color.green_neon)
            else getColor(R.color.brand_red)
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val service = "$packageName/${packageName}.service.InstagramWatcherService"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(service)
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
