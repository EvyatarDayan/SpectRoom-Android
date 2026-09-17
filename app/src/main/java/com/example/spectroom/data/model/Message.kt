package com.example.spectroom.data.model

import java.util.Date
import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val senderId: String,
    val senderFirebaseId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val createdAt: Date = Date(),
    val isRead: Boolean = false,
    val readAt: Date? = null,
    val voiceDurationSeconds: Double? = null,
)

enum class MessageType(val rawValue: String) {
    TEXT("text"),
    IMAGE("image"),
    VOICE("voice"),
    VIDEO("video"),
    SYSTEM("system");

    companion object {
        fun fromRaw(value: String): MessageType =
            entries.firstOrNull { it.rawValue == value } ?: TEXT
    }
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val participantIds: List<String>,
    val lastMessage: Message? = null,
    val createdAt: Date = Date(),
    val isActive: Boolean = true,
    val unreadCount: Int = 0,
)

data class ConversationDisplay(
    val id: String,
    val otherUserId: String,
    val otherUserName: String,
    val otherUserPhotoURL: String?,
    val lastMessage: String,
    val lastMessageAt: Date,
    val timestamp: String,
    val isOnline: Boolean,
    val unreadCount: Int,
)

data class MessageDisplay(
    val id: String,
    val content: String,
    val messageType: MessageType,
    val senderName: String,
    val recipientName: String,
    val senderId: String,
    val recipientId: String,
    val createdAt: Date,
    val isRead: Boolean,
    val senderProfilePhotoURL: String? = null,
    val recipientProfilePhotoURL: String? = null,
    val voiceDurationSeconds: Double? = null,
    val thumbnailURL: String? = null,
    val videoDurationSeconds: Double? = null,
) {
    val deduplicationKey: String
        get() = "${senderId}_${recipientId}_${messageType.rawValue}_$content"
}

data class ConversationMessagesPage(
    val messages: List<MessageDisplay>,
    val hasMoreOlder: Boolean,
)
