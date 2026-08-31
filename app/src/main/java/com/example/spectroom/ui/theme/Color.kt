package com.example.spectroom.ui.theme

import androidx.compose.ui.graphics.Color

// SpectRoom brand tokens (ported from iOS SpectRoomTheme / asset colors)
val AppBackgroundLight = Color(0xFFF7F4EF)
val AppBackgroundDark = Color(0xFF121216)
val AppCardLight = Color(0xFFFFFFFF)
val AppCardDark = Color(0xFF25252E)
val AppTextLight = Color(0xFF1C1C1E)
val AppTextDark = Color(0xFFE5E5EA)
val AppSecondaryTextLight = Color(0xFF8A8680)
val AppSecondaryTextDark = Color(0xFFA1A1A6)

val EnergyLow = Color(0xFF64B5F6)
val EnergyModerate = Color(0xFFFFB74D)
val EnergyFullySocial = Color(0xFF66BB6A)
val DistanceAccent = Color(0xFF69C440)
/** Pass action (iOS uses red X). */
val PassRed = Color(0xFFE53935)
/** @deprecated Prefer [PassRed] for pass / [LikeGreen] for like. Kept for error text. */
val LikeRed = PassRed
val LikeGreen = Color(0xFF66BB6A)
val PassGray = Color(0xFF9E9E9E)
val TabSelected = Color(0xFF007AFF)
/** WhatsApp-style outgoing chat bubble (#D9FDD3). */
val OutgoingChatBubble = Color(0xFFD9FDD3)
val IncomingChatBubble = Color.White
val ChatBubbleText = Color.Black
val ChatReadMore = Color(0xFF128C78)
/** iOS systemGray2 composer bar background. */
val ChatComposerBar = Color(0xFFAEAEB2)
