package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Script
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScriptCard(
    script: Script,
    onPlayClick: (Script) -> Unit,
    onFloatingQuickLaunch: (Script) -> Unit,
    onEditClick: (Script) -> Unit,
    onDeleteClick: (Script) -> Unit,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    // Math indicators
    val wordCount = script.content.split("\\s+".toRegex()).count { it.isNotBlank() }
    
    // Exact 150 wpm reading time calculation
    val readingWpm = 150
    val readingMinutes = (wordCount + readingWpm - 1) / readingWpm
    val durationText = if (wordCount == 0) "0 min read" else "$readingMinutes ${if (readingMinutes == 1) "min" else "mins"} read"

    val friendlyTimeStr = getRelativeTimeSpan(script.updatedAt)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        CosmicBorder.copy(alpha = 0.9f),
                        CosmicBorder.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = { onEditClick(script) },
                onLongClick = { onDeleteClick(script) }
            )
            .testTag("script_card_${script.id}"),
        colors = CardDefaults.cardColors(
            containerColor = CosmicSurface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Title & Quick Run Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SearchHighlightedText(
                        text = script.title.ifBlank { "Untitled Script" },
                        query = searchQuery,
                        baseColor = SlateTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Updated $friendlyTimeStr",
                        fontSize = 11.sp,
                        color = SlateTextMuted
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Launch Floating Button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricPurple.copy(alpha = 0.12f))
                            .border(1.dp, ElectricPurple.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .clickable { onFloatingQuickLaunch(script) }
                            .testTag("quick_float_button_${script.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Quick Launch Floating Mode",
                            tint = ElectricPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play/Launch Button with studio cyan gradient
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricCyan, ElectricCyan.copy(alpha = 0.8f))
                                )
                            )
                            .clickable { onPlayClick(script) }
                            .testTag("play_button_${script.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Teleprompter",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body Content Truncated Overlap
            SearchHighlightedText(
                text = script.content.ifBlank { "(No script content yet — tap to edit)" },
                query = searchQuery,
                baseColor = SlateTextSecondary,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.8.dp)
                    .background(CosmicBorder)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row & action controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Info badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge: Word count
                    InfoPill(
                        icon = Icons.Default.Description,
                        text = "$wordCount words",
                        color = ElectricPurple
                    )

                    // Badge: Chrono estimates
                    InfoPill(
                        icon = Icons.Default.Schedule,
                        text = durationText,
                        color = ElectricCyan
                    )

                    // Badge Indicator if Mirrored
                    if (script.isMirrored) {
                        InfoPill(
                            icon = Icons.Default.Flip,
                            text = "Mirror",
                            color = WarmAmber
                        )
                    }
                }

                // Edit/Delete interactive buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onEditClick(script) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Script",
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(script) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_button_${script.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Script",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.09f), RoundedCornerShape(6.dp))
            .border(0.75.dp, color.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getRelativeTimeSpan(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 60_000) {
        return "Just now"
    }
    val minutes = diff / 60_000
    if (minutes < 60) {
        return "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
    }
    val hours = minutes / 60
    if (hours < 24) {
        return "$hours ${if (hours == 1L) "hour" else "hours"} ago"
    }
    val days = hours / 24
    if (days == 1L) {
        return "Yesterday"
    }
    if (days < 7) {
        return "$days days ago"
    }
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SearchHighlightedText(
    text: String,
    query: String,
    baseColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    modifier: Modifier = Modifier
) {
    if (query.isBlank()) {
        Text(
            text = text,
            color = baseColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
        return
    }

    val annotatedString = remember(text, query) {
        buildAnnotatedString {
            var startIdx = 0
            while (true) {
                val index = text.indexOf(query, startIdx, ignoreCase = true)
                if (index == -1) {
                    append(text.substring(startIdx))
                    break
                }
                append(text.substring(startIdx, index))
                withStyle(style = SpanStyle(background = Color(0xFFEAB308).copy(alpha = 0.35f), color = Color.White, fontWeight = FontWeight.Bold)) {
                    append(text.substring(index, index + query.length))
                }
                startIdx = index + query.length
            }
        }
    }

    Text(
        text = annotatedString,
        color = baseColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}


