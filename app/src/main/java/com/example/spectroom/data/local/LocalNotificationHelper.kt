package com.example.spectroom.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spectroom.MainActivity
import com.example.spectroom.R
import com.example.spectroom.data.remote.SpectRoomMessagingService

/**
 * Local notifications + launcher badge number (via NotificationCompat.setNumber).
 * Mirrors iOS NotificationService for background message/match banners.
 */
class LocalNotificationHelper(private val context: Context) {
    private val prefs = AppPrefsStore(context)

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            SpectRoomMessagingService.CHANNEL_ID,
            context.getString(R.string.notifications_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setShowBadge(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyNewMessage(
        senderId: String,
        senderName: String,
        messagePreview: String,
        badgeCount: Int,
        senderPhoto: String? = null,
    ) {
        if (!prefs.notificationsEnabled) return
        if (prefs.shouldSuppressMessageNotification(senderId)) return
        ensureChannel()
        val firstName = senderName.substringBefore(' ').ifBlank { senderName }.ifBlank {
            context.getString(R.string.app_name)
        }
        val preview = sanitizedPreview(messagePreview)
        val title = context.getString(R.string.message_notification_push_title)
        val body = context.getString(R.string.message_notification_push_body, firstName, preview)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SpectRoomMessagingService.EXTRA_NOTIFICATION_TYPE, TYPE_MESSAGE)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_ID, senderId)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_NAME, senderName)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_CHAT_USER_PHOTO, senderPhoto)
        }
        val pending = PendingIntent.getActivity(
            context,
            ("msg_$senderId").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SpectRoomMessagingService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(badgeCount.coerceAtLeast(0))
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pending)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationIdForMessage(senderId), notification)
        }
        syncLauncherBadge(badgeCount)
    }

    fun notifyNewMatch(
        matchUserId: String,
        matchName: String,
        matchId: String,
        matchPhoto: String? = null,
        badgeCount: Int = 1,
    ) {
        if (!prefs.notificationsEnabled) return
        if (prefs.shouldSuppressMatchNotification()) return
        ensureChannel()
        val firstName = matchName.substringBefore(' ').ifBlank { matchName }
        val title = context.getString(R.string.match_notification_title)
        val body = context.getString(R.string.match_notification_body, firstName)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SpectRoomMessagingService.EXTRA_NOTIFICATION_TYPE, TYPE_MATCH)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_ID, matchUserId)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_NAME, matchName)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_USER_PHOTO, matchPhoto)
            putExtra(SpectRoomMessagingService.EXTRA_OPEN_MATCH_ID, matchId)
        }
        val pending = PendingIntent.getActivity(
            context,
            ("match_$matchId").hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SpectRoomMessagingService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setNumber(badgeCount.coerceAtLeast(1))
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pending)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationIdForMatch(matchId), notification)
        }
    }

    fun syncLauncherBadge(count: Int) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (count <= 0) {
            manager.cancel(BADGE_NOTIFICATION_ID)
            return
        }
        val notification = NotificationCompat.Builder(context, SpectRoomMessagingService.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.messages_unread_badge, count))
            .setNumber(count)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setOngoing(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(BADGE_NOTIFICATION_ID, notification) }
    }

    fun clearBadge() {
        NotificationManagerCompat.from(context).cancel(BADGE_NOTIFICATION_ID)
    }

    fun cancelMessageNotification(senderId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdForMessage(senderId))
    }

    fun cancelMatchNotification(matchId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdForMatch(matchId))
    }

    fun cancelAllMessageNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun notificationIdForMessage(senderId: String): Int = ("msg_$senderId").hashCode()
    private fun notificationIdForMatch(matchId: String): Int = ("match_$matchId").hashCode()

    companion object {
        private const val BADGE_NOTIFICATION_ID = 42_001
        const val TYPE_MESSAGE = "message"
        const val TYPE_MATCH = "match"

        fun sanitizedPreview(content: String): String {
            val trimmed = content.trim()
            if (trimmed.isBlank()) return "…"
            val lower = trimmed.lowercase()
            return when {
                lower.startsWith("http") && (lower.contains("voice") || lower.contains(".m4a")) ->
                    "🎤 Voice message"
                lower.startsWith("http") && (
                    lower.contains("image") ||
                        lower.contains(".jpg") ||
                        lower.contains(".jpeg") ||
                        lower.contains(".png") ||
                        lower.contains("message_images")
                    ) -> "📷 Photo"
                trimmed.length > 50 -> trimmed.take(50) + "…"
                else -> trimmed
            }
        }
    }
}
