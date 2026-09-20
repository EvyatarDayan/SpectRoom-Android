package com.example.spectroom.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges FCM moderation_account_restored into [com.example.spectroom.viewmodel.AppStateViewModel]
 * so the restored popup can appear while signed out (iOS `presentAccountRestoredNotice`).
 */
object AccountRestoredEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun emit() {
        _events.tryEmit(Unit)
    }
}
