package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun currentColorScheme(): ColorScheme {
    val isLight = ThemeState.currentTheme == "light"
    
    // Dynamic spring-loaded theme color transitions
    val primary by animateColorAsState(targetValue = ElectricPurple, animationSpec = tween(450), label = "primary")
    val background by animateColorAsState(targetValue = CosmicBackground, animationSpec = tween(450), label = "background")
    val surface by animateColorAsState(targetValue = CosmicSurface, animationSpec = tween(450), label = "surface")
    val surfaceVariant by animateColorAsState(targetValue = CosmicSurfaceElevated, animationSpec = tween(450), label = "surfaceVariant")
    val outline by animateColorAsState(targetValue = CosmicBorder, animationSpec = tween(450), label = "outline")
    val secondary by animateColorAsState(targetValue = ElectricCyan, animationSpec = tween(450), label = "secondary")
    val onBackground by animateColorAsState(targetValue = SlateTextPrimary, animationSpec = tween(450), label = "onBackground")
    val onSurface by animateColorAsState(targetValue = SlateTextPrimary, animationSpec = tween(450), label = "onSurface")
    val onSurfaceVariant by animateColorAsState(targetValue = SlateTextSecondary, animationSpec = tween(450), label = "onSurfaceVariant")

    return if (isLight) {
        lightColorScheme(
            primary = primary,
            onPrimary = background,
            secondary = secondary,
            onSecondary = background,
            tertiary = WarmAmber,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    } else {
        darkColorScheme(
            primary = primary,
            onPrimary = background,
            secondary = secondary,
            onSecondary = background,
            tertiary = WarmAmber,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline
        )
    }
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val isLight = ThemeState.currentTheme == "light"
    val backgroundArgb = CosmicBackground.toArgb()
    
    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = view.context
            while (currentContext is android.content.ContextWrapper && currentContext !is Activity) {
                currentContext = currentContext.baseContext
            }
            if (currentContext is Activity) {
                val window = currentContext.window
                window.statusBarColor = backgroundArgb
                window.navigationBarColor = backgroundArgb
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = currentColorScheme(),
        typography = Typography,
        content = content
    )
}
