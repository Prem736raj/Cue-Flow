package com.example

import com.example.util.HardwareButtonController
import com.example.util.WifiRemoteServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExampleUnitTest {

    private class RecordingListener : HardwareButtonController.Listener {
        var speedUpCalls = 0
        var speedDownCalls = 0
        var playPauseCalls = 0
        var nextBookmarkCalls = 0
        var previousBookmarkCalls = 0

        override fun onSpeedUp() { speedUpCalls++ }
        override fun onSpeedDown() { speedDownCalls++ }
        override fun onPlayPause() { playPauseCalls++ }
        override fun onSkipToNextBookmark() { nextBookmarkCalls++ }
        override fun onPrevBookmark() { previousBookmarkCalls++ }
    }

    private lateinit var listener: RecordingListener

    @Before
    fun setUp() {
        listener = RecordingListener()
        HardwareButtonController.isEnabled = false
        HardwareButtonController.register(listener)
    }

    @After
    fun tearDown() {
        HardwareButtonController.unregister(listener)
        HardwareButtonController.isEnabled = false
    }

    @Test
    fun `disabled hardware controls ignore ordinary dispatch`() {
        HardwareButtonController.dispatchPlayPause()
        HardwareButtonController.dispatchSpeedUp()
        HardwareButtonController.dispatchSpeedDown()
        HardwareButtonController.dispatchSkipToNextBookmark()
        HardwareButtonController.dispatchPrevBookmark()

        assertEquals(0, listener.playPauseCalls)
        assertEquals(0, listener.speedUpCalls)
        assertEquals(0, listener.speedDownCalls)
        assertEquals(0, listener.nextBookmarkCalls)
        assertEquals(0, listener.previousBookmarkCalls)
    }

    @Test
    fun `enabled hardware controls dispatch each mapped action`() {
        HardwareButtonController.isEnabled = true

        HardwareButtonController.dispatchPlayPause()
        HardwareButtonController.dispatchSpeedUp()
        HardwareButtonController.dispatchSpeedDown()
        HardwareButtonController.dispatchSkipToNextBookmark()
        HardwareButtonController.dispatchPrevBookmark()

        assertEquals(1, listener.playPauseCalls)
        assertEquals(1, listener.speedUpCalls)
        assertEquals(1, listener.speedDownCalls)
        assertEquals(1, listener.nextBookmarkCalls)
        assertEquals(1, listener.previousBookmarkCalls)
    }

    @Test
    fun `authenticated remote dispatch can force controls without enabling physical buttons`() {
        HardwareButtonController.dispatchPlayPause(force = true)
        HardwareButtonController.dispatchSpeedUp(force = true)

        assertEquals(1, listener.playPauseCalls)
        assertEquals(1, listener.speedUpCalls)
    }

    @Test
    fun `remote state model exposes playback progress without requiring server startup`() {
        HardwareButtonController.pActiveTitle = "Private title"
        HardwareButtonController.pIsPlaying = true
        HardwareButtonController.pSpeed = 4.5f
        HardwareButtonController.pCurrentParagraph = 3
        HardwareButtonController.pTotalParagraphs = 10

        assertEquals("Private title", HardwareButtonController.pActiveTitle)
        assertTrue(HardwareButtonController.pIsPlaying)
        assertEquals(4.5f, HardwareButtonController.pSpeed, 0.01f)
        assertEquals(3, HardwareButtonController.pCurrentParagraph)
        assertEquals(10, HardwareButtonController.pTotalParagraphs)
    }

    @Test
    fun `wifi remote uses stable local port and is opt in`() {
        assertEquals(8990, WifiRemoteServer.PORT)
        assertFalse(WifiRemoteServer.isRunning)
    }
}
