package com.doomscroll.destroyer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.doomscroll.destroyer.overlay.OverlayManager
import com.doomscroll.destroyer.utils.BlockerState

/**
 * InstagramWatcherService
 *
 * This is the core detection engine. Android calls onAccessibilityEvent()
 * every time any app's window changes foreground. We watch for Instagram's
 * package name and trigger the blocker accordingly.
 *
 * This service CANNOT be stopped by the user without going to
 * Accessibility Settings and disabling it manually. It also restarts
 * automatically after reboots (via BootReceiver).
 *
 * Watched packages (expandable):
 *   - com.instagram.android
 *   - com.zhiliaoapp.musically  (TikTok)
 *   - com.ss.android.ugc.trill  (TikTok alt)
 */
class InstagramWatcherService : AccessibilityService() {

    companion object {
        val WATCHED_PACKAGES = setOf(
            "com.instagram.android",
            // Uncomment to also block TikTok:
            // "com.zhiliaoapp.musically",
            // "com.ss.android.ugc.trill",
        )
        private const val OUR_PACKAGE = "com.doomscroll.destroyer"
    }

    private var currentForegroundPackage: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Configure what events we want to listen to
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        // Start the foreground service that keeps timers ticking
        val intent = Intent(this, BlockerForegroundService::class.java)
        startForegroundService(intent)

        // Check if it's a new day and reset if needed
        BlockerState.checkAndResetIfNewDay(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Ignore our own overlay windows
        if (pkg == OUR_PACKAGE) return

        val wasInstagram = currentForegroundPackage in WATCHED_PACKAGES
        val isInstagram = pkg in WATCHED_PACKAGES
        currentForegroundPackage = pkg

        when {
            // Instagram just came to foreground
            isInstagram && !wasInstagram -> onInstagramOpened()

            // Instagram just went to background
            !isInstagram && wasInstagram -> onInstagramClosed()
        }
    }

    private fun onInstagramOpened() {
        BlockerState.checkAndResetIfNewDay(this)

        // If already blocked, immediately show overlay
        if (BlockerState.isBlocked(this)) {
            OverlayManager.showBlockOverlay(this)
            // Kick user back to home screen
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        // Not blocked yet — start counting
        BlockerState.onInstagramOpened(this)

        // Tell foreground service to start ticking
        val intent = Intent(this, BlockerForegroundService::class.java).apply {
            action = BlockerForegroundService.ACTION_START_TICKING
        }
        startForegroundService(intent)
    }

    private fun onInstagramClosed() {
        BlockerState.onInstagramClosed(this)

        // Tell foreground service to pause ticking
        val intent = Intent(this, BlockerForegroundService::class.java).apply {
            action = BlockerForegroundService.ACTION_PAUSE_TICKING
        }
        startService(intent)
    }

    override fun onInterrupt() {
        OverlayManager.dismiss(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayManager.dismiss(this)
    }
}
