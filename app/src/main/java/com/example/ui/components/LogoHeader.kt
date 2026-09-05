package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@Composable
fun LogoHeader(
    modifier: Modifier = Modifier,
    onVoiceRecordClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(ElectricPurple, ElectricCyan))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column {
                Text(
                    text = "CueFlow",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.testTag("app_brand_title"),
                )
                Text(
                    text = "Teleprompter Studio",
                    fontSize = 11.sp,
                    color = SlateTextMuted,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onVoiceRecordClick != null) {
                IconButton(
                    onClick = onVoiceRecordClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CosmicSurfaceElevated)
                        .testTag("home_dictate_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Dictate a script",
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CosmicSurfaceElevated)
                        .testTag("home_settings_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open settings",
                        tint = SlateTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
