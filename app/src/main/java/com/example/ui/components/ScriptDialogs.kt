package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.Script
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.util.HardwareButtonController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.view.KeyEvent

@Composable
fun AddEditScriptDialog(
    script: Script?, // null if creating a new one
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, speed: Int, size: Int, mirrored: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(script?.title ?: "") }
    var content by remember { mutableStateOf(script?.content ?: "") }
    var speed by remember { mutableFloatStateOf(script?.scrollSpeed?.toFloat() ?: 5f) }
    var fontSize by remember { mutableFloatStateOf(script?.fontSize?.toFloat() ?: 24f) }
    var isMirrored by remember { mutableStateOf(script?.isMirrored ?: false) }

    // Stat calculators
    val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }
    val speakingSpeedWpm = 130
    val totalSeconds = if (wordCount > 0) (wordCount * 60) / speakingSpeedWpm else 0
    val durationText = if (totalSeconds < 60) "${totalSeconds}s" else "${totalSeconds / 60}m ${totalSeconds % 60}s"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.95f))
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .testTag("add_edit_script_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("dialog_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Leave Editor",
                            tint = SlateTextSecondary
                        )
                    }

                    Text(
                        text = if (script == null) "Create Script" else "Edit Script",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )

                    // Glow Save Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (title.isNotBlank() && content.isNotBlank())
                                    Brush.linearGradient(listOf(ElectricPurple, DeepViolet))
                                else
                                    Brush.linearGradient(listOf(CosmicSurfaceElevated, CosmicSurfaceElevated))
                            )
                            .clickable(enabled = title.isNotBlank() && content.isNotBlank()) {
                                onSave(title, content, speed.toInt(), fontSize.toInt(), isMirrored)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("dialog_save_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (title.isNotBlank() && content.isNotBlank()) CosmicBackground else SlateTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Content Columns
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Script Title Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_script_title"),
                        placeholder = { Text("Title of your script...", color = SlateTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary,
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = CosmicBorder,
                            focusedContainerColor = CosmicSurface,
                            unfocusedContainerColor = CosmicSurface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Word count estimation bar (SaaS HUD)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicSurfaceElevated)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Stats HUD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = ElectricPurple,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$wordCount words",
                                fontSize = 12.sp,
                                color = SlateTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Box(modifier = Modifier.size(4.dp).background(SlateTextMuted, CircleShape))
                            Text(
                                text = "Speak Time: ~$durationText",
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Script Content Field
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 350.dp)
                            .testTag("input_script_content"),
                        placeholder = { Text("Paste or type your notes here...", color = SlateTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary,
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = CosmicBorder,
                            focusedContainerColor = CosmicSurface,
                            unfocusedContainerColor = CosmicSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Prompter Configuration Sliders Panel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CosmicSurface)
                            .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Prompter Playback Settings",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )

                        // Scroll Speed Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Scroll Speed",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                Text(
                                    text = "${speed.toInt()}x",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPurple,
                                    modifier = Modifier.testTag("display_speed")
                                )
                            }
                            Slider(
                                value = speed,
                                onValueChange = { speed = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    activeTickColor = ElectricPurple,
                                    activeTrackColor = ElectricPurple,
                                    inactiveTrackColor = CosmicBorder,
                                    thumbColor = ElectricPurple
                                ),
                                modifier = Modifier.testTag("slider_speed")
                            )
                        }

                        // Font size Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = null,
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Text Size",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary
                                    )
                                }
                                Text(
                                    text = "${fontSize.toInt()} sp",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan,
                                    modifier = Modifier.testTag("display_size")
                                )
                            }
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                valueRange = 16f..48f,
                                steps = 16,
                                colors = SliderDefaults.colors(
                                    activeTickColor = ElectricCyan,
                                    activeTrackColor = ElectricCyan,
                                    inactiveTrackColor = CosmicBorder,
                                    thumbColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("slider_size")
                            )
                        }

                        // Mirror toggle switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = null,
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Physically Mirrored",
                                        fontSize = 12.sp,
                                        color = SlateTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Horizontal flip for prompter screens",
                                        fontSize = 10.sp,
                                        color = SlateTextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = isMirrored,
                                onCheckedChange = { isMirrored = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CosmicBackground,
                                    checkedTrackColor = ElectricPurple,
                                    uncheckedThumbColor = SlateTextSecondary,
                                    uncheckedTrackColor = CosmicSurfaceElevated
                                ),
                                modifier = Modifier.testTag("switch_mirror")
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun TeleprompterPlaybackDialog(
    script: Script,
    isPracticeMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE) }
    
    val hasSystemCamera = remember(context) {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }

    var internalPracticeMode by remember {
        mutableStateOf(isPracticeMode || !hasSystemCamera)
    }

    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var cameraRetryCount by remember { mutableIntStateOf(0) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var voiceSyncErrorMessage by remember { mutableStateOf<String?>(null) }
    var isNoisyEnvironment by remember { mutableStateOf(false) }
    var noiseThresholdCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(hasSystemCamera) {
        if (!hasSystemCamera && !isPracticeMode) {
            android.widget.Toast.makeText(
                context,
                "No camera detected. Switching automatically to Practice Mode.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var isRecording by remember { mutableStateOf(false) }

    // Voice-Sync scrolling states
    var isVoiceSyncActive by remember { mutableStateOf(false) }
    var currentVoiceParaIndex by remember { mutableIntStateOf(0) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var isHearingSpoken by remember { mutableStateOf(false) }
    var spokenWordsText by remember { mutableStateOf("") }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            isVoiceSyncActive = true
        }
    }
    var recordingDurationSec by remember { mutableIntStateOf(0) }
    var recordedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showRecordingSavedDialog by remember { mutableStateOf(false) }

    // Rehearsal stats variables
    var practiceDurationSec by remember { mutableIntStateOf(0) }
    var pauseCount by remember { mutableIntStateOf(0) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
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
    }

    var showPermissionExplanation by remember { mutableStateOf(!internalPracticeMode && !hasCameraPermission) }
    var isCountingDown by remember { mutableStateOf(false) }
    val defaultCountdown = remember { prefs.getInt("default_countdown_duration", 3) }
    var countdownTime by remember { mutableIntStateOf(defaultCountdown) }

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val defaultCamStr = remember { prefs.getString("recording_default_camera", "front") ?: "front" }
    val defaultCamFront = defaultCamStr == "front"
    var isFrontCamera by remember { mutableStateOf(prefs.getBoolean("last_camera_front", defaultCamFront)) }
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isTextMirrored by remember { mutableStateOf(prefs.getBoolean("script_text_mirrored_${script.id}", script.isMirrored)) }
    var isCameraMirrored by remember { mutableStateOf(prefs.getBoolean("script_camera_mirrored_${script.id}", true)) }
    var textGuideOffsetY by remember { mutableFloatStateOf(prefs.getFloat("script_text_offset_${script.id}", 180f)) }

    var isEyelineGuideEnabled by remember {
        mutableStateOf(prefs.getBoolean("script_eyeline_enabled_${script.id}", false))
    }
    var lensXPercent by remember {
        mutableStateOf(prefs.getFloat("script_lens_x_percent_${script.id}", if (isFrontCamera) 0.5f else 0.15f))
    }
    var lensYPercent by remember {
        mutableStateOf(prefs.getFloat("script_lens_y_percent_${script.id}", 0.02f))
    }

    LaunchedEffect(isFrontCamera) {
        val draggedKey = "script_lens_dragged_${script.id}"
        if (!prefs.contains(draggedKey)) {
            if (isFrontCamera) {
                lensXPercent = 0.5f
                lensYPercent = 0.02f
            } else {
                lensXPercent = 0.15f
                lensYPercent = 0.02f
            }
        }
    }

    fun saveLensPosition(x: Float, y: Float) {
        lensXPercent = x
        lensYPercent = y
        prefs.edit()
            .putFloat("script_lens_x_percent_${script.id}", x)
            .putFloat("script_lens_y_percent_${script.id}", y)
            .putBoolean("script_lens_dragged_${script.id}", true)
            .apply()
    }

    fun resetLensPositionToDefault() {
        prefs.edit().remove("script_lens_dragged_${script.id}").apply()
        if (isFrontCamera) {
            lensXPercent = 0.5f
            lensYPercent = 0.02f
        } else {
            lensXPercent = 0.15f
            lensYPercent = 0.02f
        }
        prefs.edit()
            .putFloat("script_lens_x_percent_${script.id}", lensXPercent)
            .putFloat("script_lens_y_percent_${script.id}", lensYPercent)
            .apply()
    }
    
    // Voice-sync fine tuning and calibration states
    var voiceSensitivity by remember { mutableIntStateOf(prefs.getInt("voice_sync_sensitivity", 1)) }
    var voicePauseThreshold by remember { mutableIntStateOf(prefs.getInt("voice_sync_pause_threshold", 1)) }
    var voiceMinSpeedCrawl by remember { mutableIntStateOf(prefs.getInt("voice_sync_min_crawl", 0)) }
    var voiceMaxSpeedLimit by remember { mutableIntStateOf(prefs.getInt("voice_sync_max_limit", 0)) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    
    var lastMatchTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val maxOffset = if (isLandscape) 180f else 450f
    val currentGuideOffsetY = textGuideOffsetY.coerceIn(40f, maxOffset)

    var cameraAngle by remember { mutableStateOf(0f) }
    val animatedCameraAngle by animateFloatAsState(
        targetValue = cameraAngle,
        animationSpec = tween(durationMillis = 400),
        label = "CameraFlipAnimation"
    )

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    var isPlaying by remember { mutableStateOf(false) }
    var isScriptComplete by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var showAdvancedPlaybackSettings by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(script.scrollSpeed.toFloat()) }
    val animatedSpeedDisplay by animateFloatAsState(
        targetValue = speed,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedSpeed"
    )
    var fontScale by remember { mutableFloatStateOf(script.fontSize.toFloat()) }
    var isMirrored by remember { mutableStateOf(script.isMirrored) }
    var textColor by remember { mutableStateOf(script.textColor) }
    var bgOpacity by remember { mutableFloatStateOf(script.bgOpacity) }
    var textAlignment by remember { mutableStateOf(script.textAlignment) }
    var lineSpacing by remember { mutableStateOf(script.lineSpacing) }

    // Unified elapsed seconds counter and auto-reset on scroll rewind
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }
    LaunchedEffect(scrollState.firstVisibleItemIndex) {
        if (scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0) {
            elapsedSeconds = 0
        }
    }

    var isHardwareControlActive by remember {
        mutableStateOf(prefs.getBoolean("hardware_buttons_enabled", false))
    }
    var isWifiRemoteActive by remember { mutableStateOf(false) }

    LaunchedEffect(isWifiRemoteActive) {
        if (isWifiRemoteActive) {
            com.example.util.WifiRemoteServer.start(context)
        } else {
            com.example.util.WifiRemoteServer.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { com.example.util.WifiRemoteServer.stop() }
    }

    var hardwareButtonIndicatorText by remember { mutableStateOf<String?>(null) }

    val prompterParagraphsCount = remember(script.content) {
        script.content.split("\n").size
    }

    LaunchedEffect(script.title, isPlaying, speed, scrollState.firstVisibleItemIndex, prompterParagraphsCount) {
        HardwareButtonController.pActiveTitle = script.title
        HardwareButtonController.pIsPlaying = isPlaying
        HardwareButtonController.pSpeed = speed
        HardwareButtonController.pCurrentParagraph = scrollState.firstVisibleItemIndex
        HardwareButtonController.pTotalParagraphs = prompterParagraphsCount
    }

    LaunchedEffect(hardwareButtonIndicatorText) {
        if (hardwareButtonIndicatorText != null) {
            delay(1200)
            hardwareButtonIndicatorText = null
        }
    }

    DisposableEffect(context, isHardwareControlActive) {
        val listener = object : HardwareButtonController.Listener {
            override fun onSpeedUp() {
                speed = (speed + 0.5f).coerceAtMost(15.0f)
                hardwareButtonIndicatorText = "Speed: ${String.format("%.1f", speed)}x"
            }

            override fun onSpeedDown() {
                speed = (speed - 0.5f).coerceAtLeast(0.5f)
                hardwareButtonIndicatorText = "Speed: ${String.format("%.1f", speed)}x"
            }

            override fun onPlayPause() {
                isPlaying = !isPlaying
                hardwareButtonIndicatorText = if (isPlaying) "Playing" else "Paused"
            }

            override fun onSkipToNextBookmark() {
                val paragraphs = script.content.split("\n")
                val bookmarkedLines = prefs.getStringSet("script_bookmarks_${script.id}", emptySet()) ?: emptySet()
                if (paragraphs.isNotEmpty()) {
                    val currentIndex = scrollState.firstVisibleItemIndex
                    var nextBookmarkIndex = -1
                    for (i in (currentIndex + 1) until paragraphs.size) {
                        val cleanP = paragraphs[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                        val bookmarkedClean = bookmarkedLines.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanP }
                        if (bookmarkedClean || bookmarkedLines.contains(paragraphs[i])) {
                            nextBookmarkIndex = i
                            break
                        }
                    }
                    if (nextBookmarkIndex == -1) {
                        for (i in 0 until currentIndex) {
                            val cleanP = paragraphs[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            val bookmarkedClean = bookmarkedLines.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanP }
                            if (bookmarkedClean || bookmarkedLines.contains(paragraphs[i])) {
                                nextBookmarkIndex = i
                                break
                            }
                        }
                    }
                    if (nextBookmarkIndex != -1) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(nextBookmarkIndex)
                        }
                        hardwareButtonIndicatorText = "Skipped to Bookmark #${nextBookmarkIndex + 1}"
                    } else {
                        hardwareButtonIndicatorText = "No Bookmarks found"
                    }
                }
            }

            override fun onPrevBookmark() {
                val paragraphs = script.content.split("\n")
                val bookmarkedLines = prefs.getStringSet("script_bookmarks_${script.id}", emptySet()) ?: emptySet()
                if (paragraphs.isNotEmpty()) {
                    val currentIndex = scrollState.firstVisibleItemIndex
                    var prevBookmarkIndex = -1
                    for (i in (currentIndex - 1) downTo 0) {
                        val cleanP = paragraphs[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                        val bookmarkedClean = bookmarkedLines.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanP }
                        if (bookmarkedClean || bookmarkedLines.contains(paragraphs[i])) {
                            prevBookmarkIndex = i
                            break
                        }
                    }
                    if (prevBookmarkIndex == -1) {
                        for (i in (paragraphs.size - 1) downTo currentIndex) {
                            val cleanP = paragraphs[i].trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            val bookmarkedClean = bookmarkedLines.any { b -> b.trim().lowercase().replace(Regex("[^\\p{L}\\p{N}\\s]"), "") == cleanP }
                            if (bookmarkedClean || bookmarkedLines.contains(paragraphs[i])) {
                                prevBookmarkIndex = i
                                break
                            }
                        }
                    }
                    if (prevBookmarkIndex != -1) {
                        coroutineScope.launch {
                            scrollState.animateScrollToItem(prevBookmarkIndex)
                        }
                        hardwareButtonIndicatorText = "Skipped to Bookmark #${prevBookmarkIndex + 1}"
                    } else {
                        hardwareButtonIndicatorText = "No Bookmarks found"
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
            mediaSession = MediaSession(context, "CueFlowMediaSession_${script.id}").apply {
                setCallback(object : MediaSession.Callback() {
                    private var lastPlayPauseClickTime = 0L
                    override fun onMediaButtonEvent(mediaButtonIntent: android.content.Intent): Boolean {
                        val keyEvent = mediaButtonIntent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
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

    // Auto-hide controls after 4 seconds of inactivity if playing
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Interactive speaking progress percentage calculation
    val progress = remember {
        derivedStateOf {
            val totalItems = scrollState.layoutInfo.totalItemsCount
            if (totalItems <= 1) {
                val itemSize = scrollState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                val viewportHeight = scrollState.layoutInfo.viewportEndOffset - scrollState.layoutInfo.viewportStartOffset
                val maxScroll = itemSize - viewportHeight + 510f
                if (maxScroll > 1f) {
                    val res = (scrollState.firstVisibleItemScrollOffset.toFloat() / maxScroll)
                    if (res.isNaN() || res.isInfinite()) 0f else res.coerceIn(0f, 1f)
                } else {
                    0f
                }
            } else {
                val firstVisibleIndex = scrollState.firstVisibleItemIndex
                val firstVisibleOffset = scrollState.firstVisibleItemScrollOffset
                val visibleItems = scrollState.layoutInfo.visibleItemsInfo
                
                if (visibleItems.isEmpty()) {
                    0f
                } else {
                    val avgItemSize = visibleItems.map { it.size }.average().toFloat()
                    val totalEstimatedHeight = totalItems * avgItemSize
                    val scrolledDistance = firstVisibleIndex * avgItemSize + firstVisibleOffset
                    val viewportHeight = scrollState.layoutInfo.viewportEndOffset - scrollState.layoutInfo.viewportStartOffset
                    val maxScrollPossible = totalEstimatedHeight - viewportHeight
                    if (maxScrollPossible > 1f) {
                        val res = scrolledDistance / maxScrollPossible
                        if (res.isNaN() || res.isInfinite()) 0f else res.coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val prompterWordCount = remember(script.content) {
        script.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    
    // Total reading duration based on word count and speed setting
    val totalReadingSeconds = remember(prompterWordCount, speed) {
        val speedValue = speed.coerceAtLeast(0.5f)
        val speakingSpeedWpm = speedValue * 30f
        if (prompterWordCount > 0) ((prompterWordCount.toFloat() / speakingSpeedWpm) * 60f).toInt() else 0
    }
    
    // Remaining time calculated dynamically using scroll progress fraction
    val remainingReadingSeconds = remember(totalReadingSeconds) {
        derivedStateOf {
            val leftFraction = 1f - progress.value
            (totalReadingSeconds * leftFraction).toInt().coerceAtLeast(0)
        }
    }

    fun formatElapsedTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", mins, secs)
    }

    fun formatRemainingTime(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins > 0 && secs > 0 -> "~${mins}m ${secs}s"
            mins > 0 -> "~${mins}m"
            else -> "~${secs}s"
        }
    }

    var lastIsPlaying by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (internalPracticeMode) {
            if (lastIsPlaying && !isPlaying && !isScriptComplete) {
                pauseCount++
            }
            lastIsPlaying = isPlaying
        }
    }

    LaunchedEffect(isPlaying, isScriptComplete) {
        if (isPlaying && internalPracticeMode && !isScriptComplete) {
            while (isPlaying && !isScriptComplete) {
                delay(1000)
                practiceDurationSec++
            }
        }
    }

    val wordCount = remember(script.content) {
        script.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val wordsRead = remember(isScriptComplete, progress.value) {
        if (isScriptComplete && !scrollState.canScrollForward) {
            wordCount
        } else {
            (wordCount * progress.value).toInt().coerceIn(0, wordCount)
        }
    }
    val wpm = remember(practiceDurationSec, wordsRead) {
        if (practiceDurationSec > 0) {
            ((wordsRead * 60) / practiceDurationSec).coerceAtLeast(0)
        } else {
            0
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
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
    }

    LaunchedEffect(hasCameraPermission, showPermissionExplanation) {
        if (!showPermissionExplanation && (internalPracticeMode || hasCameraPermission)) {
            isCountingDown = true
        }
    }

    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            isPlaying = false
            countdownTime = prefs.getInt("default_countdown_duration", 3)
            while (countdownTime > 0) {
                delay(1000)
                countdownTime--
            }
            isCountingDown = false
            isPlaying = true
            
            // Record mode starts only after camera consent, then asks separately for microphone audio.
            if (!internalPracticeMode && hasCameraPermission && !isRecording) {
                requestRecordingStart()
            }
        }
    }

    // Highly efficient frame-based vertical auto-scroll loop (deactivated if voice sync is active)
    LaunchedEffect(isPlaying, speed, isVoiceSyncActive) {
        if (isPlaying && !isVoiceSyncActive) {
            isScriptComplete = false
            try {
                scrollState.scroll(scrollPriority = androidx.compose.foundation.MutatePriority.Default) {
                    var lastFrameNanos = System.nanoTime()
                    while (isPlaying && !isVoiceSyncActive) {
                        androidx.compose.runtime.withFrameNanos { frameTimeNanos ->
                            val elapsedSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                            lastFrameNanos = frameTimeNanos
                            
                            if (scrollState.canScrollForward) {
                                // Dynamic physics-based subpixel scroll increment aligned to frame refresh rate
                                val scrollPixels = (speed * 45f) * elapsedSeconds
                                if (scrollPixels > 0f) {
                                    scrollBy(scrollPixels)
                                }
                            } else {
                                isPlaying = false
                                isScriptComplete = true
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

    // Voice sync smooth scrolling transition
    LaunchedEffect(currentVoiceParaIndex, isVoiceSyncActive, isPlaying) {
        if (isVoiceSyncActive && isPlaying) {
            try {
                // Scroll beautifully with a negative offset to align near the middle reading area
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
    val resolvedParagraphs = remember(script.content) { script.content.split("\n") }
    LaunchedEffect(currentVoiceParaIndex, resolvedParagraphs) {
        if (isVoiceSyncActive && isPlaying && currentVoiceParaIndex >= resolvedParagraphs.lastIndex - 1 && resolvedParagraphs.isNotEmpty()) {
            delay(1500) // Delay slightly to let user finish speaking the last sentence
            if (currentVoiceParaIndex >= resolvedParagraphs.lastIndex - 1 && isPlaying) {
                isPlaying = false
                isScriptComplete = true
            }
        }
    }

    // Word token maps for matching inside SpeechRecognizer callback on UI thread
    val voiceParagraphWords = remember(resolvedParagraphs) {
        resolvedParagraphs.map { para ->
            para.lowercase()
                .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                .split("\\s+".toRegex())
                .filter { it.isNotBlank() }
        }
    }

    // Main SpeechRecognizer management loop with sensitivity, pause threshold, and max limit support
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
                        
                        // Human-friendly pause thresholds mapped to speech recognizer complete silences
                        val silenceLen = when (voicePauseThreshold) {
                            0 -> 1000L    // Short Pauses: fast processing cut-off
                            2 -> 4500L    // Long Pauses: relaxed cut-off
                            else -> 2200L  // Normal Pauses: balanced cut-off
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
                } catch (e: Exception) {
                    e.printStackTrace()
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
                        android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        android.speech.SpeechRecognizer.ERROR_AUDIO -> {
                            voiceSyncErrorMessage = "The microphone is currently being used by another application (like a phone call, voice memo, or camera app). We have gracefully fallen back to manual scrolling control."
                            isVoiceSyncActive = false
                        }
                        android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            voiceSyncErrorMessage = "Microphone permission is required to use Voice Sync. Please enable it in your device settings or use manual scrolling mode."
                            isVoiceSyncActive = false
                        }
                        android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        android.speech.SpeechRecognizer.ERROR_NO_MATCH -> {
                            // Restart listening if active and playing
                            if (isPlaying && isVoiceSyncActive) {
                                coroutineScope.launch {
                                    delay(400)
                                    if (isPlaying && isVoiceSyncActive) {
                                        try {
                                            startListeningRunnable.run()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            // Restart listening if active and playing
                            if (isPlaying && isVoiceSyncActive) {
                                coroutineScope.launch {
                                    delay(500)
                                    if (isPlaying && isVoiceSyncActive) {
                                        try {
                                            startListeningRunnable.run()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
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
                        spokenWordsText = spoken
                        
                        val spokenWords = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                        
                        if (spokenWords.isNotEmpty()) {
                            val currentIdx = currentVoiceParaIndex
                            
                            // Adjust search range dynamically based on sensitivity
                            val startRange = when (voiceSensitivity) {
                                0 -> (currentIdx - 1).coerceAtLeast(0) // Stable: look very close behind
                                2 -> (currentIdx - 2).coerceAtLeast(0) // Responsive: look wider behind
                                else -> (currentIdx - 1).coerceAtLeast(0) // Balanced
                            }
                            val endRange = when (voiceSensitivity) {
                                0 -> (currentIdx + 2).coerceAtMost(resolvedParagraphs.lastIndex) // Stable: small forward window
                                2 -> (currentIdx + 7).coerceAtMost(resolvedParagraphs.lastIndex) // Responsive: wide forward window
                                else -> (currentIdx + 4).coerceAtMost(resolvedParagraphs.lastIndex) // Balanced
                            }
                            
                            var bestMatchIdx = -1
                            var bestScore = 0
                            val recentSpoken = spokenWords.takeLast(10)
                            
                            for (paraIdx in startRange..endRange) {
                                val words = voiceParagraphWords[paraIdx]
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
                                
                                // Adjust score passing constraints based on sensitivity setting
                                val isValidMatch = when (voiceSensitivity) {
                                    0 -> score >= 2 || (score >= 1 && hasLongWord)  // Stable: Requires higher threshold
                                    2 -> score >= 1                               // Responsive: Highly reactive to any matched word
                                    else -> score > 0 && (hasLongWord || score >= 2) // Balanced: standard validation
                                }
                                
                                if (isValidMatch) {
                                    if (score > bestScore) {
                                        bestScore = score
                                        bestMatchIdx = paraIdx
                                    }
                                }
                            }
                            
                            if (bestMatchIdx != -1) {
                                // Apply Max Speed Jump limit constraints
                                val maxJump = when (voiceMaxSpeedLimit) {
                                    2 -> 1 // Strict Cap: jump exactly 1 paragraph max at a time
                                    1 -> 2 // Paced Cap: jump 2 paragraphs max at a time
                                    else -> 9999 // Unlimited: go directly to match
                                }
                                val cappedIdx = if (bestMatchIdx > currentIdx) {
                                    currentIdx + (bestMatchIdx - currentIdx).coerceAtMost(maxJump)
                                } else {
                                    bestMatchIdx // allow backward matching without constraints
                                }
                                
                                currentVoiceParaIndex = cappedIdx
                                lastMatchTime = System.currentTimeMillis()
                            }
                        }
                    }
                    
                    if (isPlaying && isVoiceSyncActive) {
                        try {
                            startListeningRunnable.run()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        isHearingSpoken = true
                        val spoken = matches[0] ?: ""
                        spokenWordsText = spoken
                        
                        val spokenWords = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                            
                        if (spokenWords.isNotEmpty()) {
                            val currentIdx = currentVoiceParaIndex
                            
                            // Adjust search range dynamically based on sensitivity
                            val startRange = when (voiceSensitivity) {
                                0 -> (currentIdx - 1).coerceAtLeast(0)
                                2 -> (currentIdx - 3).coerceAtLeast(0)
                                else -> (currentIdx - 2).coerceAtLeast(0)
                            }
                            val endRange = when (voiceSensitivity) {
                                0 -> (currentIdx + 3).coerceAtMost(resolvedParagraphs.lastIndex)
                                2 -> (currentIdx + 8).coerceAtMost(resolvedParagraphs.lastIndex)
                                else -> (currentIdx + 5).coerceAtMost(resolvedParagraphs.lastIndex)
                            }
                            
                            var bestMatchIdx = -1
                            var bestScore = 0
                            val recentSpoken = spokenWords.takeLast(10)
                            
                            for (paraIdx in startRange..endRange) {
                                val words = voiceParagraphWords[paraIdx]
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
                                
                                // Adjust passing constraints for partial matches based on sensitivity
                                val isValidMatch = when (voiceSensitivity) {
                                    0 -> score >= 3 || (score >= 2 && hasLongWord)  // Stable: high threshold to avoid stutter leaps
                                    2 -> score >= 1                               // Responsive: reacts immediately on partial hits
                                    else -> score > 1 && (hasLongWord || score >= 2) // Balanced: standard validation
                                }
                                
                                if (isValidMatch) {
                                    if (score > bestScore) {
                                        bestScore = score
                                        bestMatchIdx = paraIdx
                                    }
                                }
                            }
                            
                            if (bestMatchIdx != -1 && bestMatchIdx != currentIdx) {
                                // Apply Max Speed Jump limit constraints
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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    recognizer?.stopListening()
                    recognizer?.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isHearingSpoken = false
                rmsLevel = 0f
            }
        } else {
            isHearingSpoken = false
            rmsLevel = 0f
        }
    }

    // Auto-crawl fallback loop for voice sync (if min speed limit is active and user pauses speaking)
    LaunchedEffect(isPlaying, isVoiceSyncActive, voiceMinSpeedCrawl, currentVoiceParaIndex) {
        if (isPlaying && isVoiceSyncActive && voiceMinSpeedCrawl > 0) {
            val intervalMs = when (voiceMinSpeedCrawl) {
                1 -> 5000L  // Slow Crawl: 5 seconds of silence allowed before sliding
                2 -> 2500L  // Medium Crawl: 2.5 seconds of silence allowed before sliding
                else -> 999999L
            }
            while (isPlaying && isVoiceSyncActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - lastMatchTime
                if (elapsed >= intervalMs) {
                    if (currentVoiceParaIndex < resolvedParagraphs.lastIndex) {
                        currentVoiceParaIndex++
                        lastMatchTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070B))
                .testTag("teleprompter_playback_workspace")
        ) {
            val screenWidthDp = maxWidth
            val screenHeightDp = maxHeight
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenWidthPx = with(density) { screenWidthDp.toPx() }
            val screenHeightPx = with(density) { screenHeightDp.toPx() }

            val calculatedGuideOffsetY = if (isEyelineGuideEnabled) {
                (lensYPercent * screenHeightDp.value - 40f).coerceIn(10f, maxOffset)
            } else {
                currentGuideOffsetY
            }

            val columnAlignment = if (isEyelineGuideEnabled) {
                androidx.compose.ui.BiasAlignment(
                    horizontalBias = (lensXPercent * 2f - 1f).coerceIn(-1f, 1f),
                    verticalBias = -1f
                )
            } else {
                Alignment.TopCenter
            }
            // Hardware Button adjustment feedback indicator overlay
            AnimatedVisibility(
                visible = hardwareButtonIndicatorText != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricCyan.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.testTag("hardware_buttons_hud_indicator")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = hardwareButtonIndicatorText ?: "",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            // Real-time camera feed backdrop
            if (!internalPracticeMode && hasCameraPermission) {
                CameraPreview(
                    isFrontCamera = isFrontCamera,
                    isFlashEnabled = isFlashEnabled,
                    isCameraMirrored = isCameraMirrored,
                    isRecording = isRecording,
                    onRecordingStarted = {
                        // Callback when recording start is processed
                    },
                    onRecordingStopped = { uri ->
                        isRecording = false
                        if (uri != null) {
                            recordedVideoUri = uri
                            showRecordingSavedDialog = true
                        }
                    },
                    onCameraError = { errMsg ->
                        cameraErrorMessage = errMsg
                    },
                    retryTrigger = cameraRetryCount,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("camera_preview_feed")
                )
            } else {
                if (internalPracticeMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("practice_backdrop"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElectricCyan.copy(alpha = 0.12f))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PRACTICE MODE — CAMERA OFF",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 2f
                            // Center focus circle
                            drawCircle(
                                color = Color.White.copy(alpha = 0.12f),
                                radius = 120f,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                            )
                            // Guidelines
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height)
                            )
                            // Corner crop marks
                            val cropLen = 40f
                            drawLine(Color.White.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(40f, 40f), androidx.compose.ui.geometry.Offset(40f + cropLen, 40f), strokeWidth = stroke)
                            drawLine(Color.White.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(40f, 40f), androidx.compose.ui.geometry.Offset(40f, 40f + cropLen), strokeWidth = stroke)
                            drawLine(Color.White.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(size.width - 40f, 40f), androidx.compose.ui.geometry.Offset(size.width - 40f - cropLen, 40f), strokeWidth = stroke)
                            drawLine(Color.White.copy(alpha = 0.15f), androidx.compose.ui.geometry.Offset(size.width - 40f, 40f), androidx.compose.ui.geometry.Offset(size.width - 40f, 40f + cropLen), strokeWidth = stroke)
                        }
                        
                        Text(
                            text = "Camera permission is required for Record Video mode.",
                            color = Color.White.copy(alpha = 0.22f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 120.dp)
                        )
                    }
                }
            }

            // Darken background overlay with full-screen tap-trigger detection to show/hide controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgOpacity))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = !showControls
                    }
            ) {
                // Smoothly scalable core scrolling content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isTextMirrored) {
                                rotationY = 180f
                            }
                        }
                ) {
                    val context = LocalContext.current
                    val paragraphs = remember(script.content) {
                        script.content.split("\n")
                    }
                    val bookmarkedLines = remember(script.id) {
                        val prefs = context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.getStringSet("script_bookmarks_${script.id}", emptySet()) ?: emptySet()
                    }
                    val parsedTextColor = remember(textColor) { parseColorSafely(textColor) }
                    val lineSpacingFactor = remember(lineSpacing) {
                        when (lineSpacing) {
                            "tight" -> 1.15f
                            "relaxed" -> 1.8f
                            "double" -> 2.4f
                            else -> 1.45f
                        }
                    }

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier
                            .fillMaxHeight()
                            .then(if (isLandscape) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                            .align(columnAlignment)
                            .padding(horizontal = 24.dp)
                            .testTag("playback_text_column"),
                        contentPadding = PaddingValues(
                            top = (calculatedGuideOffsetY + if (isLandscape) 20f else 40f).dp,
                            bottom = if (isLandscape) 200.dp else 440.dp
                        )
                    ) {
                        itemsIndexed(paragraphs, key = { index, _ -> index }) { index, paragraphText ->
                            val alignVal = remember(paragraphText, textAlignment, script.textDirection) {
                                com.example.util.RtlHelper.getTextAlign(textAlignment, script.textDirection, paragraphText)
                            }
                            val isBookmarked = remember(paragraphText, bookmarkedLines) {
                                bookmarkedLines.contains(paragraphText.trim())
                            }

                            if (paragraphText.trim().isEmpty()) {
                                Spacer(modifier = Modifier.height((fontScale * 0.5f).dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = (fontScale * 0.15f).dp)
                                        .then(
                                            if (isBookmarked) {
                                                Modifier
                                                    .background(
                                                        color = Color(0xFFFFEA00).copy(alpha = 0.18f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        width = 1.2.dp,
                                                        color = Color(0xFFFFEA00).copy(alpha = 0.45f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) {
                                    val textStyleDirection = remember(paragraphText, script.textDirection) {
                                        com.example.util.RtlHelper.getTextDirection(script.textDirection, paragraphText)
                                    }
                                    Text(
                                        text = paragraphText,
                                        fontSize = fontScale.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBookmarked) Color(0xFFFFEA00) else parsedTextColor,
                                        lineHeight = (fontScale * lineSpacingFactor).sp,
                                        textAlign = alignVal,
                                        style = androidx.compose.ui.text.TextStyle(
                                            textDirection = textStyleDirection
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Reader Guide target line overlay positioned safely near the front camera lens level
                    Box(
                        modifier = Modifier
                            .then(if (isLandscape) Modifier.width(480.dp) else Modifier.fillMaxWidth())
                            .padding(top = calculatedGuideOffsetY.dp)
                            .height(80.dp)
                            .align(columnAlignment)
                            .border(2.dp, ElectricCyan.copy(alpha = 0.25f))
                            .background(ElectricCyan.copy(alpha = 0.03f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Current speaking lines indicator",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // TOP BAR: Quick knobs and Exit (Animated)
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(WindowInsets.statusBars.asPaddingValues())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (internalPracticeMode) {
                                    isPlaying = false
                                    isScriptComplete = true
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .testTag("playback_exit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Leave Playback",
                                tint = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cameraswitch Mode Toggle
                            IconButton(
                                onClick = {
                                    cameraAngle += 180f
                                    isFrontCamera = !isFrontCamera
                                    prefs.edit().putBoolean("last_camera_front", isFrontCamera).apply()
                                    if (isFrontCamera) {
                                        isFlashEnabled = false
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (!isFrontCamera) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .testTag("playback_camera_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = if (!isFrontCamera) ElectricCyan else Color.White,
                                    modifier = Modifier.graphicsLayer {
                                        rotationY = animatedCameraAngle
                                    }
                                )
                            }

                            // Flashlight / Torch Toggle (visible/functional for back camera)
                            if (!isFrontCamera) {
                                IconButton(
                                    onClick = { isFlashEnabled = !isFlashEnabled },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (isFlashEnabled) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                        .testTag("playback_flash_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Toggle Flash",
                                        tint = if (isFlashEnabled) ElectricCyan else Color.White
                                    )
                                }
                            }

                            // Camera Mirror Mode Toggle
                            IconButton(
                                onClick = {
                                    isCameraMirrored = !isCameraMirrored
                                    prefs.edit().putBoolean("script_camera_mirrored_${script.id}", isCameraMirrored).apply()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isCameraMirrored) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .testTag("playback_camera_mirror_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Portrait,
                                    contentDescription = "Mirror Camera Feed",
                                    tint = if (isCameraMirrored) ElectricCyan else Color.White
                                )
                            }

                            // Text Mirror Mode Toggle
                            IconButton(
                                onClick = {
                                    isTextMirrored = !isTextMirrored
                                    prefs.edit().putBoolean("script_text_mirrored_${script.id}", isTextMirrored).apply()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isTextMirrored) ElectricPurple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .testTag("playback_mirror_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flip,
                                    contentDescription = "Mirror Text",
                                    tint = if (isTextMirrored) ElectricPurple else Color.White
                                )
                            }



                            // Interactive Font scale widget
                            Row(
                                modifier = Modifier
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "A-",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { fontScale = (fontScale - 2f).coerceAtLeast(14f) }
                                )
                                Text(
                                    text = "${fontScale.toInt()}",
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A+",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { fontScale = (fontScale + 2f).coerceAtMost(60f) }
                                )
                            }
                        }
                    }
                }

                // BOTTOM HUD OVERLAYS: Progress trackers, dynamic range sliders, transport buttons (Animated)
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                ) {
                    if (isLandscape) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        ) {
                            // PROGRESS INDICATOR: Visual line across top of the bottom controls
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.value)
                                        .background(Brush.horizontalGradient(listOf(ElectricPurple, ElectricCyan)))
                                        .testTag("playback_progress_bar")
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Sliders Column (Speed & Lens offset)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Speed Slider Row (dimmed when Voice-sync behaves as speed controller)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.graphicsLayer {
                                            alpha = if (isVoiceSyncActive) 0.35f else 1f
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = "Scroll Speed",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Slider(
                                            value = speed,
                                            onValueChange = { speed = it },
                                            valueRange = 0.5f..15.0f,
                                            enabled = !isVoiceSyncActive,
                                            modifier = Modifier.weight(1f).height(24.dp).testTag("playback_speed_slider"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = ElectricCyan,
                                                activeTrackColor = ElectricCyan,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                        Text(
                                            text = "${String.format("%.1f", animatedSpeedDisplay)}x",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    // Lens Alignment Slider Row (Dimmed/disabled if Eye-Line Guide binds it dynamically)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.graphicsLayer {
                                            alpha = if (isEyelineGuideEnabled) 0.35f else 1f
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Straighten,
                                            contentDescription = "Lens Alignment Offset",
                                            tint = ElectricPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Slider(
                                            value = currentGuideOffsetY,
                                            onValueChange = {
                                                textGuideOffsetY = it
                                                prefs.edit().putFloat("script_text_offset_${script.id}", textGuideOffsetY).apply()
                                            },
                                            valueRange = 40f..maxOffset,
                                            enabled = !isEyelineGuideEnabled,
                                            modifier = Modifier.weight(1f).height(24.dp).testTag("playback_height_slider"),
                                            colors = SliderDefaults.colors(
                                                thumbColor = ElectricPurple,
                                                activeTrackColor = ElectricPurple,
                                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                            )
                                        )
                                        Text(
                                            text = "${currentGuideOffsetY.toInt()}dp",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(36.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    // Compact Voice Sync Row for Landscape
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val pulseScale by animateFloatAsState(
                                                targetValue = if (isVoiceSyncActive && isPlaying && isHearingSpoken) {
                                                    1f + (rmsLevel.coerceIn(0f, 15f) / 15f) * 0.3f
                                                } else 1.0f,
                                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
                                                label = "MicPulseLandscape animate"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .scale(pulseScale)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isVoiceSyncActive) {
                                                            if (isHearingSpoken) ElectricCyan.copy(alpha = 0.25f)
                                                            else ElectricPurple.copy(alpha = 0.15f)
                                                        } else Color.White.copy(alpha = 0.05f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isVoiceSyncActive) Icons.Default.Mic else Icons.Default.MicOff,
                                                    contentDescription = "Landscape Voice Sync",
                                                    tint = if (isVoiceSyncActive) {
                                                        if (isHearingSpoken) ElectricCyan else ElectricPurple
                                                    } else Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isVoiceSyncActive) "Voice Sync: Active" else "Voice Sync: Off",
                                                color = if (isVoiceSyncActive) ElectricCyan else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            if (isVoiceSyncActive) {
                                                IconButton(
                                                    onClick = { showVoiceSettingsDialog = true },
                                                    modifier = Modifier.size(28.dp).testTag("voice_sync_tune_button_landscape")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Tune Settings",
                                                        tint = ElectricCyan,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Switch(
                                            checked = isVoiceSyncActive,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    if (hasAudioPermission) {
                                                        isVoiceSyncActive = true
                                                    } else {
                                                        voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                    }
                                                } else {
                                                    isVoiceSyncActive = false
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ElectricCyan,
                                                checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                                uncheckedThumbColor = Color.LightGray,
                                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.scale(0.7f).testTag("voice_sync_toggle_landscape")
                                        )
                                    }

                                    // Compact Eye-Line Guide Row for Landscape
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Eye-Line Guide",
                                                tint = if (isEyelineGuideEnabled) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isEyelineGuideEnabled) "Eye-Line: Enabled" else "Eye-Line: Off",
                                                color = if (isEyelineGuideEnabled) ElectricCyan else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isEyelineGuideEnabled) {
                                                IconButton(
                                                    onClick = { resetLensPositionToDefault() },
                                                    modifier = Modifier.size(24.dp).testTag("reset_lens_position_button_landscape")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Reset Lens Spot",
                                                        tint = ElectricCyan,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = isEyelineGuideEnabled,
                                                onCheckedChange = { checked ->
                                                    isEyelineGuideEnabled = checked
                                                    prefs.edit().putBoolean("script_eyeline_enabled_${script.id}", checked).apply()
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = ElectricCyan,
                                                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                                    uncheckedThumbColor = Color.LightGray,
                                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                                ),
                                                modifier = Modifier.scale(0.7f).testTag("eyeline_guide_toggle_landscape")
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 1.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isHardwareControlActive) {
                                                            ElectricCyan.copy(alpha = 0.15f)
                                                        } else Color.White.copy(alpha = 0.05f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = "Landscape Hardware Key Info",
                                                    tint = if (isHardwareControlActive) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isHardwareControlActive) "Keys: Enabled" else "Keys: Disabled",
                                                color = if (isHardwareControlActive) ElectricCyan else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Switch(
                                            checked = isHardwareControlActive,
                                            onCheckedChange = { checked ->
                                                isHardwareControlActive = checked
                                                prefs.edit().putBoolean("hardware_buttons_enabled", checked).apply()
                                                HardwareButtonController.isEnabled = checked
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = ElectricCyan,
                                                checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                                uncheckedThumbColor = Color.LightGray,
                                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier.scale(0.7f).testTag("hardware_buttons_toggle_landscape")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Controls & Audio Indicator on Right
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(260.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    scrollState.scrollToItem(0, 0)
                                                    isScriptComplete = false
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                                .testTag("playback_skip_beginning")
                                        ) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Skip to Beginning", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }

                                        val playPauseBgModifier = if (isPlaying) {
                                            Modifier.background(Color.White.copy(alpha = 0.15f))
                                        } else {
                                            Modifier.background(Brush.linearGradient(colors = listOf(ElectricPurple, ElectricCyan)))
                                        }
                                        IconButton(
                                            onClick = { isPlaying = !isPlaying },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .then(playPauseBgModifier)
                                                .testTag("playback_toggle_scrolling")
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause Scroll" else "Play Scroll",
                                                tint = if (isPlaying) Color.White else CosmicBackground,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (!internalPracticeMode) {
                                            val recordBgModifier = if (isRecording) {
                                                Modifier.background(Color.Red.copy(alpha = 0.25f)).border(2.dp, Color.Red, CircleShape)
                                            } else {
                                                Modifier.background(Color.White.copy(alpha = 0.15f))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (!hasCameraPermission) {
                                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                    } else if (isRecording) {
                                                        isRecording = false
                                                    } else {
                                                        requestRecordingStart()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .then(recordBgModifier)
                                                    .testTag("playback_record_toggle")
                                            ) {
                                                Icon(
                                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                                    tint = if (isRecording) Color.Red else ElectricCyan,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    scrollState.scrollToItem(0, 1000000)
                                                    isScriptComplete = true
                                                }
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                                .testTag("playback_skip_end")
                                        ) {
                                            Icon(Icons.Default.SkipNext, contentDescription = "Skip to End", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${(progress.value * 100).toInt()}% Speaking Progress", fontSize = 10.sp, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                                        if (isRecording) {
                                            Text("Rec: ${formatDuration(recordingDurationSec)}", fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(bottom = 24.dp)
                        ) {
                            // PROGRESS INDICATOR: Visual line across top of the bottom controls
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress.value)
                                        .background(Brush.horizontalGradient(listOf(ElectricPurple, ElectricCyan)))
                                        .testTag("playback_progress_bar")
                                )
                            }

                            // Progress percentage layout details
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speaking Progress",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Advanced Settings",
                                        tint = if (showAdvancedPlaybackSettings) ElectricCyan else SlateTextSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { showAdvancedPlaybackSettings = !showAdvancedPlaybackSettings }
                                    )
                                    Text(
                                        text = "${(progress.value * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showAdvancedPlaybackSettings,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Voice Sync Adaptive Pacing Toggle
                                    Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Live reactive floating level pulse indicator
                                    val pulseScale by animateFloatAsState(
                                        targetValue = if (isVoiceSyncActive && isPlaying && isHearingSpoken) {
                                            1f + (rmsLevel.coerceIn(0f, 15f) / 15f) * 0.35f
                                        } else if (isVoiceSyncActive && isPlaying) {
                                            1.1f
                                        } else {
                                            1.0f
                                        },
                                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
                                        label = "MicReactiveScale animate"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .scale(pulseScale)
                                            .clip(CircleShape)
                                            .background(
                                                if (isVoiceSyncActive) {
                                                    if (isHearingSpoken) ElectricCyan.copy(alpha = 0.25f)
                                                    else ElectricPurple.copy(alpha = 0.15f)
                                                } else {
                                                    Color.White.copy(alpha = 0.05f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isVoiceSyncActive) Icons.Default.Mic else Icons.Default.MicOff,
                                            contentDescription = "Voice Sync Mic Indicator",
                                            tint = if (isVoiceSyncActive) {
                                                if (isHearingSpoken) ElectricCyan else ElectricPurple
                                            } else {
                                                Color.White.copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Voice Sync Scrolling",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isVoiceSyncActive) {
                                                if (isHearingSpoken) "Listening... (Pacing teleprompter)" else "Waiting for speech..."
                                            } else {
                                                "Adheres scrolling speed to your speaking pace"
                                            },
                                            color = if (isVoiceSyncActive && isHearingSpoken) ElectricCyan else SlateTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isVoiceSyncActive,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (hasAudioPermission) {
                                                isVoiceSyncActive = true
                                            } else {
                                                voicePermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                        } else {
                                            isVoiceSyncActive = false
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricCyan,
                                        checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.scale(0.85f).testTag("voice_sync_toggle")
                                )
                            }
                            
                            if (isVoiceSyncActive) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showVoiceSettingsDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("voice_sync_tune_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Tune Settings",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Fine-Tune & Calibrate Voice",
                                            color = ElectricCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            if (isVoiceSyncActive && isNoisyEnvironment) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElectricPurple.copy(alpha = 0.15f))
                                        .border(1.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            isVoiceSyncActive = false
                                            isNoisyEnvironment = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Noisy environment detected",
                                        tint = ElectricPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Very noisy environment detected 🔊",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Click to fallback to manual scrolling control, or calibrate sensitivity/noise inside Tuning.",
                                            color = SlateTextSecondary,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }

                            // EYE-LINE / CAMERA GUIDE CONTROLS PORTRAIT
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isEyelineGuideEnabled) {
                                                    ElectricCyan.copy(alpha = 0.15f)
                                                } else {
                                                    Color.White.copy(alpha = 0.05f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Eye-Line Guide",
                                            tint = if (isEyelineGuideEnabled) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Eye-Line / Camera Guide",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Align text with physical lens to keep perfect eye contact",
                                            color = SlateTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isEyelineGuideEnabled,
                                    onCheckedChange = { checked ->
                                        isEyelineGuideEnabled = checked
                                        prefs.edit().putBoolean("script_eyeline_enabled_${script.id}", checked).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricCyan,
                                        checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.scale(0.85f).testTag("eyeline_guide_toggle")
                                )
                            }

                            if (isEyelineGuideEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.03f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Camera Lens Calibration: Drag the glowing target indicator on the screen to match your phone's physical camera lens. The scrolling reading zone will snap perfectly to this vertical level.",
                                        color = SlateTextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Lens Spot: Y=${(lensYPercent * 100).toInt()}% X=${(lensXPercent * 100).toInt()}%",
                                            color = ElectricCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("eyeline_lens_position_text")
                                        )

                                        TextButton(
                                            onClick = { resetLensPositionToDefault() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("reset_lens_position_button")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset Lens Spot",
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text("Reset to Default", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isHardwareControlActive) {
                                                    ElectricCyan.copy(alpha = 0.15f)
                                                } else {
                                                    Color.White.copy(alpha = 0.05f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Hardware Button Controls",
                                            tint = if (isHardwareControlActive) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Hardware Key Control",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Control speeds and play via volume/earphone buttons",
                                            color = SlateTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isHardwareControlActive,
                                    onCheckedChange = { checked ->
                                        isHardwareControlActive = checked
                                        prefs.edit().putBoolean("hardware_buttons_enabled", checked).apply()
                                        HardwareButtonController.isEnabled = checked
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ElectricCyan,
                                        checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                        uncheckedThumbColor = Color.LightGray,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.scale(0.85f).testTag("hardware_buttons_toggle")
                                )
                            }

                            // Bluetooth Remote Connection & Customizable Key Mapping Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (com.example.util.RemoteClickerManager.isConnected) {
                                                        ElectricCyan.copy(alpha = 0.15f)
                                                    } else {
                                                        Color.White.copy(alpha = 0.05f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Bluetooth,
                                                contentDescription = "Bluetooth Remote Section",
                                                tint = if (com.example.util.RemoteClickerManager.isConnected) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "Bluetooth Remote",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (com.example.util.RemoteClickerManager.isConnected) {
                                                    "Connected: ${com.example.util.RemoteClickerManager.connectedDeviceName}"
                                                } else {
                                                    "No remote clicker detected"
                                                },
                                                color = if (com.example.util.RemoteClickerManager.isConnected) ElectricCyan else SlateTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (com.example.util.RemoteClickerManager.isConnected) {
                                                    Color(0xFF00E676).copy(alpha = 0.15f)
                                                } else {
                                                    Color.White.copy(alpha = 0.05f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (com.example.util.RemoteClickerManager.isConnected) "Remote Active" else "Disconnected",
                                            color = if (com.example.util.RemoteClickerManager.isConnected) Color(0xFF00E676) else Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Configure Control Mappings:",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                com.example.util.RemoteClickerManager.CUSTOMIZABLE_KEYS.forEach { (keyCode, keyLabel) ->
                                    val currentAction = com.example.util.RemoteClickerManager.getActionForKey(keyCode)
                                    var showMapMenu by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = keyLabel,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )

                                        Box {
                                            TextButton(
                                                onClick = {
                                                    val options = com.example.util.ClickerAction.values()
                                                    val currentIndex = options.indexOf(currentAction)
                                                    val nextAction = options[(currentIndex + 1) % options.size]
                                                    com.example.util.RemoteClickerManager.setActionForKey(keyCode, nextAction)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("map_button_${keyCode}")
                                            ) {
                                                Text(
                                                    text = currentAction.label,
                                                    color = ElectricCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Change Action",
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(14.dp).padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // WiFi Remote Control UI Card
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = if (isWifiRemoteActive) ElectricCyan else Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "WiFi Web Remote",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Control prompter from any second device",
                                                color = SlateTextSecondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isWifiRemoteActive,
                                        onCheckedChange = { checked ->
                                            isWifiRemoteActive = checked
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ElectricCyan,
                                            checkedTrackColor = ElectricCyan.copy(alpha = 0.4f),
                                            uncheckedThumbColor = Color.LightGray,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                        ),
                                        modifier = Modifier.scale(0.85f).testTag("wifi_remote_toggle")
                                    )
                                }

                                if (isWifiRemoteActive) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = CosmicBorder, thickness = 0.5.dp)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val activeIp = com.example.util.WifiRemoteServer.serverIpAddress ?: "127.0.0.1"
                                    val pairingToken = com.example.util.WifiRemoteServer.pairingToken
                                    val serverUrl = "http://$activeIp:${com.example.util.WifiRemoteServer.PORT}/?token=$pairingToken"

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Scan QR Code or visit URL on your second device:",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center
                                        )

                                        // QR CODE GENERATION
                                        val qrBitmap = remember(activeIp, pairingToken) {
                                            try {
                                                val writer = com.google.zxing.qrcode.QRCodeWriter()
                                                val bitMatrix = writer.encode(serverUrl, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200)
                                                val width = bitMatrix.width
                                                val height = bitMatrix.height
                                                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                                for (x in 0 until width) {
                                                    for (y in 0 until height) {
                                                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                                                    }
                                                }
                                                bitmap
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }

                                        if (qrBitmap != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(150.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "WiFi Remote QR Code",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.foundation.text.selection.SelectionContainer {
                                                Text(
                                                    text = serverUrl,
                                                    color = ElectricCyan,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("CueFlow Remote URL", serverUrl)
                                                    clipboard.setPrimaryClip(clip)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = "Copy",
                                                    color = ElectricCyan,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Both devices must be on the same trusted Wi-Fi network. This pairing link is temporary and expires when the remote stops or goes idle.",
                                            color = SlateTextSecondary,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                            // SPEED CONTROLLER SLIDER: Fine-grained scroll speed adjustments (dimmed when voice-sync matches speed)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 2.dp)
                                    .graphicsLayer {
                                        alpha = if (isVoiceSyncActive) 0.35f else 1f
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (!isVoiceSyncActive) speed = (speed - 0.2f).coerceAtLeast(0.5f) },
                                    enabled = !isVoiceSyncActive,
                                    modifier = Modifier.size(48.dp).testTag("speed_decrease_fine")
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrement Speed", tint = Color.White)
                                }

                                Slider(
                                    value = speed,
                                    onValueChange = { speed = it },
                                    valueRange = 0.5f..15.0f,
                                    enabled = !isVoiceSyncActive,
                                    modifier = Modifier.weight(1f).testTag("playback_speed_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                IconButton(
                                    onClick = { if (!isVoiceSyncActive) speed = (speed + 0.2f).coerceAtMost(15.0f) },
                                    enabled = !isVoiceSyncActive,
                                    modifier = Modifier.size(48.dp).testTag("speed_increase_fine")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increment Speed", tint = Color.White)
                                }

                                Text(
                                            text = "${String.format("%.1f", animatedSpeedDisplay)}x",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(42.dp),
                                            textAlign = TextAlign.End
                                        )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // TEXT HEIGHT / POSITION LENS ALIGNER
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        textGuideOffsetY = (textGuideOffsetY - 10f).coerceAtLeast(80f)
                                        prefs.edit().putFloat("script_text_offset_${script.id}", textGuideOffsetY).apply()
                                    },
                                    modifier = Modifier.size(48.dp).testTag("height_decrease_fine")
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Text Up", tint = Color.White)
                                }

                                Slider(
                                    value = currentGuideOffsetY,
                                    onValueChange = {
                                        textGuideOffsetY = it
                                        prefs.edit().putFloat("script_text_offset_${script.id}", textGuideOffsetY).apply()
                                    },
                                    valueRange = 80f..450f,
                                    modifier = Modifier.weight(1f).testTag("playback_height_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricPurple,
                                        activeTrackColor = ElectricPurple,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        textGuideOffsetY = (textGuideOffsetY + 10f).coerceAtMost(450f)
                                        prefs.edit().putFloat("script_text_offset_${script.id}", textGuideOffsetY).apply()
                                    },
                                    modifier = Modifier.size(48.dp).testTag("height_increase_fine")
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Text Down", tint = Color.White)
                                }

                                Text(
                                    text = "Lens Alignment",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(90.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // CORE TRANSPORT CONTROLS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Skip to Beginning
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            scrollState.scrollToItem(0, 0)
                                            isScriptComplete = false
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                        .testTag("playback_skip_beginning")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipPrevious,
                                        contentDescription = "Skip to Beginning",
                                        tint = Color.White
                                    )
                                }

                                // Main Play/Pause Button
                                val playPauseBgModifier = if (isPlaying) {
                                    Modifier.background(Color.White.copy(alpha = 0.15f))
                                } else {
                                    Modifier.background(
                                        Brush.linearGradient(
                                            colors = listOf(ElectricPurple, ElectricCyan)
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .then(playPauseBgModifier)
                                        .testTag("playback_toggle_scrolling")
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause Scroll" else "Play Scroll",
                                        tint = if (isPlaying) Color.White else CosmicBackground,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Record Button
                                if (!internalPracticeMode) {
                                    val recordBgModifier = if (isRecording) {
                                        Modifier.background(Color.Red.copy(alpha = 0.25f))
                                            .border(2.dp, Color.Red, CircleShape)
                                    } else {
                                        Modifier.background(Color.White.copy(alpha = 0.15f))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (!hasCameraPermission) {
                                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                            } else if (isRecording) {
                                                isRecording = false
                                            } else {
                                                requestRecordingStart()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .then(recordBgModifier)
                                            .testTag("playback_record_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                            tint = if (isRecording) Color.Red else ElectricCyan,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                // Skip to End
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            scrollState.scrollToItem(0, 1000000)
                                            isScriptComplete = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                        .testTag("playback_skip_end")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SkipNext,
                                        contentDescription = "Skip to End",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // SCRIPT COMPLETION OVERLAY / PRACTICE STATS
                if (isScriptComplete) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .widthIn(max = 440.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CosmicSurface.copy(alpha = 0.98f))
                            .border(1.5.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                            .testTag("script_complete_feedback"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (internalPracticeMode) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    text = "Practice Complete! 🎯",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Here's how you performed during this rehearsal session.",
                                    fontSize = 12.sp,
                                    color = SlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Grid of stats cards
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Row 1: Duration & WPM
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Duration card
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Duration", fontSize = 11.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = formatDuration(practiceDurationSec),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White,
                                                    modifier = Modifier.testTag("stat_duration")
                                                )
                                            }
                                        }

                                        // WPM Card
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Reading Pace", fontSize = 11.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$wpm WPM",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = ElectricCyan,
                                                    modifier = Modifier.testTag("stat_wpm")
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Pauses & Progress Words
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Pauses Card
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Pauses", fontSize = 11.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$pauseCount",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = ElectricPurple,
                                                    modifier = Modifier.testTag("stat_pauses")
                                                )
                                            }
                                        }

                                        // Words read vs total
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Words Read", fontSize = 11.sp, color = SlateTextMuted, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "$wordsRead / $wordCount",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Suggestion or comment text
                                val suggestion = when {
                                    wpm == 0 -> "Keep practicing to find your natural speech speed!"
                                    wpm < 110 -> "A bit slow. Standard public speaking pace is around 120-150 WPM."
                                    wpm in 110..155 -> "Magnificent! This is the sweet spot for maximum vocal clarity and listener comprehension."
                                    else -> "Excellent flow, but quite fast. Aim to pause occasionally to let key points sink in."
                                }
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                scrollState.scrollToItem(0, 0)
                                                practiceDurationSec = 0
                                                pauseCount = 0
                                                isScriptComplete = false
                                                isPlaying = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricCyan,
                                            contentColor = CosmicBackground
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("practice_restart_button")
                                    ) {
                                        Text("Rehearse Again", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = SlateTextSecondary
                                        ),
                                        border = BorderStroke(1.dp, CosmicBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .testTag("practice_close_button")
                                    ) {
                                        Text("Finish & Close", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Default script complete overlay
                                Text(
                                    text = "Prompting Complete 🎉",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap the restart button below to replay.",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            scrollState.scrollToItem(0, 0)
                                            isScriptComplete = false
                                            isPlaying = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("playback_restart_button")
                                ) {
                                    Text("Restart Scrolling", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showPermissionExplanation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE607070B))
                        .padding(24.dp)
                        .testTag("permissions_explanation_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(CosmicSurface)
                            .border(1.dp, CosmicBorder, RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Allow Camera Preview",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "CueFlow uses the camera only for the live preview and Record Video mode. Microphone access is requested separately when audio recording or voice sync needs it.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                Text("Maintain direct natural eye-contact with the lens", fontSize = 11.sp, color = SlateTextPrimary, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CosmicSurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Text("Camera preview stays on-device. Video is saved only during Record Video mode.", fontSize = 11.sp, color = SlateTextPrimary, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { 
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("enable_camera_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = CosmicBackground
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Allow Camera Access", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { 
                                    showPermissionExplanation = false
                                    internalPracticeMode = true
                                    isCountingDown = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                    .testTag("continue_without_camera_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = SlateTextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Continue Without Camera", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (isCountingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .testTag("countdown_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "GET READY",
                            color = ElectricCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(ElectricPurple.copy(alpha = 0.3f), Color.Transparent)))
                                .border(2.dp, Brush.linearGradient(listOf(ElectricPurple, ElectricCyan)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = countdownTime,
                                transitionSpec = {
                                    (scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(animationSpec = tween(150)))
                                        .togetherWith(scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)))
                                },
                                label = "countdown_animation"
                            ) { targetTime ->
                                Text(
                                    text = if (targetTime > 0) targetTime.toString() else "GO!",
                                    color = Color.White,
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic Recording Status (REC • 00:03) - ALWAYS visible when recording
            if (isRecording) {
                val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                val redDotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "red_dot"
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { isRecording = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("recording_indicator"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .graphicsLayer { alpha = redDotAlpha }
                            .background(Color.Red)
                    )
                    Text(
                        text = "REC ${formatDuration(recordingDurationSec)} (Tap to Stop)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (voiceSyncErrorMessage != null) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { voiceSyncErrorMessage = null }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface,
                            contentColor = SlateTextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MicOff,
                                    contentDescription = "Voice Sync Issue",
                                    tint = ElectricPurple,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Voice Sync Interrupt",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = voiceSyncErrorMessage ?: "",
                                fontSize = 13.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Button(
                                onClick = { voiceSyncErrorMessage = null },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Text("Switch to Manual Scrolling", color = CosmicBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (cameraErrorMessage != null) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { cameraErrorMessage = null }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface,
                            contentColor = SlateTextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Camera Warning",
                                    tint = Color.Red,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "Camera Unavailable",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                            Text(
                                text = cameraErrorMessage ?: "",
                                fontSize = 13.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        cameraErrorMessage = null
                                        cameraRetryCount++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retry Connection", color = CosmicBackground, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        cameraErrorMessage = null
                                        internalPracticeMode = true
                                    },
                                    border = BorderStroke(1.dp, CosmicBorder),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = SlateTextPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Practice Mode", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }


            // Success feedback dialog card is shown only after a successful CameraX Finalize event
            if (showRecordingSavedDialog) {
                Dialog(
                    onDismissRequest = { showRecordingSavedDialog = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CosmicSurface,
                            contentColor = SlateTextPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(2.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .testTag("recording_saved_dialog")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Recording Saved! 🎬",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "CameraX finished saving this recording to Movies/CueFlow. You can open it from your gallery or play it now.",
                                fontSize = 13.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showRecordingSavedDialog = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = SlateTextSecondary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.0f)
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(10.dp))
                                ) {
                                    Text("Dismiss", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        showRecordingSavedDialog = false
                                        recordedVideoUri?.let { uri ->
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "video/*")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Could not open video player", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricCyan,
                                        contentColor = CosmicBackground
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("view_recorded_video_button")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Play Video", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showVoiceSettingsDialog) {
                VoiceSyncSettingsDialog(
                    onDismiss = { showVoiceSettingsDialog = false },
                    sensitivity = voiceSensitivity,
                    onSensitivityChange = { newVal ->
                        voiceSensitivity = newVal
                        prefs.edit().putInt("voice_sync_sensitivity_${script.id}", newVal).apply()
                    },
                    pauseThreshold = voicePauseThreshold,
                    onPauseThresholdChange = { newVal ->
                        voicePauseThreshold = newVal
                        prefs.edit().putInt("voice_sync_pause_threshold_${script.id}", newVal).apply()
                    },
                    minCrawl = voiceMinSpeedCrawl,
                    onMinCrawlChange = { newVal ->
                        voiceMinSpeedCrawl = newVal
                        prefs.edit().putInt("voice_sync_min_crawl_${script.id}", newVal).apply()
                    },
                    maxLimit = voiceMaxSpeedLimit,
                    onMaxLimitChange = { newVal ->
                        voiceMaxSpeedLimit = newVal
                        prefs.edit().putInt("voice_sync_max_limit_${script.id}", newVal).apply()
                    },
                    context = context
                )
            }

            // Elegant, subtle, always-on timing indicators & visual progress bar at the bottom center of screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("teleprompter_smart_timer_bar"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Elapsed: ${formatElapsedTime(elapsedSeconds)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("teleprompter_elapsed_time")
                    )
                    Text(
                        text = "Remaining: ${formatRemainingTime(remainingReadingSeconds.value)}",
                        color = ElectricCyan.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("teleprompter_remaining_time")
                    )
                }
                
                // Thin Visual Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.value)
                            .background(Brush.horizontalGradient(listOf(ElectricPurple, ElectricCyan)))
                            .testTag("teleprompter_progress_bar")
                    )
                }
            }

            if (isEyelineGuideEnabled) {
                val posX = lensXPercent * screenWidthDp.value
                val posY = lensYPercent * screenHeightDp.value

                Box(
                    modifier = Modifier
                        .offset(x = posX.dp - 24.dp, y = posY.dp - 24.dp)
                        .size(48.dp)
                        .testTag("teleprompter_camera_lens_guide")
                        .then(
                            if (showControls) {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newX = (lensXPercent + dragAmount.x / screenWidthPx).coerceIn(0.01f, 0.99f)
                                        val newY = (lensYPercent + dragAmount.y / screenHeightPx).coerceIn(0.01f, 0.95f)
                                        lensXPercent = newX
                                        lensYPercent = newY
                                        prefs.edit()
                                            .putFloat("script_lens_x_percent_${script.id}", newX)
                                            .putFloat("script_lens_y_percent_${script.id}", newY)
                                            .putBoolean("script_lens_dragged_${script.id}", true)
                                            .apply()
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (showControls) ElectricCyan else ElectricCyan.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (showControls) ElectricCyan.copy(alpha = 0.25f) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = if (showControls) ElectricCyan else ElectricCyan.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "Look Here indicator",
                            tint = if (showControls) ElectricCyan else ElectricCyan.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(16.dp)
                                .offset(y = (-2).dp)
                        )
                    }

                    if (showControls) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    isFrontCamera: Boolean,
    isFlashEnabled: Boolean,
    isCameraMirrored: Boolean,
    isRecording: Boolean,
    onRecordingStarted: () -> Unit,
    onRecordingStopped: (android.net.Uri?) -> Unit,
    onCameraError: (String) -> Unit = {},
    retryTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    val recordingQuality = remember(context) {
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
    }

    val currentRecording = remember { mutableStateOf<androidx.camera.video.Recording?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                currentRecording.value?.stop()
                currentRecording.value = null
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Error on dispose: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, "PromptRecord_${System.currentTimeMillis()}")
                    put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(android.provider.MediaStore.Video.Media.RELATIVE_PATH, "Movies/CueFlow")
                }
                
                val mediaStoreOutputOptions = androidx.camera.video.MediaStoreOutputOptions
                    .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    .setContentValues(contentValues)
                    .build()
                
                val pendingRecording = videoCapture.output
                    .prepareRecording(context, mediaStoreOutputOptions)
                
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    pendingRecording.withAudioEnabled()
                }
                
                val activeRec = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                    when (event) {
                        is androidx.camera.video.VideoRecordEvent.Start -> {
                            onRecordingStarted()
                        }
                        is androidx.camera.video.VideoRecordEvent.Finalize -> {
                            currentRecording.value = null
                            if (!event.hasError()) {
                                onRecordingStopped(event.outputResults.outputUri)
                            } else {
                                android.util.Log.e("CameraPreview", "Recording finalized error: ${event.error}")
                                onCameraError("Recording could not be saved (CameraX error ${event.error}). Check available storage and try again.")
                                onRecordingStopped(null)
                            }
                        }
                    }
                }
                currentRecording.value = activeRec
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Error starting recording: ${e.localizedMessage}")
                onCameraError("Recording could not start. ${e.localizedMessage ?: "Please retry."}")
                onRecordingStopped(null)
            }
        } else {
            currentRecording.value?.stop()
            currentRecording.value = null
        }
    }

    var activeCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }

    // Rebind only when camera selection, use case, or retry trigger changes
    LaunchedEffect(isFrontCamera, previewViewRef.value, videoCapture, retryTrigger) {
        val view = previewViewRef.value ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                activeCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Camera bind error: ${e.localizedMessage}")
                onCameraError("The camera might be used by another app. Please close other camera apps and retry, or switch to Practice Mode.")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Toggle torch dynamically without flickers
    LaunchedEffect(isFlashEnabled, activeCamera, isFrontCamera) {
        if (!isFrontCamera) {
            try {
                activeCamera?.cameraControl?.enableTorch(isFlashEnabled)
            } catch (e: Exception) {
                android.util.Log.e("CameraPreview", "Torch toggle error: ${e.localizedMessage}")
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewViewRef.value = this
            }
        },
        modifier = modifier.graphicsLayer {
            if (isCameraMirrored) {
                scaleX = -1f
            } else {
                scaleX = 1f
            }
        }
    )
}

fun parseColorSafely(hexStr: String): Color {
    return try {
        val clean = hexStr.trim().replace("#", "")
        if (clean.length == 6) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else if (clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            Color.White
        }
    } catch (e: Exception) {
        Color.White
    }
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun PromptModeSelectionDialog(
    script: Script,
    onDismiss: () -> Unit,
    onModeSelected: (isPracticeMode: Boolean) -> Unit,
    onFloatingSelected: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsCustomizer by remember { mutableStateOf(false) }
    var configs by remember { mutableStateOf(com.example.data.FloatingSettings.getConfigs(context)) }

    val dialogWordCount = remember(script.content) {
        script.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val dialogCharCount = remember(script.content) {
        script.content.length
    }
    val dialogSpeedValue = script.scrollSpeed.toFloat().coerceAtLeast(0.5f)
    val dialogSpeakingSpeedWpm = dialogSpeedValue * 30f
    val dialogTotalSeconds = if (dialogWordCount > 0) ((dialogWordCount.toFloat() / dialogSpeakingSpeedWpm) * 60f).toInt() else 0
    val dialogMins = dialogTotalSeconds / 60
    val dialogSecs = dialogTotalSeconds % 60
    val dialogSpeechDurationText = when {
        dialogMins > 0 && dialogSecs > 0 -> "$dialogMins min $dialogSecs sec"
        dialogMins > 0 -> "$dialogMins min"
        else -> "$dialogSecs sec"
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(24.dp))
                .testTag("mode_selection_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Prompting Options 🚀",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Choose how you want to read \"${script.title}\".",
                    fontSize = 13.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center
                )

                // Smart Script Duration Estimator summary card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicSurfaceElevated)
                        .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Words", fontSize = 10.sp, color = SlateTextMuted)
                            Text("$dialogWordCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary, modifier = Modifier.testTag("dialog_word_counter"))
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(CosmicBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Characters", fontSize = 10.sp, color = SlateTextMuted)
                            Text("$dialogCharCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary, modifier = Modifier.testTag("dialog_char_counter"))
                        }
                        Box(modifier = Modifier.width(1.dp).height(16.dp).background(CosmicBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Scroll Speed", fontSize = 10.sp, color = SlateTextMuted)
                            Text("${String.format("%.1f", dialogSpeedValue)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Estimated duration",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "This script will take approximately $dialogSpeechDurationText to read",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("dialog_duration_estimate")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Custom Mode Cards
                Card(
                    onClick = { onModeSelected(true) },
                    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .testTag("select_practice_mode_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Practice Mode",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Rehearse without camera. Track your speed, timing and pause statistics.",
                                fontSize = 11.sp,
                                color = SlateTextMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Card(
                    onClick = { onModeSelected(false) },
                    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
                        .testTag("select_record_mode_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Record Video",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Camera overlay active. Record and save high-quality clean speaker footage.",
                                fontSize = 11.sp,
                                color = SlateTextMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Card(
                    onClick = onFloatingSelected,
                    colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.5.dp, WarmAmber.copy(alpha = 0.35f)), RoundedCornerShape(16.dp))
                        .testTag("select_floating_mode_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WarmAmber.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = WarmAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Floating Overlay",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Stay on top of Instagram, TikTok, Zoom, or any camera app! Read script scroll smoothly in glass overlay.",
                                fontSize = 11.sp,
                                color = SlateTextMuted,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Expandable Customization Settings Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSettingsCustomizer = !showSettingsCustomizer }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Overlay Settings & Customization",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = if (showSettingsCustomizer) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Settings Panel",
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showSettingsCustomizer,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Text Size slider
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Text Size", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text("${configs.textSize.toInt()} sp", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.padding(top = 4.dp)) {
                                    Slider(
                                        value = configs.textSize,
                                        onValueChange = {
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
                                        modifier = Modifier.height(24.dp).testTag("dialog_floating_text_size_slider")
                                    )
                                }
                            }

                            // Background Opacity slider
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Backdrop Opacity", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text("${(configs.bgOpacity * 100).toInt()}%", fontSize = 11.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.padding(top = 4.dp)) {
                                    Slider(
                                        value = configs.bgOpacity,
                                        onValueChange = {
                                            configs = configs.copy(bgOpacity = it)
                                            com.example.data.FloatingSettings.saveConfigs(context, configs)
                                        },
                                        valueRange = 0.15f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ElectricPurple,
                                            activeTrackColor = ElectricPurple,
                                            inactiveTrackColor = CosmicBorder
                                        ),
                                        modifier = Modifier.height(24.dp).testTag("dialog_floating_bg_opacity_slider")
                                    )
                                }
                            }

                            // Overall Window Opacity slider
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Window Opacity", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                    Text("${(configs.windowOpacity * 100).toInt()}%", fontSize = 11.sp, color = WarmAmber, fontWeight = FontWeight.Bold)
                                }
                                Box(modifier = Modifier.padding(top = 4.dp)) {
                                    Slider(
                                        value = configs.windowOpacity,
                                        onValueChange = {
                                            configs = configs.copy(windowOpacity = it)
                                            com.example.data.FloatingSettings.saveConfigs(context, configs)
                                        },
                                        valueRange = 0.3f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = WarmAmber,
                                            activeTrackColor = WarmAmber,
                                            inactiveTrackColor = CosmicBorder
                                        ),
                                        modifier = Modifier.height(24.dp).testTag("dialog_floating_window_opacity_slider")
                                    )
                                }
                            }

                            // Text Color Palette Selection Grid
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Speech Text Color", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    com.example.data.FloatingSettings.TEXT_COLORS.forEach { colorName ->
                                        val actualColor = com.example.data.FloatingSettings.mapTextColor(colorName)
                                        val isSelected = configs.textColorName == colorName
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(actualColor)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) Color.White else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    configs = configs.copy(textColorName = colorName)
                                                    com.example.data.FloatingSettings.saveConfigs(context, configs)
                                                }
                                                .testTag("dialog_text_color_$colorName"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (actualColor == Color.White) Color.Black else Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Background Color Palette Selection Grid
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Backdrop Color Theme", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    com.example.data.FloatingSettings.BG_COLORS.forEach { bgName ->
                                        val actualColor = com.example.data.FloatingSettings.mapBgColor(bgName)
                                        val isSelected = configs.bgColorName == bgName
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(actualColor)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) ElectricCyan else CosmicBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    configs = configs.copy(bgColorName = bgName)
                                                    com.example.data.FloatingSettings.saveConfigs(context, configs)
                                                }
                                                .testTag("dialog_bg_color_$bgName"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = ElectricCyan,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Default Position selector (Top / Center / Bottom)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Default Screen Alignment", fontSize = 11.sp, color = SlateTextSecondary, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    com.example.data.FloatingSettings.POSITION_GRAVITIES.forEach { position ->
                                        val isSelected = configs.defaultGravity == position
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) ElectricCyan else CosmicBorder,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    configs = configs.copy(defaultGravity = position)
                                                    com.example.data.FloatingSettings.saveConfigs(context, configs)
                                                }
                                                .testTag("dialog_position_$position"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = position,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) ElectricCyan else SlateTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = SlateTextSecondary
                    ),
                    border = BorderStroke(1.dp, CosmicBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("cancel_mode_selection_button")
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OverlayPermissionExplanationDialog(
    onDismiss: () -> Unit,
    onForceGrant: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(BorderStroke(1.dp, CosmicBorder), RoundedCornerShape(24.dp))
                .testTag("overlay_permission_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Seamless Floating Overlay 🚀",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "To stay visible on top of Instagram, TikTok, Zoom, or any other camera and video call app, CueFlow requires the 'Draw over other apps' authorization.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Text(
                    text = "This allows you to read your script smoothly without breaking natural eye-contact with your audience!",
                    fontSize = 11.sp,
                    color = SlateTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = SlateTextSecondary
                        ),
                        border = BorderStroke(1.dp, CosmicBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dismiss_permission_dialog_button")
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onForceGrant,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("grant_permission_settings_button")
                    ) {
                        Text("Authorize Now", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceSyncSettingsDialog(
    onDismiss: () -> Unit,
    sensitivity: Int,
    onSensitivityChange: (Int) -> Unit,
    pauseThreshold: Int,
    onPauseThresholdChange: (Int) -> Unit,
    minCrawl: Int,
    onMinCrawlChange: (Int) -> Unit,
    maxLimit: Int,
    onMaxLimitChange: (Int) -> Unit,
    context: android.content.Context
) {
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0f) }
    var calibrationStatusText by remember { mutableStateOf("Ready to begin. Tap calibrate to start.") }
    var spokenList by remember { mutableStateOf<List<String>>(emptyList()) }
    var calibrationRmsLevel by remember { mutableStateOf(0f) }
    
    val testSentence = "Welcome to CueFlow. This smart scrolling system follows my speech pace perfectly, adjusting as I talk."
    val testWords = remember {
        testSentence.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    val coroutineScope = rememberCoroutineScope()

    // Local calibration speech recognizer
    DisposableEffect(isCalibrating) {
        var localRecognizer: android.speech.SpeechRecognizer? = null
        if (isCalibrating) {
            val listener = object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    calibrationStatusText = "Listening... Speak the sentence below clearly."
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    calibrationRmsLevel = rmsdB
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    calibrationRmsLevel = 0f
                }
                override fun onError(error: Int) {
                    calibrationRmsLevel = 0f
                    if (isCalibrating) {
                        coroutineScope.launch {
                            delay(400)
                            if (isCalibrating) {
                                try {
                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                                    }
                                    localRecognizer?.startListening(intent)
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
                override fun onResults(results: android.os.Bundle?) {
                    calibrationRmsLevel = 0f
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spoken = matches[0] ?: ""
                        val list = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                        spokenList = list
                        
                        val matchedCount = testWords.count { list.contains(it) }
                        val pct = matchedCount.toFloat() / testWords.size.toFloat()
                        calibrationProgress = pct
                        
                        if (matchedCount >= 5) {
                            isCalibrating = false
                            calibrationStatusText = "Calibration Successful! Style settings calculated."
                            
                            if (list.size > testWords.size + 3) {
                                onSensitivityChange(0) // More Stable
                                onPauseThresholdChange(2) // Long Pauses
                            } else {
                                onSensitivityChange(1) // Balanced
                                onPauseThresholdChange(1) // Normal Pauses
                            }
                        }
                    }
                    if (isCalibrating) {
                        try {
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                            }
                            localRecognizer?.startListening(intent)
                        } catch(e: Exception){}
                    }
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spoken = matches[0] ?: ""
                        val list = spoken.lowercase()
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .split("\\s+".toRegex())
                            .filter { it.isNotBlank() }
                        spokenList = list
                        
                        val matchedCount = testWords.count { list.contains(it) }
                        val pct = matchedCount.toFloat() / testWords.size.toFloat()
                        calibrationProgress = pct
                        
                        if (pct >= 0.5f) {
                            isCalibrating = false
                            calibrationStatusText = "Calibration Successful! Style settings calculated."
                            if (list.size > testWords.size + 3) {
                                onSensitivityChange(0) // More Stable
                                onPauseThresholdChange(2) // Long Pauses
                            } else {
                                onSensitivityChange(1) // Balanced
                                onPauseThresholdChange(1) // Normal Pauses
                            }
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            }
            
            try {
                if (android.speech.SpeechRecognizer.isRecognitionAvailable(context)) {
                    localRecognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(listener)
                    }
                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    }
                    localRecognizer.startListening(intent)
                }
            } catch (e: Exception) {
                calibrationStatusText = "Speech recognizer is busy or unavailable."
            }
        }
        onDispose {
            try {
                localRecognizer?.stopListening()
                localRecognizer?.destroy()
            } catch (e: Exception) {}
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicBackground)
                .border(1.2.dp, CosmicBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Calibrate & Tune Voice Sync",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Fine-tune tracking to your accent and cadence",
                            fontSize = 11.sp,
                            color = SlateTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(CosmicBorder))

                // Calibration Area Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pulse by animateFloatAsState(
                            targetValue = if (isCalibrating) 1f + (calibrationRmsLevel.coerceIn(0f, 15f) / 15f) * 0.4f else 1f,
                            animationSpec = spring(dampingRatio = 0.5f),
                            label = "CalibrationMic animate"
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .scale(pulse)
                                .clip(CircleShape)
                                .background(if (isCalibrating) ElectricCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCalibrating) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Calibration Mic",
                                tint = if (isCalibrating) ElectricCyan else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text("Voice Style Calibration", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(calibrationStatusText, color = if (isCalibrating) ElectricCyan else SlateTextSecondary, fontSize = 10.sp)
                        }
                    }

                    // Test Sentence Display with highlight matches
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(12.dp)
                    ) {
                        val annotatedText = buildAnnotatedString {
                            val originalTokens = testSentence.split(" ")
                            originalTokens.forEachIndexed { idx, token ->
                                val clean = token.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
                                val isMatched = spokenList.contains(clean)
                                withStyle(
                                    style = androidx.compose.ui.text.SpanStyle(
                                        color = if (isMatched) ElectricCyan else Color.White.copy(alpha = 0.45f),
                                        fontWeight = if (isMatched) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                ) {
                                    append(token)
                                }
                                if (idx < originalTokens.lastIndex) append(" ")
                            }
                        }
                        Text(text = annotatedText, lineHeight = 18.sp)
                    }

                    // Calibration Control Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                spokenList = emptyList()
                                calibrationProgress = 0f
                                isCalibrating = !isCalibrating
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCalibrating) Color.Red.copy(alpha = 0.6f) else ElectricCyan,
                                contentColor = if (isCalibrating) Color.White else CosmicBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).height(48.dp)
                        ) {
                            Text(
                                text = if (isCalibrating) "Stop Calibration" else "Start Calibration",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (calibrationProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${(calibrationProgress * 100).toInt()}% Match",
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Sensitivity Setting UI
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Voice Sensitivity", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("More Stable", "Balanced", "More Responsive").forEachIndexed { index, label ->
                            val isSelected = sensitivity == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSensitivityChange(index) }
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        text = when (sensitivity) {
                            0 -> "Stable: Best for noisy spaces or echoing accents. Less jumping."
                            2 -> "Responsive: Immediate tracking of rapid speech."
                            else -> "Balanced: standard matching for general environments."
                        },
                        color = SlateTextSecondary,
                        fontSize = 9.sp
                    )
                }

                // Pause Threshold Setting UI
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pause Detection Threshold", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Short Pauses", "Normal Pauses", "Long Pauses").forEachIndexed { index, label ->
                            val isSelected = pauseThreshold == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPauseThresholdChange(index) }
                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        text = when (pauseThreshold) {
                            0 -> "Short: Pauses scrolling quickly when you take a small breath."
                            2 -> "Long: Keeps scrolling active during long natural pauses or thinking periods."
                            else -> "Normal: Balanced pause tracking aligned to conversational beats."
                        },
                        color = SlateTextSecondary,
                        fontSize = 9.sp
                    )
                }

                // Min auto-crawl Speed Limit UI
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Minimum Speed (Auto-Crawl Fallback)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Off (Strict Voice)", "Slow Crawl", "Medium Crawl").forEachIndexed { index, label ->
                            val isSelected = minCrawl == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMinCrawlChange(index) }
                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) ElectricPurple else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        text = when (minCrawl) {
                            0 -> "Strict: Stops entirely when speech stops."
                            1 -> "Slow: Automatically rolls forward slowly after 5 seconds of silence."
                            else -> "Medium: Automatically rolls forward after 2.5 seconds of silence."
                        },
                        color = SlateTextSecondary,
                        fontSize = 9.sp
                    )
                }

                // Max Speed Jump Limit UI
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Maximum Speed (Paragraph Jumps)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Unlimited", "Paced Cap", "Strict Cap").forEachIndexed { index, label ->
                            val isSelected = maxLimit == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMaxLimitChange(index) }
                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) ElectricPurple else Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Text(
                        text = when (maxLimit) {
                            0 -> "No Cap: Instantly jumps as far ahead as speech is matching."
                            1 -> "Paced: Restricts forward movement to max 2 paragraphs per update."
                            else -> "Strict: Restricts forward movement to exactly 1 paragraph max. Highly linear."
                        },
                        color = SlateTextSecondary,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Save & Apply Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

