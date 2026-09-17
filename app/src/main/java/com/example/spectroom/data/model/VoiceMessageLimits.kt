package com.example.spectroom.data.model

/** Mirrors iOS `VoiceMessageLimits` — hard cap for chat voice recordings. */
object VoiceMessageLimits {
    /** Maximum recording length (1 minute). */
    const val MAX_DURATION_SECONDS: Double = 60.0
    const val MAX_DURATION_MILLIS: Int = 60_000
}
