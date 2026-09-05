package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FloatingConfigs
import com.example.data.FloatingSettings
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.ThemeState
import com.example.util.HardwareButtonController
import com.example.util.LanguageManager
import com.example.util.RemoteClickerManager

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE) }

    var defaultSpeed by remember { mutableFloatStateOf(prefs.getFloat("default_speed", 5f)) }
    var defaultFontSize by remember { mutableFloatStateOf(prefs.getFloat("default_font_size", 24f)) }
    var defaultTextColor by remember { mutableStateOf(prefs.getString("default_text_color", "#FFFFFF") ?: "#FFFFFF") }
    var defaultBgOpacity by remember { mutableFloatStateOf(prefs.getFloat("default_bg_opacity", 0.4f)) }
    var defaultCountdown by remember { mutableIntStateOf(prefs.getInt("default_countdown_duration", 3)) }
    var defaultAlignment by remember { mutableStateOf(prefs.getString("default_text_alignment", "left") ?: "left") }

    var floatingConfigs by remember { mutableStateOf(FloatingSettings.getConfigs(context)) }

    var voiceSensitivity by remember { mutableIntStateOf(prefs.getInt("voice_sync_sensitivity", 1)) }
    var voicePauseThreshold by remember { mutableIntStateOf(prefs.getInt("voice_sync_pause_threshold", 1)) }
    var hardwareButtonsEnabled by remember { mutableStateOf(prefs.getBoolean("hardware_buttons_enabled", false)) }

    var videoQuality by remember { mutableStateOf(prefs.getString("recording_video_quality", "1080p") ?: "1080p") }
    var defaultCamera by remember { mutableStateOf(prefs.getString("recording_default_camera", "front") ?: "front") }

    var showPrivacy by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val appVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "1.0.0" }
    }

    fun saveFloating(updated: FloatingConfigs) {
        floatingConfigs = updated
        FloatingSettings.saveConfigs(context, updated)
    }

    fun resetSettings() {
        prefs.edit()
            .remove("default_speed")
            .remove("default_font_size")
            .remove("default_text_color")
            .remove("default_bg_opacity")
            .remove("default_countdown_duration")
            .remove("default_text_alignment")
            .remove("floating_text_size_v3")
            .remove("floating_text_color_v3")
            .remove("floating_bg_color_v3")
            .remove("floating_bg_opacity_v3")
            .remove("floating_window_opacity_v3")
            .remove("floating_default_gravity_v3")
            .remove("voice_sync_sensitivity")
            .remove("voice_sync_pause_threshold")
            .remove("hardware_buttons_enabled")
            .remove("recording_video_quality")
            .remove("recording_default_camera")
            .apply()

        ThemeState.saveTheme(context, "dark")
        ThemeState.saveAccentColor(context, null)

        defaultSpeed = 5f
        defaultFontSize = 24f
        defaultTextColor = "#FFFFFF"
        defaultBgOpacity = 0.4f
        defaultCountdown = 3
        defaultAlignment = "left"
        floatingConfigs = FloatingSettings.getConfigs(context)
        voiceSensitivity = 1
        voicePauseThreshold = 1
        hardwareButtonsEnabled = false
        HardwareButtonController.isEnabled = false
        videoQuality = "1080p"
        defaultCamera = "front"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.92f)
                    .border(1.dp, CosmicBorder, RoundedCornerShape(22.dp))
                    .testTag("settings_dialog"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Close settings", tint = SlateTextPrimary)
                            }
                            Column {
                                Text("CueFlow settings", color = SlateTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Only settings used by the app are shown", color = SlateTextMuted, fontSize = 11.sp)
                            }
                        }
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(22.dp))
                    }

                    HorizontalDivider(color = CosmicBorder)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SettingsSection(title = "Appearance", icon = Icons.Default.ColorLens) {
                            Text("Theme", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf(
                                    "dark" to "Dark",
                                    "amoled" to "AMOLED",
                                    "light" to "Light",
                                    "midnight" to "Midnight",
                                    "sunset" to "Sunset",
                                ),
                                selected = ThemeState.currentTheme,
                                onSelected = { ThemeState.saveTheme(context, it) },
                            )

                            Text("Accent", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            AccentPicker(
                                selected = ThemeState.customAccentColor,
                                onSelected = { ThemeState.saveAccentColor(context, it) },
                            )
                        }

                        SettingsSection(title = "Language", icon = Icons.Default.Language) {
                            Text(
                                "Changes CueFlow's built-in interface translations. Script text is never translated automatically.",
                                color = SlateTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                            ChoiceRow(
                                options = LanguageManager.supportedLanguages.map { it.code to it.localName },
                                selected = LanguageManager.currentLanguage,
                                onSelected = { LanguageManager.saveLanguage(context, it) },
                            )
                        }

                        SettingsSection(title = "New script defaults", icon = Icons.Default.TextFields) {
                            LabeledSlider(
                                label = "Scroll speed",
                                valueText = String.format("%.1fx", defaultSpeed),
                                value = defaultSpeed,
                                range = 1f..15f,
                                onValueChange = {
                                    defaultSpeed = it
                                    prefs.edit().putFloat("default_speed", it).apply()
                                },
                            )
                            LabeledSlider(
                                label = "Text size",
                                valueText = "${defaultFontSize.toInt()} sp",
                                value = defaultFontSize,
                                range = 14f..48f,
                                onValueChange = {
                                    defaultFontSize = it
                                    prefs.edit().putFloat("default_font_size", it).apply()
                                },
                            )
                            LabeledSlider(
                                label = "Backdrop opacity",
                                valueText = "${(defaultBgOpacity * 100).toInt()}%",
                                value = defaultBgOpacity,
                                range = 0f..1f,
                                onValueChange = {
                                    defaultBgOpacity = it
                                    prefs.edit().putFloat("default_bg_opacity", it).apply()
                                },
                            )

                            Text("Text color", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf(
                                    "#FFFFFF" to "White",
                                    "#00E5FF" to "Cyan",
                                    "#9D4EDD" to "Purple",
                                    "#FFB703" to "Amber",
                                    "#00E676" to "Green",
                                ),
                                selected = defaultTextColor,
                                onSelected = {
                                    defaultTextColor = it
                                    prefs.edit().putString("default_text_color", it).apply()
                                },
                            )

                            Text("Countdown", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf(0 to "None", 3 to "3 sec", 5 to "5 sec", 10 to "10 sec"),
                                selected = defaultCountdown,
                                onSelected = {
                                    defaultCountdown = it
                                    prefs.edit().putInt("default_countdown_duration", it).apply()
                                },
                            )

                            Text("Alignment", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf("left" to "Left", "center" to "Center", "right" to "Right"),
                                selected = defaultAlignment,
                                onSelected = {
                                    defaultAlignment = it
                                    prefs.edit().putString("default_text_alignment", it).apply()
                                },
                            )
                        }

                        SettingsSection(title = "Floating overlay defaults", icon = Icons.Default.PhoneAndroid) {
                            LabeledSlider(
                                label = "Overlay text size",
                                valueText = "${floatingConfigs.textSize.toInt()} sp",
                                value = floatingConfigs.textSize,
                                range = 12f..36f,
                                onValueChange = { saveFloating(floatingConfigs.copy(textSize = it)) },
                            )
                            LabeledSlider(
                                label = "Backdrop opacity",
                                valueText = "${(floatingConfigs.bgOpacity * 100).toInt()}%",
                                value = floatingConfigs.bgOpacity,
                                range = 0.2f..1f,
                                onValueChange = { saveFloating(floatingConfigs.copy(bgOpacity = it)) },
                            )
                            LabeledSlider(
                                label = "Whole-window opacity",
                                valueText = "${(floatingConfigs.windowOpacity * 100).toInt()}%",
                                value = floatingConfigs.windowOpacity,
                                range = 0.3f..1f,
                                onValueChange = { saveFloating(floatingConfigs.copy(windowOpacity = it)) },
                            )

                            Text("Text color", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = FloatingSettings.TEXT_COLORS.map { it to it },
                                selected = floatingConfigs.textColorName,
                                onSelected = { saveFloating(floatingConfigs.copy(textColorName = it)) },
                            )
                            Text("Backdrop", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = FloatingSettings.BG_COLORS.map { it to it },
                                selected = floatingConfigs.bgColorName,
                                onSelected = { saveFloating(floatingConfigs.copy(bgColorName = it)) },
                            )
                            Text("Initial position", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = FloatingSettings.POSITION_GRAVITIES.map { it to it },
                                selected = floatingConfigs.defaultGravity,
                                onSelected = { saveFloating(floatingConfigs.copy(defaultGravity = it)) },
                            )
                        }

                        SettingsSection(title = "Voice sync defaults", icon = Icons.Default.Mic) {
                            Text(
                                "Voice sync uses Android's speech-recognition service. Depending on your device/provider, recognition may require a network connection.",
                                color = SlateTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                            Text("Tracking sensitivity", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf(0 to "Stable", 1 to "Balanced", 2 to "Reactive"),
                                selected = voiceSensitivity,
                                onSelected = {
                                    voiceSensitivity = it
                                    prefs.edit().putInt("voice_sync_sensitivity", it).apply()
                                },
                            )
                            Text("Pause threshold", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf(0 to "Short", 1 to "Normal", 2 to "Long"),
                                selected = voicePauseThreshold,
                                onSelected = {
                                    voicePauseThreshold = it
                                    prefs.edit().putInt("voice_sync_pause_threshold", it).apply()
                                },
                            )
                        }

                        SettingsSection(title = "Physical controls", icon = Icons.Default.AccessibilityNew) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Hardware button controls", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Allow mapped volume/keyboard/clicker keys to control an active prompter.",
                                        color = SlateTextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                    )
                                }
                                Switch(
                                    checked = hardwareButtonsEnabled,
                                    onCheckedChange = {
                                        hardwareButtonsEnabled = it
                                        prefs.edit().putBoolean("hardware_buttons_enabled", it).apply()
                                        HardwareButtonController.isEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricPurple),
                                )
                            }
                            Text(
                                if (RemoteClickerManager.isConnected) {
                                    "External control detected: ${RemoteClickerManager.connectedDeviceName ?: "remote"}"
                                } else {
                                    "External keyboard-style clickers are detected automatically; no Bluetooth device-list permission is needed."
                                },
                                color = if (RemoteClickerManager.isConnected) ElectricCyan else SlateTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }

                        SettingsSection(title = "Recording defaults", icon = Icons.Default.Videocam) {
                            Text(
                                "CueFlow requests your preferred quality and lets CameraX fall back when a camera cannot provide it.",
                                color = SlateTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                            Text("Video quality", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf("720p" to "720p", "1080p" to "1080p", "4K" to "4K"),
                                selected = videoQuality,
                                onSelected = {
                                    videoQuality = it
                                    prefs.edit().putString("recording_video_quality", it).apply()
                                },
                            )
                            Text("Default camera", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            ChoiceRow(
                                options = listOf("front" to "Front", "rear" to "Rear"),
                                selected = defaultCamera,
                                onSelected = {
                                    defaultCamera = it
                                    prefs.edit().putString("recording_default_camera", it).apply()
                                },
                            )
                            Text(
                                "Recordings are saved through Android MediaStore under Movies/CueFlow so they remain visible to gallery apps without broad storage permission.",
                                color = SlateTextMuted,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }

                        SettingsSection(title = "Privacy & app info", icon = Icons.Default.Lock) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("CueFlow", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Version $appVersion", color = SlateTextMuted, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.Info, contentDescription = null, tint = ElectricCyan)
                            }
                            OutlinedButton(
                                onClick = { showPrivacy = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                border = BorderStroke(1.dp, CosmicBorder),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Privacy summary")
                            }
                        }

                        OutlinedButton(
                            onClick = { showResetConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("reset_to_defaults_button"),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Reset functional settings")
                        }
                    }

                    HorizontalDivider(color = CosmicBorder)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                            .testTag("settings_done_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            containerColor = CosmicSurface,
            title = { Text("CueFlow privacy summary", color = SlateTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrivacyParagraph("Scripts and folders", "Stored locally in CueFlow's app database. CueFlow has no account system or analytics SDK.")
                    PrivacyParagraph("Camera and microphone", "Used only when you start recording, dictation, or voice-sync features. Recordings are saved through Android MediaStore.")
                    PrivacyParagraph("Speech recognition", "Handled by the Android speech-recognition service selected on your device. That service may process audio online depending on its provider and settings.")
                    PrivacyParagraph("Web and Google Docs import", "Network access happens only when you explicitly import a URL. CueFlow downloads the requested content directly to your device and limits the response size.")
                    PrivacyParagraph("Wi-Fi remote", "When you explicitly enable it during playback, CueFlow starts a temporary local-network control session protected by a per-session token. It does not expose your script text or title to the remote page.")
                    PrivacyParagraph("Backups", "Android cloud/device backup is disabled for CueFlow so private scripts and preferences are not automatically copied by the app's backup configuration.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Done", color = ElectricCyan)
                }
            },
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = CosmicSurface,
            title = { Text("Reset settings?", color = SlateTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This resets appearance, prompting, overlay, voice, hardware-control and recording defaults. Your scripts, folders, onboarding state and selected interface language are kept.",
                    color = SlateTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetSettings()
                        showResetConfirmation = false
                    },
                ) {
                    Text("Reset", color = Color(0xFFFFB4AB), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable Column.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CosmicBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(19.dp))
                Text(title, color = SlateTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        items(options, key = { it.first.toString() }) { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricPurple.copy(alpha = 0.2f),
                    selectedLabelColor = ElectricPurple,
                    containerColor = CosmicSurface,
                    labelColor = SlateTextSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == value,
                    borderColor = CosmicBorder,
                    selectedBorderColor = ElectricPurple,
                ),
            )
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ElectricPurple,
                activeTrackColor = ElectricPurple,
                inactiveTrackColor = CosmicBorder,
            ),
        )
    }
}

@Composable
private fun AccentPicker(
    selected: Color?,
    onSelected: (Color?) -> Unit,
) {
    val options = listOf<Color?>(
        null,
        Color(0xFF8B5CF6),
        Color(0xFF38BDF8),
        Color(0xFF00C853),
        Color(0xFFF97316),
        Color(0xFFE91E63),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { color ->
            val isSelected = if (color == null) selected == null else selected?.toArgb() == color.toArgb()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onSelected(color) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color ?: MaterialTheme.colorScheme.primary, CircleShape)
                        .then(
                            if (isSelected) Modifier.border(2.dp, SlateTextPrimary, CircleShape)
                            else Modifier.border(1.dp, CosmicBorder, CircleShape),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (color == null) {
                        Text("A", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyParagraph(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = SlateTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(body, color = SlateTextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
