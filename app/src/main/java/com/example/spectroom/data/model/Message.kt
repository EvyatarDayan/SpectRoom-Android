package com.example.spectroom.data.model

import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

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
    val lastMessageDeleted: Boolean = false,
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
    val deletedForViewer: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val reactions: Map<String, String> = emptyMap(),
) {
    val isDeleted: Boolean
        get() = deletedForViewer || deletedForEveryone

    val deduplicationKey: String
        get() = if (content.startsWith("pending://")) {
            "${senderId}_${recipientId}_${messageType.rawValue}_$content"
        } else {
            id
        }

    /** Aggregated reaction chips for the bubble footer. */
    val reactionSummary: List<Pair<String, Int>>
        get() = reactions.values
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }

    fun canDeleteForEveryone(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs - createdAt.time <= DELETE_FOR_EVERYONE_WINDOW_MS

    fun markingDeleted(forEveryone: Boolean): MessageDisplay = copy(
        deletedForViewer = true,
        deletedForEveryone = forEveryone || deletedForEveryone,
    )

    fun withReactions(reactions: Map<String, String>): MessageDisplay = copy(reactions = reactions)

    fun togglingReaction(emoji: String, userId: String): MessageDisplay {
        val next = reactions.toMutableMap()
        if (next[userId] == emoji) {
            next.remove(userId)
        } else {
            next[userId] = emoji
        }
        return withReactions(next)
    }

    companion object {
        val DELETE_FOR_EVERYONE_WINDOW_MS: Long = TimeUnit.HOURS.toMillis(1)
        val QUICK_REACTION_EMOJIS = listOf("❤️", "😂", "👍", "🙏", "🔥")
    }
}

enum class MessageDeleteOption {
    FOR_ME,
    FOR_EVERYONE,
}

data class MessageSoftDeleteEvent(
    val messageId: String,
    val partnerId: String,
    val deletedForEveryone: Boolean,
    val deletedForViewer: Boolean,
    /** null means reactions were not part of this update. */
    val reactions: Map<String, String>?,
)

data class ConversationMessagesPage(
    val messages: List<MessageDisplay>,
    val hasMoreOlder: Boolean,
)
