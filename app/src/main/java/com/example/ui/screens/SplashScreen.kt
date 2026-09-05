package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (hasSeenOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    // Read preferences to decide which screen to head into next
    val prefs = remember {
        context.getSharedPreferences("cueflow_prefs", Context.MODE_PRIVATE)
    }
    val hasSeenOnboarding = remember {
        prefs.getBoolean("has_seen_onboarding", false)
    }

    LaunchedEffect(key1 = true) {
        startAnimation = true
        // Delay of 1300ms for premium aesthetic timing
        delay(1300)
        onSplashFinished(hasSeenOnboarding)
    }

    // Interactive scale animation
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    // Interactive alpha transition
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "logo_alpha"
    )

    // Subtle pulsing ambient light effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        // Glowing brand colored backdrop
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scale * 1.2f)
                .alpha(alpha * 0.15f * pulseAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ElectricPurple, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Premium Vector Representative of the Custom CueFlow App Icon
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                
                // BACKDROP SCREEN LAYER (Represents background recording context)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = (-8).dp, y = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Stylized camera reticle center
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(ElectricCyan.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), CircleShape)
                    )
                }

                // FOREGROUND FLOATING CARD LAYER (The Teleprompter window)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .offset(x = 8.dp, y = (-8).dp)
                        .graphicsLayer {
                            shadowElevation = 18f
                            shape = RoundedCornerShape(18.dp)
                            clip = true
                        }
                        .background(Color(0xFF0D0F16))
                        .border(2.5.dp, ElectricPurple, RoundedCornerShape(18.dp))
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Title header line with circular status light
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(ElectricCyan, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Text line 1 (Warm text)
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                        )

                        // Text line 2 (Active Cyan highlighting line with upward scroll representation)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(5.dp)
                                    .background(ElectricCyan, RoundedCornerShape(2.dp))
                            )
                            
                            // Visual Upward Scroll arrow
                            Canvas(modifier = Modifier.size(8.dp)) {
                                val strokeW = 1.5.dp.toPx()
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width * 0.1f, size.height * 0.6f)
                                    lineTo(size.width * 0.5f, size.height * 0.15f)
                                    lineTo(size.width * 0.9f, size.height * 0.6f)
                                    moveTo(size.width * 0.5f, size.height * 0.15f)
                                    lineTo(size.width * 0.5f, size.height * 0.9f)
                                }
                                drawPath(path, ElectricCyan, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeW))
                            }
                        }

                        // Text line 3 (Subtle upcoming text)
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(5.dp)
                                .background(Color(0xFF94A3B8), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Brand app name with fade and scale animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .alpha(alpha)
                    .graphicsLayer {
                        translationY = (1.0f - scale) * 30f
                    }
            ) {
                Text(
                    text = "CueFlow",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    color = SlateTextPrimary,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Your script, anywhere on screen.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ElectricCyan,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
