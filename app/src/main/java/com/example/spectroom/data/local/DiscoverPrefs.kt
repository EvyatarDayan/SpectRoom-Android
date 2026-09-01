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
 * Mirrors iOS `DiscoverDailyLimit` (10 swipes/day, undo, second thoughts).
 */
class DiscoverDailyLimitStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
        currentCount(accountKey) >= MAX_SWIPES_PER_DAY

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

    /** Clears today's swipe quota (mirrors iOS `DiscoverDailyLimit.reset()`). */
    fun reset(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        val today = todayKey()
        prefs.edit()
            .putString(dateKey(accountKey), today)
            .putInt(countKey(accountKey), 0)
            .putString(secondThoughtsDateKey(accountKey), today)
            .putBoolean(secondThoughtsKey(accountKey), false)
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

    companion object {
        const val MAX_SWIPES_PER_DAY = 10
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
