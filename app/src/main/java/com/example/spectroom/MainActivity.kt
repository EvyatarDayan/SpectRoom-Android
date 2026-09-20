package com.example.spectroom

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.spectroom.data.local.LocalNotificationHelper
import com.example.spectroom.data.remote.SpectRoomMessagingService
import com.example.spectroom.ui.SpectRoomApp

import com.example.spectroom.data.local.AppPrefsStore
import com.example.spectroom.ui.locale.AppLocale

class MainActivity : AppCompatActivity() {
    private var pendingNotificationDeepLink by mutableStateOf<NotificationDeepLink?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        val languageCode = AppPrefsStore.readLanguageCode(newBase)
        super.attachBaseContext(AppLocale.localizedContext(newBase, languageCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingNotificationDeepLink = intent.toNotificationDeepLink()
        enableEdgeToEdge()
        setContent {
            SpectRoomApp(
                pendingNotificationDeepLink = pendingNotificationDeepLink,
                onNotificationDeepLinkConsumed = { pendingNotificationDeepLink = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationDeepLink = intent.toNotificationDeepLink()
    }
}

sealed class NotificationDeepLink {
    data class Chat(
        val userId: String,
        val userName: String,
        val userPhoto: String?,
    ) : NotificationDeepLink()

    data class Match(
        val userId: String,
        val userName: String,
        val userPhoto: String?,
        val matchId: String?,
    ) : NotificationDeepLink()

    data object AccountRestored : NotificationDeepLink()
}

/** @deprecated Prefer [NotificationDeepLink]; kept for call-site clarity during migration. */
typealias ChatDeepLink = NotificationDeepLink.Chat

fun Intent?.toNotificationDeepLink(): NotificationDeepLink? {
    if (this == null) return null
    val type = getStringExtra(SpectRoomMessagingService.EXTRA_NOTIFICATION_TYPE)
        ?.takeIf { it.isNotBlank() }
        ?: when {
            hasExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_ID) ->
                LocalNotificationHelper.TYPE_MATCH
            else -> LocalNotificationHelper.TYPE_MESSAGE
        }
    return when (type) {
        LocalNotificationHelper.TYPE_ACCOUNT_RESTORED,
        "moderation_account_restored",
        -> NotificationDeepLink.AccountRestored
        LocalNotificationHelper.TYPE_MATCH -> {
            val id = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_ID)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            NotificationDeepLink.Match(
                userId = id,
                userName = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_NAME).orEmpty(),
                userPhoto = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_PHOTO),
                matchId = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_ID),
            )
        }
        else -> {
            val id = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_ID)
                ?.takeIf { it.isNotBlank() }
                ?: return null
            NotificationDeepLink.Chat(
                userId = id,
                userName = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_NAME).orEmpty(),
                userPhoto = getStringExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_PHOTO),
            )
        }
    }
}
