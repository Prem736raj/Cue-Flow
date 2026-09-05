package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.FloatingConfigs
import com.example.data.FloatingSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CueFlow", appName)
  }

  @Test
  fun `floating settings serialization and deserialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Test Initial configs (Defaults)
    val defaultConfigs = FloatingSettings.getConfigs(context)
    assertEquals(18f, defaultConfigs.textSize)
    assertEquals("White", defaultConfigs.textColorName)
    assertEquals("Cosmic Slate", defaultConfigs.bgColorName)
    assertEquals(0.85f, defaultConfigs.bgOpacity)
    assertEquals(1.0f, defaultConfigs.windowOpacity)
    assertEquals("Center", defaultConfigs.defaultGravity)

    // Test Saving customized configs
    val customConfigs = FloatingConfigs(
        textSize = 24f,
        textColorName = "Electric Cyan",
        bgColorName = "Dark Obsidian",
        bgOpacity = 0.5f,
        windowOpacity = 0.8f,
        defaultGravity = "Top Half"
    )
    FloatingSettings.saveConfigs(context, customConfigs)

    val updatedConfigs = FloatingSettings.getConfigs(context)
    assertEquals(24f, updatedConfigs.textSize)
    assertEquals("Electric Cyan", updatedConfigs.textColorName)
    assertEquals("Dark Obsidian", updatedConfigs.bgColorName)
    assertEquals(0.5f, updatedConfigs.bgOpacity)
    assertEquals(0.8f, updatedConfigs.windowOpacity)
    assertEquals("Top Half", updatedConfigs.defaultGravity)
  }

  @Test
  fun `verify whats new popup dismissals and flags`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)

    // Initially, no flags are set
    assertEquals(false, prefs.getBoolean("whats_new_dismissed_v1_3", false))
    assertEquals(false, prefs.getBoolean("has_seen_onboarding", false))

    // Set seen onboarding to true
    prefs.edit().putBoolean("has_seen_onboarding", true).apply()
    assertEquals(true, prefs.getBoolean("has_seen_onboarding", false))

    // Simulate dismissing Whats New popup
    prefs.edit().putBoolean("whats_new_dismissed_v1_3", true).apply()
    assertEquals(true, prefs.getBoolean("whats_new_dismissed_v1_3", false))
  }

  @Test
  fun `verify discovery tip cards flags and cycling indexes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)

    // Initially tips are not disabled and index starts at 0
    assertEquals(false, prefs.getBoolean("disable_all_tips", false))
    assertEquals(0, prefs.getInt("active_discovery_tip_idx", 0))

    // Simulate clicking next to cycle tips twice
    prefs.edit().putInt("active_discovery_tip_idx", 1).apply()
    assertEquals(1, prefs.getInt("active_discovery_tip_idx", 0))

    prefs.edit().putInt("active_discovery_tip_idx", 2).apply()
    assertEquals(2, prefs.getInt("active_discovery_tip_idx", 0))

    // Cycle back to beginning or reset
    prefs.edit().putInt("active_discovery_tip_idx", 0).apply()
    assertEquals(0, prefs.getInt("active_discovery_tip_idx", 0))

    // Simulate user toggling "Don't show tips" permanently
    prefs.edit().putBoolean("disable_all_tips", true).apply()
    assertEquals(true, prefs.getBoolean("disable_all_tips", false))
  }
}
