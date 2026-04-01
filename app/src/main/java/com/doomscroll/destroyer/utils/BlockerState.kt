package com.doomscroll.destroyer.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * BlockerState — the single source of truth for all timing logic.
 *
 * RULES ENCODED HERE:
 *  - 20 min (1200s) total per day → after that, HARD LOCK until midnight
 *  - Every 1 cumulative minute of Instagram open → 30 min lockout
 *  - Seconds are tracked per-session and accumulated across opens/closes
 *  - State persists across restarts via SharedPreferences
 *  - Backup flag set so Android backup can carry state across reinstalls
 */
object BlockerState {

    private const val PREFS_NAME = "doomscroll_state"
    private const val KEY_USED_SECONDS = "used_seconds"       // total used today
    private const val KEY_SESSION_SECONDS = "session_seconds" // accumulated this 1-min window
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"     // epoch ms when lockout ends
    private const val KEY_DAILY_EXHAUSTED = "daily_exhausted" // hard lock for the day
    private const val KEY_LAST_RESET_DAY = "last_reset_day"   // day-of-year of last reset
    private const val KEY_LOCKOUT_COUNT = "lockout_count"     // shame stat
    private const val KEY_INSTA_OPEN_SINCE = "insta_open_since" // epoch ms when insta opened

    // Config (user-adjustable, stored in same prefs)
    private const val KEY_DAILY_LIMIT = "daily_limit_seconds"
    private const val KEY_TRIP_SECONDS = "trip_seconds"
    private const val KEY_LOCKOUT_DURATION = "lockout_duration_seconds"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Config ─────────────────────────────────────────────────────────────

    fun getDailyLimitSeconds(ctx: Context) =
        prefs(ctx).getInt(KEY_DAILY_LIMIT, 1200) // default 20 min

    fun getTripSeconds(ctx: Context) =
        prefs(ctx).getInt(KEY_TRIP_SECONDS, 60) // default 1 min

    fun getLockoutDurationSeconds(ctx: Context) =
        prefs(ctx).getInt(KEY_LOCKOUT_DURATION, 1800) // default 30 min

    fun setConfig(ctx: Context, dailyMin: Int, tripSec: Int, lockoutMin: Int) {
        prefs(ctx).edit()
            .putInt(KEY_DAILY_LIMIT, dailyMin * 60)
            .putInt(KEY_TRIP_SECONDS, tripSec)
            .putInt(KEY_LOCKOUT_DURATION, lockoutMin * 60)
            .apply()
    }

    // ── Daily Reset ────────────────────────────────────────────────────────

    fun checkAndResetIfNewDay(ctx: Context) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = prefs(ctx).getInt(KEY_LAST_RESET_DAY, -1)
        if (today != lastReset) {
            prefs(ctx).edit()
                .putInt(KEY_USED_SECONDS, 0)
                .putInt(KEY_SESSION_SECONDS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .putBoolean(KEY_DAILY_EXHAUSTED, false)
                .putInt(KEY_LOCKOUT_COUNT, 0)
                .putLong(KEY_INSTA_OPEN_SINCE, 0L)
                .putInt(KEY_LAST_RESET_DAY, today)
                .apply()
        }
    }

    // ── State Queries ──────────────────────────────────────────────────────

    fun isDailyExhausted(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DAILY_EXHAUSTED, false)

    fun isLockedOut(ctx: Context): Boolean {
        val until = prefs(ctx).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    fun isBlocked(ctx: Context): Boolean =
        isDailyExhausted(ctx) || isLockedOut(ctx)

    fun getLockoutRemainingSeconds(ctx: Context): Long {
        val until = prefs(ctx).getLong(KEY_LOCKOUT_UNTIL, 0L)
        return maxOf(0L, (until - System.currentTimeMillis()) / 1000)
    }

    fun getUsedSeconds(ctx: Context): Int =
        prefs(ctx).getInt(KEY_USED_SECONDS, 0)

    fun getRemainingSeconds(ctx: Context): Int =
        maxOf(0, getDailyLimitSeconds(ctx) - getUsedSeconds(ctx))

    fun getSessionSeconds(ctx: Context): Int =
        prefs(ctx).getInt(KEY_SESSION_SECONDS, 0)

    fun getLockoutCount(ctx: Context): Int =
        prefs(ctx).getInt(KEY_LOCKOUT_COUNT, 0)

    // ── Instagram Open/Close Tracking ──────────────────────────────────────

    /**
     * Called when Instagram comes to foreground.
     * Records the timestamp when it was opened.
     */
    fun onInstagramOpened(ctx: Context) {
        if (prefs(ctx).getLong(KEY_INSTA_OPEN_SINCE, 0L) == 0L) {
            prefs(ctx).edit()
                .putLong(KEY_INSTA_OPEN_SINCE, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Called every second while Instagram is open (by the foreground service tick).
     * Accumulates time. Returns whether a lockout or daily-exhaust just triggered.
     */
    fun tickWhileOpen(ctx: Context): TickResult {
        val p = prefs(ctx)
        val usedSec = p.getInt(KEY_USED_SECONDS, 0)
        val sessionSec = p.getInt(KEY_SESSION_SECONDS, 0)
        val dailyLimit = getDailyLimitSeconds(ctx)
        val tripSec = getTripSeconds(ctx)
        val lockoutDur = getLockoutDurationSeconds(ctx)

        val newUsed = usedSec + 1
        val newSession = sessionSec + 1

        val editor = p.edit()
            .putInt(KEY_USED_SECONDS, newUsed)
            .putInt(KEY_SESSION_SECONDS, newSession)

        // Check daily exhaustion first (most severe)
        if (newUsed >= dailyLimit) {
            editor.putBoolean(KEY_DAILY_EXHAUSTED, true)
                .putLong(KEY_INSTA_OPEN_SINCE, 0L)
                .apply()
            return TickResult.DAILY_EXHAUSTED
        }

        // Check trip wire (1 cumulative min → 30 min lockout)
        if (newSession >= tripSec) {
            val lockoutUntil = System.currentTimeMillis() + (lockoutDur * 1000L)
            val count = p.getInt(KEY_LOCKOUT_COUNT, 0) + 1
            editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                .putInt(KEY_SESSION_SECONDS, 0) // reset session window
                .putInt(KEY_LOCKOUT_COUNT, count)
                .putLong(KEY_INSTA_OPEN_SINCE, 0L)
                .apply()
            return TickResult.LOCKOUT_TRIGGERED
        }

        editor.apply()
        return TickResult.CONTINUE
    }

    /**
     * Called when Instagram goes to background.
     * Does NOT reset session seconds — they keep accumulating across opens!
     */
    fun onInstagramClosed(ctx: Context) {
        prefs(ctx).edit()
            .putLong(KEY_INSTA_OPEN_SINCE, 0L)
            .apply()
    }

    enum class TickResult {
        CONTINUE,           // still within limits
        LOCKOUT_TRIGGERED,  // 1 min trip wire hit → show 30 min lockout
        DAILY_EXHAUSTED     // 20 min daily budget gone → permanent lock
    }
}
