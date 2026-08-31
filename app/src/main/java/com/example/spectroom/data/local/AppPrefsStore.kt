package com.example.spectroom.data.local

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.spectroom.data.model.AppLanguage

class AppPrefsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var skipPhotoShareWarning: Boolean
        get() = prefs.getBoolean(KEY_PHOTO_WARNING, false)
        set(value) = prefs.edit().putBoolean(KEY_PHOTO_WARNING, value).apply()

    /** Local flags for FCM client-side suppress (mirrors Firestore presence fields). */
    var appIsActive: Boolean
        get() = prefs.getBoolean(KEY_APP_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_ACTIVE, value).apply()

    var messagesTabActive: Boolean
        get() = prefs.getBoolean(KEY_MESSAGES_TAB, false)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGES_TAB, value).apply()

    var openConversationUserId: String?
        get() = prefs.getString(KEY_OPEN_CONVERSATION, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_OPEN_CONVERSATION)
                else putString(KEY_OPEN_CONVERSATION, value)
            }.apply()
        }

    fun shouldSuppressMessageNotification(senderId: String?): Boolean {
        if (!appIsActive) return false
        if (messagesTabActive) return true
        val openId = openConversationUserId
        return !senderId.isNullOrBlank() && openId == senderId
    }

    fun shouldSuppressMatchNotification(): Boolean = appIsActive


    fun shouldShowCompleteProfileWelcome(userId: String?): Boolean {
        if (userId.isNullOrBlank()) return !prefs.getBoolean(KEY_WELCOME_LEGACY, false)
        return !prefs.getBoolean(welcomeKey(userId), false)
    }

    fun markCompleteProfileWelcomeSeen(userId: String?) {
        if (userId.isNullOrBlank()) {
            prefs.edit().putBoolean(KEY_WELCOME_LEGACY, true).apply()
        } else {
            prefs.edit().putBoolean(welcomeKey(userId), true).apply()
        }
    }

    fun prepareCompleteProfileWelcomeForSignup(userId: String) {
        prefs.edit().remove(welcomeKey(userId)).apply()
    }

    private fun welcomeKey(userId: String) = "$KEY_WELCOME_PREFIX$userId"

    fun shouldShowBlockedAlert(userId: String): Boolean =
        userId !in prefs.getStringSet(KEY_BLOCKED_ALERT_SKIP, emptySet()).orEmpty()

    fun skipBlockedAlert(userId: String) {
        val next = prefs.getStringSet(KEY_BLOCKED_ALERT_SKIP, emptySet()).orEmpty().toMutableSet()
        next.add(userId)
        prefs.edit().putStringSet(KEY_BLOCKED_ALERT_SKIP, next).apply()
    }

    fun applyStoredLocale() {
        applyLocale(languageCode)
    }

    fun setLanguage(language: AppLanguage) {
        languageCode = language.code
        applyLocale(language.code)
    }

    var pendingMfaPhoneE164: String?
        get() = prefs.getString(KEY_PENDING_MFA_PHONE, null)
        set(value) {
            prefs.edit().apply {
                if (value.isNullOrBlank()) remove(KEY_PENDING_MFA_PHONE)
                else putString(KEY_PENDING_MFA_PHONE, value)
            }.apply()
        }

    private fun applyLocale(code: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
    }

    companion object {
        private const val PREFS = "spectroom_app"
        private const val KEY_DARK = "isDarkMode"
        private const val KEY_ONBOARDING = "hasCompletedOnboarding"
        private const val KEY_LANGUAGE = "selectedLanguage"
        private const val KEY_NOTIFICATIONS = "notificationsEnabled"
        private const val KEY_SOUND = "soundEnabled"
        private const val KEY_PHOTO_WARNING = "dontShowPhotoShareWarning"
        private const val KEY_APP_ACTIVE = "notifAppIsActive"
        private const val KEY_MESSAGES_TAB = "notifMessagesTabActive"
        private const val KEY_OPEN_CONVERSATION = "notifOpenConversationUserId"
        private const val KEY_PENDING_MFA_PHONE = "pendingMfaPhoneE164"
        private const val KEY_BLOCKED_ALERT_SKIP = "blockedAlertSkipIds"
        private const val KEY_WELCOME_LEGACY = "dontShowCompleteProfileWelcome"
        private const val KEY_WELCOME_PREFIX = "dontShowCompleteProfileWelcome."

        fun readLanguageCode(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en") ?: "en"
    }
}
