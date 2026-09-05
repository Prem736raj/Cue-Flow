package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.94f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("whats_new_dialog_container"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(ElectricPurple, ElectricCyan)),
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("whats_new_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Update Tag Header
                    Box(
                        modifier = Modifier
                            .background(ElectricPurple.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "UPDATE V1.3.0",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricPurple,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Dialog Header Slogans
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "What's New in CueFlow",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = SlateTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "We built amazing new tools to unleash your recordings.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

                    // Feature list (2-3 items)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WhatsNewFeatureRow(
                            icon = Icons.Default.FlipToFront,
                            iconBg = ElectricPurple,
                            title = "Enhanced Floating Window V2",
                            desc = "Draggable overlay interface with transparent controls. Speaks perfectly over YouTube, Snapchat, Zoom and Instagram!"
                        )

                        WhatsNewFeatureRow(
                            icon = Icons.Default.AutoAwesome,
                            iconBg = ElectricCyan,
                            title = "AI Prompt Generator",
                            desc = "Instantly draft creative, high-engaging video scripts tailored for social media reels, TikTok, or business pitches."
                        )

                        WhatsNewFeatureRow(
                            icon = Icons.Default.Palette,
                            iconBg = WarmAmber,
                            title = "Amoled & Creative Themes",
                            desc = "Aesthetic AMOLED pure pitch-black themes, customizable display font sizes, and warm accents customized for night-time speaking lists."
                        )
                    }

                    HorizontalDivider(color = CosmicBorder, thickness = 1.dp)

                    // Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPurple,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("whats_new_dismiss_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Awesome, Let's Go!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhatsNewFeatureRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, iconBg.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBg,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = SlateTextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}
