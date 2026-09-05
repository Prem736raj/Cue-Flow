package com.example.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    NONE("None", "Ignore this button click")
}

object RemoteClickerManager {
    var isConnected by mutableStateOf(false)
    var connectedDeviceName by mutableStateOf<String?>("No clicker connected")
    var deviceType by mutableStateOf<String?>("")
    var bluetoothEnabled by mutableStateOf(false)
    
    // List of keyCodes we support customization for
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
        KeyEvent.KEYCODE_DPAD_RIGHT to "Arrow Right Action"
    )

    private var isInitialized = false
    private lateinit var appContext: Context
    private val mainHandler = Handler(Looper.getMainLooper())

    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            checkCurrentDevices()
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            checkCurrentDevices()
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            checkCurrentDevices()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                bluetoothEnabled = (state == BluetoothAdapter.STATE_ON)
            } else if (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                // Wait briefly for InputManager to update devices list
                mainHandler.postDelayed({
                    checkCurrentDevices()
                }, 1000)
            }
        }
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        appContext = context.applicationContext
        isInitialized = true

        val inputManager = appContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(inputDeviceListener, mainHandler)

        val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        bluetoothEnabled = adapter?.isEnabled == true

        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        appContext.registerReceiver(bluetoothReceiver, filter)

        checkCurrentDevices(notify = false)
    }

    fun checkCurrentDevices(notify: Boolean = true) {
        if (!isInitialized) return
        val inputManager = appContext.getSystemService(Context.INPUT_SERVICE) as InputManager
        val deviceIds = inputManager.inputDeviceIds
        var foundRemote = false
        var deviceName = "Unknown Clicker"
        var resolvedType = "Bluetooth Remote"

        for (id in deviceIds) {
            val device = inputManager.getInputDevice(id) ?: continue
            // Exclude virtual standard keyboards built-in or simulated
            if (!device.isVirtual && device.isExternal) {
                val sources = device.sources
                val isKeyboard = (sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
                val isDpad = (sources and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
                
                if (isKeyboard || isDpad) {
                    foundRemote = true
                    deviceName = device.name
                    resolvedType = when {
                        deviceName.contains("shutter", ignoreCase = true) || deviceName.contains("selfie", ignoreCase = true) -> "Bluetooth Selfie Shutter"
                        deviceName.contains("present", ignoreCase = true) || deviceName.contains("clicker", ignoreCase = true) -> "Presentation Clicker"
                        deviceName.contains("keyboard", ignoreCase = true) -> "External Mini Keyboard"
                        else -> "Handheld Remote"
                    }
                    break
                }
            }
        }
        
        // Also fallback or complement using actual standard connected devices via Bluetooth Profile adapter if permission allowed
        if (!foundRemote) {
            try {
                val bluetoothManager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = bluetoothManager.adapter
                if (adapter != null && adapter.isEnabled) {
                    val bonded = adapter.bondedDevices
                    if (!bonded.isNullOrEmpty()) {
                        // Scan for common remote control device profiles if permission was given
                        for (device in bonded) {
                            val name = device.name ?: ""
                            val isClickerName = name.contains("remote", ignoreCase = true) ||
                                    name.contains("clicker", ignoreCase = true) ||
                                    name.contains("shutter", ignoreCase = true) ||
                                    name.contains("keyboard", ignoreCase = true) ||
                                    name.contains("presenter", ignoreCase = true)
                            
                            if (isClickerName) {
                                // Since bonded is paired, we can't fully know if ACL is connected unless we check connected devices profile.
                                // Simple fallback: if ACL is connected just trust it
                                foundRemote = true
                                deviceName = name
                                resolvedType = "Bluetooth Presentation Remote"
                                break
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Missing BLUETOOTH_CONNECT permission on Android 12+, perfectly fine since we fall back to InputManager
            } catch (e: Exception) {
                // General error safety
            }
        }

        val previousConnected = isConnected
        isConnected = foundRemote
        connectedDeviceName = if (foundRemote) deviceName else null
        deviceType = if (foundRemote) resolvedType else null

        if (notify && previousConnected != isConnected) {
            mainHandler.post {
                if (isConnected) {
                    Toast.makeText(appContext, "Remote Connected: $deviceName ($resolvedType)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(appContext, "Bluetooth Remote Disconnected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun getActionForKey(keyCode: Int): ClickerAction {
        if (!isInitialized) return ClickerAction.NONE
        val prefs = appContext.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        val defaultVal = when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_UP -> ClickerAction.SPEED_UP.name
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_DOWN -> ClickerAction.SPEED_DOWN.name
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_LEFT -> ClickerAction.PREV_BOOKMARK.name
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> ClickerAction.NEXT_BOOKMARK.name
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> ClickerAction.PLAY_PAUSE.name
            else -> ClickerAction.NONE.name
        }
        val savedName = prefs.getString("clicker_map_key_$keyCode", defaultVal) ?: defaultVal
        return try {
            ClickerAction.valueOf(savedName)
        } catch (e: Exception) {
            ClickerAction.NONE
        }
    }

    fun setActionForKey(keyCode: Int, action: ClickerAction) {
        if (!isInitialized) return
        val prefs = appContext.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("clicker_map_key_$keyCode", action.name).apply()
    }

    fun handleKeyCode(keyCode: Int): Boolean {
        val action = getActionForKey(keyCode)
        if (action == ClickerAction.NONE) return false

        when (action) {
            ClickerAction.PLAY_PAUSE -> HardwareButtonController.dispatchPlayPause()
            ClickerAction.SPEED_UP -> HardwareButtonController.dispatchSpeedUp()
            ClickerAction.SPEED_DOWN -> HardwareButtonController.dispatchSpeedDown()
            ClickerAction.PREV_BOOKMARK -> HardwareButtonController.dispatchPrevBookmark()
            ClickerAction.NEXT_BOOKMARK -> HardwareButtonController.dispatchSkipToNextBookmark()
            ClickerAction.NONE -> {}
        }
        return true
    }
}
