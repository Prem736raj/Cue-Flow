package com.example

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.Script
import com.example.ui.ScriptViewModel
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme

import android.view.KeyEvent
import com.example.util.HardwareButtonController

class MainActivity : ComponentActivity() {
    private var isLongPressTriggered = false
    private var lastPlayPauseClickTime = 0L
    private var pendingQuickFloatingScript: Script? = null

    private val voiceSyncPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableFloatingVoiceSync()
        } else {
            Toast.makeText(
                this,
                "Voice Sync stays off until microphone access is allowed.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val script = pendingQuickFloatingScript
        pendingQuickFloatingScript = null
        if (!isGranted) {
            Toast.makeText(
                this,
                "Floating Mode will work, but its notification controls may be hidden.",
                Toast.LENGTH_LONG
            ).show()
        }
        if (script != null) {
            launchFloatingService(script)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PerformanceBalancer.initialize(applicationContext)
        com.example.ui.theme.ThemeState.initialize(applicationContext)
        com.example.util.LanguageManager.initialize(applicationContext)
        
        val prefs = getSharedPreferences("cueflow_prefs", MODE_PRIVATE)
        HardwareButtonController.isEnabled = prefs.getBoolean("hardware_buttons_enabled", false)
        com.example.util.RemoteClickerManager.initialize(applicationContext)
        
        if (intent?.action == "com.example.action.QUICK_FLOAT") {
            startQuickFloatingMode()
            return
        }

        val shouldRequestVoiceSyncPermission =
            intent?.action == com.example.service.FloatingPrompterService.ACTION_REQUEST_VOICE_SYNC_PERMISSION

        setupDynamicShortcuts()
        
        val hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ScriptViewModel = viewModel()
                    val navController = rememberNavController()

                    androidx.compose.runtime.DisposableEffect(navController) {
                        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, arguments ->
                            val route = destination.route ?: ""
                            val editorMatch = route.startsWith("editor")
                            val editorId = if (editorMatch) {
                                arguments?.getInt("scriptId") ?: -1
                            } else {
                                -1
                            }
                            // Store current screen layout states to auto resume if app gets restarted or exits abruptly
                            prefs.edit().apply {
                                putString("last_route", route)
                                putInt("last_editor_script_id", editorId)
                                apply()
                            }
                        }
                        navController.addOnDestinationChangedListener(listener)
                        onDispose {
                            navController.removeOnDestinationChangedListener(listener)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable(
                            route = "splash",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(400)) }
                        ) {
                            com.example.ui.screens.SplashScreen(
                                onSplashFinished = { seen ->
                                    if (seen) {
                                        val lastRoute = prefs.getString("last_route", "home") ?: "home"
                                        val lastScriptId = prefs.getInt("last_editor_script_id", -1)
                                        
                                        if (lastRoute.startsWith("editor")) {
                                            navController.navigate("home") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                            navController.navigate("editor/$lastScriptId")
                                        } else if (lastRoute == "onboarding") {
                                            navController.navigate("onboarding") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("home") {
                                                popUpTo("splash") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        navController.navigate("onboarding") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "onboarding",
                            enterTransition = { fadeIn(animationSpec = tween(300)) },
                            exitTransition = { fadeOut(animationSpec = tween(300)) }
                        ) {
                            com.example.ui.screens.OnboardingScreen(
                                onFinish = { createFirstScript ->
                                    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
                                    if (createFirstScript) {
                                        navController.navigate("home") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                        navController.navigate("editor/-1")
                                    } else {
                                        navController.navigate("home") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "home",
                            enterTransition = {
                                fadeIn(animationSpec = tween(350))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            }
                        ) {
                            HomeScreen(
                                viewModel = viewModel,
                                onCreateScript = {
                                    navController.navigate("editor/-1")
                                },
                                onEditScript = { scriptId ->
                                    navController.navigate("editor/$scriptId")
                                }
                            )
                        }

                        composable(
                            route = "editor/{scriptId}",
                            arguments = listOf(navArgument("scriptId") { type = NavType.IntType }),
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            },
                            popEnterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(350)
                                ) + fadeIn(animationSpec = tween(350))
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(350)
                                ) + fadeOut(animationSpec = tween(350))
                            }
                        ) { backStackEntry ->
                            val scriptId = backStackEntry.arguments?.getInt("scriptId") ?: -1
                            EditorScreen(
                                viewModel = viewModel,
                                scriptId = scriptId,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }

        if (shouldRequestVoiceSyncPermission) {
            window.decorView.post { requestVoiceSyncPermission() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == com.example.service.FloatingPrompterService.ACTION_REQUEST_VOICE_SYNC_PERMISSION) {
            window.decorView.post { requestVoiceSyncPermission() }
        }
    }

    private fun requestVoiceSyncPermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            enableFloatingVoiceSync()
        } else {
            voiceSyncPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun enableFloatingVoiceSync() {
        val intent = Intent(this, com.example.service.FloatingPrompterService::class.java).apply {
            action = com.example.service.FloatingPrompterService.ACTION_ENABLE_VOICE_SYNC
        }
        startService(intent)
    }

    private fun setupDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val shortcutManager = getSystemService(ShortcutManager::class.java) ?: return
            
            val intent = Intent(this, MainActivity::class.java).apply {
                action = "com.example.action.QUICK_FLOAT"
            }
            
            val shortcut = ShortcutInfo.Builder(this, "shortcut_quick_float")
                .setShortLabel("Quick Float")
                .setLongLabel("Start Floating Prompter")
                .setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_slideshow))
                .setIntent(intent)
                .build()
                
            try {
                shortcutManager.dynamicShortcuts = listOf(shortcut)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startQuickFloatingMode() {
        val database = com.example.data.ScriptDatabase.getDatabase(applicationContext)
        val repository = com.example.data.ScriptRepository(database.scriptDao)
        
        lifecycleScope.launch {
            val scripts = withTimeoutOrNull(2000) {
                repository.allScripts.first()
            }
            val scriptToLaunch = scripts?.firstOrNull() ?: com.example.data.Script(
                title = "CueFlow Quickstart",
                content = "Welcome to CueFlow Floating overlay!\n\nWrite some scripts in the app, click they to start dynamic overlay, and configure sizes, transparency and colors directly from the settings drawer.",
                createdAt = System.currentTimeMillis()
            )
            
            launchFloatingService(scriptToLaunch)
        }
    }

    private fun launchFloatingService(script: Script) {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }

        val prefs = getSharedPreferences("cueflow_prefs", MODE_PRIVATE)
        val notificationPermissionNeeded =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED &&
                !prefs.getBoolean("notification_permission_prompted", false)

        if (notificationPermissionNeeded) {
            prefs.edit().putBoolean("notification_permission_prompted", true).apply()
            pendingQuickFloatingScript = script
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val serviceIntent = Intent(this, com.example.service.FloatingPrompterService::class.java).apply {
            putExtra("SCRIPT", script)
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (HardwareButtonController.isEnabled && HardwareButtonController.hasActiveListeners()) {
            if (com.example.util.RemoteClickerManager.handleKeyCode(keyCode)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (HardwareButtonController.isEnabled && HardwareButtonController.hasActiveListeners()) {
            val action = com.example.util.RemoteClickerManager.getActionForKey(keyCode)
            if (action != com.example.util.ClickerAction.NONE) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

object PerformanceBalancer {
    var isLowResourceDevice = false
        private set

    fun initialize(context: android.content.Context) {
        val amReal = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        isLowResourceDevice = if (amReal != null) {
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            amReal.getMemoryInfo(memoryInfo)
            memoryInfo.lowMemory || (android.os.Build.VERSION.SDK_INT >= 19 && amReal.isLowRamDevice)
        } else {
            false
        }
    }
}

