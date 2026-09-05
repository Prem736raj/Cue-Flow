package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: (createFirstScript: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })
    
    // Gradient backgrounds based on the page
    val primaryColor = ElectricPurple
    val secondaryColor = ElectricCyan
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen_container")
    ) {
        // Decorative glowing ambient background gradients (Subtle and rich)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 0.35f
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val purpleB = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.15f),
                    radius = size.minDimension * 0.7f
                )
                val cyanB = Brush.radialGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.3f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.85f),
                    radius = size.minDimension * 0.7f
                )
                drawRect(purpleB)
                drawRect(cyanB)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < 3) {
                    Text(
                        text = "Skip",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onFinish(false)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("onboarding_skip_button")
                    )
                }
            }

            // Horizontal Pager for swipe steps
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                OnboardingPageContent(pageIndex = pageIndex)
            }

            // Footer navigation area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { idx ->
                        val isSelected = pagerState.currentPage == idx
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        val color = if (isSelected) primaryColor else CosmicBorder
                        
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(idx)
                                    }
                                }
                                .testTag("onboarding_indicator_dot_$idx")
                        )
                    }
                }

                // Action Button: "Next", or "Get Started" on the last page
                Box(contentAlignment = Alignment.Center) {
                    if (pagerState.currentPage == 3) {
                        Button(
                            onClick = { onFinish(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = CosmicBackground
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 2.dp
                            ),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("onboarding_get_started_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Create First Script",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Get Started",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = CosmicSurfaceElevated,
                                contentColor = primaryColor
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .border(1.dp, CosmicBorder, CircleShape)
                                .testTag("onboarding_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next page",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (pageIndex) {
            0 -> ScreenWelcome()
            1 -> ScreenCoreTelemetry()
            2 -> ScreenKillerFloating()
            3 -> ScreenGetStarted()
        }
    }
}

@Composable
fun ScreenWelcome() {
    // Beautiful Enter Animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val scaleValue by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "logo_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Glowing animated logo circle
        Box(
            modifier = Modifier
                .size(130.dp)
                .scale(scaleValue)
                .background(
                    Brush.radialGradient(
                        listOf(ElectricPurple.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
                .border(2.dp, ElectricPurple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "CueFlow Icon",
                tint = ElectricPurple,
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        rotationY = 15f
                    }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App Name
        Text(
            text = "CueFlow",
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            color = SlateTextPrimary,
            letterSpacing = (-1).sp,
            modifier = Modifier.animateContentSize()
        )

        // Subtitle slogan
        Text(
            text = "Your script, anywhere on screen.",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ElectricCyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Key Value Badges (Emphasize free, no login, offline)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OnboardingHighlightRow(
                icon = Icons.Default.Done,
                title = "100% Free",
                description = "All features are completely unlocked."
            )
            OnboardingHighlightRow(
                icon = Icons.Default.PersonOff,
                title = "No Registration Required",
                description = "Launch and starting instantly without accounts."
            )
            OnboardingHighlightRow(
                icon = Icons.Default.CloudOff,
                title = "Runs offline",
                description = "Private, highly secure & works without internet."
            )
        }
    }
}

@Composable
fun ScreenCoreTelemetry() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Beautiful Camera & Teleprompter scrolling Mockup Component
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicSurface)
                .border(1.5.dp, CosmicBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Camera feed visual backdrop (soft gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E1330), Color(0xFF0F0B1E))
                        )
                    )
            )

            // Dynamic background camera focus reticle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val thickness = 2.dp.toPx()
                val length = 16.dp.toPx()
                val inset = 16.dp.toPx()
                // Top Left
                drawLine(ElectricCyan, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + length, inset), thickness)
                drawLine(ElectricCyan, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset, inset + length), thickness)
                // Bottom Right
                drawLine(ElectricCyan, androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset), androidx.compose.ui.geometry.Offset(size.width - inset - length, size.height - inset), thickness)
                drawLine(ElectricCyan, androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset), androidx.compose.ui.geometry.Offset(size.width - inset, size.height - inset - length), thickness)
            }

            // Central Soft Camera representation (avatar circle)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .alpha(0.18f)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Scrolling script visor overlay layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CueFlow is the modern prompter",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                
                // Active focus highlight line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElectricPurple.copy(alpha = 0.25f))
                        .border(1.dp, ElectricPurple, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Scroll beautifully while recording",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Never forget your script lines again.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

            // Badge overlay with Camera Status
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .background(Color.Red.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.White, CircleShape)
                )
                Text(
                    text = "PROMPTER + CAM",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Camera & Teleprompter Sync",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Record clean videos looking directly at the lenses with a customizable, automated scrolling text script.",
            fontSize = 14.sp,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ScreenKillerFloating() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Overlay Mockup Representation showing stacked apps
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CosmicSurface)
                .border(1.5.dp, CosmicBorder, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // App Layout Backdrop (Grey mock design of IG or Snapchat)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mock header representing other app (e.g. YouTube/Instagram Live)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(24.dp).background(SlateTextMuted.copy(alpha = 0.3f), CircleShape))
                        Box(modifier = Modifier.size(48.dp, 8.dp).background(SlateTextMuted.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                    }
                    Box(
                        modifier = Modifier
                            .background(ElectricCyan.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", color = ElectricCyan, fontWeight = FontWeight.Black, fontSize = 8.sp)
                    }
                }

                // Mock body post content with overlapping network badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(CosmicSurfaceElevated, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Any Social or Video App",
                            color = SlateTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MockNetworkIcon(label = "Instagram", color = Color(0xFFE1306C))
                            MockNetworkIcon(label = "Zoom", color = Color(0xFF2D8CFF))
                            MockNetworkIcon(label = "Snapchat", color = Color(0xFFFFFC00), textColor = Color.Black)
                        }
                    }
                }
            }

            // The Floating Teleprompter overlay window that stands ON TOP
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .height(95.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        translationY = -12f
                        shadowElevation = 18f
                    },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, CosmicBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Header of the float with resize dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(ElectricPurple, CircleShape))
                            Text("CueFlow Overlay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                        }
                        Icon(imageVector = Icons.Default.OpenWith, contentDescription = null, tint = SlateTextSecondary, modifier = Modifier.size(10.dp))
                    }
                    HorizontalDivider(color = CosmicBorder)
                    
                    Text(
                        text = "Speak with flawless precision overlays on top of Instagram tags, TikTok videos, Zoom meetings, and YouTube!",
                        fontSize = 9.sp,
                        color = SlateTextSecondary,
                        lineHeight = 11.sp,
                        maxLines = 3
                    )
                }
            }

            // Glowing indicator beacon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, ElectricPurple.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Killer Floating Widget Mode",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Display a highly scalable, translucent text script overlay directly on top of Zoom, Snapchat, Instagram, YouTube, and Teams.",
            fontSize = 14.sp,
            color = SlateTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ScreenGetStarted() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        // Massive celebratory key trophy representation
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    Brush.radialGradient(
                        listOf(ElectricCyan.copy(alpha = 0.25f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(76.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome to CueFlow!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = SlateTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Let's level up your virtual speeches, content creations, and remote recordings instantly.",
                fontSize = 15.sp,
                color = SlateTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 22.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(CosmicSurfaceElevated, RoundedCornerShape(16.dp))
                .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "No trackers, fully self-contained offline storage.",
                    color = SlateTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OnboardingHighlightRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CosmicSurface, RoundedCornerShape(14.dp))
            .border(1.dp, CosmicBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(ElectricPurple.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricPurple,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = SlateTextSecondary
            )
        }
    }
}

@Composable
fun MockNetworkIcon(label: String, color: Color, textColor: Color = Color.White) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black
        )
    }
}
