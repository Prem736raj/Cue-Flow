package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SlateTextPrimary

/**
 * Lightweight routing surface used while Compose resolves the first destination.
 *
 * Android already provides the platform launch splash. CueFlow does not add an artificial delay on
 * top of it, which keeps cold-start latency low on budget devices.
 */
@Composable
fun SplashScreen(
    onSplashFinished: (hasSeenOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasSeenOnboarding = remember(context) {
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
            .getBoolean("has_seen_onboarding", false)
    }

    LaunchedEffect(hasSeenOnboarding) {
        onSplashFinished(hasSeenOnboarding)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(ElectricPurple, ElectricCyan)),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = "CueFlow",
                color = SlateTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
