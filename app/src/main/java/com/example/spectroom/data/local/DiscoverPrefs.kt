package com.example.spectroom.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.spectroom.data.model.DiscoverDistanceOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Per-account discover like/pass quota (local calendar day).
 * Mirrors iOS `DiscoverDailyLimit` — swipe/undo limits driven by [PlanEntitlementsStore].
 */
class DiscoverDailyLimitStore(
    context: Context,
    private val planStore: PlanEntitlementsStore,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun maxSwipesPerDay(): Int = planStore.currentPlanTier.dailyDiscoverSwipeLimit

    /** `null` means unlimited undos (Premium). */
    fun maxUndosPerDay(): Int? = planStore.currentPlanTier.dailyDiscoverUndoLimit

    fun currentCount(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        ensureToday(accountKey)
        return prefs.getInt(countKey(accountKey), 0)
    }

    fun recordSwipe(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        val count = currentCount(accountKey) + 1
        prefs.edit()
            .putString(dateKey(accountKey), todayKey())
            .putInt(countKey(accountKey), count)
            .apply()
        return count
    }

    fun undoLastSwipe(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        val count = maxOf(0, currentCount(accountKey) - 1)
        prefs.edit()
            .putString(dateKey(accountKey), todayKey())
            .putInt(countKey(accountKey), count)
            .apply()
        return count
    }

    fun hasReachedLimit(accountKey: String?): Boolean =
        currentCount(accountKey) >= maxSwipesPerDay()

    fun currentUndoCount(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        ensureToday(accountKey)
        return prefs.getInt(undoCountKey(accountKey), 0)
    }

    fun hasReachedUndoLimit(accountKey: String?): Boolean {
        val max = maxUndosPerDay() ?: return false
        return currentUndoCount(accountKey) >= max
    }

    fun recordUndo(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        val count = currentUndoCount(accountKey) + 1
        prefs.edit()
            .putString(undoDateKey(accountKey), todayKey())
            .putInt(undoCountKey(accountKey), count)
            .apply()
        return count
    }

    fun hasUsedSecondThoughtsToday(accountKey: String?): Boolean {
        if (accountKey.isNullOrBlank()) return true
        ensureToday(accountKey)
        return prefs.getBoolean(secondThoughtsKey(accountKey), false)
    }

    fun markSecondThoughtsUsed(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit()
            .putString(secondThoughtsDateKey(accountKey), todayKey())
            .putBoolean(secondThoughtsKey(accountKey), true)
            .apply()
    }

    /** Clears today's swipe/undo quota (mirrors iOS `DiscoverDailyLimit.reset()`). */
    fun reset(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        val today = todayKey()
        prefs.edit()
            .putString(dateKey(accountKey), today)
            .putInt(countKey(accountKey), 0)
            .putString(secondThoughtsDateKey(accountKey), today)
            .putBoolean(secondThoughtsKey(accountKey), false)
            .putString(undoDateKey(accountKey), today)
            .putInt(undoCountKey(accountKey), 0)
            .apply()
    }

    private fun ensureToday(accountKey: String) {
        val today = todayKey()
        if (prefs.getString(dateKey(accountKey), null) != today) {
            prefs.edit()
                .putString(dateKey(accountKey), today)
                .putInt(countKey(accountKey), 0)
                .putString(secondThoughtsDateKey(accountKey), today)
                .putBoolean(secondThoughtsKey(accountKey), false)
                .putString(undoDateKey(accountKey), today)
                .putInt(undoCountKey(accountKey), 0)
                .apply()
        }
    }

    private fun todayKey(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
            calendar = java.util.Calendar.getInstance()
        }
        return formatter.format(Date())
    }

    private fun dateKey(accountKey: String) = "discoverDailySwipeDate_$accountKey"
    private fun countKey(accountKey: String) = "discoverDailySwipeCount_$accountKey"
    private fun secondThoughtsKey(accountKey: String) = "discoverSecondThoughtsUsed_$accountKey"
    private fun secondThoughtsDateKey(accountKey: String) = "discoverSecondThoughtsDate_$accountKey"
    private fun undoDateKey(accountKey: String) = "discoverDailyUndoDate_$accountKey"
    private fun undoCountKey(accountKey: String) = "discoverDailyUndoCount_$accountKey"

    companion object {
        private const val PREFS_NAME = "spectroom_discover"
    }
}

class DiscoverDistanceStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spectroom_discover", Context.MODE_PRIVATE)

    fun currentOption(accountKey: String?): DiscoverDistanceOption {
        if (accountKey.isNullOrBlank()) return DiscoverDistanceOption.DEFAULT
        if (!prefs.contains(levelKey(accountKey))) return DiscoverDistanceOption.DEFAULT
        return DiscoverDistanceOption.fromLevel(prefs.getInt(levelKey(accountKey), DiscoverDistanceOption.DEFAULT.level))
    }

    fun setOption(accountKey: String?, option: DiscoverDistanceOption) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit().putInt(levelKey(accountKey), option.level).apply()
    }

    fun resetToDefault(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit().putInt(levelKey(accountKey), DiscoverDistanceOption.DEFAULT.level).apply()
    }

    private fun levelKey(accountKey: String) = "discoverMaxDistanceLevel_$accountKey"
}
