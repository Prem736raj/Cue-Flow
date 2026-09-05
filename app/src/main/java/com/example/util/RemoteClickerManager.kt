package com.example.util

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ClickerAction(val label: String, val desc: String) {
    PLAY_PAUSE("Play / Pause", "Toggle scrolling play or pause"),
    SPEED_UP("Speed Up", "Increase scrolling speed by 0.5x"),
    SPEED_DOWN("Speed Down", "Decrease scrolling speed by 0.5x"),
    PREV_BOOKMARK("Previous Bookmark", "Scroll up to the previous bookmarked line"),
    NEXT_BOOKMARK("Next Bookmark", "Scroll down to the next bookmarked line"),
    NONE("None", "Ignore this button click"),
}

/**
 * Detects external keyboard/D-pad style remotes through Android's InputManager.
 * This intentionally avoids Bluetooth APIs, bonded-device enumeration and Bluetooth runtime
 * permissions. A paired Bluetooth clicker that exposes itself as an input device is still detected.
 */
object RemoteClickerManager {
    var isConnected by mutableStateOf(false)
        private set
    var connectedDeviceName by mutableStateOf<String?>(null)
        private set
    var deviceType by mutableStateOf<String?>(null)
        private set

    // Retained for settings/UI compatibility. It now means "external input remote available".
    var bluetoothEnabled by mutableStateOf(false)
        private set

    val CUSTOMIZABLE_KEYS = listOf(
        KeyEvent.KEYCODE_VOLUME_UP to "Volume Up Action",
        KeyEvent.KEYCODE_VOLUME_DOWN to "Volume Down Action",
        KeyEvent.KEYCODE_PAGE_UP to "Page Up Action (Prev)",
        KeyEvent.KEYCODE_PAGE_DOWN to "Page Down Action (Next)",
        KeyEvent.KEYCODE_SPACE to "Spacebar Action",
        KeyEvent.KEYCODE_ENTER to "Enter / Center Click Action",
        KeyEvent.KEYCODE_DPAD_UP to "Arrow Up Action",
        KeyEvent.KEYCODE_DPAD_DOWN to "Arrow Down Action",
        KeyEvent.KEYCODE_DPAD_LEFT to "Arrow Left Action",
        KeyEvent.KEYCODE_DPAD_RIGHT to "Arrow Right Action",
    )

    private var isInitialized = false
    private lateinit var appContext: Context
    private lateinit var inputManager: InputManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = checkCurrentDevices()
        override fun onInputDeviceRemoved(deviceId: Int) = checkCurrentDevices()
        override fun onInputDeviceChanged(deviceId: Int) = checkCurrentDevices()
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        inputManager = appContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(inputDeviceListener, mainHandler)
        isInitialized = true
        checkCurrentDevices(notify = false)
    }

    fun checkCurrentDevices(notify: Boolean = true) {
        if (!isInitialized) return

        val candidate = inputManager.inputDeviceIds
            .asSequence()
            .mapNotNull(inputManager::getInputDevice)
            .firstOrNull(::isSupportedExternalRemote)

        val wasConnected = isConnected
        isConnected = candidate != null
        bluetoothEnabled = candidate != null
        connectedDeviceName = candidate?.name
        deviceType = candidate?.let(::classifyDevice)

        if (notify && wasConnected != isConnected) {
            mainHandler.post {
                val message = if (candidate != null) {
                    "Remote connected: ${candidate.name} (${classifyDevice(candidate)})"
                } else {
                    "External remote disconnected"
                }
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isSupportedExternalRemote(device: InputDevice): Boolean {
        if (device.isVirtual || !device.isExternal) return false
        val sources = device.sources
        val keyboard = sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD
        val dpad = sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD
        return keyboard || dpad
    }

    private fun classifyDevice(device: InputDevice): String {
        val name = device.name
        return when {
            name.contains("shutter", ignoreCase = true) || name.contains("selfie", ignoreCase = true) -> "Selfie Shutter"
            name.contains("present", ignoreCase = true) || name.contains("clicker", ignoreCase = true) -> "Presentation Clicker"
            name.contains("keyboard", ignoreCase = true) -> "External Keyboard"
            else -> "External Input Remote"
        }
    }

    fun getActionForKey(keyCode: Int): ClickerAction {
        if (!isInitialized) return ClickerAction.NONE
        val prefs = appContext.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        val defaultValue = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_UP -> ClickerAction.SPEED_UP.name
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> ClickerAction.SPEED_DOWN.name
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_LEFT -> ClickerAction.PREV_BOOKMARK.name
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> ClickerAction.NEXT_BOOKMARK.name
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            -> ClickerAction.PLAY_PAUSE.name
            else -> ClickerAction.NONE.name
        }
        return runCatching {
            ClickerAction.valueOf(prefs.getString("clicker_map_key_$keyCode", defaultValue) ?: defaultValue)
        }.getOrDefault(ClickerAction.NONE)
    }

    fun setActionForKey(keyCode: Int, action: ClickerAction) {
        if (!isInitialized) return
        appContext.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("clicker_map_key_$keyCode", action.name)
            .apply()
    }

    fun handleKeyCode(keyCode: Int): Boolean {
        return when (getActionForKey(keyCode)) {
            ClickerAction.PLAY_PAUSE -> HardwareButtonController.dispatchPlayPause().let { true }
            ClickerAction.SPEED_UP -> HardwareButtonController.dispatchSpeedUp().let { true }
            ClickerAction.SPEED_DOWN -> HardwareButtonController.dispatchSpeedDown().let { true }
            ClickerAction.PREV_BOOKMARK -> HardwareButtonController.dispatchPrevBookmark().let { true }
            ClickerAction.NEXT_BOOKMARK -> HardwareButtonController.dispatchSkipToNextBookmark().let { true }
            ClickerAction.NONE -> false
        }
    }
}
