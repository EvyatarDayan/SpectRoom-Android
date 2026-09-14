package com.example.spectroom.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.spectroom.data.model.BillingInterval
import com.example.spectroom.data.model.PlanTier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Local cache of Firestore `planTier` + `billingInterval`.
 * Firestore is the source of truth; this store mirrors it for offline gates.
 */
class PlanEntitlementsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentPlanTier: PlanTier
        get() = PlanTier.fromRaw(prefs.getString(KEY_PLAN_TIER, PlanTier.FREE.rawValue))
        set(value) {
            // commit() so plan changes survive process death immediately (like iOS UserDefaults).
            prefs.edit().putString(KEY_PLAN_TIER, value.rawValue).commit()
        }

    var currentBillingInterval: BillingInterval
        get() = BillingInterval.fromRaw(
            prefs.getString(KEY_BILLING_INTERVAL, BillingInterval.MONTHLY.rawValue),
        )
        set(value) {
            prefs.edit().putString(KEY_BILLING_INTERVAL, value.rawValue).commit()
        }

    fun syncPlanTier(tier: PlanTier) {
        syncBillingPlan(tier, interval = null)
    }

    fun syncBillingPlan(tier: PlanTier, interval: BillingInterval?) {
        currentPlanTier = tier
        currentBillingInterval = when {
            tier == PlanTier.FREE -> BillingInterval.MONTHLY
            interval != null -> interval
            else -> currentBillingInterval
        }
    }

    companion object {
        const val KEY_PLAN_TIER = "billingCurrentPlanTier"
        const val KEY_BILLING_INTERVAL = "billingCurrentInterval"
        private const val PREFS_NAME = "spectroom_billing"
    }
}

/**
 * Combined daily image + voice message quota (local calendar day).
 * Mirrors iOS `MediaDailyLimit`.
 */
class MediaDailyLimitStore(
    context: Context,
    private val planStore: PlanEntitlementsStore,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun maxMediaPerDay(): Int = planStore.currentPlanTier.dailyMediaMessageLimit

    fun currentCount(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        ensureToday(accountKey)
        return prefs.getInt(countKey(accountKey), 0)
    }

    fun remaining(accountKey: String?): Int =
        maxOf(0, maxMediaPerDay() - currentCount(accountKey))

    fun hasReachedLimit(accountKey: String?): Boolean =
        currentCount(accountKey) >= maxMediaPerDay()

    fun canSend(accountKey: String?): Boolean = !hasReachedLimit(accountKey)

    fun recordSend(accountKey: String?): Int {
        if (accountKey.isNullOrBlank()) return 0
        val count = currentCount(accountKey) + 1
        prefs.edit()
            .putString(dateKey(accountKey), todayKey())
            .putInt(countKey(accountKey), count)
            .apply()
        return count
    }

    fun reset(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit()
            .putString(dateKey(accountKey), todayKey())
            .putInt(countKey(accountKey), 0)
            .apply()
    }

    private fun ensureToday(accountKey: String) {
        val today = todayKey()
        if (prefs.getString(dateKey(accountKey), null) != today) {
            prefs.edit()
                .putString(dateKey(accountKey), today)
                .putInt(countKey(accountKey), 0)
                .apply()
        }
    }

    private fun todayKey(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return formatter.format(Date())
    }

    private fun dateKey(accountKey: String) = "mediaDailySendDate_$accountKey"
    private fun countKey(accountKey: String) = "mediaDailySendCount_$accountKey"

    companion object {
        private const val PREFS_NAME = "spectroom_media_limits"
    }
}

/**
 * Local cooldown + active-window tracking for profile boosts.
 * Mirrors iOS `ProfileBoostLimit`.
 */
class ProfileBoostLimitStore(
    context: Context,
    private val planStore: PlanEntitlementsStore,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun cooldownDays(): Int? = planStore.currentPlanTier.profileBoostCooldownDays

    fun isAvailableOnPlan(): Boolean = cooldownDays() != null

    fun lastBoostAtMillis(accountKey: String?): Long? {
        if (accountKey.isNullOrBlank()) return null
        val value = prefs.getLong(lastBoostKey(accountKey), 0L)
        return value.takeIf { it > 0L }
    }

    fun boostedUntilMillis(accountKey: String?): Long? {
        if (accountKey.isNullOrBlank()) return null
        val value = prefs.getLong(boostedUntilKey(accountKey), 0L)
        if (value <= 0L || value <= System.currentTimeMillis()) return null
        return value
    }

    fun isBoostActive(accountKey: String?): Boolean = boostedUntilMillis(accountKey) != null

    fun nextBoostAvailableAtMillis(accountKey: String?): Long? {
        val days = cooldownDays() ?: return null
        val last = lastBoostAtMillis(accountKey) ?: return null
        return last + days * DAY_MS
    }

    fun canActivate(accountKey: String?): Boolean {
        if (!isAvailableOnPlan()) return false
        if (isBoostActive(accountKey)) return false
        val next = nextBoostAvailableAtMillis(accountKey) ?: return true
        return System.currentTimeMillis() >= next
    }

    fun syncFromRemote(accountKey: String?, lastBoostAtMillis: Long?, boostedUntilMillis: Long?) {
        if (accountKey.isNullOrBlank()) return
        val editor = prefs.edit()
        if (lastBoostAtMillis != null && lastBoostAtMillis > 0L) {
            editor.putLong(lastBoostKey(accountKey), lastBoostAtMillis)
        }
        if (boostedUntilMillis != null && boostedUntilMillis > System.currentTimeMillis()) {
            editor.putLong(boostedUntilKey(accountKey), boostedUntilMillis)
        } else {
            editor.remove(boostedUntilKey(accountKey))
        }
        editor.apply()
    }

    fun recordActivation(accountKey: String?, atMillis: Long = System.currentTimeMillis()) {
        if (accountKey.isNullOrBlank()) return
        val until = atMillis + ACTIVE_DURATION_MS
        prefs.edit()
            .putLong(lastBoostKey(accountKey), atMillis)
            .putLong(boostedUntilKey(accountKey), until)
            .apply()
    }

    fun reset(accountKey: String?) {
        if (accountKey.isNullOrBlank()) return
        prefs.edit()
            .remove(lastBoostKey(accountKey))
            .remove(boostedUntilKey(accountKey))
            .apply()
    }

    private fun lastBoostKey(accountKey: String) = "profileLastBoostAt_$accountKey"
    private fun boostedUntilKey(accountKey: String) = "profileBoostedUntil_$accountKey"

    companion object {
        const val ACTIVE_DURATION_MS = 24L * 60L * 60L * 1000L
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val PREFS_NAME = "spectroom_profile_boost"
    }
}
