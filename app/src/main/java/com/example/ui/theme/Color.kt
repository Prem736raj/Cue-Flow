package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import android.content.Context

// Observable State for active Theme & Custom Accent Color
object ThemeState {
    var currentTheme by mutableStateOf("dark")
    var customAccentColor by mutableStateOf<Color?>(null)

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        currentTheme = prefs.getString("app_theme", "dark") ?: "dark"
        val accentHex = prefs.getString("app_accent_hex", null)
        customAccentColor = if (accentHex != null && accentHex.startsWith("#")) {
            try {
                Color(android.graphics.Color.parseColor(accentHex))
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun saveTheme(context: Context, theme: String) {
        currentTheme = theme
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_theme", theme)
            .apply()
    }

    fun saveAccentColor(context: Context, color: Color?) {
        customAccentColor = color
        val hex = color?.let { String.format("#%06X", (0xFFFFFF and it.toArgb())) }
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_accent_hex", hex)
            .apply()
    }

    val currentBackground: Color
        get() = when (currentTheme) {
            "amoled" -> Color(0xFF000000)
            "light" -> Color(0xFFF8FAFC)
            "midnight" -> Color(0xFF080D1A)
            "sunset" -> Color(0xFF120A07)
            else -> Color(0xFF0B0D14) // Studio Dark Graphite
        }

    val currentSurface: Color
        get() = when (currentTheme) {
            "amoled" -> Color(0xFF0D0D0D)
            "light" -> Color(0xFFFFFFFF)
            "midnight" -> Color(0xFF0F1A30)
            "sunset" -> Color(0xFF1E100B)
            else -> Color(0xFF141724) // Studio Surface
        }

    val currentSurfaceElevated: Color
        get() = when (currentTheme) {
            "amoled" -> Color(0xFF171717)
            "light" -> Color(0xFFF1F5F9)
            "midnight" -> Color(0xFF182848)
            "sunset" -> Color(0xFF2C1811)
            else -> Color(0xFF1D2133) // Studio Elevated
        }

    val currentBorder: Color
        get() = when (currentTheme) {
            "amoled" -> Color(0xFF262626)
            "light" -> Color(0xFFE2E8F0)
            "midnight" -> Color(0xFF223A63)
            "sunset" -> Color(0xFF452419)
            else -> Color(0xFF262B3F) // Crisp Subtle Border
        }

    val currentPrimaryAccent: Color
        get() = customAccentColor ?: when (currentTheme) {
            "amoled" -> Color(0xFFA855F7)
            "light" -> Color(0xFF6366F1)
            "midnight" -> Color(0xFF38BDF8)
            "sunset" -> Color(0xFFF97316)
            else -> Color(0xFF8B5CF6) // Modern Studio Violet
        }

    val currentSecondaryAccent: Color
        get() = customAccentColor?.copy(alpha = 0.85f) ?: when (currentTheme) {
            "amoled" -> Color(0xFF2DD4BF)
            "light" -> Color(0xFF0284C7)
            "midnight" -> Color(0xFF00E5FF)
            "sunset" -> Color(0xFFFBBF24)
            else -> Color(0xFF38BDF8) // Modern Studio Sky Cyan
        }

    val currentTextPrimary: Color
        get() = when (currentTheme) {
            "light" -> Color(0xFF0F172A)
            "midnight" -> Color(0xFFF0F6FC)
            "sunset" -> Color(0xFFFFF7F4)
            else -> Color(0xFFF8FAFC)
        }

    val currentTextSecondary: Color
        get() = when (currentTheme) {
            "light" -> Color(0xFF475569)
            "midnight" -> Color(0xFF94A9C9)
            "sunset" -> Color(0xFFE5987E)
            else -> Color(0xFF94A3B8)
        }

    val currentTextMuted: Color
        get() = when (currentTheme) {
            "light" -> Color(0xFF94A3B8)
            "midnight" -> Color(0xFF5E7599)
            "sunset" -> Color(0xFFA16553)
            else -> Color(0xFF64748B)
        }
}

// Map getters dynamically to existing constants so no code needs to change
val CosmicBackground: Color get() = ThemeState.currentBackground
val CosmicSurface: Color get() = ThemeState.currentSurface
val CosmicSurfaceElevated: Color get() = ThemeState.currentSurfaceElevated
val CosmicBorder: Color get() = ThemeState.currentBorder

val ElectricPurple: Color get() = ThemeState.currentPrimaryAccent
val DeepViolet: Color get() = ThemeState.currentPrimaryAccent
val ElectricCyan: Color get() = ThemeState.currentSecondaryAccent
val WarmAmber: Color get() = if (ThemeState.currentTheme == "sunset") Color(0xFFFBBF24) else Color(0xFFFBBF24)

val SlateTextPrimary: Color get() = ThemeState.currentTextPrimary
val SlateTextSecondary: Color get() = ThemeState.currentTextSecondary
val SlateTextMuted: Color get() = ThemeState.currentTextMuted

// Compat
val Purple80: Color get() = ElectricPurple
val PurpleGrey80: Color get() = ElectricPurple.copy(alpha = 0.8f)
val Pink80: Color get() = ElectricCyan

val Purple40: Color get() = DeepViolet
val PurpleGrey40: Color get() = DeepViolet.copy(alpha = 0.8f)
val Pink40: Color get() = ElectricCyan
