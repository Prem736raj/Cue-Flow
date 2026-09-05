package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.ui.theme.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScriptGeneratorDialog(
    onDismiss: () -> Unit,
    onScriptAccepted: (title: String, content: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(GeneratorStep.INPUT_SETTINGS) }
    
    // Inputs
    var topicText by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf("1 minute") }
    var selectedTone by remember { mutableStateOf("Professional") }
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    
    // Output & States
    var generatedScriptText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var activeGenerationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Loading Text cycler
    var loadingMessageIndex by remember { mutableStateOf(0) }
    val loadingMessages = listOf(
        "Initiating Gemini brain sync...",
        "Hooking listener attention...",
        "Balancing conversational pitch...",
        "Structuring script blocks...",
        "Perfecting teleprompter delivery margins..."
    )
    
    LaunchedEffect(currentStep) {
        if (currentStep == GeneratorStep.GENERATING) {
            loadingMessageIndex = 0
            while (isActive && currentStep == GeneratorStep.GENERATING) {
                delay(2000)
                loadingMessageIndex = (loadingMessageIndex + 1) % loadingMessages.size
            }
        }
    }

    Dialog(
        onDismissRequest = { if (currentStep != GeneratorStep.GENERATING) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = (currentStep != GeneratorStep.GENERATING),
            dismissOnClickOutside = (currentStep != GeneratorStep.GENERATING),
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground.copy(alpha = 0.7f))
                .clickable(enabled = currentStep != GeneratorStep.GENERATING) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 680.dp)
                    .clickable(enabled = false) {}
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(ElectricPurple, ElectricCyan)),
                        RoundedCornerShape(24.dp)
                    )
                    .testTag("ai_generator_dialog_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicSurfaceElevated)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "AiGeneratorTransition"
                ) { step ->
                    when (step) {
                        GeneratorStep.INPUT_SETTINGS -> {
                            InputSettingsScreen(
                                topic = topicText,
                                onTopicChange = { topicText = it },
                                duration = selectedDuration,
                                onDurationSelect = { selectedDuration = it },
                                tone = selectedTone,
                                onToneSelect = { selectedTone = it },
                                platform = selectedPlatform,
                                onPlatformSelect = { selectedPlatform = it },
                                onCancel = onDismiss,
                                onGenerate = {
                                    if (topicText.isBlank()) {
                                        errorMessage = "Please enter a topic or subject to write about."
                                    } else {
                                        errorMessage = null
                                        currentStep = GeneratorStep.GENERATING
                                        activeGenerationJob = triggerGeminiGeneration(
                                            topic = topicText,
                                            duration = selectedDuration,
                                            tone = selectedTone,
                                            platform = selectedPlatform,
                                            onSuccess = { text ->
                                                generatedScriptText = text
                                                currentStep = GeneratorStep.PREVIEW_RESULT
                                                activeGenerationJob = null
                                            },
                                            onFailure = { err ->
                                                errorMessage = err
                                                currentStep = GeneratorStep.INPUT_SETTINGS
                                                activeGenerationJob = null
                                            },
                                            scope = coroutineScope
                                        )
                                    }
                                },
                                errorMessage = errorMessage
                            )
                        }
                        GeneratorStep.GENERATING -> {
                            GeneratingScreen(
                                currentMessage = loadingMessages[loadingMessageIndex],
                                onCancel = {
                                    activeGenerationJob?.cancel()
                                    errorMessage = "AI generation canceled by user."
                                    currentStep = GeneratorStep.INPUT_SETTINGS
                                    activeGenerationJob = null
                                }
                            )
                        }
                        GeneratorStep.PREVIEW_RESULT -> {
                            PreviewResultScreen(
                                topic = topicText,
                                scriptText = generatedScriptText,
                                onScriptTextChange = { generatedScriptText = it },
                                onUseScript = {
                                    val title = if (topicText.length > 25) topicText.take(22) + "..." else topicText
                                    onScriptAccepted("AI Script: $title", generatedScriptText)
                                },
                                onRegenerate = {
                                    currentStep = GeneratorStep.GENERATING
                                    activeGenerationJob = triggerGeminiGeneration(
                                        topic = topicText,
                                        duration = selectedDuration,
                                        tone = selectedTone,
                                        platform = selectedPlatform,
                                        onSuccess = { text ->
                                            generatedScriptText = text
                                            currentStep = GeneratorStep.PREVIEW_RESULT
                                        },
                                        onFailure = { err ->
                                            errorMessage = err
                                            currentStep = GeneratorStep.INPUT_SETTINGS
                                        },
                                        scope = coroutineScope
                                    )
                                },
                                onAdjustSettings = {
                                    currentStep = GeneratorStep.INPUT_SETTINGS
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class GeneratorStep {
    INPUT_SETTINGS,
    GENERATING,
    PREVIEW_RESULT
}

@Composable
fun InputSettingsScreen(
    topic: String,
    onTopicChange: (String) -> Unit,
    duration: String,
    onDurationSelect: (String) -> Unit,
    tone: String,
    onToneSelect: (String) -> Unit,
    platform: String,
    onPlatformSelect: (String) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    errorMessage: String?
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Generator Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ElectricPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Magic Icon",
                    tint = ElectricPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "AI Script Generator",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextPrimary
                )
                Text(
                    text = "Premium Content Engine",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarmAmber
                )
            }
        }

        Divider(color = CosmicBorder, thickness = 1.dp)

        // Internet Notice Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ElectricCyan.copy(alpha = 0.08f))
                .border(1.dp, ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Online feature",
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Requires Internet Connection",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
                Text(
                    text = "This specific feature streams from Gemini AI models. All other core features operate offline.",
                    fontSize = 10.sp,
                    color = SlateTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }

        // Error message if any
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE57373).copy(alpha = 0.15f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error icon",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF5350),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Topic TextField
        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChange,
            label = { Text("What do you want to talk about?", color = SlateTextSecondary) },
            placeholder = { Text("Ex: 5 tips to buy your first electric car...", color = SlateTextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_topic_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricPurple,
                unfocusedBorderColor = CosmicBorder,
                focusedLabelColor = ElectricPurple,
                unfocusedLabelColor = SlateTextSecondary,
                focusedTextColor = SlateTextPrimary,
                unfocusedTextColor = SlateTextPrimary,
                focusedContainerColor = CosmicSurface,
                unfocusedContainerColor = CosmicSurface
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 3
        )

        // Target Platform Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Target Platform / Display Screen",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("YouTube", "TikTok", "Instagram Reel", "Presentation").forEach { item ->
                    val isSelected = platform == item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                            .border(
                                1.dp,
                                if (isSelected) ElectricPurple else CosmicBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onPlatformSelect(item) }
                            .padding(vertical = 10.dp)
                            .testTag("platform_chip_$item"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ElectricPurple else SlateTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Expected Script Duration Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Approximate Duration / Length",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("30 seconds", "1 minute", "2 minutes", "5 minutes").forEach { item ->
                    val isSelected = duration == item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                            .border(
                                1.dp,
                                if (isSelected) ElectricPurple else CosmicBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onDurationSelect(item) }
                            .padding(vertical = 10.dp)
                            .testTag("duration_chip_$item"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ElectricPurple else SlateTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Tone & Delivery Style Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Delivery Tone & Presentation Style",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextSecondary,
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Professional", "Casual", "Funny", "Motivational").forEach { item ->
                    val isSelected = tone == item
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ElectricPurple.copy(alpha = 0.15f) else CosmicSurface)
                            .border(
                                1.dp,
                                if (isSelected) ElectricPurple else CosmicBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onToneSelect(item) }
                            .padding(vertical = 10.dp)
                            .testTag("tone_chip_$item"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) ElectricPurple else SlateTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ai_cancel_button"),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.linearGradient(listOf(CosmicBorder, CosmicBorder))
                ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlateTextSecondary)
            ) {
                Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onGenerate,
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("ai_generate_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(ElectricPurple, DeepViolet))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CosmicBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Synthesize Script",
                            color = CosmicBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingScreen(
    currentMessage: String,
    onCancel: () -> Unit
) {
    // Beautiful loader matching a spinning atomic radar orb
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSpinner")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = SineIntervalEasing()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulsingAlpha"
    )
    
    val lineSweepSpeed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepPercent"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Glowing Atomic Canvas
        Box(
            modifier = Modifier
                .size(160.dp)
                .testTag("glowing_ai_loader_canvas_box"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = center
                val baseRadius = size.minDimension / 2.3f

                // Outer Glowing Purple/Cyan Boundary Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricPurple.copy(alpha = 0.25f * ringAlpha), Color.Transparent),
                        center = center
                    ),
                    radius = baseRadius * 1.3f
                )

                // Stationary Base Ring
                drawCircle(
                    color = CosmicBorder,
                    radius = baseRadius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Scanning Arc Sweeper Visualizer
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(ElectricPurple.copy(alpha = 0f), ElectricCyan, ElectricPurple),
                        center = centerOffset
                    ),
                    startAngle = rotationAngle,
                    sweepAngle = 90f + (90f * lineSweepSpeed),
                    useCenter = false,
                    size = Size(baseRadius * 2, baseRadius * 2),
                    topLeft = Offset(center.x - baseRadius, center.y - baseRadius),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Interactive orbiting electrons (particles)
                val angleOneRad = Math.toRadians(rotationAngle.toDouble())
                val angleTwoRad = Math.toRadians((rotationAngle + 180f).toDouble())
                
                val particleOneOffset = Offset(
                    center.x + (baseRadius * cos(angleOneRad)).toFloat(),
                    center.y + (baseRadius * sin(angleOneRad)).toFloat()
                )
                val particleTwoOffset = Offset(
                    center.x + (baseRadius * cos(angleTwoRad)).toFloat(),
                    center.y + (baseRadius * sin(angleTwoRad)).toFloat()
                )

                // Particle Orbs
                drawCircle(
                    color = ElectricPurple,
                    radius = 6.dp.toPx(),
                    center = particleOneOffset
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = 4.dp.toPx(),
                    center = particleTwoOffset
                )
            }

            // Central Core Orb fading inside
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricPurple, ElectricPurple.copy(alpha = 0.3f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Fusing core logic icon",
                    tint = CosmicBackground,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Informative Text fields Loading
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Generating Your Script",
                fontSize = 18.sp,
                color = SlateTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Animated message ticker
            Text(
                text = currentMessage,
                fontSize = 13.sp,
                color = ElectricCyan,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .animateContentSize()
            )

            Text(
                text = "This usually takes 8-15 seconds depending on connection speeds.",
                fontSize = 11.sp,
                color = SlateTextSecondary,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCancel,
                border = BorderStroke(1.dp, CosmicBorder),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = SlateTextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(40.dp)
                    .testTag("ai_generator_cancel")
            ) {
                Text("Cancel Generation", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

class SineIntervalEasing : Easing {
    override fun transform(fraction: Float): Float {
        return (sin(fraction * Math.PI - Math.PI / 2) + 1).toFloat() / 2f
    }
}

@Composable
fun PreviewResultScreen(
    topic: String,
    scriptText: String,
    onScriptTextChange: (String) -> Unit,
    onUseScript: () -> Unit,
    onRegenerate: () -> Unit,
    onAdjustSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preview Header info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Success magic stars",
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Script Ready!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextPrimary
                    )
                    Text(
                        text = "Customize and write directly to teleprompter",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                }
            }

            // Adjust settings gear
            IconButton(
                onClick = onAdjustSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CosmicSurface)
                    .testTag("ai_adjust_settings_gears_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Adjust settings button",
                    tint = SlateTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Divider(color = CosmicBorder, thickness = 1.dp)

        // Editable result box scrollable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CosmicSurface)
                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
        ) {
            OutlinedTextField(
                value = scriptText,
                onValueChange = onScriptTextChange,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ai_generated_script_preview_editor"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = SlateTextPrimary,
                    unfocusedTextColor = SlateTextPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, lineHeight = 20.sp)
            )
        }

        // Action workflow CTAs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Regenerate button
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier
                    .weight(0.9f)
                    .height(48.dp)
                    .testTag("ai_regenerate_button"),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.linearGradient(listOf(ElectricCyan, ElectricCyan))
                ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Use Script button
            Button(
                onClick = onUseScript,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("ai_use_script_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(ElectricPurple, ElectricCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Approve script icon",
                            tint = CosmicBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Open in Editor",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CosmicBackground
                        )
                    }
                }
            }
        }
    }
}

private fun triggerGeminiGeneration(
    topic: String,
    duration: String,
    tone: String,
    platform: String,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit,
    scope: CoroutineScope
): kotlinx.coroutines.Job {
    return scope.launch(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Key checking safety validation
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.equals("placeholder", ignoreCase = true)) {
            withContext(Dispatchers.Main) {
                onFailure("Gemini API Key is not configured. Please use the Secrets Panel in the AI Studio editor to enter your GEMINI_API_KEY secure credential.")
            }
            return@launch
        }

        // Form prompt instructions to enforce Spoken Teleprompter-friendly styles
        val prompt = "Write a complete, well-structured script of spoken content for a $platform video/presentation. " +
                "The topic/subject of the talk is: \"$topic\". " +
                "The estimated speaking duration of the script should be approximately $duration. " +
                "The tone and style of delivery must be strictly $tone. " +
                "\n\nCRITICAL DELIVERY INSTRUCTIONS FOR TELEPROMPTER SCREEN:" +
                "\n- Only output the spoken content text itself." +
                "\n- DO NOT include scene numbers, camera directions, bracketed prompts, music triggers, colon cues, parenthesized descriptors, speaker names (like 'Host:', 'Presenter:'), stage directions, actor guides or blockings." +
                "\n- DO NOT add headers like 'Intro', 'Body', 'Outro', or 'Hook'." +
                "\n- The speaker must be able to read your response text verbatim from the teleprompter screen without seeing director notes or speaking helpers." +
                "\n- Segment the monologue content into punchy, highly readable paragraphs with comfortable spacing so it scrolls gracefully in dynamic teleprompter viewports."

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val jsonMedia = "application/json; charset=utf-8".toMediaType()

        try {
            // Build request JSON safely with JSONObject to handle escaping correctly
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            // We must STRICTLY use gemini-3.5-flash as mandated by supported models in security skill
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestJson.toString().toRequestBody(jsonMedia))
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string()
            
            if (!response.isSuccessful || rawResponse == null) {
                withContext(Dispatchers.Main) {
                    onFailure("API call failed with code ${response.code}. Please ensure your API key is correctly configured in AI Studio.")
                }
                return@launch
            }

            // Parse response content safely
            val responseObj = JSONObject(rawResponse)
            val candidates = responseObj.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val generatedText = firstPart?.optString("text")

            withContext(Dispatchers.Main) {
                if (!generatedText.isNullOrBlank()) {
                    onSuccess(generatedText)
                } else {
                    onFailure("No script text was returned by Gemini. Please try again with longer topics.")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onFailure("Could not connect to Gemini AI. This feature requires an active internet connection. All other core features of CueFlow operate fully offline, so you can edit and read your scripts completely offline without any internet connection!")
            }
        }
    }
}
