package com.example.spectroom.data.remote

import android.util.Log
import com.example.spectroom.data.local.AppPrefsStore
import com.example.spectroom.data.local.LocalNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SpectRoomMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed")
        scope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            runCatching {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
                    .await()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message from=${message.from} data=${message.data}")
        val prefs = AppPrefsStore(this)
        if (!prefs.notificationsEnabled) return

        val helper = LocalNotificationHelper(this)
        val type = (message.data["type"] ?: message.data["notificationType"] ?: "message")
            .trim()
            .lowercase()
        val badge = listOf("badge", "notificationCount", "unreadCount")
            .firstNotNullOfOrNull { key -> message.data[key]?.toIntOrNull() }
            ?: 1

        when (type) {
            LocalNotificationHelper.TYPE_MATCH, "new_match", "newmatch" -> {
                if (prefs.shouldSuppressMatchNotification()) return
                val matchId = message.data["matchId"]
                    ?: message.data["match_id"]
                    ?: message.data["id"]
                    ?: "match_${System.currentTimeMillis()}"
                val matchUserId = message.data["matchUserId"]
                    ?: message.data["otherUserId"]
                    ?: message.data["userId"]
                    ?: message.data["senderId"]
                    ?: ""
                val matchName = message.data["matchName"]
                    ?: message.data["name"]
                    ?: message.data["senderName"]
                    ?: message.notification?.title
                    ?: getString(com.example.spectroom.R.string.app_name)
                val matchPhoto = message.data["matchPhoto"]
                    ?: message.data["photoURL"]
                    ?: message.data["senderPhoto"]
                helper.notifyNewMatch(
                    matchUserId = matchUserId,
                    matchName = matchName,
                    matchId = matchId,
                    matchPhoto = matchPhoto,
                    badgeCount = badge,
                )
            }
            "moderation_account_restored" -> {
                prefs.markRequiresRestoredAcknowledgement()
                AccountRestoredEvents.emit()
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: getString(com.example.spectroom.R.string.admin_restored_popup_title)
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: getString(com.example.spectroom.R.string.admin_restored_popup_message)
                helper.notifyModeration(
                    type = LocalNotificationHelper.TYPE_ACCOUNT_RESTORED,
                    title = title,
                    body = body,
                    badgeCount = badge,
                )
            }
            "moderation_warning",
            "moderation_freeze",
            "moderation_freeze_extended",
            "moderation_unfreeze",
            "moderation_ban",
            "moderation_unban",
            -> {
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: getString(com.example.spectroom.R.string.app_name)
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: message.data["message"]
                    ?: getString(com.example.spectroom.R.string.app_name)
                helper.notifyModeration(
                    type = type,
                    title = title,
                    body = body,
                    badgeCount = badge,
                )
            }
            else -> {
                val senderId = message.data["senderId"]
                    ?: message.data["sender_id"]
                    ?: message.data["fromUserId"]
                    ?: "unknown"
                if (prefs.shouldSuppressMessageNotification(senderId)) return
                val title = message.notification?.title
                    ?: message.data["title"]
                    ?: getString(com.example.spectroom.R.string.messages_new_message)
                val body = message.notification?.body
                    ?: message.data["body"]
                    ?: message.data["preview"]
                    ?: getString(com.example.spectroom.R.string.app_name)
                val senderName = message.data["senderName"]
                    ?: message.data["sender_name"]
                    ?: message.data["name"]
                    ?: title
                val senderPhoto = message.data["senderPhoto"]
                    ?: message.data["sender_photo"]
                    ?: message.data["photoURL"]
                helper.notifyNewMessage(
                    senderId = senderId,
                    senderName = senderName,
                    messagePreview = body,
                    badgeCount = badge,
                    senderPhoto = senderPhoto,
                )
            }
        }
    }

    companion object {
        private const val TAG = "SpectRoomFCM"
        const val CHANNEL_ID = "spectroom_messages"
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_OPEN_CHAT_USER_ID = "open_chat_user_id"
        const val EXTRA_OPEN_CHAT_USER_NAME = "open_chat_user_name"
        const val EXTRA_OPEN_CHAT_USER_PHOTO = "open_chat_user_photo"
        const val EXTRA_OPEN_MATCH_USER_ID = "open_match_user_id"
        const val EXTRA_OPEN_MATCH_USER_NAME = "open_match_user_name"
        const val EXTRA_OPEN_MATCH_USER_PHOTO = "open_match_user_photo"
        const val EXTRA_OPEN_MATCH_ID = "open_match_id"
    }
}
