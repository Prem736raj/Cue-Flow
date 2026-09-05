package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.FloatingConfigs
import com.example.data.FloatingSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun resetPreferences() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `launcher brand is CueFlow`() {
        assertEquals("CueFlow", context.getString(R.string.app_name))
    }

    @Test
    fun `floating settings persist supported overlay values`() {
        val defaults = FloatingSettings.getConfigs(context)
        assertEquals(18f, defaults.textSize)
        assertEquals("White", defaults.textColorName)
        assertEquals("Cosmic Slate", defaults.bgColorName)
        assertEquals(0.85f, defaults.bgOpacity)
        assertEquals(1.0f, defaults.windowOpacity)
        assertEquals("Center", defaults.defaultGravity)

        val custom = FloatingConfigs(
            textSize = 24f,
            textColorName = "Electric Cyan",
            bgColorName = "Dark Obsidian",
            bgOpacity = 0.5f,
            windowOpacity = 0.8f,
            defaultGravity = "Top Half",
        )
        FloatingSettings.saveConfigs(context, custom)

        assertEquals(custom, FloatingSettings.getConfigs(context))
    }

    @Test
    fun `onboarding and v1 whats new flags are independent`() {
        val prefs = context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        assertFalse(prefs.getBoolean("has_seen_onboarding", false))
        assertFalse(prefs.getBoolean("whats_new_dismissed_v1_0", false))

        prefs.edit().putBoolean("has_seen_onboarding", true).commit()
        assertTrue(prefs.getBoolean("has_seen_onboarding", false))
        assertFalse(prefs.getBoolean("whats_new_dismissed_v1_0", false))

        prefs.edit().putBoolean("whats_new_dismissed_v1_0", true).commit()
        assertTrue(prefs.getBoolean("whats_new_dismissed_v1_0", false))
    }

    @Test
    fun `discovery preferences keep their default and explicit values`() {
        val prefs = context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
        assertFalse(prefs.getBoolean("disable_all_tips", false))
        assertEquals(0, prefs.getInt("active_discovery_tip_idx", 0))

        prefs.edit()
            .putInt("active_discovery_tip_idx", 2)
            .putBoolean("disable_all_tips", true)
            .commit()

        assertEquals(2, prefs.getInt("active_discovery_tip_idx", 0))
        assertTrue(prefs.getBoolean("disable_all_tips", false))
    }
}
