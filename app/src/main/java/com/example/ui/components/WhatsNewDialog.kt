package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CosmicBackground
import com.example.ui.theme.CosmicBorder
import com.example.ui.theme.CosmicSurface
import com.example.ui.theme.CosmicSurfaceElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary
import com.example.ui.theme.WarmAmber

@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.94f))
                .testTag("whats_new_dialog_container"),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .testTag("whats_new_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, CosmicBorder),
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = "CUEFLOW 1.0",
                        color = ElectricPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "Built for distraction-free prompting",
                        color = SlateTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Your scripts stay local by default, while optional camera, speech, import and remote features activate only when you choose them.",
                        color = SlateTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                    )

                    HorizontalDivider(color = CosmicBorder)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WhatsNewFeatureRow(
                            icon = Icons.Default.FlipToFront,
                            iconBg = ElectricPurple,
                            title = "Floating teleprompter",
                            desc = "Drag and resize a recoverable overlay above camera, social, meeting, or streaming apps.",
                        )
                        WhatsNewFeatureRow(
                            icon = Icons.Default.FolderOpen,
                            iconBg = ElectricCyan,
                            title = "Offline script workspace",
                            desc = "Create, organize, bookmark, rehearse and edit scripts locally without an account.",
                        )
                        WhatsNewFeatureRow(
                            icon = Icons.Default.SettingsRemote,
                            iconBg = WarmAmber,
                            title = "Physical and Wi-Fi controls",
                            desc = "Use keyboard-style clickers or a temporary paired local-network remote during a prompting session.",
                        )
                        WhatsNewFeatureRow(
                            icon = Icons.Default.Security,
                            iconBg = ElectricCyan,
                            title = "Explicit online features",
                            desc = "Web imports and Android speech recognition are clearly identified instead of being presented as fully offline.",
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("whats_new_dismiss_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    ) {
                        Text("Start using CueFlow", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
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
    desc: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurfaceElevated, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconBg, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SlateTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = SlateTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
