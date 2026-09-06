package com.example.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object HardwareButtonController {
    var isEnabled by mutableStateOf(false)

    var pActiveTitle by mutableStateOf<String?>("No Active Script")
    var pIsPlaying by mutableStateOf(false)
    var pSpeed by mutableStateOf(1f)
    var pCurrentParagraph by mutableStateOf(0)
    var pTotalParagraphs by mutableStateOf(0)

    interface Listener {
        fun onSpeedUp()
        fun onSpeedDown()
        fun onPlayPause()
        fun onSkipToNextBookmark()
        fun onPrevBookmark()
    }

    private val listeners = mutableListOf<Listener>()

    fun register(listener: Listener) {
        synchronized(listeners) {
            if (listener !in listeners) listeners.add(listener)
        }
    }

    fun unregister(listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun hasActiveListeners(): Boolean = synchronized(listeners) { listeners.isNotEmpty() }

    private fun snapshotListeners(): List<Listener> = synchronized(listeners) { listeners.toList() }

    fun dispatchSpeedUp(force: Boolean = false) {
        if (!isEnabled && !force) return
        snapshotListeners().forEach { it.onSpeedUp() }
    }

    fun dispatchSpeedDown(force: Boolean = false) {
        if (!isEnabled && !force) return
        snapshotListeners().forEach { it.onSpeedDown() }
    }

    fun dispatchPlayPause(force: Boolean = false) {
        if (!isEnabled && !force) return
        snapshotListeners().forEach { it.onPlayPause() }
    }

    fun dispatchSkipToNextBookmark(force: Boolean = false) {
        if (!isEnabled && !force) return
        snapshotListeners().forEach { it.onSkipToNextBookmark() }
    }

    fun dispatchPrevBookmark(force: Boolean = false) {
        if (!isEnabled && !force) return
        snapshotListeners().forEach { it.onPrevBookmark() }
    }
}
