package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.util.LanguageManager
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE) }

    // APPEARANCE PREFERENCES (Reactive to ThemeState)
    val themes = listOf(
        ThemeOption("dark", "Dark Mode", "Sleek SaaS twilight atmosphere"),
        ThemeOption("amoled", "AMOLED Black", "Inky pitch black for battery saving"),
        ThemeOption("light", "Light Mode", "Crisp, daylight optimized contrast"),
        ThemeOption("midnight", "Midnight Blue", "Deep maritime navy tones"),
        ThemeOption("sunset", "Warm Sunset", "Rich sunset and fireplace amber")
    )

    val accentColors = listOf(
        AccentColorOption(null, "Default"),
        AccentColorOption(Color(0xFF9F7AEA), "Purple"),
        AccentColorOption(Color(0xFF4F46E5), "Indigo"),
        AccentColorOption(Color(0xFF2196F3), "Blue"),
        AccentColorOption(Color(0xFF00E5FF), "Cyan"),
        AccentColorOption(Color(0xFFFFB300), "Amber"),
        AccentColorOption(Color(0xFFF9663A), "Sunset"),
        AccentColorOption(Color(0xFFEC4899), "Pink"),
        AccentColorOption(Color(0xFF4CAF50), "Green")
    )

    // TELEPROMPTER DEFAULTS
    var defaultSpeed by remember { mutableFloatStateOf(prefs.getFloat("default_speed", 5.0f)) }
    var defaultFontSize by remember { mutableFloatStateOf(prefs.getFloat("default_font_size", 24.0f)) }
    var defaultTextColor by remember { mutableStateOf(prefs.getString("default_text_color", "#FFFFFF") ?: "#FFFFFF") }
    var defaultBgOpacity by remember { mutableFloatStateOf(prefs.getFloat("default_bg_opacity", 0.4f)) }
    var defaultCountdownDuration by remember { mutableIntStateOf(prefs.getInt("default_countdown_duration", 3)) }
    var defaultTextAlignment by remember { mutableStateOf(prefs.getString("default_text_alignment", "left") ?: "left") }

    // FLOATING OVERLAY CONFIGS
    var floatingTextSize by remember { mutableFloatStateOf(prefs.getFloat("floating_text_size_v3", 18f)) }
    var floatingBgOpacity by remember { mutableFloatStateOf(prefs.getFloat("floating_bg_opacity_v3", 0.85f)) }
    var floatingWindowOpacity by remember { mutableFloatStateOf(prefs.getFloat("floating_window_opacity_v3", 1.0f)) }
    var floatingDefaultGravity by remember { mutableStateOf(prefs.getString("floating_default_gravity_v3", "Center") ?: "Center") }
    var floatingScale by remember { mutableStateOf(prefs.getString("floating_scale_v3", "Medium") ?: "Medium") }

    // VOICE SYNC CONFIGS
    var voiceSensitivity by remember { mutableIntStateOf(prefs.getInt("voice_sync_default_sensitivity", 1)) }
    var voicePauseThreshold by remember { mutableIntStateOf(prefs.getInt("voice_sync_default_pause_threshold", 1)) }

    // CONTROLS CONFIGS
    var hardwareButtonsEnabled by remember { mutableStateOf(prefs.getBoolean("hardware_buttons_enabled", false)) }
    var bluetoothRemoteEnabled by remember { mutableStateOf(prefs.getBoolean("bluetooth_remote_enabled", true)) }

    // RECORDING CONFIGS
    var videoQuality by remember { mutableStateOf(prefs.getString("recording_video_quality", "1080p") ?: "1080p") }
    var defaultCameraSetting by remember { mutableStateOf(prefs.getString("recording_default_camera", "front") ?: "front") }
    var autoSaveLocation by remember { mutableStateOf(prefs.getString("recording_autosave_location", "DCIM/CueFlow") ?: "DCIM/CueFlow") }

    // SCRIPTS CONFIGS
    var defaultFolderSetting by remember { mutableStateOf(prefs.getString("default_folder", "Unassigned") ?: "Unassigned") }
    var autosaveInterval by remember { mutableStateOf(prefs.getString("autosave_interval", "1m") ?: "1m") }

    // DIALOGS STATE
    var showVoiceCalibrationDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // RESET TO DEFAULTS HANDLER
    val performResetToDefaults: () -> Unit = {
        prefs.edit().apply {
            // Clear custom preferences
            remove("default_speed")
            remove("default_font_size")
            remove("default_text_color")
            remove("default_bg_opacity")
            remove("default_countdown_duration")
            remove("default_text_alignment")
            remove("floating_text_size_v3")
            remove("floating_bg_opacity_v3")
            remove("floating_window_opacity_v3")
            remove("floating_default_gravity_v3")
            remove("floating_scale_v3")
            remove("voice_sync_default_sensitivity")
            remove("voice_sync_default_pause_threshold")
            remove("hardware_buttons_enabled")
            remove("bluetooth_remote_enabled")
            remove("recording_video_quality")
            remove("recording_default_camera")
            remove("recording_autosave_location")
            remove("default_folder")
            remove("autosave_interval")
            apply()
        }

        // Reset theme and language manually
        ThemeState.saveTheme(context, "dark")
        ThemeState.saveAccentColor(context, null)
        LanguageManager.saveLanguage(context, "en")

        // Reload states reactively
        defaultSpeed = 5.0f
        defaultFontSize = 24.0f
        defaultTextColor = "#FFFFFF"
        defaultBgOpacity = 0.4f
        defaultCountdownDuration = 3
        defaultTextAlignment = "left"
        floatingTextSize = 18f
        floatingBgOpacity = 0.85f
        floatingWindowOpacity = 1.0f
        floatingDefaultGravity = "Center"
        floatingScale = "Medium"
        voiceSensitivity = 1
        voicePauseThreshold = 1
        hardwareButtonsEnabled = false
        bluetoothRemoteEnabled = true
        videoQuality = "1080p"
        defaultCameraSetting = "front"
        autoSaveLocation = "DCIM/CueFlow"
        defaultFolderSetting = "Unassigned"
        autosaveInterval = "1m"

        Toast.makeText(context, "All settings restored to factory defaults", Toast.LENGTH_LONG).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.94f))
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f)
                    .border(1.dp, CosmicBorder, RoundedCornerShape(24.dp))
                    .testTag("app_settings_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ElectricPurple,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "CueFlow Settings Dashboard",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CosmicSurfaceElevated)
                                .testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = CosmicBorder)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable Config Sections
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // SECTION 1: APPEARANCE (Theme, Accent, Language)
                        SettingsSectionHeader(icon = Icons.Default.Brush, title = "Appearance & Visual Language", iconColor = ElectricCyan)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Theme Selector
                                Text("Primary Design Canvas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    themes.forEach { themeOpt ->
                                        val isSelected = ThemeState.currentTheme == themeOpt.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) CosmicSurface.copy(alpha = 0.5f) else Color.Transparent)
                                                .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                                .clickable { ThemeState.saveTheme(context, themeOpt.id) }
                                                .padding(8.dp)
                                                .testTag("theme_option_${themeOpt.id}"),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ThemeMockupPreview(themeName = themeOpt.id, accentColor = ThemeState.customAccentColor)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(themeOpt.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                                Text(themeOpt.description, fontSize = 10.sp, color = SlateTextSecondary)
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, "Selected", tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Accent Palette Choice
                                Text("Custom Accent Highlights", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                val chunkedAccentColors = accentColors.chunked(5)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunkedAccentColors.forEach { colorRow ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            colorRow.forEach { colorOpt ->
                                                val isSelected = if (colorOpt.color == null) {
                                                    ThemeState.customAccentColor == null
                                                } else {
                                                    ThemeState.customAccentColor?.toArgb() == colorOpt.color.toArgb()
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(CosmicSurface)
                                                        .border(1.5.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                        .clickable { ThemeState.saveAccentColor(context, colorOpt.color) }
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (colorOpt.color == null) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9F7AEA), Color(0xFF38BDF8)))),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isSelected) Icon(Icons.Default.Check, "Checked", tint = Color.White, modifier = Modifier.size(16.dp))
                                                            else Text("Def", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(colorOpt.color),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isSelected) {
                                                                Icon(
                                                                    Icons.Default.Check,
                                                                    "Selected",
                                                                    tint = if (colorOpt.displayName == "Amber") Color.Black else Color.White,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Language Selector
                                Text("System Localized Language", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                val chunkedLanguages = LanguageManager.supportedLanguages.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    chunkedLanguages.forEach { langRow ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            langRow.forEach { langOpt ->
                                                val isSelected = LanguageManager.currentLanguage == langOpt.code
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                                                        .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                        .clickable { LanguageManager.saveLanguage(context, langOpt.code) }
                                                        .testTag("lang_chip_${langOpt.code}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(langOpt.localName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricPurple else SlateTextPrimary)
                                                }
                                            }
                                            if (langRow.size < 3) {
                                                repeat(3 - langRow.size) { Spacer(modifier = Modifier.weight(1f)) }
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        // SECTION 2: TELEPROMPTER DEFAULTS
                        SettingsSectionHeader(icon = Icons.Default.Tv, title = "Teleprompter Defaults", iconColor = ElectricPurple)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Default Speed Slider
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Scroll Pace Speed", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${String.format("%.1f", defaultSpeed)}x", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Initial speed rating when launching scrolling scripts", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = defaultSpeed,
                                        onValueChange = {
                                            defaultSpeed = it
                                            prefs.edit().putFloat("default_speed", it).apply()
                                        },
                                        valueRange = 1.0f..15.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricCyan, thumbColor = ElectricCyan)
                                    )
                                }

                                // Default Font Size Slider
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Text Heading Size", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${defaultFontSize.toInt()} sp", fontSize = 12.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Display sizing scale of Teleprompter characters", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = defaultFontSize,
                                        onValueChange = {
                                            defaultFontSize = it
                                            prefs.edit().putFloat("default_font_size", it).apply()
                                        },
                                        valueRange = 14.0f..48.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricPurple, thumbColor = ElectricPurple)
                                    )
                                }

                                // Default Text Color Presets
                                Column {
                                    Text("Text Color Preset", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Pre-selected tint rendering for scripts", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val colorPresets = listOf(
                                        "#FFFFFF" to "White",
                                        "#00E5FF" to "Cyan",
                                        "#9D4EDD" to "Purple",
                                        "#FFB703" to "Warm Amber",
                                        "#00E676" to "Neon Green"
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        colorPresets.forEach { (hexCode, label) ->
                                            val isSelected = defaultTextColor == hexCode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(android.graphics.Color.parseColor(hexCode)).copy(alpha = 0.2f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) Color(android.graphics.Color.parseColor(hexCode)) else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        defaultTextColor = hexCode
                                                        prefs.edit().putString("default_text_color", hexCode).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, color = Color(android.graphics.Color.parseColor(hexCode)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Default Transparency Slider
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Background Opacity", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${(defaultBgOpacity * 100).toInt()}%", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Alpha transparency level for the screen visor layer", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = defaultBgOpacity,
                                        onValueChange = {
                                            defaultBgOpacity = it
                                            prefs.edit().putFloat("default_bg_opacity", it).apply()
                                        },
                                        valueRange = 0.0f..1.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricCyan, thumbColor = ElectricCyan)
                                    )
                                }

                                // Countdown Duration
                                Column {
                                    Text("Start Countdown Duration", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Preparation delay before scrolling execution commences", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(0 to "None", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (secs, label) ->
                                            val isSelected = defaultCountdownDuration == secs
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        defaultCountdownDuration = secs
                                                        prefs.edit().putInt("default_countdown_duration", secs).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricPurple else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Default Text Position (Alignment)
                                Column {
                                    Text("Screen Text Alignment", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Primary paragraph alignment position in teleprompter", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("left" to "Left Align", "center" to "Center Align", "right" to "Right Align").forEach { (align, label) ->
                                            val isSelected = defaultTextAlignment == align
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        defaultTextAlignment = align
                                                        prefs.edit().putString("default_text_alignment", align).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricCyan else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        // SECTION 3: FLOATING OVERLAY CONFIGS
                        SettingsSectionHeader(icon = Icons.Default.AspectRatio, title = "Floating Window Overlay Defaults", iconColor = ElectricCyan)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Default Window Size Scale
                                Column {
                                    Text("Default Window Width Scale", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Initial horizontal footprint scaling of floating widget", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Small", "Medium", "Large").forEach { scaleOpt ->
                                            val isSelected = floatingScale == scaleOpt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        floatingScale = scaleOpt
                                                        prefs.edit().putString("floating_scale_v3", scaleOpt).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(scaleOpt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricPurple else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Default Gravity Position
                                Column {
                                    Text("Initial Screen Position Anchor", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("System gravity alignment on overlay launch", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("Top Half", "Center", "Bottom Half").forEach { gravityOpt ->
                                            val isSelected = floatingDefaultGravity == gravityOpt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        floatingDefaultGravity = gravityOpt
                                                        prefs.edit().putString("floating_default_gravity_v3", gravityOpt).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(gravityOpt, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricCyan else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Overlay Text Size
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Overlay Default Text Size", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${floatingTextSize.toInt()} sp", fontSize = 12.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Default font representation dimension for overlay widgets", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = floatingTextSize,
                                        onValueChange = {
                                            floatingTextSize = it
                                            prefs.edit().putFloat("floating_text_size_v3", it).apply()
                                        },
                                        valueRange = 12.0f..36.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricPurple, thumbColor = ElectricPurple)
                                    )
                                }

                                // BG Transparency
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Widget Shading Transparency", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${(floatingBgOpacity * 100).toInt()}%", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Controls how transparent the floating text-body backing is", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = floatingBgOpacity,
                                        onValueChange = {
                                            floatingBgOpacity = it
                                            prefs.edit().putFloat("floating_bg_opacity_v3", it).apply()
                                        },
                                        valueRange = 0.2f..1.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricCyan, thumbColor = ElectricCyan)
                                    )
                                }

                                // Window Transparency
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Global Window Transparency", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("${(floatingWindowOpacity * 100).toInt()}%", fontSize = 12.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Total visual pass-through alpha opacity for the entire widget boundary", fontSize = 10.sp, color = SlateTextMuted)
                                    Slider(
                                        value = floatingWindowOpacity,
                                        onValueChange = {
                                            floatingWindowOpacity = it
                                            prefs.edit().putFloat("floating_window_opacity_v3", it).apply()
                                        },
                                        valueRange = 0.3f..1.0f,
                                        colors = SliderDefaults.colors(activeTrackColor = ElectricPurple, thumbColor = ElectricPurple)
                                    )
                                }
                            }
                        }


                        // SECTION 4: VOICE SYNC CONFIGS & CALIBRATION
                        SettingsSectionHeader(icon = Icons.Default.Mic, title = "Smart Voice Sync Tracker Settings", iconColor = ElectricPurple)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Default Sensitivity
                                Column {
                                    Text("Vocal Tracker Sensitivity", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Pacing reaction rate to vocal triggers", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(0 to "Stable / Conserv", 1 to "Balanced", 2 to "Reactive / Fast").forEach { (valInt, label) ->
                                            val isSelected = voiceSensitivity == valInt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        voiceSensitivity = valInt
                                                        prefs.edit().putInt("voice_sync_default_sensitivity", valInt).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricPurple else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Default Pause Threshold
                                Column {
                                    Text("Pause Silence Threshold", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Duration threshold of silence recognition style", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(0 to "Short Wait", 1 to "Normal Pause", 2 to "Extended Pause").forEach { (valInt, label) ->
                                            val isSelected = voicePauseThreshold == valInt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        voicePauseThreshold = valInt
                                                        prefs.edit().putInt("voice_sync_default_pause_threshold", valInt).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricCyan else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Vocal Style Calibration Button
                                Button(
                                    onClick = { showVoiceCalibrationDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Build, "Launch Calibration", tint = CosmicBackground, modifier = Modifier.size(18.dp))
                                        Text("Run Calibrate CueFlow", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Text("Analyses vocal rhythm, reading cadences and accents to lock default tracking sync presets automatically.", fontSize = 10.sp, color = SlateTextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            }
                        }


                        // SECTION 5: SYSTEM HARDWARE CONTROLS
                        SettingsSectionHeader(icon = Icons.Default.VolumeUp, title = "Hardware Trigger Controls", iconColor = ElectricCyan)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Volume Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Volume Button Capture", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Intercept hardware volume presses to increase scroll speed, pause playback, or skip markers.", fontSize = 10.sp, color = SlateTextMuted)
                                    }
                                    val scaleHardware by animateFloatAsState(
                                        targetValue = if (hardwareButtonsEnabled) 1.15f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "scale_hardware"
                                    )
                                    Switch(
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = scaleHardware
                                            scaleY = scaleHardware
                                        },
                                        checked = hardwareButtonsEnabled,
                                        onCheckedChange = {
                                            hardwareButtonsEnabled = it
                                            prefs.edit().putBoolean("hardware_buttons_enabled", it).apply()
                                            // Real-time update global controllers
                                            com.example.util.HardwareButtonController.isEnabled = it
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan, checkedThumbColor = CosmicBackground, uncheckedThumbColor = SlateTextSecondary, uncheckedTrackColor = CosmicSurface)
                                    )
                                }

                                HorizontalDivider(color = CosmicBorder.copy(alpha = 0.5f))

                                // Bluetooth clicker tracking
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Auto-Detect Bluetooth Remote", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Detect standard presentation clicker controllers, keyboards or remote selfie shutter triggers automatically.", fontSize = 10.sp, color = SlateTextMuted)
                                    }
                                    val scaleBluetooth by animateFloatAsState(
                                        targetValue = if (bluetoothRemoteEnabled) 1.15f else 1.0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "scale_bluetooth"
                                    )
                                    Switch(
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = scaleBluetooth
                                            scaleY = scaleBluetooth
                                        },
                                        checked = bluetoothRemoteEnabled,
                                        onCheckedChange = {
                                            bluetoothRemoteEnabled = it
                                            prefs.edit().putBoolean("bluetooth_remote_enabled", it).apply()
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = ElectricPurple, checkedThumbColor = CosmicBackground, uncheckedThumbColor = SlateTextSecondary, uncheckedTrackColor = CosmicSurface)
                                    )
                                }
                            }
                        }


                        // SECTION 6: VIDEO RECORDING DEFAULTS
                        SettingsSectionHeader(icon = Icons.Default.Videocam, title = "Video Recording Quality & Cameras", iconColor = ElectricPurple)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Camera quality
                                Column {
                                    Text("Primary Video Capture Quality", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Resolution preset for script rehearsal recording sessions", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("720p" to "HD Resolution", "1080p" to "Full-HD (1080p)", "4K" to "4K Ultra-HD").forEach { (resKey, label) ->
                                            val isSelected = videoQuality == resKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        videoQuality = resKey
                                                        prefs.edit().putString("recording_video_quality", resKey).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricPurple else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Default Cam front/rear
                                Column {
                                    Text("Default Active Camera Finder", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Launcher camera lens orientation at startup", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("front" to "Front FaceTime Camera", "rear" to "Rear Primary Camera").forEach { (camKey, label) ->
                                            val isSelected = defaultCameraSetting == camKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        defaultCameraSetting = camKey
                                                        prefs.edit().putString("recording_default_camera", camKey).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricCyan else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }

                                // Storage Local Directory Selector
                                Column {
                                    Text("Rehearsals Save Directory", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Storage path for recorded rehearsal outputs", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = autoSaveLocation,
                                        onValueChange = {
                                            autoSaveLocation = it
                                            prefs.edit().putString("recording_autosave_location", it).apply()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricCyan,
                                            unfocusedBorderColor = CosmicBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = SlateTextSecondary,
                                            focusedContainerColor = CosmicSurface,
                                            unfocusedContainerColor = CosmicSurface
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }


                        // SECTION 7: SCRIPTS MANAGEMENT
                        SettingsSectionHeader(icon = Icons.Default.Folder, title = "Scripts Folder & Auto-Save Options", iconColor = ElectricCyan)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Default Directory Group
                                Column {
                                    Text("Fallback Folder Category", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Initial folder assignment for newly generated scripts", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = defaultFolderSetting,
                                        onValueChange = {
                                            defaultFolderSetting = it
                                            prefs.edit().putString("default_folder", it).apply()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ElectricPurple,
                                            unfocusedBorderColor = CosmicBorder,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = SlateTextSecondary,
                                            focusedContainerColor = CosmicSurface,
                                            unfocusedContainerColor = CosmicSurface
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                // Save Sync Interval
                                Column {
                                    Text("Automated Saving Interval", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    Text("Timer interval for automatic local content backups", fontSize = 10.sp, color = SlateTextMuted)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("30s" to "30 Sec", "1m" to "1 Min", "5m" to "5 Min", "Off" to "Disabled").forEach { (intKey, label) ->
                                            val isSelected = autosaveInterval == intKey
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.15f) else CosmicSurface)
                                                    .border(1.dp, if (isSelected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        autosaveInterval = intKey
                                                        prefs.edit().putString("autosave_interval", intKey).apply()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) ElectricCyan else SlateTextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }


                        // SECTION 8: ABOUT & PRIVACY
                        SettingsSectionHeader(icon = Icons.Default.Info, title = "Product Info & Legal Policy", iconColor = ElectricPurple)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showAboutDialog = true }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, "About application", tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                        Text("About CueFlow", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("v1.4.2", fontSize = 12.sp, color = SlateTextSecondary)
                                        Icon(Icons.Default.ArrowForwardIos, "More", tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                                    }
                                }

                                HorizontalDivider(color = CosmicBorder.copy(alpha = 0.5f))

                                // Functional action item: Rate App
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val packageName = context.packageName
                                            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                                            }
                                            try {
                                                context.startActivity(marketIntent)
                                            } catch (e: Exception) {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                                                } catch (err: Exception) {
                                                    Toast.makeText(context, "Play Store link unavailable.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, "Rate app", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Text("Rate CueFlow on Store", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, "More", tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                                }

                                // Functional action item: Share App
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Speak Confidently with CueFlow")
                                                putExtra(Intent.EXTRA_TEXT, "I am speaking with flawless confidence using CueFlow: Floating Teleprompter! Try it out for crystal-clear scripts, custom dual overlays, and smart voice tracking sync: https://play.google.com/store/apps/details?id=${context.packageName}")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share CueFlow with Friends"))
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Share, "Share app", tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                        Text("Share with Presenters", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, "More", tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                                }

                                // Functional action item: Privacy Policy
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showPrivacyPolicyDialog = true
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, "Privacy policy", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Text("Decentralized Privacy Mandate", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, "More", tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                                }

                                // Functional action item: Support Contact
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:")
                                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@cueflow-app.com"))
                                                    putExtra(Intent.EXTRA_SUBJECT, "CueFlow Teleprompter Inquiry")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Contact directly via: support@cueflow-app.com", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Email, "Contact Support", tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                        Text("Speak with Support Engineers", fontSize = 12.sp, color = SlateTextPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Default.ArrowForwardIos, "More", tint = SlateTextSecondary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // RESET TO DEFAULT TRIGGER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.08f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable { showResetConfirmation = true }
                                .padding(16.dp)
                                .testTag("reset_to_defaults_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, "Reset defaults", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All Settings to Factory Defaults", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    HorizontalDivider(color = CosmicBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Done/Save Apply Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_done_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                    ) {
                        Text(
                            text = "Save & Apply Custom Configurations",
                            color = CosmicBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Voice Sync Calibration overlay
    if (showVoiceCalibrationDialog) {
        VoiceSyncSettingsDialog(
            onDismiss = { showVoiceCalibrationDialog = false },
            sensitivity = voiceSensitivity,
            onSensitivityChange = { newVal ->
                voiceSensitivity = newVal
                prefs.edit().putInt("voice_sync_default_sensitivity", newVal).apply()
            },
            pauseThreshold = voicePauseThreshold,
            onPauseThresholdChange = { newVal ->
                voicePauseThreshold = newVal
                prefs.edit().putInt("voice_sync_default_pause_threshold", newVal).apply()
            },
            minCrawl = 3,
            onMinCrawlChange = {},
            maxLimit = 15,
            onMaxLimitChange = {},
            context = context
        )
    }

    // Reset Defaults Confirmation Popup Dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            containerColor = CosmicSurface,
            titleContentColor = SlateTextPrimary,
            textContentColor = SlateTextSecondary,
            title = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, "Warning", tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Text("Factory System Reset", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you completely sure you wish to wipe any custom speed rates, styling heading sizes, countdown timers, overlay opacity settings, and controls maps? Your existing written scripts folders will be kept intact.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        performResetToDefaults()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Reset", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel", color = SlateTextSecondary, fontSize = 12.sp)
                }
            },
            modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(20.dp)).testTag("reset_confirm_dialog")
        )
    }

    if (showAboutDialog) {
        Dialog(
            onDismissRequest = { showAboutDialog = false }
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
                            imageVector = Icons.Default.Info,
                            contentDescription = "About icon",
                            tint = ElectricPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "About CueFlow",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "CueFlow is a production-grade, offline-first floating teleprompter designed for presenters, content creators, speakers, and video professionals. Focus purely on speaking with bulletproof confidence and crystal-clear execution.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CosmicSurfaceElevated, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Version", fontSize = 11.sp, color = SlateTextMuted)
                            Text("1.4.2 (Stable Release)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Compliance", fontSize = 11.sp, color = SlateTextMuted)
                            Text("Android 8.0+ Oreo (API 26+)", fontSize = 11.sp, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Credits", fontSize = 11.sp, color = SlateTextMuted)
                            Text("CueFlow Open Engineers", fontSize = 11.sp, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Contact", fontSize = 11.sp, color = SlateTextMuted)
                            Text("support@cueflow-app.com", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("License", fontSize = 11.sp, color = SlateTextMuted)
                            Text("Apache License 2.0", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = { showAboutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Awesome", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        Dialog(
            onDismissRequest = { showPrivacyPolicyDialog = false }
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
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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
                            .background(ElectricCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Privacy policy icon",
                            tint = ElectricCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Decentralized Privacy Mandate",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Your written thoughts and spoken voice are strictly your own property.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SlateTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "CueFlow operates under a 100% decentralized local privacy standard:\n\n" +
                                "• Zero Data Collection: We do not collect, transmit, or monetize any of your data.\n" +
                                "• Completely Local Scripts: All script templates and custom text reside purely in your device's local Room database storage.\n" +
                                "• Real-Time Vocal Recognition: Speech processing runs locally on-device via Android's native offline recognition service.\n" +
                                "• Absolute Isolation: The app requests no unnecessary background accounts, tracking profiles, or analytics tools.",
                        fontSize = 11.sp,
                        color = SlateTextSecondary,
                        textAlign = TextAlign.Left,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { showPrivacyPolicyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Done", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
    }
}

data class ThemeOption(
    val id: String,
    val displayName: String,
    val description: String
)

data class AccentColorOption(
    val color: Color?,
    val displayName: String
)

@Composable
fun ThemeMockupPreview(themeName: String, accentColor: Color?) {
    val bgColor = when (themeName) {
        "amoled" -> Color(0xFF000000)
        "light" -> Color(0xFFF1F5F9)
        "midnight" -> Color(0xFF060D1E)
        "sunset" -> Color(0xFF0F0806)
        else -> Color(0xFF090A0F)
    }
    val surfColor = when (themeName) {
        "amoled" -> Color(0xFF0A0A0A)
        "light" -> Color(0xFFFFFFFF)
        "midnight" -> Color(0xFF0E1A30)
        "sunset" -> Color(0xFF1F120E)
        else -> Color(0xFF12131C)
    }
    val actualAccent = accentColor ?: Color(0xFF9F7AEA)
    Box(
        modifier = Modifier
            .size(32.dp, 22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(0.5.dp, CosmicBorder, RoundedCornerShape(4.dp))
            .padding(2.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(actualAccent)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(0.5.dp))
                    .background(surfColor)
            )
        }
    }
}
