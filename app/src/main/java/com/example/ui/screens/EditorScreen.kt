package com.example.ui.screens

import com.example.util.LanguageManager

import androidx.compose.animation.*
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import com.example.data.Script
import com.example.data.Folder
import com.example.ui.ScriptViewModel
import com.example.ui.components.ImportDialog
import com.example.ui.components.TeleprompterPlaybackDialog
import com.example.ui.components.PromptModeSelectionDialog
import com.example.ui.components.OverlayPermissionExplanationDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.service.FloatingPrompterService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: ScriptViewModel,
    scriptId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE) }

    // Document states
    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    val content = contentValue.text
    var speed by remember { mutableFloatStateOf(prefs.getFloat("default_speed", 5f)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("default_font_size", 24f)) }
    var isMirrored by remember { mutableStateOf(false) }
    
    val defaultFolderSetting = prefs.getString("default_folder", "Unassigned") ?: "Unassigned"
    val initialFolder = if (defaultFolderSetting == "Unassigned" || defaultFolderSetting.isBlank()) null else defaultFolderSetting
    
    var selectedFolder by remember { mutableStateOf<String?>(initialFolder) }
    var textColor by remember { mutableStateOf(prefs.getString("default_text_color", "#FFFFFF") ?: "#FFFFFF") }
    var bgOpacity by remember { mutableFloatStateOf(prefs.getFloat("default_bg_opacity", 0.4f)) }
    var textAlignment by remember { mutableStateOf(prefs.getString("default_text_alignment", "left") ?: "left") }
    var lineSpacing by remember { mutableStateOf("normal") }
    var textDirectionState by remember { mutableStateOf("auto") }
    var existingScript by remember { mutableStateOf<Script?>(null) }
    
    // Bookmarks and Tab layout support
    var currentTab by remember { mutableStateOf("edit") }
    var bookmarkedParagraphs by remember(existingScript?.id) {
        val id = existingScript?.id ?: 0
        val saved = prefs.getStringSet("script_bookmarks_$id", emptySet()) ?: emptySet()
        mutableStateOf(saved)
    }

    fun toggleBookmark(paragraphText: String) {
        val cleanText = paragraphText.trim()
        if (cleanText.isEmpty()) return
        val newSet = if (bookmarkedParagraphs.contains(cleanText)) {
            bookmarkedParagraphs - cleanText
        } else {
            bookmarkedParagraphs + cleanText
        }
        bookmarkedParagraphs = newSet
        val id = existingScript?.id ?: 0
        prefs.edit().putStringSet("script_bookmarks_$id", newSet).apply()
    }

    // UI HUD states
    var isLoading by remember { mutableStateOf(scriptId != -1) }
    var isSavingState by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showPlaybackPreview by remember { mutableStateOf(false) }
    var showPlaybackModeSelection by remember { mutableStateOf(false) }
    var showOverlayPermissionExplain by remember { mutableStateOf(false) }
    var isPlaybackPracticeMode by remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()
    val folders by viewModel.allFolders.collectAsState(initial = emptyList())

    // Fetch script if editing an existing script
    LaunchedEffect(scriptId) {
        if (scriptId != -1) {
            val script = viewModel.getScriptById(scriptId)
            if (script == null) {
                isLoading = false
                onBack()
                return@LaunchedEffect
            }
            existingScript = script
                title = script.title
                contentValue = TextFieldValue(script.content)
                speed = script.scrollSpeed.toFloat()
                fontSize = script.fontSize.toFloat()
                isMirrored = script.isMirrored
                selectedFolder = script.folderName
                textColor = script.textColor
                bgOpacity = script.bgOpacity
                textAlignment = script.textAlignment
                lineSpacing = script.lineSpacing
                textDirectionState = script.textDirection
            isLoading = false
        }
    }

    // Direct save function with SQLite ID tracking to avoid duplicates
    val executeSave: suspend () -> Unit = {
        if (existingScript != null || title.isNotBlank() || content.isNotBlank()) {
            isSavingState = true
             val currentScript = existingScript?.copy(
                title = title.ifBlank { "Untitled Script" },
                content = content,
                scrollSpeed = speed.toInt(),
                fontSize = fontSize.toInt(),
                isMirrored = isMirrored,
                folderName = selectedFolder,
                textColor = textColor,
                bgOpacity = bgOpacity,
                textAlignment = textAlignment,
                lineSpacing = lineSpacing,
                textDirection = textDirectionState
            ) ?: Script(
                title = title.ifBlank { "Untitled Script" },
                content = content,
                scrollSpeed = speed.toInt(),
                fontSize = fontSize.toInt(),
                isMirrored = isMirrored,
                folderName = selectedFolder,
                textColor = textColor,
                bgOpacity = bgOpacity,
                textAlignment = textAlignment,
                lineSpacing = lineSpacing,
                textDirection = textDirectionState,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val savedId = viewModel.saveScript(currentScript)
            val newId = savedId.toInt()
            if (existingScript == null) {
                existingScript = currentScript.copy(id = newId)
                // Migrate bookmarks if any
                val tempBookmarks = prefs.getStringSet("script_bookmarks_0", emptySet())
                if (!tempBookmarks.isNullOrEmpty()) {
                    prefs.edit().putStringSet("script_bookmarks_$newId", tempBookmarks).apply()
                    bookmarkedParagraphs = tempBookmarks
                    prefs.edit().remove("script_bookmarks_0").apply()
                }
            } else {
                existingScript = currentScript
            }
            isSavingState = false
        }
    }

    // Intercept system back gestures to autosave and return
    BackHandler {
        coroutineScope.launch {
            executeSave()
            onBack()
        }
    }


    // Auto-save debouncer (1.5 seconds of user input pause)
    LaunchedEffect(title, content, speed, fontSize, isMirrored, selectedFolder, textColor, bgOpacity, textAlignment, lineSpacing, textDirectionState) {
        if (!isLoading) {
            delay(1500)
            executeSave()
        }
    }

    // Word Count and Speech Duration HUD Calculator
    val wordCount = remember(content) {
        content.split("\\s+".toRegex()).count { it.isNotBlank() }
    }
    val charCount = remember(content) {
        content.length
    }
    val speechDurationText = remember(wordCount, speed) {
        val speedValue = speed.coerceAtLeast(0.5f)
        val speakingSpeedWpm = speedValue * 30f
        val totalSeconds = if (wordCount > 0) ((wordCount.toFloat() / speakingSpeedWpm) * 60f).toInt() else 0
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        when {
            mins > 0 && secs > 0 -> "$mins min $secs sec"
            mins > 0 -> "$mins min"
            else -> "$secs sec"
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground),
        containerColor = CosmicBackground,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back/Exit
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                executeSave()
                                onBack()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Go back",
                            tint = SlateTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Auto-Saving HUD with premium glowing pulse
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateContentSize()
                    ) {
                        if (isSavingState) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = ElectricPurple,
                                strokeWidth = 1.5.dp
                            )
                            Text(
                                text = "Saving draft...",
                                fontSize = 12.sp,
                                color = SlateTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (existingScript != null) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(ElectricCyan, CircleShape)
                            )
                            Text(
                                text = "Draft Saved",
                                fontSize = 12.sp,
                                color = SlateTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text(
                                text = "New Script",
                                fontSize = 12.sp,
                                color = SlateTextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Row with Import and Save buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CosmicSurfaceElevated, RoundedCornerShape(10.dp))
                                .border(1.dp, CosmicBorder, RoundedCornerShape(10.dp))
                                .testTag("editor_import_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Import Content",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Premium "Start Prompting" trigger
                        if (content.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricPurple.copy(alpha = 0.25f))
                                    .border(1.dp, ElectricPurple.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            executeSave()
                                            showPlaybackModeSelection = true
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("editor_prompt_button"),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Prompting",
                                    tint = ElectricPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Prompt",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Save / Done glassmorphic text button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (title.isNotBlank() || content.isNotBlank())
                                        Brush.linearGradient(listOf(ElectricPurple, ElectricCyan))
                                    else
                                        Brush.linearGradient(listOf(CosmicSurfaceElevated, CosmicSurfaceElevated))
                                )
                                .clickable(enabled = title.isNotBlank() || content.isNotBlank()) {
                                    coroutineScope.launch {
                                        executeSave()
                                        onBack()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("editor_save_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (title.isNotBlank() || content.isNotBlank()) CosmicBackground else SlateTextMuted
                            )
                        }
                    }
                }
                HorizontalDivider(color = CosmicBorder, thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            val infiniteTransition = rememberInfiniteTransition(label = "shimmer_loading")
            val shimmerAlpha by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.85f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 950, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer"
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title placeholder pulse
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .graphicsLayer { alpha = shimmerAlpha }
                        .background(CosmicSurfaceElevated)
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Body placeholder pulse
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .graphicsLayer { alpha = shimmerAlpha }
                            .background(CosmicSurfaceElevated)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .graphicsLayer { alpha = shimmerAlpha }
                        .background(CosmicSurfaceElevated)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // CRITICAL: Makes entire column resize so keyboard never overlaps text areas
            ) {
                // Main editing canvas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Document Title
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("editor_input_title"),
                        placeholder = {
                            Text(
                                "Enter a descriptive title...",
                                color = SlateTextMuted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = SlateTextPrimary,
                            unfocusedTextColor = SlateTextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = ElectricPurple
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Document stats HUD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CosmicSurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$wordCount words",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextSecondary,
                                modifier = Modifier.testTag("word_counter")
                            )
                            Text(
                                text = "$charCount characters",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextSecondary,
                                modifier = Modifier.testTag("char_counter")
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.08f))
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "This script will take approximately $speechDurationText to read",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan,
                                modifier = Modifier.testTag("duration_estimate")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Premium Folder Selection pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        var showCreateFolderDialog by remember { mutableStateOf(false) }
                        var newFolderNameInput by remember { mutableStateOf("") }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Folder:",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElectricPurple.copy(alpha = 0.12f))
                                        .clickable { expanded = true }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("editor_folder_spinner")
                                ) {
                                    Text(
                                        text = selectedFolder ?: "None (All Scripts)",
                                        fontSize = 12.sp,
                                        color = ElectricPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = ElectricPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(CosmicSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("None (All Scripts)", color = SlateTextPrimary) },
                                        onClick = {
                                            selectedFolder = null
                                            expanded = false
                                        }
                                    )
                                    folders.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.name, color = SlateTextPrimary) },
                                            onClick = {
                                                selectedFolder = f.name
                                                expanded = false
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = CosmicBorder)
                                    DropdownMenuItem(
                                        text = { 
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                                Text("Create Folder...", color = ElectricCyan)
                                            }
                                        },
                                        onClick = {
                                            expanded = false
                                            showCreateFolderDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        if (showCreateFolderDialog) {
                            AlertDialog(
                                onDismissRequest = { showCreateFolderDialog = false },
                                containerColor = CosmicSurface,
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Create New Folder", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        OutlinedTextField(
                                            value = newFolderNameInput,
                                            onValueChange = { newFolderNameInput = it },
                                            placeholder = { Text("Folder Name", color = SlateTextMuted) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedTextColor = SlateTextPrimary,
                                                focusedTextColor = SlateTextPrimary,
                                                focusedBorderColor = ElectricPurple,
                                                unfocusedBorderColor = CosmicBorder
                                            ),
                                            singleLine = true,
                                            modifier = Modifier.testTag("editor_create_folder_input")
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        if (newFolderNameInput.isNotBlank()) {
                                            viewModel.createFolder(newFolderNameInput)
                                            selectedFolder = newFolderNameInput.trim()
                                            newFolderNameInput = ""
                                        }
                                        showCreateFolderDialog = false
                                    }) {
                                        Text("Create", color = ElectricCyan)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreateFolderDialog = false }) {
                                        Text("Cancel", color = SlateTextSecondary)
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(CosmicSurfaceElevated, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentTab == "edit") ElectricPurple else Color.Transparent)
                                .clickable { currentTab = "edit" }
                                .padding(vertical = 8.dp)
                                .testTag("tab_edit_script"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (currentTab == "edit") Color.White else SlateTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = LanguageManager.get("tab_edit"),
                                    color = if (currentTab == "edit") Color.White else SlateTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (currentTab == "bookmarks") ElectricPurple else Color.Transparent)
                                .clickable { currentTab = "bookmarks" }
                                .padding(vertical = 8.dp)
                                .testTag("tab_bookmarks"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = if (currentTab == "bookmarks") Color.White else SlateTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = LanguageManager.get("tab_bookmarks"),
                                    color = if (currentTab == "bookmarks") Color.White else SlateTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentTab == "edit") {

                        // Script Core Body Text Area
                        TextField(
                            value = contentValue,
                            onValueChange = { contentValue = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Uses remaining vertical viewport screen above keyboard
                                .testTag("editor_input_content"),
                            placeholder = {
                                Text(
                                    "Write or paste the full script body text here...",
                                    color = SlateTextMuted,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Default
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = ElectricPurple
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                textAlign = com.example.util.RtlHelper.getTextAlign(textAlignment, textDirectionState, content),
                                textDirection = com.example.util.RtlHelper.getTextDirection(textDirectionState, content)
                            )
                        )
                    } else {
                        // Bookmarks and Highlights interactive mode
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Bookmarks List / HUD at top
                            val paragraphs = remember(content) {
                                content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            }
                            val bookmarkedList = remember(paragraphs, bookmarkedParagraphs) {
                                paragraphs.filter { bookmarkedParagraphs.contains(it) }
                            }

                            if (bookmarkedList.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicSurfaceElevated)
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                            Text("Saved Key Points (${bookmarkedList.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                        }
                                        Text("Tap to jump & edit", fontSize = 10.sp, color = SlateTextMuted)
                                    }
                                    
                                    // Horizontal row of bookmarked chips
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("saved_bookmarks_lazy_row")
                                    ) {
                                        items(bookmarkedList) { bookmarkText ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(ElectricCyan.copy(alpha = 0.15f))
                                                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val targetIndex = content.indexOf(bookmarkText)
                                                        if (targetIndex != -1) {
                                                            contentValue = contentValue.copy(
                                                                selection = TextRange(targetIndex, targetIndex + bookmarkText.length)
                                                            )
                                                            currentTab = "edit"
                                                        }
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("bookmark_chip_${bookmarkText.take(10)}")
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(12.dp))
                                                    Text(
                                                        text = if (bookmarkText.length > 20) bookmarkText.take(20) + "..." else bookmarkText,
                                                        fontSize = 11.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove Bookmark",
                                                        tint = Color.White.copy(alpha = 0.6f),
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable { toggleBookmark(bookmarkText) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Empty state card
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CosmicSurfaceElevated)
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = SlateTextMuted, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "No bookmarks added yet",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SlateTextSecondary
                                        )
                                        Text(
                                            text = "Tap on any paragraph below to bookmark highlighted key points.",
                                            fontSize = 11.sp,
                                            color = SlateTextMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // List of paragraphs
                            Text(
                                text = "TAP PARAGRAPHS TO BOOKMARK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (paragraphs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(LanguageManager.get("script_empty_state"), color = SlateTextMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                ) {
                                    itemsIndexed(paragraphs) { index, paragraph ->
                                        val isBookmarked = bookmarkedParagraphs.contains(paragraph)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    width = 1.2.dp,
                                                    color = if (isBookmarked) ElectricPurple else Color.White.copy(alpha = 0.08f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .testTag("bookmark_paragraph_item_$index"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isBookmarked) ElectricPurple.copy(alpha = 0.12f) else CosmicSurfaceElevated
                                            ),
                                            onClick = { toggleBookmark(paragraph) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = if (isBookmarked) ElectricCyan else Color.White.copy(alpha = 0.15f),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Text(
                                                        text = paragraph,
                                                        fontSize = 13.sp,
                                                        color = if (isBookmarked) Color.White else SlateTextPrimary,
                                                        lineHeight = 18.sp,
                                                        textAlign = com.example.util.RtlHelper.getTextAlign(null, textDirectionState, paragraph),
                                                        modifier = Modifier.weight(1f),
                                                        style = androidx.compose.ui.text.TextStyle(
                                                            textDirection = com.example.util.RtlHelper.getTextDirection(textDirectionState, paragraph)
                                                        )
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                    contentDescription = if (isBookmarked) "Bookmarked" else "Not bookmarked",
                                                    tint = if (isBookmarked) ElectricCyan else SlateTextMuted,
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

                // Interactive Expandable Quick Settings HUD at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurface)
                        .border(1.dp, CosmicBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQuickSettings = !showQuickSettings }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Prompter Appearance & Preview",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextPrimary
                            )
                        }

                        Icon(
                            imageVector = if (showQuickSettings) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Quick Settings",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Collapsible prompt configure settings
                    AnimatedVisibility(
                        visible = showQuickSettings,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 16.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Live Preview (at top of the panel to see changes instantly!)
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "LIVE SPEECHER PREVIEW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextMuted,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                // Real-time preview container with simulated transparency and contrast
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(96.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F0F15))
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(8.dp))
                                ) {
                                    // Simulated Grid lines
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.05f),
                                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                                            strokeWidth = 1f
                                        )
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.05f),
                                            start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                            end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Foreground text screen with translucency
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = bgOpacity))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val previewTextAlign = when (textAlignment) {
                                            "center" -> androidx.compose.ui.text.style.TextAlign.Center
                                            "right" -> androidx.compose.ui.text.style.TextAlign.Right
                                            else -> androidx.compose.ui.text.style.TextAlign.Left
                                        }
                                        val previewLineHeight = when (lineSpacing) {
                                            "tight" -> (fontSize * 1.15f).sp
                                            "relaxed" -> (fontSize * 1.8f).sp
                                            "double" -> (fontSize * 2.4f).sp
                                            else -> (fontSize * 1.45f).sp
                                        }
                                        Text(
                                            text = if (content.isNotBlank()) {
                                                if (content.length > 80) content.take(80) + "..." else content
                                            } else {
                                                "Beautiful prompt customized alignment, spacing and color contrast live preview system..."
                                            },
                                            color = parseColorSafely(textColor),
                                            fontSize = (fontSize * 0.7f).coerceIn(12f, 24f).sp,
                                            textAlign = previewTextAlign,
                                            lineHeight = previewLineHeight * 0.7f,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

                            // Speed control slider
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(text = "Auto-scroll seed speed", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    Text(
                                        text = "${speed.toInt()}x",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricPurple
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
                                    modifier = Modifier.testTag("editor_speed_slider")
                                )
                            }

                            // Font size slider
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(text = "Teleprompter text size", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    Text(
                                        text = "${fontSize.toInt()} sp",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                }
                                Slider(
                                    value = fontSize,
                                    onValueChange = { fontSize = it },
                                    valueRange = 14f..64f,
                                    colors = SliderDefaults.colors(
                                        activeTickColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan,
                                        inactiveTrackColor = CosmicBorder,
                                        thumbColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("editor_size_slider")
                                )
                            }

                            // Background Transparency Opacity Slider
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                            imageVector = Icons.Default.Opacity,
                                            contentDescription = null,
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(text = "Background transparency", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    val transparencyPercent = ((1.0f - bgOpacity) * 100).toInt()
                                    Text(
                                        text = "$transparencyPercent% Transparent",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricCyan
                                    )
                                }
                                Slider(
                                    value = bgOpacity,
                                    onValueChange = { bgOpacity = it },
                                    valueRange = 0.0f..0.9f,
                                    colors = SliderDefaults.colors(
                                        activeTickColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan,
                                        inactiveTrackColor = CosmicBorder,
                                        thumbColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("editor_transparency_slider")
                                )
                            }

                            // Text Color Presets + Custom Hex Picker Row
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(text = "Text Color Preset or Custom Hex", fontSize = 11.sp, color = SlateTextSecondary)
                                    }
                                    Text(
                                        text = textColor.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = parseColorSafely(textColor)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val presets = listOf(
                                        "#FFFFFF" to "White",
                                        "#FACC15" to "Yellow",
                                        "#22C55E" to "Green",
                                        "#06B6D4" to "Cyan",
                                        "#FF7A00" to "Orange",
                                        "#EC4899" to "Pink"
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        presets.forEach { (hexStr, label) ->
                                            val presetColor = parseColorSafely(hexStr)
                                            val isSelected = textColor.equals(hexStr, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(presetColor)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) ElectricPurple else Color.White.copy(alpha = 0.3f),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { textColor = hexStr }
                                                    .testTag("color_preset_$label")
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    OutlinedTextField(
                                        value = textColor,
                                        onValueChange = { input ->
                                            val filtered = input.trim()
                                            if (filtered.startsWith("#") || filtered.length <= 6) {
                                                textColor = if (!filtered.startsWith("#") && filtered.isNotEmpty()) {
                                                    "#$filtered"
                                                } else {
                                                    filtered
                                                }
                                            }
                                        },
                                        placeholder = { Text("#FFFFFF", color = SlateTextMuted) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(44.dp)
                                            .testTag("custom_color_hex_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = SlateTextPrimary,
                                            unfocusedTextColor = SlateTextPrimary,
                                            focusedContainerColor = CosmicSurfaceElevated,
                                            unfocusedContainerColor = CosmicSurfaceElevated,
                                            focusedBorderColor = ElectricPurple,
                                            unfocusedBorderColor = CosmicBorder
                                        ),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            // Text alignment segmented icons row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Text Alignment",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(
                                        "left" to Icons.Default.FormatAlignLeft,
                                        "center" to Icons.Default.FormatAlignCenter,
                                        "right" to Icons.Default.FormatAlignRight
                                    ).forEach { (align, icon) ->
                                        val selected = textAlignment == align
                                        IconButton(
                                            onClick = { textAlignment = align },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) ElectricPurple.copy(alpha = 0.2f) else CosmicSurfaceElevated)
                                                .border(1.dp, if (selected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                .testTag("align_button_$align")
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "Align $align",
                                                tint = if (selected) ElectricPurple else SlateTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Line spacing segmented text chips row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Line Spacing",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("tight", "normal", "relaxed", "double").forEach { spacing ->
                                        val selected = lineSpacing == spacing
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) ElectricCyan.copy(alpha = 0.2f) else CosmicSurfaceElevated)
                                                .border(1.dp, if (selected) ElectricCyan else CosmicBorder, RoundedCornerShape(8.dp))
                                                .clickable { lineSpacing = spacing }
                                                .testTag("spacing_chip_$spacing"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = spacing.uppercase(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) ElectricCyan else SlateTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Text Direction (RTL Arabic/Hebrew Support) Segmented Picker Row
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Text Direction (RTL Arabic/Hebrew support)",
                                    fontSize = 11.sp,
                                    color = SlateTextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("auto" to "AUTO DETECT", "ltr" to "FORCE LTR", "rtl" to "FORCE RTL").forEach { (dir, label) ->
                                        val selected = textDirectionState == dir
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) ElectricPurple.copy(alpha = 0.2f) else CosmicSurfaceElevated)
                                                .border(1.dp, if (selected) ElectricPurple else CosmicBorder, RoundedCornerShape(8.dp))
                                                .clickable { textDirectionState = dir }
                                                .testTag("direction_chip_$dir"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) ElectricPurple else SlateTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Mirror toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flip,
                                        contentDescription = null,
                                        tint = SlateTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Physically Mirrored Display",
                                            fontSize = 11.sp,
                                            color = SlateTextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Flips feed horizontally for prompter screens",
                                            fontSize = 9.sp,
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
                                    modifier = Modifier.testTag("editor_mirror_switch")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportText = { text, mode ->
                showImportDialog = false
                if (mode == "append" && content.isNotBlank()) {
                    val merged = if (content.endsWith("\n")) {
                        content + text
                    } else {
                        content + "\n\n" + text
                    }
                    contentValue = TextFieldValue(merged)
                } else {
                    contentValue = TextFieldValue(text)
                }
            },
            hasExistingText = content.isNotBlank()
        )
    }

    if (showPlaybackModeSelection) {
        val currentScriptForPlayback = Script(
            id = existingScript?.id ?: 0,
            title = title.ifBlank { "Untitled" },
            content = content,
            folderName = selectedFolder,
            scrollSpeed = speed.toInt(),
            fontSize = fontSize.toInt(),
            isMirrored = isMirrored,
            textColor = textColor,
            bgOpacity = bgOpacity,
            textAlignment = textAlignment,
            lineSpacing = lineSpacing,
            textDirection = textDirectionState,
            createdAt = existingScript?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        PromptModeSelectionDialog(
            script = currentScriptForPlayback,
            onDismiss = { showPlaybackModeSelection = false },
            onModeSelected = { isPractice ->
                showPlaybackModeSelection = false
                isPlaybackPracticeMode = isPractice
                showPlaybackPreview = true
            },
            onFloatingSelected = {
                showPlaybackModeSelection = false
                if (Settings.canDrawOverlays(context)) {
                    val intent = Intent(context, FloatingPrompterService::class.java).apply {
                        putExtra("SCRIPT", currentScriptForPlayback)
                    }
                    androidx.core.content.ContextCompat.startForegroundService(context, intent)
                    
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(homeIntent)
                } else {
                    showOverlayPermissionExplain = true
                }
            }
        )
    }

    if (showOverlayPermissionExplain) {
        val currentScriptForPlayback = Script(
            id = existingScript?.id ?: 0,
            title = title.ifBlank { "Untitled" },
            content = content,
            folderName = selectedFolder,
            scrollSpeed = speed.toInt(),
            fontSize = fontSize.toInt(),
            isMirrored = isMirrored,
            textColor = textColor,
            bgOpacity = bgOpacity,
            textAlignment = textAlignment,
            lineSpacing = lineSpacing,
            textDirection = textDirectionState,
            createdAt = existingScript?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        OverlayPermissionExplanationDialog(
            onDismiss = { showOverlayPermissionExplain = false },
            onForceGrant = {
                showOverlayPermissionExplain = false
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )
    }

    if (showPlaybackPreview) {
        val currentScriptForPlayback = Script(
            id = existingScript?.id ?: 0,
            title = title.ifBlank { "Untitled" },
            content = content,
            folderName = selectedFolder,
            scrollSpeed = speed.toInt(),
            fontSize = fontSize.toInt(),
            isMirrored = isMirrored,
            textColor = textColor,
            bgOpacity = bgOpacity,
            textAlignment = textAlignment,
            lineSpacing = lineSpacing,
            textDirection = textDirectionState,
            createdAt = existingScript?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        TeleprompterPlaybackDialog(
            script = currentScriptForPlayback,
            isPracticeMode = isPlaybackPracticeMode,
            onDismiss = { showPlaybackPreview = false }
        )
    }

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
