package com.example.data

import android.content.Context
import androidx.compose.ui.graphics.Color

data class FloatingConfigs(
    val textSize: Float,
    val textColorName: String,
    val bgColorName: String,
    val bgOpacity: Float,
    val windowOpacity: Float,
    val defaultGravity: String
)

object FloatingSettings {
    const val PREFS_NAME = "cueflow_prefs"
    
    val TEXT_COLORS = listOf("White", "Electric Cyan", "Electric Purple", "Warm Amber", "Neon Green")
    val BG_COLORS = listOf("Cosmic Slate", "Dark Obsidian", "Navy Depths", "Velvet Plum")
    val POSITION_GRAVITIES = listOf("Top Half", "Center", "Bottom Half")

    fun getConfigs(context: Context): FloatingConfigs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FloatingConfigs(
            textSize = prefs.getFloat("floating_text_size_v3", 18f),
            textColorName = prefs.getString("floating_text_color_v3", "White") ?: "White",
            bgColorName = prefs.getString("floating_bg_color_v3", "Cosmic Slate") ?: "Cosmic Slate",
            bgOpacity = prefs.getFloat("floating_bg_opacity_v3", 0.85f),
            windowOpacity = prefs.getFloat("floating_window_opacity_v3", 1.0f),
            defaultGravity = prefs.getString("floating_default_gravity_v3", "Center") ?: "Center"
        )
    }

    fun saveConfigs(context: Context, configs: FloatingConfigs) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("floating_text_size_v3", configs.textSize)
            putString("floating_text_color_v3", configs.textColorName)
            putString("floating_bg_color_v3", configs.bgColorName)
            putFloat("floating_bg_opacity_v3", configs.bgOpacity)
            putFloat("floating_window_opacity_v3", configs.windowOpacity)
            putString("floating_default_gravity_v3", configs.defaultGravity)
            apply()
        }
    }

    fun mapTextColor(name: String): Color {
        return when (name) {
            "Electric Cyan" -> Color(0xFF00E5FF)
            "Electric Purple" -> Color(0xFF9D4EDD)
            "Warm Amber" -> Color(0xFFFFB703)
            "Neon Green" -> Color(0xFF00E676)
            else -> Color.White
        }
    }

    fun mapBgColor(name: String): Color {
        return when (name) {
            "Dark Obsidian" -> Color(0xFF0E0826)
            "Navy Depths" -> Color(0xFF0D1B2A)
            "Velvet Plum" -> Color(0xFF1E0A3C)
            else -> Color(0xFF16113A) // Slate theme
        }
    }
}
