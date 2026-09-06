package com.example.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.Script
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.testTag
import com.example.util.HardwareButtonController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.lifecycle.setViewTreeViewModelStoreOwner

class FloatingPrompterService : Service(), LifecycleOwner, SavedStateRegistryOwner, androidx.lifecycle.ViewModelStoreOwner {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_REQUEST_VOICE_SYNC_PERMISSION = "com.example.action.REQUEST_VOICE_SYNC_PERMISSION"
        const val ACTION_ENABLE_VOICE_SYNC = "com.example.action.ENABLE_VOICE_SYNC"
        const val ACTION_DISABLE_VOICE_SYNC = "com.example.action.DISABLE_VOICE_SYNC"
        const val CHANNEL_ID = "cueflow_floating_channel"
        const val NOTIFICATION_ID = 1010

        val isPlayingState = mutableStateOf(false)
        val voiceSyncEnableRequest = mutableIntStateOf(0)
        var isServiceRunning = false
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var params = WindowManager.LayoutParams()
    private var microphoneForegroundActive = false

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = androidx.lifecycle.ViewModelStore()

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
        
    override val viewModelStore: androidx.lifecycle.ViewModelStore
        get() = store

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        com.example.ui.theme.ThemeState.initialize(applicationContext)
        com.example.util.LanguageManager.initialize(applicationContext)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_PLAY_PAUSE) {
            isPlayingState.value = !isPlayingState.value
            showNotification()
            return START_NOT_STICKY
        } else if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        } else if (action == ACTION_ENABLE_VOICE_SYNC) {
            if (hasMicrophonePermission()) {
                // Android 14+ requires the microphone FGS type to be active before
                // speech recognition starts from the floating overlay.
                microphoneForegroundActive = true
                showNotification()
                voiceSyncEnableRequest.intValue += 1
            }
            return START_NOT_STICKY
        } else if (action == ACTION_DISABLE_VOICE_SYNC) {
            microphoneForegroundActive = false
            showNotification()
            return START_NOT_STICKY
        }

        microphoneForegroundActive = false
        showNotification()

        val script = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra("SCRIPT", Script::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra("SCRIPT") as? Script
        }

        if (script == null) {
            if (composeView == null) {
                stopSelf()
            }
            return START_NOT_STICKY
        }

        // Clean up any existing floating overlay
        removeOverlay()

        // Initialize state
        isPlayingState.value = false
        showNotification()
        setupOverlay(script)

        return START_NOT_STICKY
    }

    private fun showNotification() {
        val playPauseText = if (isPlayingState.value) "Pause" else "Play"
        val playPauseIcon = if (isPlayingState.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, FloatingPrompterService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 1, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingPrompterService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CueFlow Floating Mode")
            .setContentText(if (isPlayingState.value) "Floating prompter is active and scrolling..." else "Floating prompter is paused")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // safe system speaking headset icon
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        val hasMicrophonePermission = hasMicrophonePermission()

        if (!hasMicrophonePermission) {
            microphoneForegroundActive = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (microphoneForegroundActive && hasMicrophonePermission) {
                fgsType = fgsType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun hasMicrophonePermission(): Boolean = androidx.core.content.ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CueFlow Floating Mode Notification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enables control over playback and state of floating teleprompter"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun setupOverlay(script: Script) {
        val density = resources.displayMetrics.density
        val defaultWidth = (345 * density).toInt()
        val defaultHeight = (315 * density).toInt()

        val configs = com.example.data.FloatingSettings.getConfigs(this)
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        params = WindowManager.LayoutParams(
            defaultWidth,
            defaultHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - defaultWidth) / 2
            y = when (configs.defaultGravity) {
                "Top Half" -> (screenHeight * 0.15f).toInt()
                "Bottom Half" -> (screenHeight * 0.55f).toInt()
                else -> (screenHeight - defaultHeight) / 2 // Center
            }
        }

        val themeContext = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault)
        composeView = ComposeView(themeContext).apply {
            setViewTreeLifecycleOwner(this@FloatingPrompterService)
            setViewTreeSavedStateRegistryOwner(this@FloatingPrompterService)
            setViewTreeViewModelStoreOwner(this@FloatingPrompterService)
            setContent {
                MyApplicationTheme {
                    val currentPlayState = isPlayingState.value
                    LaunchedEffect(currentPlayState) {
                        showNotification()
                    }
                    FloatingPrompterUI(
                        script = script,
                        onClose = {
                            stopSelf()
                        },
                        onDrag = { dx, dy ->
                            val maxX = (screenWidth - params.width).coerceAtLeast(0)
                            val maxY = (screenHeight - params.height).coerceAtLeast(0)
                            params.x = (params.x + dx).coerceIn(0, maxX)
                            params.y = (params.y + dy).coerceIn(0, maxY)
                            updateView()
                        },
                        onResize = { dw, dh ->
                            val minWidth = (180 * density).toInt()
                            val minHeight = (180 * density).toInt()
                            params.width = (params.width + dw).coerceIn(minWidth, screenWidth)
                            params.height = (params.height + dh).coerceIn(minHeight, screenHeight)
                            params.x = params.x.coerceIn(0, (screenWidth - params.width).coerceAtLeast(0))
                            params.y = params.y.coerceIn(0, (screenHeight - params.height).coerceAtLeast(0))
                            updateView()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun updateView() {
        composeView?.let { view ->
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeOverlay() {
        composeView?.let { view ->
            try {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            composeView = null
        }
    }

    override fun onDestroy() {
        removeOverlay()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isServiceRunning = false
        microphoneForegroundActive = false
        isPlayingState.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}

@Composable
fun FloatingPrompterUI(
    script: Script,
    onClose: () -> Unit,
    onDrag: (dx: Int, dy: Int) -> Unit,
    onResize: (dw: Int, dh: Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE) }
    val hasMicrophonePermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val lines = remember(script.content) {
        script.content.split("\n\n", "\n").filter { it.isNotBlank() }
    }
    var configs by remember { mutableStateOf(com.example.data.FloatingSettings.getConfigs(context)) }

    var isCollapsed by remember { mutableStateOf(false) }
    var isPlaying by FloatingPrompterService.isPlayingState
    var speed by remember { mutableFloatStateOf(script.scrollSpeed.toFloat()) }
    var fontSize by remember { mutableFloatStateOf(configs.textSize) }
    var opacity by remember { mutableFloatStateOf(configs.bgOpacity) }
    var windowOpacity by remember { mutableFloatStateOf(configs.windowOpacity) }
    var textColorName by remember { mutableStateOf(configs.textColorName) }
    var bgColorName by remember { mutableStateOf(configs.bgColorName) }
    var showInOverlaySettings by remember { mutableStateOf(false) }

    var isMirrored by remember { mutableStateOf(script.isMirrored) }
    var alignment by remember { mutableStateOf(script.textAlignment) }
    
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var isHardwareControlActive by remember {
        mutableStateOf(prefs.getBoolean("hardware_buttons_enabled", false))
    }
    var hardwareIndicatorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(script.title, isPlaying, speed, scrollState.firstVisibleItemIndex, lines.size) {
        HardwareButtonController.pActiveTitle = script.title
        HardwareButtonController.pIsPlaying = isPlaying
        HardwareButtonController.pSpeed = speed
        HardwareButtonController.pCurrentParagraph = scrollState.firstVisibleItemIndex
        HardwareButtonController.pTotalParagraphs = lines.size
    }
    
    LaunchedEffect(hardwareIndicatorMessage) {
        if (hardwareIndicatorMessage != null) {
            delay(1200)
            hardwareIndicatorMessage = null
        }
    }
    
    DisposableEffect(context, isHardwareControlActive) {
        val listener = object : HardwareButtonController.Listener {
            override fun onSpeedUp() {
                speed = (speed + 1.0f).coerceAtMost(20.0f)
                hardwareIndicatorMessage = "Speed: ${speed.toInt()}x"
            }
            
            override fun onSpeedDown() {
                speed = (speed - 1.0f).coerceAtLeast(1.0f)
                hardwareIndicatorMessage = "Speed: ${speed.toInt()}x"
            }
            
            override fun onPlayPause() {
                isPlaying = !isPlaying
                hardwareIndicatorMessage = if (isPlaying) "Playing" else "Paused"
            }
            
            override fun onSkipToNextBookmark() {
                val bookmarkedSet = prefs.getStringSet("script_bookmarks_${script.id}", emptySet()) ?: emptySet()
                if (lines.isNotEmpty()) {
                    val currentIndex = scrollState.firstVisibleItemIndex
                    var nextBookmarkIndex = -1
                    for (i in (currentIndex + 1) until lines.size) {
                        val cleanLine = lines[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                        val bookmarkedClean = bookmarkedSet.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanLine }
                        if (bookmarkedClean || bookmarkedSet.contains(lines[i])) {
                            nextBookmarkIndex = i
                            break
                        }
                    }
                    if (nextBookmarkIndex == -1) {
                        for (i in 0 until currentIndex) {
                            val cleanLine = lines[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            val bookmarkedClean = bookmarkedSet.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanLine }
                            if (bookmarkedClean || bookmarkedSet.contains(lines[i])) {
                                nextBookmarkIndex = i
                                break
                            }
                        }
                    }
                    if (nextBookmarkIndex != -1) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(nextBookmarkIndex)
                        }
                        hardwareIndicatorMessage = "Bookmark #${nextBookmarkIndex + 1}"
                    } else {
                        hardwareIndicatorMessage = "No Bookmarks"
                    }
                }
            }

            override fun onPrevBookmark() {
                val bookmarkedSet = prefs.getStringSet("script_bookmarks_${script.id}", emptySet()) ?: emptySet()
                if (lines.isNotEmpty()) {
                    val currentIndex = scrollState.firstVisibleItemIndex
                    var prevBookmarkIndex = -1
                    for (i in (currentIndex - 1) downTo 0) {
                        val cleanLine = lines[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                        val bookmarkedClean = bookmarkedSet.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanLine }
                        if (bookmarkedClean || bookmarkedSet.contains(lines[i])) {
                            prevBookmarkIndex = i
                            break
                        }
                    }
                    if (prevBookmarkIndex == -1) {
                        for (i in (lines.size - 1) downTo currentIndex) {
                            val cleanLine = lines[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            val bookmarkedClean = bookmarkedSet.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanLine }
                            if (bookmarkedClean || bookmarkedSet.contains(lines[i])) {
                                prevBookmarkIndex = i
                                break
                            }
                        }
                    }
                    if (prevBookmarkIndex != -1) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(prevBookmarkIndex)
                        }
                        hardwareIndicatorMessage = "Bookmark #${prevBookmarkIndex + 1}"
                    } else {
                        hardwareIndicatorMessage = "No Bookmarks"
                    }
                }
            }
        }
        
        if (isHardwareControlActive) {
            HardwareButtonController.register(listener)
        }
        
        onDispose {
            HardwareButtonController.unregister(listener)
        }
    }

    DisposableEffect(context, isHardwareControlActive, isPlaying) {
        var mediaSession: MediaSession? = null
        if (isHardwareControlActive) {
            mediaSession = MediaSession(context, "CueFlowFloatingMediaSession_${script.id}").apply {
                setCallback(object : MediaSession.Callback() {
                    private var lastPlayPauseClickTime = 0L
                    override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
                        val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                        if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                            val keyCode = keyEvent.keyCode
                            when (keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PLAY,
                                KeyEvent.KEYCODE_MEDIA_PAUSE,
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                                KeyEvent.KEYCODE_HEADSETHOOK -> {
                                    val now = System.currentTimeMillis()
                                    if (now - lastPlayPauseClickTime < 450) {
                                        HardwareButtonController.dispatchSkipToNextBookmark()
                                    } else {
                                        HardwareButtonController.dispatchPlayPause()
                                    }
                                    lastPlayPauseClickTime = now
                                    return true
                                }
                                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                                    HardwareButtonController.dispatchSkipToNextBookmark()
                                    return true
                                }
                                KeyEvent.KEYCODE_VOLUME_UP -> {
                                    HardwareButtonController.dispatchSpeedUp()
                                    return true
                                }
                                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                                    HardwareButtonController.dispatchSpeedDown()
                                    return true
                                }
                            }
                        }
                        return super.onMediaButtonEvent(mediaButtonIntent)
                    }
                })
                val state = PlaybackState.Builder()
                    .setState(
                        if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        0,
                        if (isPlaying) 1.0f else 0.0f
                    )
                    .setActions(
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT
                    )
                    .build()
                setPlaybackState(state)
                isActive = true
            }
        }
        
        onDispose {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
    }
    var isCompleted by remember { mutableStateOf(false) }

    val onInteraction: () -> Unit = {
        lastInteractionTime = System.currentTimeMillis()
        if (!controlsVisible) {
            controlsVisible = true
        }
    }

    // Floating Voice Sync must be enabled by an explicit user action in this
    // session. This prevents a persisted preference from starting microphone
    // recognition before the service has been promoted to the microphone FGS type.
    var isVoiceSyncActive by remember { mutableStateOf(false) }
    var voiceSensitivity by remember { mutableIntStateOf(prefs.getInt("voice_sync_sensitivity_${script.id}", prefs.getInt("voice_sync_sensitivity", 1))) }
    var voicePauseThreshold by remember { mutableIntStateOf(prefs.getInt("voice_sync_pause_threshold_${script.id}", prefs.getInt("voice_sync_pause_threshold", 1))) }
    var voiceMaxSpeedLimit by remember { mutableIntStateOf(prefs.getInt("voice_sync_max_limit_${script.id}", prefs.getInt("voice_sync_max_limit", 0))) }
    var isHearingSpoken by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var micErrorText by remember { mutableStateOf<String?>(null) }
    var currentVoiceParaIndex by remember { mutableIntStateOf(0) }
    var lastMatchTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isNoisyEnvironment by remember { mutableStateOf(false) }
    var noiseThresholdCounter by remember { mutableIntStateOf(0) }

    val voiceSyncEnableRequest = FloatingPrompterService.voiceSyncEnableRequest.intValue

    fun sendVoiceSyncServiceAction(action: String) {
        val intent = android.content.Intent(context, FloatingPrompterService::class.java).apply {
            this.action = action
        }
        context.startService(intent)
    }

    fun requestVoiceSyncPermission() {
        micErrorText = "Open CueFlow to allow microphone access for Voice Sync."
        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            action = FloatingPrompterService.ACTION_REQUEST_VOICE_SYNC_PERMISSION
            addFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        runCatching { context.startActivity(intent) }
            .onFailure { micErrorText = "Open CueFlow to allow microphone access for Voice Sync." }
    }

    LaunchedEffect(voiceSyncEnableRequest) {
        if (voiceSyncEnableRequest > 0) {
            if (hasMicrophonePermission) {
                isVoiceSyncActive = true
                micErrorText = null
                prefs.edit().putBoolean("voice_sync_enabled_${script.id}", true).apply()
            } else {
                isVoiceSyncActive = false
                micErrorText = "Microphone permission is required for Voice Sync."
            }
        }
    }

    LaunchedEffect(isVoiceSyncActive, hasMicrophonePermission) {
        if (isVoiceSyncActive && !hasMicrophonePermission) {
            isVoiceSyncActive = false
            prefs.edit().putBoolean("voice_sync_enabled_${script.id}", false).apply()
            sendVoiceSyncServiceAction(FloatingPrompterService.ACTION_DISABLE_VOICE_SYNC)
            micErrorText = "Microphone permission is required for Voice Sync."
        }
    }

    LaunchedEffect(scrollState.firstVisibleItemIndex) {
        if (!isVoiceSyncActive) {
            currentVoiceParaIndex = scrollState.firstVisibleItemIndex
        }
    }

    // Voice sync smooth scrolling transition
    LaunchedEffect(currentVoiceParaIndex, isVoiceSyncActive, isPlaying) {
        if (isVoiceSyncActive && isPlaying) {
            try {
                scrollState.animateScrollToItem(
                    index = currentVoiceParaIndex,
                    scrollOffset = -150
                )
            } catch (e: Exception) {
                // Cancel smoothly
            }
        }
    }

    // Voice sync auto-completion trigger
    LaunchedEffect(currentVoiceParaIndex, lines) {
        if (isVoiceSyncActive && isPlaying && currentVoiceParaIndex >= lines.lastIndex - 1 && lines.isNotEmpty()) {
            delay(1500)
            if (currentVoiceParaIndex >= lines.lastIndex - 1 && isPlaying) {
                isPlaying = false
                isCompleted = true
            }
        }
    }

    // Word token maps for matching
    val voiceParagraphWords = remember(lines) {
        lines.map { para ->
            para.lowercase()
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }
    }

    // SpeechRecognizer loop for floating mode
    LaunchedEffect(isVoiceSyncActive, isPlaying, voiceSensitivity, voicePauseThreshold, voiceMaxSpeedLimit) {
        if (isVoiceSyncActive && isPlaying) {
            var recognizer: android.speech.SpeechRecognizer? = null
            lastMatchTime = System.currentTimeMillis()
            
            val startListeningRunnable = Runnable {
                try {
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                        
                        val silenceLen = when (voicePauseThreshold) {
                            0 -> 1000L
                            2 -> 4500L
                            else -> 2200L
                        }
                        val possibleSilenceLen = when (voicePauseThreshold) {
                            0 -> 700L
                            2 -> 3500L
                            else -> 1600L
                        }
                        putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", silenceLen)
                        putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", possibleSilenceLen)
                    }
                    recognizer?.startListening(intent)
                } catch (e: java.lang.Exception) {
                    micErrorText = "Mic share error: ${e.message}"
                }
            }

            val listener = object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    isHearingSpoken = true
                }
                override fun onBeginningOfSpeech() {
                    isHearingSpoken = true
                }
                override fun onRmsChanged(rmsdB: Float) {
                    rmsLevel = rmsdB
                    // High RMS level consistently suggests a noisy environment
                    if (rmsdB > 11f) {
                        noiseThresholdCounter++
                        if (noiseThresholdCounter > 20) { // Consistently high for ~2 seconds
                            isNoisyEnvironment = true
                        }
                    } else {
                        if (noiseThresholdCounter > 0) noiseThresholdCounter--
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isHearingSpoken = false
                }
                override fun onError(error: Int) {
                    isHearingSpoken = false
                    rmsLevel = 0f
                    
                    when (error) {
                        android.speech.SpeechRecognizer.ERROR_AUDIO,
                        android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            micErrorText = "The microphone is being used by another app (e.g., Instagram/Snapchat)."
                        }
                        android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            micErrorText = "Microphone permission is required for Voice Sync."
                        }
                        android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        android.speech.SpeechRecognizer.ERROR_NO_MATCH -> {
                            if (isPlaying && isVoiceSyncActive) {
                                coroutineScope.launch {
                                    delay(400)
                                    if (isPlaying && isVoiceSyncActive) {
                                        try {
                                            startListeningRunnable.run()
                                        } catch (e: java.lang.Exception) {}
                                    }
                                }
                            }
                        }
                        else -> {
                            if (isPlaying && isVoiceSyncActive) {
                                coroutineScope.launch {
                                    delay(500)
                                    if (isPlaying && isVoiceSyncActive) {
                                        try {
                                            startListeningRunnable.run()
                                        } catch (e: java.lang.Exception) {}
                                    }
                                }
                            }
                        }
                    }
                }
                
                override fun onResults(results: android.os.Bundle?) {
                    isHearingSpoken = false
                    rmsLevel = 0f
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spoken = matches[0] ?: ""
                        
                        val spokenWords = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                        
                        if (spokenWords.isNotEmpty()) {
                            val currentIdx = currentVoiceParaIndex
                            
                            val startRange = when (voiceSensitivity) {
                                0 -> (currentIdx - 1).coerceAtLeast(0)
                                2 -> (currentIdx - 2).coerceAtLeast(0)
                                else -> (currentIdx - 1).coerceAtLeast(0)
                            }
                            val endRange = when (voiceSensitivity) {
                                0 -> (currentIdx + 2).coerceAtMost(lines.lastIndex)
                                2 -> (currentIdx + 7).coerceAtMost(lines.lastIndex)
                                else -> (currentIdx + 4).coerceAtMost(lines.lastIndex)
                            }
                            
                            var bestMatchIdx = -1
                            var bestScore = 0
                            val recentSpoken = spokenWords.takeLast(10)
                            
                            for (paraIdx in startRange..endRange) {
                                val words = voiceParagraphWords.getOrNull(paraIdx) ?: continue
                                if (words.isEmpty()) continue
                                
                                var score = 0
                                var hasLongWord = false
                                for (sw in recentSpoken) {
                                    if (words.contains(sw)) {
                                        score++
                                        if (sw.length > 3) {
                                            hasLongWord = true
                                        }
                                    }
                                }
                                
                                val isValidMatch = when (voiceSensitivity) {
                                    0 -> score >= 2 || (score >= 1 && hasLongWord)
                                    2 -> score >= 1
                                    else -> score > 0 && (hasLongWord || score >= 2)
                                }
                                
                                if (isValidMatch) {
                                    if (score > bestScore) {
                                        bestScore = score
                                        bestMatchIdx = paraIdx
                                    }
                                }
                            }
                            
                            if (bestMatchIdx != -1) {
                                val maxJump = when (voiceMaxSpeedLimit) {
                                    2 -> 1
                                    1 -> 2
                                    else -> 9999
                                }
                                val cappedIdx = if (bestMatchIdx > currentIdx) {
                                    currentIdx + (bestMatchIdx - currentIdx).coerceAtMost(maxJump)
                                } else {
                                    bestMatchIdx
                                }
                                
                                currentVoiceParaIndex = cappedIdx
                                lastMatchTime = System.currentTimeMillis()
                            }
                        }
                    }
                    
                    if (isPlaying && isVoiceSyncActive) {
                        try {
                            startListeningRunnable.run()
                        } catch (e: java.lang.Exception) {}
                    }
                }
                
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        isHearingSpoken = true
                        val spoken = matches[0] ?: ""
                        
                        val spokenWords = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                            
                        if (spokenWords.isNotEmpty()) {
                            val currentIdx = currentVoiceParaIndex
                            
                            val startRange = when (voiceSensitivity) {
                                0 -> (currentIdx - 1).coerceAtLeast(0)
                                2 -> (currentIdx - 3).coerceAtLeast(0)
                                else -> (currentIdx - 2).coerceAtLeast(0)
                            }
                            val endRange = when (voiceSensitivity) {
                                0 -> (currentIdx + 3).coerceAtMost(lines.lastIndex)
                                2 -> (currentIdx + 8).coerceAtMost(lines.lastIndex)
                                else -> (currentIdx + 5).coerceAtMost(lines.lastIndex)
                            }
                            
                            var bestMatchIdx = -1
                            var bestScore = 0
                            val recentSpoken = spokenWords.takeLast(10)
                            
                            for (paraIdx in startRange..endRange) {
                                val words = voiceParagraphWords.getOrNull(paraIdx) ?: continue
                                if (words.isEmpty()) continue
                                
                                var score = 0
                                var hasLongWord = false
                                for (sw in recentSpoken) {
                                    if (words.contains(sw)) {
                                        score++
                                        if (sw.length > 3) {
                                            hasLongWord = true
                                        }
                                    }
                                }
                                
                                val isValidMatch = when (voiceSensitivity) {
                                    0 -> score >= 3 || (score >= 2 && hasLongWord)
                                    2 -> score >= 1
                                    else -> score > 1 && (hasLongWord || score >= 2)
                                }
                                
                                if (isValidMatch) {
                                    if (score > bestScore) {
                                        bestScore = score
                                        bestMatchIdx = paraIdx
                                    }
                                }
                            }
                            
                            if (bestMatchIdx != -1 && bestMatchIdx != currentIdx) {
                                val maxJump = when (voiceMaxSpeedLimit) {
                                    2 -> 1
                                    1 -> 2
                                    else -> 9999
                                }
                                val cappedIdx = if (bestMatchIdx > currentIdx) {
                                    currentIdx + (bestMatchIdx - currentIdx).coerceAtMost(maxJump)
                                } else {
                                    bestMatchIdx
                                }
                                
                                currentVoiceParaIndex = cappedIdx
                                lastMatchTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
                
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            }
            
            try {
                if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                    recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                    currentVoiceParaIndex = scrollState.firstVisibleItemIndex
                    startListeningRunnable.run()
                    kotlinx.coroutines.awaitCancellation()
                } else {
                    micErrorText = "Speech recognition is not available."
                }
            } catch (e: java.lang.Exception) {
                micErrorText = "Unable to start listening: ${e.message}"
            } finally {
                try {
                    recognizer?.stopListening()
                    recognizer?.destroy()
                } catch (e: java.lang.Exception) {}
                isHearingSpoken = false
                rmsLevel = 0f
            }
        }
    }

    // Highly efficient frame-based vertical auto-scroll loop aligned to frame refresh rates
    LaunchedEffect(isPlaying, speed, isVoiceSyncActive) {
        if (isPlaying && !isVoiceSyncActive) {
            isCompleted = false
            try {
                scrollState.scroll(scrollPriority = androidx.compose.foundation.MutatePriority.Default) {
                    var lastFrameNanos = System.nanoTime()
                    while (isPlaying && !isVoiceSyncActive) {
                        androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                            val elapsedSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                            lastFrameNanos = frameTimeNanos
                            
                            if (scrollState.canScrollForward) {
                                // Dynamic physics-based subpixel scroll increment aligned to frame refresh rate
                                val scrollPixels = (speed * 43f) * elapsedSeconds
                                if (scrollPixels > 0f) {
                                    scrollBy(scrollPixels)
                                }
                            } else if (lines.isNotEmpty() && scrollState.firstVisibleItemIndex > 0) {
                                isPlaying = false
                                isCompleted = true
                            } else {
                                isPlaying = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    isPlaying = false
                    throw e
                }
            }
        }
    }

    // Auto-collapse footer and header after 4 seconds of idle playing time
    LaunchedEffect(isPlaying, lastInteractionTime, controlsVisible) {
        if (isPlaying && controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }

    if (isCollapsed) {
        // Collapsed micro-pill floating indicator
        Box(
            modifier = Modifier
                .width(if (isVoiceSyncActive) 175.dp else 140.dp)
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(CosmicSurface.copy(alpha = 0.95f))
                .border(BorderStroke(2.dp, ElectricCyan.copy(alpha = 0.8f)), RoundedCornerShape(30.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                    }
                }
                .testTag("floating_prompter_collapsed_badge"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                // Drag dots handle
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(6.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(SlateTextMuted)
                        )
                    }
                }

                if (isVoiceSyncActive) {
                    val collapsedMicPulse by animateFloatAsState(
                        targetValue = if (isHearingSpoken) 1.2f + (rmsLevel.coerceIn(0f, 15f) / 15f) * 0.3f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "CollapsedMicPulse"
                    )
                    Box(
                        modifier = Modifier
                            .graphicsLayer(scaleX = collapsedMicPulse, scaleY = collapsedMicPulse)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isHearingSpoken) ElectricCyan.copy(alpha = 0.3f) else ElectricCyan.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice active",
                            tint = ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { 
                        isPlaying = !isPlaying
                        onInteraction()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFFEF4444).copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f))
                        .testTag("floating_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = if (isPlaying) Color(0xFFEF4444) else ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { 
                        isCollapsed = false
                        onInteraction()
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("floating_expand_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "Expand controls",
                        tint = SlateTextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        // Transparent glassmorphic window mode
        Card(
            colors = CardDefaults.cardColors(containerColor = com.example.data.FloatingSettings.mapBgColor(bgColorName).copy(alpha = opacity)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = windowOpacity)
                .border(BorderStroke(1.5.dp, ElectricPurple.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                .clickable { onInteraction() }
                .testTag("floating_prompter_window")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val boxScope = this
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                // Header (Animate visible/invisible)
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(CosmicSurfaceElevated.copy(alpha = 0.85f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                                }
                            }
                            .padding(horizontal = 12.dp)
                            .testTag("floating_drag_header"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Drag Window",
                                tint = ElectricPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = script.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // BACK TO CUEFLOW INTERACTIVE BUTTON
                            IconButton(
                                onClick = {
                                    onInteraction()
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_launch_app_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Return to CueFlow App",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            IconButton(
                                onClick = { 
                                    isCollapsed = true
                                    onInteraction()
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_minimize_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Minimize",
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("floating_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Main Prompt Text Container Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    val textAlignConv = when (alignment) {
                        "center" -> TextAlign.Center
                        "right" -> TextAlign.Right
                        else -> TextAlign.Left
                    }
                    val mappedTextColor = remember(textColorName) {
                        com.example.data.FloatingSettings.mapTextColor(textColorName)
                    }

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (isMirrored) {
                                    rotationY = 180f
                                }
                            }
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                        
                        items(lines.size, key = { it }) { index ->
                            val lineText = lines[index]
                            val lineTextAlign = remember(lineText, alignment, script.textDirection) {
                                com.example.util.RtlHelper.getTextAlign(alignment, script.textDirection, lineText)
                            }
                            val lineTextDirection = remember(lineText, script.textDirection) {
                                com.example.util.RtlHelper.getTextDirection(script.textDirection, lineText)
                            }
                            Text(
                                text = lineText,
                                fontSize = fontSize.sp,
                                color = mappedTextColor,
                                textAlign = lineTextAlign,
                                fontWeight = FontWeight.Bold,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDirection = lineTextDirection
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }

                    // Reading Guidance Spotlight Lines overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                            .background(ElectricCyan.copy(alpha = 0.03f))
                    )

                    // Overlay indicator micro-pill when controls are hidden
                    if (!controlsVisible && !isCompleted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { onInteraction() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(CosmicSurfaceElevated.copy(alpha = 0.85f))
                                    .border(BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.5f)), CircleShape)
                                    .testTag("floating_expand_controls_handle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Expand Controls",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    // Completion state overlay
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CosmicBackground.copy(alpha = 0.92f))
                                .padding(12.dp)
                                .testTag("floating_complete_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "🎉 Script Complete!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Great job speaking! Restart or return to CueFlow.",
                                    fontSize = 11.sp,
                                    color = SlateTextMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            scrollState.requestScrollToItem(0, 0)
                                            isCompleted = false
                                            isPlaying = false
                                            onInteraction()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("floating_complete_restart")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Text("Restart", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isCompleted = false
                                            onInteraction()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = SlateTextSecondary
                                        ),
                                        border = BorderStroke(1.dp, CosmicBorder),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .testTag("floating_complete_dismiss")
                                    ) {
                                        Text("Dismiss", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }

                    // In-overlay Settings Popup Panel
                    if (showInOverlaySettings) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clickable { /* Prevent passing interactions down */ }
                                .testTag("floating_in_overlay_settings_panel")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Overlay Settings", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { showInOverlaySettings = false },
                                        modifier = Modifier.size(20.dp).testTag("close_overlay_settings_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Red, modifier = Modifier.size(12.dp))
                                    }
                                }

                                // Text Size adjustment
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Text Size", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${fontSize.toInt()} sp", fontSize = 9.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = fontSize,
                                        onValueChange = {
                                            fontSize = it
                                            configs = configs.copy(textSize = it)
                                            com.example.data.FloatingSettings.saveConfigs(context, configs)
                                        },
                                        valueRange = 12f..36f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElectricCyan,
                                            activeTrackColor = ElectricCyan,
                                            inactiveTrackColor = CosmicBorder
                                        ),
                                        modifier = Modifier.height(20.dp).testTag("overlay_text_size_slider")
                                    )
                                }

                                // Backdrop Opacity adjustment
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Backdrop Opacity", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${(opacity * 100).toInt()}%", fontSize = 9.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = opacity,
                                        onValueChange = {
                                            opacity = it
                                            configs = configs.copy(bgOpacity = it)
                                            com.example.data.FloatingSettings.saveConfigs(context, configs)
                                        },
                                        valueRange = 0.15f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElectricPurple,
                                            activeTrackColor = ElectricPurple,
                                            inactiveTrackColor = CosmicBorder
                                        ),
                                        modifier = Modifier.height(20.dp).testTag("overlay_bg_opacity_slider")
                                    )
                                }

                                // Window Opacity adjustment
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Window Opacity", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                        Text("${(windowOpacity * 100).toInt()}%", fontSize = 9.sp, color = WarmAmber, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = windowOpacity,
                                        onValueChange = {
                                            windowOpacity = it
                                            configs = configs.copy(windowOpacity = it)
                                            com.example.data.FloatingSettings.saveConfigs(context, configs)
                                        },
                                        valueRange = 0.3f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = WarmAmber,
                                            activeTrackColor = WarmAmber,
                                            inactiveTrackColor = CosmicBorder
                                        ),
                                        modifier = Modifier.height(20.dp).testTag("overlay_window_opacity_slider")
                                    )
                                }

                                // Text Color row
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Text Color", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.example.data.FloatingSettings.TEXT_COLORS.forEach { colorName ->
                                            val actualColor = com.example.data.FloatingSettings.mapTextColor(colorName)
                                            val isSelected = textColorName == colorName
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(actualColor)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        textColorName = colorName
                                                        configs = configs.copy(textColorName = colorName)
                                                        com.example.data.FloatingSettings.saveConfigs(context, configs)
                                                    }
                                                    .testTag("overlay_text_color_$colorName"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (actualColor == Color.White) Color.Black else Color.White,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Backdrop color theme
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Backdrop Theme", fontSize = 9.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        com.example.data.FloatingSettings.BG_COLORS.forEach { bgName ->
                                            val actualColor = com.example.data.FloatingSettings.mapBgColor(bgName)
                                            val isSelected = bgColorName == bgName
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(actualColor)
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) ElectricCyan else CosmicBorder,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        bgColorName = bgName
                                                        configs = configs.copy(bgColorName = bgName)
                                                        com.example.data.FloatingSettings.saveConfigs(context, configs)
                                                    }
                                                    .testTag("overlay_bg_color_$bgName"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = ElectricCyan,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                HorizontalDivider(color = CosmicBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(4.dp))

                                // BLUETOOTH REMOTE CONTROL STATUS DISPLAY
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = null,
                                                tint = if (com.example.util.RemoteClickerManager.isConnected) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Bluetooth Remote",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (com.example.util.RemoteClickerManager.isConnected) {
                                                        Color(0xFF00E676).copy(alpha = 0.15f)
                                                    } else {
                                                        Color.White.copy(alpha = 0.05f)
                                                    }
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (com.example.util.RemoteClickerManager.isConnected) "Active" else "Disconnected",
                                                color = if (com.example.util.RemoteClickerManager.isConnected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (com.example.util.RemoteClickerManager.isConnected) {
                                            "Remote: ${com.example.util.RemoteClickerManager.connectedDeviceName ?: "Detected"}\nType: ${com.example.util.RemoteClickerManager.deviceType ?: "Remote Clicker"}"
                                        } else {
                                            "No remote clicker detected. Pair a clicker/presenter in system Settings."
                                        },
                                        fontSize = 8.sp,
                                        color = SlateTextSecondary,
                                        lineHeight = 10.sp
                                    )

                                    if (com.example.util.RemoteClickerManager.isConnected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Control Mappings:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = Color.White
                                        )

                                        com.example.util.RemoteClickerManager.CUSTOMIZABLE_KEYS.forEach { (keyCode, keyLabel) ->
                                            val currentAction = com.example.util.RemoteClickerManager.getActionForKey(keyCode)
                                            var showMapMenu by remember { mutableStateOf(false) }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color.White.copy(alpha = 0.02f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = keyLabel.replace(" Action", ""),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 8.sp
                                                )

                                                Box {
                                                    TextButton(
                                                        onClick = {
                                                            val options = com.example.util.ClickerAction.values()
                                                            val currentIndex = options.indexOf(currentAction)
                                                            val nextAction = options[(currentIndex + 1) % options.size]
                                                            com.example.util.RemoteClickerManager.setActionForKey(keyCode, nextAction)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                                                        modifier = Modifier.height(20.dp).testTag("floating_map_button_${keyCode}")
                                                    ) {
                                                        Text(
                                                            text = currentAction.label,
                                                            color = ElectricCyan,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Change Action",
                                                            tint = ElectricCyan,
                                                            modifier = Modifier.size(10.dp).padding(start = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Scroll Tuning control inputs (Animated visibility)
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurfaceElevated.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isVoiceSyncActive && isNoisyEnvironment) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ElectricPurple.copy(alpha = 0.90f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        isVoiceSyncActive = false
                                        isNoisyEnvironment = false
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "High ambient noise detected 🔊",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "The environment is loud. Click here to fallback to manual scrolling.",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        if (micErrorText != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.90f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        isVoiceSyncActive = false
                                        micErrorText = null
                                    }
                                    .testTag("floating_voice_sync_error_fallback")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = micErrorText ?: "Microphone is busy/unavailable.",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (micErrorText?.startsWith("Open CueFlow") == true) {
                                            "Grant permission in CueFlow, then enable Voice Sync again."
                                        } else {
                                            "Click to fall back to manual speed control."
                                        },
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Slider Row 1: Speed, Sizing, Opacity modifiers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Play state trigger
                                java.lang.Object() // Force clean separation
                                IconButton(
                                    onClick = { 
                                        isPlaying = !isPlaying
                                        onInteraction()
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) Color(0xFFEF4444).copy(alpha = 0.15f) else ElectricCyan.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play / Pause",
                                        tint = if (isPlaying) Color(0xFFEF4444) else ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Voice Sync Toggle & Pulsing Mic Indicator
                                val micPulse by animateFloatAsState(
                                    targetValue = if (isVoiceSyncActive && isHearingSpoken) 1.25f + (rmsLevel.coerceIn(0f, 15f) / 15f) * 0.3f else 1.0f,
                                    animationSpec = spring(dampingRatio = 0.6f),
                                    label = "FloatingMic animate"
                                )
                                IconButton(
                                    onClick = {
                                        if (isVoiceSyncActive) {
                                            isVoiceSyncActive = false
                                            micErrorText = null
                                            prefs.edit().putBoolean("voice_sync_enabled_${script.id}", false).apply()
                                            sendVoiceSyncServiceAction(FloatingPrompterService.ACTION_DISABLE_VOICE_SYNC)
                                        } else if (hasMicrophonePermission) {
                                            micErrorText = null
                                            sendVoiceSyncServiceAction(FloatingPrompterService.ACTION_ENABLE_VOICE_SYNC)
                                        } else {
                                            requestVoiceSyncPermission()
                                        }
                                        onInteraction()
                                    },
                                    modifier = Modifier
                                        .graphicsLayer(scaleX = micPulse, scaleY = micPulse)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isVoiceSyncActive) {
                                                if (isHearingSpoken) ElectricCyan.copy(alpha = 0.35f) else ElectricCyan.copy(alpha = 0.15f)
                                            } else {
                                                Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .testTag("floating_voice_sync_pulse_indicator")
                                ) {
                                    Icon(
                                        imageVector = if (isVoiceSyncActive) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = "Voice Sync Toggle",
                                        tint = if (isVoiceSyncActive) ElectricCyan else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Speed indicator adjustments
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        speed = (speed - 1).coerceAtLeast(1f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Slower", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = "S:${speed.toInt()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                IconButton(
                                    onClick = { 
                                        speed = (speed + 1).coerceAtMost(20f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Faster", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                            }

                            // Sizing adjustments
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        fontSize = (fontSize - 2).coerceAtLeast(12f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Smaller", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = "A:${fontSize.toInt()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPurple
                                )
                                IconButton(
                                    onClick = { 
                                        fontSize = (fontSize + 2).coerceAtMost(48f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Bigger", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                            }

                            // Solid/Glass backdrop opacity levels
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { 
                                        opacity = (opacity - 0.1f).coerceAtLeast(0.15f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Clearer", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    text = "O:${(opacity * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmAmber
                                )
                                IconButton(
                                    onClick = { 
                                        opacity = (opacity + 0.1f).coerceAtMost(1.0f)
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Opaque", tint = SlateTextPrimary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        // Alignment, mirror horizontally, return and window sizing trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Mirror toggle
                                IconButton(
                                    onClick = { 
                                        isMirrored = !isMirrored
                                        onInteraction()
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(if (isMirrored) ElectricPurple.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(6.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = "Toggle Mirror",
                                        tint = if (isMirrored) ElectricPurple else SlateTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Alignment cycle
                                IconButton(
                                    onClick = {
                                        alignment = when (alignment) {
                                            "left" -> "center"
                                            "center" -> "right"
                                            else -> "left"
                                        }
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    val alignIcon = when (alignment) {
                                        "center" -> Icons.Default.FormatAlignCenter
                                        "right" -> Icons.Default.FormatAlignRight
                                        else -> Icons.Default.FormatAlignLeft
                                    }
                                    Icon(
                                        imageVector = alignIcon,
                                        contentDescription = "Toggle Alignment",
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Reset to vertical origin
                                IconButton(
                                    onClick = {
                                        scrollState.requestScrollToItem(0, 0)
                                        isPlaying = false
                                        isCompleted = false
                                        onInteraction()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Scroll",
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Settings button in overlay controls
                                IconButton(
                                    onClick = {
                                        showInOverlaySettings = !showInOverlaySettings
                                        onInteraction()
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(if (showInOverlaySettings) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(6.dp))
                                        .testTag("floating_settings_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Customize Settings",
                                        tint = if (showInOverlaySettings) ElectricCyan else SlateTextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Dynamic Window dimension drag corner indicator
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            onResize(dragAmount.x.toInt(), dragAmount.y.toInt())
                                            onInteraction()
                                        }
                                    }
                                    .testTag("floating_resize_handle"),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Drag to Resize",
                                    tint = ElectricCyan,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .graphicsLayer(rotationZ = -45f)
                                )
                            }
                        }
                    }
                }

                // Hardware Keys adjust HUD overlay feedback
                AnimatedVisibility(
                    visible = hardwareIndicatorMessage != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it },
                    modifier = with(boxScope) {
                        Modifier.align(Alignment.TopCenter)
                    }.padding(top = 50.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.testTag("floating_hardware_indicator_overlay")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = hardwareIndicatorMessage ?: "",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
}
