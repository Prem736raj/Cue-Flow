package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun EmptyState(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    onTemplateSelect: (title: String, content: String, speed: Int, size: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Clean, Minimalist Studio Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ElectricPurple.copy(alpha = 0.18f),
                            ElectricCyan.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    1.dp,
                    ElectricPurple.copy(alpha = 0.35f),
                    RoundedCornerShape(20.dp)
                )
                .testTag("pulsing_emblem_box"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = "Scripts",
                tint = ElectricPurple,
                modifier = Modifier.size(36.dp)
            )
        }

        // Title and Description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "No Scripts Yet",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Create your first script, import existing notes, or start from a quick template.",
                fontSize = 13.sp,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .testTag("offline_disclaimer_text")
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Primary Action: Blank Script
            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("create_script_cta_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Create Blank Script",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }


            // Secondary import option
            TextButton(
                onClick = onImportClick,
                modifier = Modifier.testTag("import_script_cta_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = SlateTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Import from file or clipboard",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Starter Templates (Clean minimal list)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "QUICK TEMPLATES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            listOf(
                Triple(
                    "YouTube Camera Opener",
                    "Welcome back! In today's video we are breaking down key strategies for creating impactful content.",
                    6
                ),
                Triple(
                    "60-Second Elevator Pitch",
                    "Hello everyone. Today I am introducing a revolutionary tool that helps presenters speak with absolute confidence.",
                    5
                ),
                Triple(
                    "Keynote Presentation Intro",
                    "Good morning. Thank you all for joining us today as we reveal our latest milestone.",
                    4
                )
            ).forEach { (title, contents, speed) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmicSurface)
                        .border(1.dp, CosmicBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            onTemplateSelect(title, contents, speed, 24)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("template_chip_$title"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = contents,
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Use template",
                        tint = ElectricPurple,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
