package com.example.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object HardwareButtonController {
    var isEnabled by mutableStateOf(false)
    
    // Remote state tracking variables
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
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }
    
    fun unregister(listener: Listener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    fun hasActiveListeners(): Boolean {
        return synchronized(listeners) {
            listeners.isNotEmpty()
        }
    }
    
    fun dispatchSpeedUp(force: Boolean = false) {
        if (!isEnabled && !force) return
        synchronized(listeners) {
            listeners.forEach { it.onSpeedUp() }
        }
    }
    
    fun dispatchSpeedDown(force: Boolean = false) {
        if (!isEnabled && !force) return
        synchronized(listeners) {
            listeners.forEach { it.onSpeedDown() }
        }
    }
    
    fun dispatchPlayPause(force: Boolean = false) {
        if (!isEnabled && !force) return
        synchronized(listeners) {
            listeners.forEach { it.onPlayPause() }
        }
    }
    
    fun dispatchSkipToNextBookmark(force: Boolean = false) {
        if (!isEnabled && !force) return
        synchronized(listeners) {
            listeners.forEach { it.onSkipToNextBookmark() }
        }
    }

    fun dispatchPrevBookmark(force: Boolean = false) {
        if (!isEnabled && !force) return
        synchronized(listeners) {
            listeners.forEach { it.onPrevBookmark() }
        }
    }
}
