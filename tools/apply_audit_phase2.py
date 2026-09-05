from pathlib import Path
import re


def read(path: str) -> str:
    return Path(path).read_text(encoding="utf-8")


def write(path: str, text: str) -> None:
    Path(path).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def sub_once(text: str, pattern: str, repl: str, label: str) -> str:
    new_text, count = re.subn(pattern, repl, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one regex match, found {count}")
    return new_text


# ---------------------------------------------------------------------------
# ScriptDialogs.kt: recording, permissions, Wi-Fi session, Unicode matching,
# accessibility and truthful UI.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/ui/components/ScriptDialogs.kt"
text = read(path)

# Unicode-safe word cleanup for voice sync, calibration and bookmark matching.
text = text.replace('Regex("[^a-zA-Z0-9\\\\s]")', 'Regex("[^\\\\p{L}\\\\p{N}\\\\s]")')
text = text.replace('Regex("[^a-zA-Z0-9]")', 'Regex("[^\\\\p{L}\\\\p{N}]")')

text = replace_once(text, '    var storageWarningMessage by remember { mutableStateOf<String?>(null) }\n', '', 'remove storage warning state')
text = replace_once(text, '    var wasRecordingInterrupted by remember { mutableStateOf(false) }\n', '', 'remove interrupted success state')

old_lifecycle = '''    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                if (isRecording) {
                    isRecording = false
                    wasRecordingInterrupted = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
                
                // Monitor storage space periodically during recording
                val currentSpace = context.filesDir.usableSpace
                if (currentSpace < 10 * 1024 * 1024) { // Less than 10MB
                    isRecording = false
                    storageWarningMessage = "Recording stopped. Device storage is currently full. We have safely saved what you loaded up to this point!"
                }
            }
        }
    }'''
new_lifecycle = '''    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && isRecording) {
                // Request a clean stop. CameraX Finalize is the single source of truth for save success.
                isRecording = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSec = 0
            while (isRecording) {
                delay(1000)
                recordingDurationSec++
            }
        }
    }'''
text = replace_once(text, old_lifecycle, new_lifecycle, 'record lifecycle/finalize state')

old_camera_launcher = '''    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
        hasCameraPermission = cameraGranted
        hasAudioPermission = permissions[android.Manifest.permission.RECORD_AUDIO] ?: false
        if (cameraGranted) {
            showPermissionExplanation = false
            permissionDeniedPermanently = false
            isCountingDown = true
        } else {
            permissionDeniedPermanently = true
            showPermissionExplanation = true
        }
    }'''
new_camera_launcher = '''    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { cameraGranted ->
        hasCameraPermission = cameraGranted
        if (cameraGranted) {
            showPermissionExplanation = false
            permissionDeniedPermanently = false
            isCountingDown = true
        } else {
            permissionDeniedPermanently = true
            showPermissionExplanation = true
        }
    }

    var startRecordingAfterAudioPrompt by remember { mutableStateOf(false) }
    val recordingAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            android.widget.Toast.makeText(
                context,
                "Microphone access was not granted. This recording will be silent.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
        if (startRecordingAfterAudioPrompt) {
            startRecordingAfterAudioPrompt = false
            isRecording = true
            if (!isPlaying) isPlaying = true
        }
    }

    fun requestRecordingStart() {
        if (hasAudioPermission) {
            isRecording = true
            if (!isPlaying) isPlaying = true
        } else {
            startRecordingAfterAudioPrompt = true
            recordingAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }'''
text = replace_once(text, old_camera_launcher, new_camera_launcher, 'separate camera/audio permissions')

old_countdown_record = '''            // Automatically start recording when countdown finishes if in camera mode
            if (!internalPracticeMode && hasCameraPermission) {
                val usableSpace = context.filesDir.usableSpace
                if (usableSpace < 50 * 1024 * 1024) {
                    storageWarningMessage = "Could not start recording because phone storage space is critically low (less than 50MB free)."
                } else if (!isRecording) {
                    isRecording = true
                }
            }'''
new_countdown_record = '''            // Record mode starts only after camera consent, then asks separately for microphone audio.
            if (!internalPracticeMode && hasCameraPermission && !isRecording) {
                requestRecordingStart()
            }'''
text = replace_once(text, old_countdown_record, new_countdown_record, 'countdown recording start')

# Camera access explanation requests camera only and a camera-free continuation becomes real Practice Mode.
old_permission_launch = '''                                    cameraPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.CAMERA,
                                            android.Manifest.permission.RECORD_AUDIO
                                        )
                                    )'''
text = replace_once(
    text,
    old_permission_launch,
    '                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)',
    'permission explanation camera request',
)
text = replace_once(
    text,
    '''                                    showPermissionExplanation = false
                                    isCountingDown = true''',
    '''                                    showPermissionExplanation = false
                                    internalPracticeMode = true
                                    isCountingDown = true''',
    'continue without camera becomes practice',
)
text = replace_once(
    text,
    'Text("100% Secure, locally buffered loop. No recording saved.", fontSize = 11.sp, color = SlateTextPrimary, fontWeight = FontWeight.Medium)',
    'Text("Camera preview stays on-device. Video is saved only during Record Video mode.", fontSize = 11.sp, color = SlateTextPrimary, fontWeight = FontWeight.Medium)',
    'camera privacy copy',
)
text = replace_once(text, 'text = "Enable Mirror Camera",', 'text = "Allow Camera Preview",', 'camera permission title')
text = replace_once(
    text,
    'text = "Stream a real-time self-reflection loop directly behind your scrolling content to optimize your posture, verify your framing, and maintain natural speaker eye contact aligned perfectly near the lens.",',
    'text = "CueFlow uses the camera only for the live preview and Record Video mode. Microphone access is requested separately when audio recording or voice sync needs it.",',
    'camera permission body',
)

# Record button: camera permission is independent, mic permission is requested only when recording begins.
old_record_click = '''                                            if (!hasCameraPermission) {
                                                cameraPermissionLauncher.launch(
                                                    arrayOf(
                                                        android.Manifest.permission.CAMERA,
                                                        android.Manifest.permission.RECORD_AUDIO
                                                    )
                                                )
                                            } else {
                                                val usableSpace = context.filesDir.usableSpace
                                                if (!isRecording && usableSpace < 50 * 1024 * 1024) {
                                                    storageWarningMessage = "Could not start recording because phone storage space is critically low (less than 50MB free)."
                                                } else {
                                                    isRecording = !isRecording
                                                    if (isRecording && !isPlaying) {
                                                        isPlaying = true
                                                    }
                                                }
                                            }'''
new_record_click = '''                                            if (!hasCameraPermission) {
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            } else if (isRecording) {
                                                isRecording = false
                                            } else {
                                                requestRecordingStart()
                                            }'''
text = replace_once(text, old_record_click, new_record_click, 'record button permission/start')

# Remove speculative storage/interruption success dialogs. Finalize feedback remains below.
text = sub_once(
    text,
    r'\n            if \(storageWarningMessage != null\) \{.*?\n            \}\n\n            if \(wasRecordingInterrupted\) \{.*?\n            \}\n\n            // Success feedback dialog card upon recording stop/finalize',
    '\n\n            // Success feedback dialog card is shown only after a successful CameraX Finalize event',
    'remove speculative recording dialogs',
)
text = replace_once(
    text,
    'text = "Your clean, high-quality speaker session without teleprompter markings has been preserved in your phone\'s Gallery / Camera Roll.",',
    'text = "CameraX finished saving this recording to Movies/CueFlow. You can open it from your gallery or play it now.",',
    'recording success copy',
)

# Wi-Fi remote is opt-in for this playback session only; never auto-start from a persisted preference.
old_wifi_state = '''    var isWifiRemoteActive by remember {
        mutableStateOf(prefs.getBoolean("wifi_remote_enabled", false))
    }

    LaunchedEffect(isWifiRemoteActive) {
        if (isWifiRemoteActive) {
            com.example.util.WifiRemoteServer.start(context)
        } else {
            com.example.util.WifiRemoteServer.stop()
        }
    }'''
new_wifi_state = '''    var isWifiRemoteActive by remember { mutableStateOf(false) }

    LaunchedEffect(isWifiRemoteActive) {
        if (isWifiRemoteActive) {
            com.example.util.WifiRemoteServer.start(context)
        } else {
            com.example.util.WifiRemoteServer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { com.example.util.WifiRemoteServer.stop() }
    }'''
text = replace_once(text, old_wifi_state, new_wifi_state, 'session-only wifi remote')
text = replace_once(
    text,
    '''                                            isWifiRemoteActive = checked
                                            prefs.edit().putBoolean("wifi_remote_enabled", checked).apply()''',
    '                                            isWifiRemoteActive = checked',
    'wifi toggle persistence',
)
old_server_url = '''                                    val activeIp = com.example.util.WifiRemoteServer.serverIpAddress ?: "127.0.0.1"
                                    val serverUrl = "http://$activeIp:8990"'''
new_server_url = '''                                    val activeIp = com.example.util.WifiRemoteServer.serverIpAddress ?: "127.0.0.1"
                                    val pairingToken = com.example.util.WifiRemoteServer.pairingToken
                                    val serverUrl = "http://$activeIp:${com.example.util.WifiRemoteServer.PORT}/?token=$pairingToken"'''
text = replace_once(text, old_server_url, new_server_url, 'paired remote URL')
text = replace_once(text, 'val qrBitmap = remember(activeIp) {', 'val qrBitmap = remember(activeIp, pairingToken) {', 'paired QR remember')
text = replace_once(
    text,
    'text = "Note: Both devices must be connected to the same WiFi network.",',
    'text = "Both devices must be on the same trusted Wi-Fi network. This pairing link is temporary and expires when the remote stops or goes idle.",',
    'wifi trust warning',
)

# Remove fake monetization badge from the free floating overlay entry point.
text = sub_once(
    text,
    r'\n                                Box\(\n                                    modifier = Modifier\n                                        \.background\(WarmAmber\.copy\(alpha = 0\.15f\), RoundedCornerShape\(6\.dp\)\)\n                                        \.border\(BorderStroke\(0\.5\.dp, WarmAmber\), RoundedCornerShape\(6\.dp\)\)\n                                        \.padding\(horizontal = 5\.dp, vertical = 2\.dp\)\n                                \) \{\n                                    Text\("PRO", fontSize = 8\.sp, color = WarmAmber, fontWeight = FontWeight\.ExtraBold\)\n                                \}',
    '',
    'remove fake pro badge',
)

# Touch target corrections for the most-used playback controls.
for tag in ["speed_decrease_fine", "speed_increase_fine", "height_decrease_fine", "height_increase_fine"]:
    text = text.replace(f'Modifier.size(32.dp).testTag("{tag}")', f'Modifier.size(48.dp).testTag("{tag}")')
text = text.replace('modifier = Modifier.size(28.dp)) {\n                        Icon(Icons.Default.Close, contentDescription = "Close"', 'modifier = Modifier.size(48.dp)) {\n                        Icon(Icons.Default.Close, contentDescription = "Close"')
text = text.replace('modifier = Modifier.weight(1.5f).height(38.dp)', 'modifier = Modifier.weight(1.5f).height(48.dp)')
text = text.replace('Modifier\n                                        .weight(1f)\n                                        .height(38.dp)', 'Modifier\n                                        .weight(1f)\n                                        .height(48.dp)')

# CameraPreview honors the user's quality preference with documented fallback behavior.
old_video_capture = '''    val videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> = remember {
        val recorder = androidx.camera.video.Recorder.Builder()
            .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.HIGHEST))
            .build()
        androidx.camera.video.VideoCapture.withOutput(recorder)
    }'''
new_video_capture = '''    val recordingQuality = remember(context) {
        context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE)
            .getString("recording_video_quality", "1080p") ?: "1080p"
    }
    val videoCapture: androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> = remember(recordingQuality) {
        val qualityOrder = when (recordingQuality) {
            "4K" -> listOf(
                androidx.camera.video.Quality.UHD,
                androidx.camera.video.Quality.FHD,
                androidx.camera.video.Quality.HD,
                androidx.camera.video.Quality.SD,
            )
            "720p" -> listOf(
                androidx.camera.video.Quality.HD,
                androidx.camera.video.Quality.SD,
            )
            else -> listOf(
                androidx.camera.video.Quality.FHD,
                androidx.camera.video.Quality.HD,
                androidx.camera.video.Quality.SD,
            )
        }
        val selector = androidx.camera.video.QualitySelector.fromOrderedList(
            qualityOrder,
            androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(androidx.camera.video.Quality.SD),
        )
        val recorder = androidx.camera.video.Recorder.Builder()
            .setQualitySelector(selector)
            .build()
        androidx.camera.video.VideoCapture.withOutput(recorder)
    }'''
text = replace_once(text, old_video_capture, new_video_capture, 'CameraX quality selection')

old_finalize = '''                        is androidx.camera.video.VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {
                                onRecordingStopped(event.outputResults.outputUri)
                            } else {
                                android.util.Log.e("CameraPreview", "Recording finalized error: ${event.error}")
                                onRecordingStopped(event.outputResults.outputUri)
                            }
                        }'''
new_finalize = '''                        is androidx.camera.video.VideoRecordEvent.Finalize -> {
                            currentRecording.value = null
                            if (!event.hasError()) {
                                onRecordingStopped(event.outputResults.outputUri)
                            } else {
                                android.util.Log.e("CameraPreview", "Recording finalized error: ${event.error}")
                                onCameraError("Recording could not be saved (CameraX error ${event.error}). Check available storage and try again.")
                                onRecordingStopped(null)
                            }
                        }'''
text = replace_once(text, old_finalize, new_finalize, 'CameraX Finalize truth')
text = replace_once(
    text,
    '''            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Error starting recording: ${e.localizedMessage}")
                onRecordingStopped(null)
            }''',
    '''            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Error starting recording: ${e.localizedMessage}")
                onCameraError("Recording could not start. ${e.localizedMessage ?: "Please retry."}")
                onRecordingStopped(null)
            }''',
    'record start error',
)

write(path, text)


# ---------------------------------------------------------------------------
# FloatingPrompterService.kt: correct FGS type, Unicode matching and recoverable
# overlay bounds.
# ---------------------------------------------------------------------------
path = "app/src/main/java/com/example/service/FloatingPrompterService.kt"
text = read(path)
text = text.replace('Regex("[^a-zA-Z0-9\\\\s]")', 'Regex("[^\\\\p{L}\\\\p{N}\\\\s]")')
text = text.replace('Regex("[^a-zA-Z0-9]")', 'Regex("[^\\\\p{L}\\\\p{N}]")')

old_fgs = '''        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }'''
new_fgs = '''        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            var fgsType = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            if (hasMicrophonePermission) {
                fgsType = fgsType or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }'''
text = replace_once(text, old_fgs, new_fgs, 'foreground service types')

text = replace_once(
    text,
    'WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,',
    'WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,',
    'remove no-limits overlay flag',
)
old_drag_resize = '''                        onDrag = { dx, dy ->
                            params.x = (params.x + dx).coerceAtLeast(0)
                            params.y = (params.y + dy).coerceAtLeast(0)
                            updateView()
                        },
                        onResize = { dw, dh ->
                            val minWidth = (180 * density).toInt()
                            val minHeight = (180 * density).toInt()
                            params.width = (params.width + dw).coerceAtLeast(minWidth)
                            params.height = (params.height + dh).coerceAtLeast(minHeight)
                            updateView()
                        }'''
new_drag_resize = '''                        onDrag = { dx, dy ->
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
                        }'''
text = replace_once(text, old_drag_resize, new_drag_resize, 'overlay drag/resize bounds')
write(path, text)


# Fix the typed Compose scope on the replacement production Settings dialog.
path = "app/src/main/java/com/example/ui/components/SettingsDialog.kt"
text = read(path)
text = replace_once(
    text,
    '    content: @Composable Column.() -> Unit,',
    '    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,',
    'settings ColumnScope receiver',
)
write(path, text)

print("Phase 2 playback/service patches applied successfully")
