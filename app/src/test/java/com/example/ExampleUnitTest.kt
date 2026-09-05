package com.example

import org.junit.Assert.*
import org.junit.Test
import com.example.util.HardwareButtonController
import com.example.util.WifiRemoteServer

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testHardwareButtonControllerRemoteTrackingFields() {
    HardwareButtonController.pActiveTitle = "Sample News Presentation"
    HardwareButtonController.pIsPlaying = true
    HardwareButtonController.pSpeed = 4.5f
    HardwareButtonController.pCurrentParagraph = 3
    HardwareButtonController.pTotalParagraphs = 10

    assertEquals("Sample News Presentation", HardwareButtonController.pActiveTitle)
    assertTrue(HardwareButtonController.pIsPlaying)
    assertEquals(4.5f, HardwareButtonController.pSpeed, 0.01f)
    assertEquals(3, HardwareButtonController.pCurrentParagraph)
    assertEquals(10, HardwareButtonController.pTotalParagraphs)
  }

  @Test
  fun testWifiRemoteServerConfiguration() {
    assertEquals(8990, WifiRemoteServer.PORT)
    assertFalse(WifiRemoteServer.isRunning)
  }

  @Test
  fun testSmartScriptDurationEstimator() {
    // Word counts
    val content = "This is a premium teleprompter app designed to be extremely polished"
    val wordCount = content.split("\\s+".toRegex()).count { it.isNotBlank() }
    assertEquals(11, wordCount)
    
    // Character counts
    val charCount = content.length
    assertEquals(68, charCount)

    // At speed = 1.0 (1x), speaking speed is 30 WPM
    // Estimated time is: (11 words / 30 wpm) * 60 = 22 seconds
    val speedValue = 1.0f
    val speakingSpeedWpm = speedValue * 30f
    val totalSeconds = if (wordCount > 0) ((wordCount.toFloat() / speakingSpeedWpm) * 60f).toInt() else 0
    assertEquals(22, totalSeconds)

    // Format duration check
    val mins = totalSeconds / 60
    val secs = totalSeconds % 60
    val durationText = when {
        mins > 0 && secs > 0 -> "$mins min $secs sec"
        mins > 0 -> "$mins min"
        else -> "$secs sec"
    }
    assertEquals("22 sec", durationText)
  }
}
