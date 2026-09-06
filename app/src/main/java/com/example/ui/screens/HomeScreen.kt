package com.example.ui.screens

import com.example.util.LanguageManager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.ui.components.bouncyClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.service.FloatingPrompterService
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Script
import com.example.data.Folder
import com.example.ui.ScriptViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: ScriptViewModel,
    onCreateScript: () -> Unit,
    onEditScript: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scripts by viewModel.allScripts.collectAsStateWithLifecycle()
    val folders by viewModel.allFolders.collectAsStateWithLifecycle()

    // Search and Folder states
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var folderToDelete by remember { mutableStateOf<com.example.data.Folder?>(null) }

    // Filtered and sorted scripts
    val filteredScripts = remember(scripts, searchQuery, selectedFolder) {
        scripts.filter { item ->
            val matchesFolder = selectedFolder == null || item.folderName == selectedFolder
            val matchesSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.content.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesSearch
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showVoiceToScriptDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE) }

    // Modals & States
    var activeScriptForPlayback by remember { mutableStateOf<Script?>(null) }
    var showRatingPromptDialog by remember { mutableStateOf(false) }
    var showModeSelectionForScript by remember { mutableStateOf<Script?>(null) }
    var showOverlayPermissionExplainForScript by remember { mutableStateOf<Script?>(null) }
    var isPlaybackPracticeMode by remember { mutableStateOf(false) }
    var scriptToDelete by remember { mutableStateOf<Script?>(null) }
    var pendingFloatingScript by remember { mutableStateOf<Script?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val script = pendingFloatingScript
        pendingFloatingScript = null
        if (!granted) {
            android.widget.Toast.makeText(
                context,
                "Floating Mode will work, but its notification controls may be hidden.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
        if (script != null) {
            val intent = Intent(context, FloatingPrompterService::class.java).apply {
                putExtra("SCRIPT", script)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
            context.startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun startFloatingService(script: Script) {
        if (!Settings.canDrawOverlays(context)) {
            showOverlayPermissionExplainForScript = script
            return
        }

        val notificationPermissionNeeded =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
                !prefs.getBoolean("notification_permission_prompted", false)

        if (notificationPermissionNeeded) {
            prefs.edit().putBoolean("notification_permission_prompted", true).apply()
            pendingFloatingScript = script
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val intent = Intent(context, FloatingPrompterService::class.java).apply {
            putExtra("SCRIPT", script)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
        context.startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground),
        containerColor = CosmicBackground,
        topBar = {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                LogoHeader(
                    onVoiceRecordClick = { showVoiceToScriptDialog = true },
                    onSettingsClick = { showSettingsDialog = true }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showImportDialog = true },
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 8.dp)
                    .clip(CircleShape)
                    .testTag("floating_add_button"),
                containerColor = ElectricPurple,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create or Import Script",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Instant Search and Categories HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Glassmorphic search field with clean borders
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(LanguageManager.get("search_hint"), color = SlateTextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = SlateTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = SlateTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary,
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = CosmicBorder,
                        focusedContainerColor = CosmicSurface,
                        unfocusedContainerColor = CosmicSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Smooth Horizontal Category list row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .testTag("folders_row")
                ) {
                    val allCount = scripts.size
                    item {
                        val isSelected = selectedFolder == null
                        val bg = if (isSelected) ElectricPurple.copy(alpha = 0.16f) else CosmicSurface
                        val borderCol = if (isSelected) ElectricPurple else CosmicBorder
                        val textCol = if (isSelected) ElectricPurple else SlateTextSecondary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                .clickable { selectedFolder = null }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("folder_chip_all")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = LanguageManager.get("all_scripts"),
                                    color = textCol,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Badge(
                                    containerColor = if (isSelected) ElectricPurple else CosmicSurfaceElevated,
                                    contentColor = if (isSelected) Color.White else SlateTextMuted
                                ) {
                                    Text("$allCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(folders, key = { it.name }) { folder ->
                        val isSelected = selectedFolder == folder.name
                        val folderCount = scripts.count { it.folderName == folder.name }
                        val bg = if (isSelected) ElectricPurple.copy(alpha = 0.16f) else CosmicSurface
                        val borderCol = if (isSelected) ElectricPurple else CosmicBorder
                        val textCol = if (isSelected) ElectricPurple else SlateTextSecondary
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(1.dp, borderCol, RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = { selectedFolder = folder.name },
                                    onLongClick = { folderToDelete = folder }
                                )
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("folder_chip_${folder.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = folder.name,
                                    color = textCol,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Badge(
                                    containerColor = if (isSelected) ElectricPurple else CosmicSurfaceElevated,
                                    contentColor = if (isSelected) Color.White else SlateTextMuted
                                ) {
                                    Text("$folderCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        IconButton(
                            onClick = { showCreateFolderDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CosmicSurface)
                                .border(1.dp, CosmicBorder, CircleShape)
                                .testTag("add_folder_chip_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Create new folder category",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Content Area Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (scripts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(vertical = 16.dp)
                        ) {
                            EmptyState(
                                onCreateClick = onCreateScript,
                                onImportClick = { showImportDialog = true },
                                onTemplateSelect = { title, content, speed, size ->
                                    viewModel.addScript(title, content, speed, size)
                                }
                            )
                        }
                    }
                } else if (filteredScripts.isEmpty()) {
                    // Empty search results state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty folder or search result",
                            tint = SlateTextMuted,
                            modifier = Modifier
                                .size(64.dp)
                                .padding(bottom = 12.dp)
                        )
                        Text(
                            text = "No matching scripts found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing your search query or selecting another category folder.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                searchQuery = ""
                                selectedFolder = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricPurple
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(LanguageManager.get("reset_filters"))
                        }
                    }
                } else {
                    // Display list of matching scripts
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("scripts_lazy_list"),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedFolder == null) LanguageManager.get("all_scripts") else "${LanguageManager.get("folders")}: $selectedFolder",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateTextPrimary,
                                    letterSpacing = 0.5.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .background(CosmicSurfaceElevated, RoundedCornerShape(20.dp))
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${filteredScripts.size} Shown",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricPurple
                                    )
                                }
                            }
                        }

                        // Order filteredScripts by most recently edited script first (updatedAt DESC)
                        val sortedFiltered = filteredScripts.sortedByDescending { it.updatedAt }

                        items(sortedFiltered, key = { item -> item.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        scriptToDelete = item
                                    }
                                    false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFEF4444).copy(alpha = 0.22f)
                                        else -> Color.Transparent
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(color)
                                            .border(
                                                1.dp,
                                                Color(0xFFEF4444).copy(alpha = 0.35f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 24.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Swipe to Delete",
                                                color = Color(0xFFEF4444),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Icon",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                enableDismissFromStartToEnd = false,
                                modifier = Modifier.animateItem(),
                                content = {
                                    ScriptCard(
                                        script = item,
                                        searchQuery = searchQuery,
                                        onPlayClick = { showModeSelectionForScript = it },
                                        onFloatingQuickLaunch = { script -> startFloatingService(script) },
                                        onEditClick = { onEditScript(item.id) },
                                        onDeleteClick = { scriptToDelete = item }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Choice Dialog for Practice Mode vs Real Promo vs Floating Overlay
    showModeSelectionForScript?.let { script ->
        PromptModeSelectionDialog(
            script = script,
            onDismiss = { showModeSelectionForScript = null },
            onModeSelected = { isPractice ->
                showModeSelectionForScript = null
                isPlaybackPracticeMode = isPractice
                activeScriptForPlayback = script
            },
            onFloatingSelected = {
                showModeSelectionForScript = null
                startFloatingService(script)
            }
        )
    }

    // Explanatory draw-over-apps authorization popup
    showOverlayPermissionExplainForScript?.let { script ->
        OverlayPermissionExplanationDialog(
            onDismiss = { showOverlayPermissionExplainForScript = null },
            onForceGrant = {
                showOverlayPermissionExplainForScript = null
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        )
    }

    // Playback Immersive Prompt Screen
    activeScriptForPlayback?.let { script ->
        TeleprompterPlaybackDialog(
            script = script,
            isPracticeMode = isPlaybackPracticeMode,
            onDismiss = {
                activeScriptForPlayback = null
                // Record session count
                val currentCount = prefs.getInt("successful_use_count", 0) + 1
                prefs.edit().putInt("successful_use_count", currentCount).apply()

                val hasRatedOrDismissed = prefs.getBoolean("has_dismissed_rate_or_rated", false)
                if (currentCount >= 3 && !hasRatedOrDismissed) {
                    showRatingPromptDialog = true
                }
            }
        )
    }

    // Beautiful Prompt Delete Confirmation Dialog
    scriptToDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { scriptToDelete = null },
            modifier = Modifier.testTag("delete_confirmation_dialog"),
            containerColor = CosmicSurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        text = LanguageManager.get("delete_script_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "${LanguageManager.get("delete_script_confirm")}\n\n(${script.title})",
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteScript(script)
                        scriptToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { scriptToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("Cancel", color = SlateTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showRatingPromptDialog) {
        Dialog(
            onDismissRequest = { 
                showRatingPromptDialog = false 
                prefs.edit().putBoolean("has_dismissed_rate_or_rated", true).apply()
            }
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
                            imageVector = Icons.Default.Star,
                            contentDescription = "Star Rating icon",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Enjoying CueFlow? 🎙️",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "You've successfully completed several rehearsals! If CueFlow helps you present with clean confidence, taking 10 seconds to rate us is the ultimate fuel for our independent creation. No ads, no watermarks, pure focus.",
                        fontSize = 12.sp,
                        color = SlateTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showRatingPromptDialog = false
                                prefs.edit().putBoolean("has_dismissed_rate_or_rated", true).apply()
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
                                        android.widget.Toast.makeText(context, "Play Store link unavailable.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Rate CueFlow on Play Store", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    showRatingPromptDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Later", color = SlateTextSecondary, fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    showRatingPromptDialog = false
                                    prefs.edit().putBoolean("has_dismissed_rate_or_rated", true).apply()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("No thanks", color = SlateTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false }
        )
    }

    // What's New Dialog State Launcher
    val updatePrefs = remember {
        context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE)
    }
    val updateHasSeenOnboarding = remember { updatePrefs.getBoolean("has_seen_onboarding", false) }
    val updateHasSeenWhatsNew = remember { updatePrefs.getBoolean("whats_new_dismissed_v1_0", false) }
    var showWhatsNewDialog by remember {
        mutableStateOf(updateHasSeenOnboarding && !updateHasSeenWhatsNew)
    }

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = {
                updatePrefs.edit().putBoolean("whats_new_dismissed_v1_0", true).apply()
                showWhatsNewDialog = false
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            containerColor = CosmicSurfaceElevated,
            title = {
                Text(
                    text = LanguageManager.get("create_folder_title"),
                    color = SlateTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text(LanguageManager.get("folder_name_hint"), color = SlateTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = SlateTextPrimary,
                            focusedTextColor = SlateTextPrimary,
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = CosmicBorder
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_create_folder_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            viewModel.createFolder(newFolderNameInput)
                            newFolderNameInput = ""
                        }
                        showCreateFolderDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_folder_button")
                ) {
                    Text(LanguageManager.get("create"), color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(LanguageManager.get("cancel"), color = SlateTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Delete Folder Confirmation Dialog
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = CosmicSurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        text = LanguageManager.get("delete_folder_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Delete folder \"${folder.name}\"? Scripts in this folder will be kept and moved back to All Scripts.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder)
                        if (selectedFolder == folder.name) {
                            selectedFolder = null
                        }
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier.testTag("confirm_delete_folder_button")
                ) {
                    Text(LanguageManager.get("delete"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(LanguageManager.get("cancel"), color = SlateTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImportText = { text, _ ->
                showImportDialog = false
                coroutineScope.launch {
                    val prefs = context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE)
                    val defSpeed = prefs.getFloat("default_speed", 5f).toInt()
                    val defFontSize = prefs.getFloat("default_font_size", 24f).toInt()
                    val defTextColor = prefs.getString("default_text_color", "#FFFFFF") ?: "#FFFFFF"
                    val defBgOpacity = prefs.getFloat("default_bg_opacity", 0.4f)
                    val defTextAlignment = prefs.getString("default_text_alignment", "left") ?: "left"
                    val defFolderSetting = prefs.getString("default_folder", "Unassigned") ?: "Unassigned"
                    val defFolder = if (defFolderSetting == "Unassigned" || defFolderSetting.isBlank()) null else defFolderSetting

                    val defaultTitle = "Imported Script"
                    val newScript = Script(
                        title = defaultTitle,
                        content = text,
                        scrollSpeed = defSpeed,
                        fontSize = defFontSize,
                        textColor = defTextColor,
                        bgOpacity = defBgOpacity,
                        textAlignment = defTextAlignment,
                        folderName = defFolder,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val insertedId = viewModel.saveScript(newScript)
                    onEditScript(insertedId.toInt())
                }
            },
            hasExistingText = false
        )
    }


    if (showVoiceToScriptDialog) {
        VoiceToScriptDialog(
            onDismiss = { showVoiceToScriptDialog = false },
            onScriptCreated = { scriptContent ->
                showVoiceToScriptDialog = false
                coroutineScope.launch {
                    val prefs = context.getSharedPreferences("cueflow_prefs", android.content.Context.MODE_PRIVATE)
                    val defSpeed = prefs.getFloat("default_speed", 5f).toInt()
                    val defFontSize = prefs.getFloat("default_font_size", 24f).toInt()
                    val defTextColor = prefs.getString("default_text_color", "#FFFFFF") ?: "#FFFFFF"
                    val defBgOpacity = prefs.getFloat("default_bg_opacity", 0.4f)
                    val defTextAlignment = prefs.getString("default_text_alignment", "left") ?: "left"
                    val defFolderSetting = prefs.getString("default_folder", "Unassigned") ?: "Unassigned"
                    val defFolder = if (defFolderSetting == "Unassigned" || defFolderSetting.isBlank()) null else defFolderSetting

                    val newScript = Script(
                        title = "Dictated Script",
                        content = scriptContent,
                        scrollSpeed = defSpeed,
                        fontSize = defFontSize,
                        textColor = defTextColor,
                        bgOpacity = defBgOpacity,
                        textAlignment = defTextAlignment,
                        folderName = defFolder,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    val insertedId = viewModel.saveScript(newScript)
                    onEditScript(insertedId.toInt())
                }
            }
        )
    }
}
