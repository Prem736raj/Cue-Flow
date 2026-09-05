package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class DiscoveryTip(
    val id: String,
    val title: String,
    val text: String,
    val actionLabel: String,
    val actionType: String,
    val icon: ImageVector,
    val tintColor: Color
)

@Composable
fun FeatureDiscoveryTipCard(
    onActionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
    }

    var isPermanentlyDisabled by remember {
        mutableStateOf(prefs.getBoolean("disable_all_tips", false))
    }

    if (isPermanentlyDisabled) return

    val tipList = remember {
        listOf(
            DiscoveryTip(
                id = "floating_overlay",
                title = "Did you know?",
                text = "You can float the teleprompter directly over Instagram, Snapchat, Zoom, or YouTube while recording!",
                actionLabel = "Try Floating Mode",
                actionType = "floating",
                icon = Icons.Default.FlipToFront,
                tintColor = ElectricPurple
            ),
            DiscoveryTip(
                id = "ai_generation",
                title = "Need writing help?",
                text = "Draft clean, high-engaging prompt scripts automatically using our Gemini AI Script generator.",
                actionLabel = "Open AI Generator",
                actionType = "ai",
                icon = Icons.Default.AutoAwesome,
                tintColor = ElectricCyan
            ),
            DiscoveryTip(
                id = "smart_settings",
                title = "Reading Optimization",
                text = "Adjust text size, scrolling speed, line spacing, and amoled night themes from settings.",
                actionLabel = "Open Settings",
                actionType = "settings",
                icon = Icons.Default.Palette,
                tintColor = WarmAmber
            ),
            DiscoveryTip(
                id = "import_scripts",
                title = "Import Instantly",
                text = "Pasting or importing raw text/files converts word docs or PDF scripts directly into telemetry scripts.",
                actionLabel = "Import Script",
                actionType = "import",
                icon = Icons.Default.CloudUpload,
                tintColor = ElectricPurple
            )
        )
    }

    // Determine which tip to show next based on SharedPreferences or state cycling
    var activeTipIndex by remember {
        val lastIdx = prefs.getInt("active_discovery_tip_idx", 0)
        mutableStateOf(lastIdx % tipList.size)
    }

    var showCard by remember { mutableStateOf(true) }

    val activeTip = tipList[activeTipIndex]

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(CosmicBorder, activeTip.tintColor.copy(alpha = 0.25f))),
                    RoundedCornerShape(16.dp)
                )
                .testTag("discovery_tip_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Topic Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(activeTip.tintColor.copy(alpha = 0.12f), CircleShape)
                                .border(0.5.dp, activeTip.tintColor.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeTip.icon,
                                contentDescription = null,
                                tint = activeTip.tintColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = activeTip.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeTip.tintColor,
                            modifier = Modifier.testTag("discovery_tip_title")
                        )
                    }

                    // Next/Close action icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Skip / Next tip
                        IconButton(
                            onClick = {
                                val nextIdx = (activeTipIndex + 1) % tipList.size
                                activeTipIndex = nextIdx
                                prefs.edit().putInt("active_discovery_tip_idx", nextIdx).apply()
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("discovery_tip_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NavigateNext,
                                contentDescription = "Next Tip",
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Close (temporary hide)
                        IconButton(
                            onClick = { showCard = false },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("discovery_tip_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Tip",
                                tint = SlateTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Tip Body Paragraph
                Text(
                    text = activeTip.text,
                    fontSize = 12.sp,
                    color = SlateTextSecondary,
                    lineHeight = 16.sp,
                    modifier = Modifier.testTag("discovery_tip_text")
                )

                HorizontalDivider(color = CosmicBorder, thickness = 0.5.dp)

                // Actions toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't show tips",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                prefs.edit().putBoolean("disable_all_tips", true).apply()
                                isPermanentlyDisabled = true
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .testTag("discovery_tip_disable_permanently_button")
                    )

                    Button(
                        onClick = { onActionSelected(activeTip.actionType) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activeTip.tintColor,
                            contentColor = CosmicBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("discovery_tip_try_it_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = activeTip.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
