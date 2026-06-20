package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ContentCollection
import com.example.data.DownloadedVideo
import com.example.data.TrackedCreator
import com.example.ui.theme.PuretikRed
import com.example.ui.theme.PuretikTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.AccentYellow
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.*

@Composable
fun PuretikApp(viewModel: PuretikViewModel) {
    val navigationStack = viewModel.navigationStack
    val currentScreen = navigationStack.lastOrNull() ?: PuretikScreen.Home

    // Handle Hardware Back Press
    BackHandler(enabled = navigationStack.size > 1) {
        viewModel.navigateBack()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    PuretikScreen.Splash -> SplashScreen()
                    PuretikScreen.Home -> DashboardController(viewModel)
                    PuretikScreen.Library -> LibraryController(viewModel)
                    PuretikScreen.Trackers -> TrackersController(viewModel)
                    PuretikScreen.Analytics -> AnalyticsController(viewModel)
                    PuretikScreen.AddEditVideo -> AddEditVideoController(viewModel)
                    PuretikScreen.AddEditCreator -> AddEditCreatorController(viewModel)
                    PuretikScreen.Settings -> SettingsController(viewModel)
                }
            }

            // Show Resolution Selector custom modal dialog overlay
            if (viewModel.showResolutionSelector && viewModel.activeFetchedVideoData != null) {
                ResolutionSelectionDialog(
                    videoData = viewModel.activeFetchedVideoData!!,
                    onDismiss = { viewModel.showResolutionSelector = false },
                    onConfirm = { resolution ->
                        viewModel.showResolutionSelector = false
                        viewModel.startRealDownload(viewModel.activeFetchedVideoData!!, resolution)
                    }
                )
            }
        }
    }
}

@Composable
fun ResolutionSelectionDialog(
    videoData: com.example.network.TikWmVideoData,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedResolution by remember { mutableStateOf("HD") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header icon representation
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PuretikTeal.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Quality Options",
                        tint = PuretikTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Configure Video Stream",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Short title
                Text(
                    text = videoData.title ?: "Puretik Video Title",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Author name description
                Text(
                    text = "by ${videoData.author?.nickname ?: "Creator"} ${videoData.author?.uniqueId?.let { "(@$it)" } ?: ""}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PuretikRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "CHOOSE DOWNLOAD RESOLUTION:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: HD 1080p
                val hdSizeText = videoData.hdSize?.let {
                    String.format("%.2f MB", it / (1024.0 * 1024.0))
                } ?: "Highest 1080p Stream"
                ResolutionOptionItem(
                    title = "HD (No Watermark)",
                    subtitle = "Clear 1080p/720p stream",
                    sizeText = hdSizeText,
                    isSelected = selectedResolution == "HD",
                    onClick = { selectedResolution = "HD" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: SD Quality
                val sdSizeText = videoData.size?.let {
                    String.format("%.2f MB", it / (1024.0 * 1024.0))
                } ?: "Standard Quality"
                ResolutionOptionItem(
                    title = "SD (No Watermark)",
                    subtitle = "Optimized resolution",
                    sizeText = sdSizeText,
                    isSelected = selectedResolution == "SD",
                    onClick = { selectedResolution = "SD" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: MP3 Sound extraction
                ResolutionOptionItem(
                    title = "MP3 Background Music",
                    subtitle = "Audio soundtrack extraction",
                    sizeText = videoData.duration?.let { "$it seconds track" } ?: "Soundtrack Only",
                    isSelected = selectedResolution == "MP3",
                    onClick = { selectedResolution = "MP3" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onConfirm(selectedResolution) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PuretikRed)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResolutionOptionItem(
    title: String,
    subtitle: String,
    sizeText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) PuretikRed.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) PuretikRed else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = PuretikRed)
                )
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = if (isSelected) PuretikRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

// ---------------- SPLASH SCREEN ----------------
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Canvas Brand Ring with Crimson & Teal Overlap
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14f
                    // Draw outer styling elements
                    drawCircle(
                        color = PuretikTeal,
                        radius = size.width / 2.2f,
                        center = center.copy(x = center.x - 12f, y = center.y - 12f),
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = PuretikRed,
                        radius = size.width / 2.2f,
                        center = center.copy(x = center.x + 12f, y = center.y + 12f),
                        style = Stroke(width = strokeWidth)
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Puretik Symbol",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Text Pairings
            Text(
                text = "PURETIK",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp,
                    fontFamily = FontFamily.SansSerif
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "LIVE DOWNLOAD MANAGER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = PuretikRed,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ---------------- DECORATIVE VECTOR ARTISTS ----------------
@Composable
fun RenderGeometricWatermark() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.85f)
            quadraticTo(
                size.width * 0.35f, size.height * 0.75f,
                size.width * 0.55f, size.height * 0.9f
            )
            quadraticTo(
                size.width * 0.8f, size.height * 1.05f,
                size.width, size.height * 0.82f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PuretikRed.copy(alpha = 0.03f),
                    PuretikTeal.copy(alpha = 0.05f)
                )
            )
        )
    }
}

@Composable
fun RenderNoDataIllustration() {
    Canvas(modifier = Modifier.size(120.dp)) {
        val w = size.width
        val h = size.height

        // Draw a folder container
        val folderPath = Path().apply {
            moveTo(w * 0.15f, h * 0.25f)
            lineTo(w * 0.45f, h * 0.25f)
            lineTo(w * 0.55f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.85f)
            lineTo(w * 0.15f, h * 0.85f)
            close()
        }
        drawPath(
            path = folderPath,
            color = PuretikRed.copy(alpha = 0.08f)
        )
        drawPath(
            path = folderPath,
            color = PuretikRed.copy(alpha = 0.3f),
            style = Stroke(width = 3f)
        )

        // Draw overlapping download lens or arrow inside folder
        drawCircle(
            color = PuretikTeal.copy(alpha = 0.2f),
            radius = w * 0.22f,
            center = Offset(w * 0.65f, h * 0.65f)
        )
        drawCircle(
            color = PuretikTeal.copy(alpha = 0.7f),
            radius = w * 0.22f,
            center = Offset(w * 0.65f, h * 0.65f),
            style = Stroke(width = 3f)
        )
    }
}

// ---------------- CORE CONTROLLERS WITH NAVIGATION FOOTERS ----------------
@Composable
fun DashboardController(viewModel: PuretikViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val videos by viewModel.videosFlow.collectAsStateWithLifecycle()
    val collections by viewModel.collectionsFlow.collectAsStateWithLifecycle()
    val downloadSimState = viewModel.downloadProgressState

    Scaffold(
        bottomBar = { PuretikBottomBar(PuretikScreen.Home, viewModel) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RenderGeometricWatermark()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Brand Header with Crimson/Teal highlighting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pure",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "tik",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                color = PuretikRed
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PuretikTeal.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ONLINE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = SecondaryTeal,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Text(
                            text = "Analyze Content Hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(PuretikScreen.Settings) },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Fast Downloader Field Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Vibrancy Video Downloader",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste your TikTok link below to save watermark-free streams cleanly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL input field
                        OutlinedTextField(
                            value = viewModel.urlInput,
                            onValueChange = { viewModel.urlInput = it },
                            placeholder = {
                                Text(
                                    text = "https://www.tiktok.com/@handle/video/123...",
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (viewModel.urlInput.isNotBlank()) {
                                    IconButton(onClick = { viewModel.urlInput = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("downloader_url_text_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PuretikRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Autofill Link button
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.getText()?.let {
                                        viewModel.urlInput = it.text
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Paste",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste URL", fontSize = 13.sp)
                                }
                            }

                            // Raw download button
                            Button(
                                onClick = { viewModel.startUrlDownload(viewModel.urlInput) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("download_submit_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PuretikRed)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Download"
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Analyze", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Online download progress overlay card
                if (downloadSimState != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (downloadSimState.error != null)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (downloadSimState.error != null)
                                            Icons.Default.Warning
                                        else if (downloadSimState.isFinished)
                                            Icons.Default.Check
                                        else
                                            Icons.Default.Refresh,
                                        contentDescription = "State",
                                        tint = if (downloadSimState.error != null)
                                            MaterialTheme.colorScheme.error
                                        else if (downloadSimState.isFinished)
                                            SecondaryTeal
                                        else
                                            PuretikRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (downloadSimState.error != null)
                                            "Execution Blocked"
                                        else if (downloadSimState.isFinished)
                                            "Engine Done"
                                        else
                                            "Bypassing Watermarks...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                IconButton(onClick = { viewModel.dismissDownloadProgress() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (downloadSimState.error != null) {
                                Text(
                                    text = downloadSimState.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            } else {
                                Text(
                                    text = downloadSimState.stepDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { downloadSimState.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (downloadSimState.isFinished) SecondaryTeal else PuretikRed,
                                    trackColor = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                  ) {
                                    Text(
                                        text = "Mode: Live Engine",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "${downloadSimState.progress}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Overall Performance Quickboard
                Text(
                    text = "Performance Quickboard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val totalDownloads = videos.size
                    val totalViews = videos.sumOf { it.viewCount }
                    val avgLikes = if (videos.isNotEmpty()) videos.sumOf { it.likeCount } / videos.size else 0L

                    QuickStatsCard(
                        title = "Local Library",
                        value = "$totalDownloads Posts",
                        subtext = "${collections.size} Folders",
                        icon = Icons.Default.List,
                        accentColor = PuretikRed,
                        modifier = Modifier.weight(1.3f)
                    )

                    QuickStatsCard(
                        title = "Sim. Total Reach",
                        value = formatNumber(totalViews),
                        subtext = "Likes avg: ${formatNumber(avgLikes)}",
                        icon = Icons.Default.Favorite,
                        accentColor = SecondaryTeal,
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Trending Creators Quick View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library Folder View",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(onClick = { viewModel.navigateTo(PuretikScreen.Library) }) {
                        Text("See Library", color = PuretikRed, fontSize = 13.sp)
                    }
                }

                if (videos.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your Saved Catalog is Empty",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Analyze links or go to Settings to import premium starter files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(videos.take(5)) { video ->
                            Card(
                                modifier = Modifier
                                    .width(230.dp)
                                    .clickable {
                                        viewModel.activeVideoForDetails = video
                                        viewModel.navigateTo(PuretikScreen.Library)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(SecondaryTeal.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = video.authorName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = SecondaryTeal
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = video.authorHandle,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(130.dp)
                                            )
                                        }

                                        if (video.isFavorite) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Starred",
                                                tint = PuretikRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        minLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PuretikRed.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = video.collectionName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PuretikRed
                                            )
                                        }

                                        Text(
                                            text = "${video.duration}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Trending Hashtag Ideas Block
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✨",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trending Ideas Generator",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Based on your localized catalog items, we recommend utilizing high pacing under #androidnative and #trendingcompile to capture 24% more retention.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifierMs(modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryTeal,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helpers for modifiers
fun modifierMs(parent: Modifier): Modifier {
    return parent
}


// ---------------- LIBRARY CONTROLLER ----------------
@Composable
fun LibraryController(viewModel: PuretikViewModel) {
    val videos by viewModel.filteredVideosFlow.collectAsStateWithLifecycle()
    val collections by viewModel.collectionsFlow.collectAsStateWithLifecycle()
    val activeDetails = viewModel.activeVideoForDetails

    Scaffold(
        bottomBar = { PuretikBottomBar(PuretikScreen.Library, viewModel) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.editingVideo = null
                    viewModel.navigateTo(PuretikScreen.AddEditVideo)
                },
                containerColor = PuretikRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Catalog Video") },
                text = { Text("Manual Catalog") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Local Library",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "${videos.size} ITEMS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field
                OutlinedTextField(
                    value = viewModel.videoSearchQuery,
                    onValueChange = { viewModel.videoSearchQuery = it },
                    placeholder = { Text("Search catalog title, creators, links...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PuretikRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Collections Tab Filter Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterTabPill(
                            label = "All Items",
                            isSelected = viewModel.selectedCollectionFilter == "All",
                            onClick = { viewModel.selectedCollectionFilter = "All" }
                        )
                    }
                    items(collections) { col ->
                        FilterTabPill(
                            label = col.name,
                            isSelected = viewModel.selectedCollectionFilter == col.name,
                            onClick = { viewModel.selectedCollectionFilter = col.name }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RenderNoDataIllustration()
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Empty Content Folder",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try typing keywords, selecting a different collection, or adding links to download video analytics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(videos) { video ->
                            VideoItemCard(
                                video = video,
                                onClick = { viewModel.activeVideoForDetails = video },
                                onFavorite = { viewModel.toggleFavoriteVideo(video) },
                                onDelete = { viewModel.deleteVideo(video) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }

            // Simulated Video Player / Analytical detail popup overlay drawer
            if (activeDetails != null) {
                SimulatedPlaybackOverlay(
                    video = activeDetails,
                    onClose = {
                        viewModel.activeVideoForDetails = null
                        viewModel.isVideoPlayerPlaying = false
                    },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun FilterTabPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) PuretikRed else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isSelected) PuretikRed else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun VideoItemCard(
    video: DownloadedVideo,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Creator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(PuretikRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = video.authorName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = PuretikRed
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = video.authorName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = video.authorHandle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Starred",
                            tint = if (video.isFavorite) PuretikRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption Title
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Micro Metadata Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Views",
                        modifier = Modifier.size(14.dp),
                        tint = SecondaryTeal
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatNumber(video.viewCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Likes",
                        modifier = Modifier.size(12.dp),
                        tint = PuretikRed
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = formatNumber(video.likeCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "${video.fileSizeMb} MB",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// Simulated High-Fidelity Video Player overlay detail card
@Composable
fun SimulatedPlaybackOverlay(
    video: DownloadedVideo,
    onClose: () -> Unit,
    viewModel: PuretikViewModel
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var playStatus by remember { mutableStateOf(viewModel.isVideoPlayerPlaying) }
    var playbackProgress by remember { mutableStateOf(viewModel.videoPlaybackPosition) }

    LaunchedEffect(playStatus) {
        if (playStatus) {
            while (playStatus) {
                delay(100)
                playbackProgress += 0.02f
                if (playbackProgress >= 1.0f) {
                    playbackProgress = 0f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable { /* prevent bubble click to close */ },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header handles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PuretikRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "P",
                                fontWeight = FontWeight.Bold,
                                color = PuretikRed,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aesthetic Media Analyzer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // High fidelity simulated media preview screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing retro sound frequency/sine waves behind player
                        val points = 30
                        val deltaX = size.width / points
                        val midY = size.height / 2f
                        val audioWavePath = Path()
                        audioWavePath.moveTo(0f, midY)

                        for (i in 0..points) {
                            // Sinusoidal representation matching elapsed frames
                            val mult = if (playStatus) (kotlin.random.Random.nextDouble().toFloat()) * 18f + 10f else 8f
                            val y = midY + kotlin.math.sin(i * 0.45f + (playbackProgress * 10f)) * mult
                            audioWavePath.lineTo(i * deltaX, y.toFloat())
                        }
                        drawPath(
                            path = audioWavePath,
                            color = PuretikTeal.copy(alpha = 0.3f),
                            style = Stroke(width = 4f)
                        )
                    }

                    // Foreground layout
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PuretikRed.copy(alpha = 0.85f))
                                .clickable {
                                    playStatus = !playStatus
                                    viewModel.isVideoPlayerPlaying = playStatus
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playStatus) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                contentDescription = if (playStatus) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (playStatus) "Streaming Watermark-Free Cache mp4..." else "Analytical Player Ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Seek bar
                Slider(
                    value = playbackProgress,
                    onValueChange = {
                        playbackProgress = it
                        viewModel.videoPlaybackPosition = it
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = PuretikRed,
                        activeTrackColor = PuretikRed,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentSec = (video.duration * playbackProgress).toInt()
                    Text(
                        text = "0:${currentSec.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "0:${video.duration}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Advanced metrics breakdown
                Text(
                    text = "High-Fidelity Engagement Audit",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Calculated values
                val engagementRate = if (video.viewCount > 0) {
                    val rate = ((video.likeCount + video.commentCount + video.shareCount).toDouble() / video.viewCount) * 100
                    String.format("%.2f", rate)
                } else {
                    "0.00"
                }

                val metricsList = listOf(
                    Triple("Views Reach", formatNumber(video.viewCount), Icons.Default.PlayArrow),
                    Triple("Heart Likes", formatNumber(video.likeCount), Icons.Default.Favorite),
                    Triple("Total Shares", formatNumber(video.shareCount), Icons.Default.Share),
                    Triple("Engagement Coefficient", "$engagementRate%", Icons.Default.CheckCircle)
                )

                metricsList.forEach { metric ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = metric.third,
                                contentDescription = null,
                                tint = PuretikRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = metric.first,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = metric.second,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Additional details (URL, file system URI, collection)
                Text(
                    text = "Local Storage Context",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Collection Target: ${video.collectionName}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Link Source: ${video.url}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Local Emulator Cache: ${video.localUri}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // CTA Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Edit details button
                    OutlinedButton(
                        onClick = {
                            viewModel.editingVideo = video
                            viewModel.activeVideoForDetails = null
                            viewModel.isVideoPlayerPlaying = false
                            viewModel.navigateTo(PuretikScreen.AddEditVideo)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize")
                    }

                    // Copy Source link button
                    Button(
                        onClick = {
                            clipboard.setText(AnnotatedString(video.url))
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Info")
                    }
                }
            }
        }
    }
}


// ---------------- TRACKERS CONTROLLER (CREATORS TRACKING) ----------------
@Composable
fun TrackersController(viewModel: PuretikViewModel) {
    val creators by viewModel.filteredCreatorsFlow.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { PuretikBottomBar(PuretikScreen.Trackers, viewModel) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.editingCreator = null
                    viewModel.navigateTo(PuretikScreen.AddEditCreator)
                },
                containerColor = SecondaryTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Creator Profile") },
                text = { Text("Track Creator") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Creators Watch",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryTeal.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "${creators.size} TRACKED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = SecondaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Field
                OutlinedTextField(
                    value = viewModel.creatorSearchQuery,
                    onValueChange = { viewModel.creatorSearchQuery = it },
                    placeholder = { Text("Search tracked handles or categories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trackers_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryTeal,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Filter Pills Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterTabPill(
                            label = "All Handlers",
                            isSelected = viewModel.selectedCategoryFilter == "All",
                            onClick = { viewModel.selectedCategoryFilter = "All" }
                        )
                    }
                    val defaultCategories = listOf("Comedy", "Inspiration", "Tutorials", "Music & Dance")
                    items(defaultCategories) { cat ->
                        FilterTabPill(
                            label = cat,
                            isSelected = viewModel.selectedCategoryFilter == cat,
                            onClick = { viewModel.selectedCategoryFilter = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (creators.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "",
                                tint = SecondaryTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Tracked Profiles Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Catalog and track video creators. Log and audit follower metrics with our secure local database tracker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(creators) { creator ->
                            CreatorTrackerCard(
                                creator = creator,
                                onEdit = {
                                    viewModel.editingCreator = creator
                                    viewModel.navigateTo(PuretikScreen.AddEditCreator)
                                },
                                onDelete = { viewModel.deleteCreator(creator) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorTrackerCard(
    creator: TrackedCreator,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Profile identity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SecondaryTeal.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = creator.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = SecondaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = creator.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Status",
                                tint = SecondaryTeal,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = creator.handle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlight statistics info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatNumber(creator.followerCount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Followers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatNumber(creator.totalLikes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Likes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${creator.videoCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Posts Count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            if (creator.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                ) {
                    Text(
                        text = creator.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Stars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SecondaryTeal.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = creator.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryTeal
                    )
                }

                // Star drawing depending on score
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", creator.rating),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


// ---------------- ANALYTICS CONTROLLER (DEEP COGNITIVE CALCULATIONS) ----------------
@Composable
fun AnalyticsController(viewModel: PuretikViewModel) {
    val videos by viewModel.videosFlow.collectAsStateWithLifecycle()
    val creators by viewModel.creatorsFlow.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { PuretikBottomBar(PuretikScreen.Analytics, viewModel) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Analytical Board",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Highly customized calculations from local library metrics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Overall score card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Global Engagement Weight",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val globalRate = if (videos.isNotEmpty()) {
                        val viewTotal = videos.sumOf { it.viewCount }
                        val likeTotal = videos.sumOf { it.likeCount }
                        val commTotal = videos.sumOf { it.commentCount }
                        val shareTotal = videos.sumOf { it.shareCount }
                        if (viewTotal > 0) {
                            ((likeTotal + commTotal + shareTotal).toDouble() / viewTotal) * 100
                        } else 0.0
                    } else 0.0

                    Text(
                        text = "${String.format("%.2f", globalRate)}%",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = PuretikRed
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (globalRate > 12.0)
                            "Strong viral retention! Pacing content captures premium focus."
                        else if (globalRate > 5.0)
                            "Average performance. Utilize higher visual hooks in early 3s."
                        else
                            "Add localized library items to calculate overall engagement rates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Graph representation: Mini visual chart matching views per item
            Text(
                text = "Simulated Library Engagement",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (videos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chart empty. Import premium demo logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        val maxViews = (videos.maxOfOrNull { it.viewCount } ?: 1L).toFloat()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            videos.take(6).forEach { video ->
                                val pct = (video.viewCount.toFloat() / maxViews).coerceIn(0.1f, 1.0f)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(pct)
                                            .width(22.dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(PuretikRed, PuretikRed.copy(alpha = 0.3f))
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = video.authorHandle.take(4),
                                        fontSize = 9.sp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Algorithmic optimization hints
            Text(
                text = "Optimization Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            val insights = listOf(
                Pair("Optimal Post Schedule", "Fridays at 6:30 PM local clocks drive 22% higher feedback loops."),
                Pair("Video Length Threshold", "Keep tutorial postings strictly inside 45-65 seconds for absolute retention bounds."),
                Pair("Audio Hook Index", "Upbeat visual rhythms with high contrast frames generate 14% higher comments index."),
                Pair("Trending Tag Formulas", "Pair #foryou with #puretik to organize localized metadata discovery.")
            )

            insights.forEach { tip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = tip.first,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tip.second,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


// ---------------- ADD / EDIT VIDEO SCREEN ----------------
@Composable
fun AddEditVideoController(viewModel: PuretikViewModel) {
    val initialVideo = viewModel.editingVideo

    var title by remember { mutableStateOf(initialVideo?.title ?: "") }
    var authorName by remember { mutableStateOf(initialVideo?.authorName ?: "") }
    var authorHandle by remember { mutableStateOf(initialVideo?.authorHandle ?: "") }
    var viewsInput by remember { mutableStateOf((initialVideo?.viewCount ?: 845000L).toString()) }
    var likesInput by remember { mutableStateOf((initialVideo?.likeCount ?: 62000L).toString()) }
    var commentsInput by remember { mutableStateOf((initialVideo?.commentCount ?: 4100).toString()) }
    var sharesInput by remember { mutableStateOf((initialVideo?.shareCount ?: 12000).toString()) }
    var durationInput by remember { mutableStateOf((initialVideo?.duration ?: 30).toString()) }
    var sizeInput by remember { mutableStateOf((initialVideo?.fileSizeMb ?: 8.4).toString()) }
    var activeCollection by remember { mutableStateOf(initialVideo?.collectionName ?: "Trending") }

    Scaffold(
        topBar = {
            OptAppBar(
                title = if (initialVideo != null) "Customize Catalog" else "Manual Video Catalog",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Manual Data Injector",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Populate raw statistical values to organize and track custom video analytics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Post Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title / Caption") },
                placeholder = { Text("Trending caption details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_video_title"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Creator Profile Name") },
                    placeholder = { Text("e.g., Bella Poarch") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_video_author_name"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )

                OutlinedTextField(
                    value = authorHandle,
                    onValueChange = { authorHandle = it },
                    label = { Text("TikTok Handle") },
                    placeholder = { Text("e.g. @bellapoarch") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("form_video_author_handle"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = viewsInput,
                    onValueChange = { viewsInput = it },
                    label = { Text("Sim views") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )

                OutlinedTextField(
                    value = likesInput,
                    onValueChange = { likesInput = it },
                    label = { Text("Sim likes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = commentsInput,
                    onValueChange = { commentsInput = it },
                    label = { Text("Sim comments") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )

                OutlinedTextField(
                    value = sharesInput,
                    onValueChange = { sharesInput = it },
                    label = { Text("Sim shares") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = durationInput,
                    onValueChange = { durationInput = it },
                    label = { Text("Duration (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )

                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { sizeInput = it },
                    label = { Text("File Size (MB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PuretikRed)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Collection selection
            Text(
                text = "Target Library Folder",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            val folderOptions = listOf("Trending", "Inspiration", "Tutorials", "Bookmarks")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                folderOptions.forEach { opt ->
                    val isSel = activeCollection == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PuretikRed else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (isSel) PuretikRed else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { activeCollection = opt }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val views = viewsInput.toLongOrNull() ?: 0L
                    val likes = likesInput.toLongOrNull() ?: 0L
                    val comments = commentsInput.toLongOrNull() ?: 0L
                    val shares = sharesInput.toLongOrNull() ?: 0L
                    val duration = durationInput.toIntOrNull() ?: 30
                    val sizeValue = sizeInput.toDoubleOrNull() ?: 5.0

                    viewModel.saveCatalogVideo(
                        title = title,
                        authorName = authorName,
                        authorHandle = authorHandle,
                        views = views,
                        likes = likes,
                        comments = comments,
                        shares = shares,
                        collection = activeCollection,
                        duration = duration,
                        fileSize = sizeValue
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_video_form_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PuretikRed)
            ) {
                Text(
                    text = if (initialVideo != null) "Update Parameters" else "Commit Catalog Item",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// ---------------- ADD / EDIT CREATOR SCREEN ----------------
@Composable
fun AddEditCreatorController(viewModel: PuretikViewModel) {
    val initialCreator = viewModel.editingCreator

    var handle by remember { mutableStateOf(initialCreator?.handle ?: "") }
    var name by remember { mutableStateOf(initialCreator?.name ?: "") }
    var followersInput by remember { mutableStateOf((initialCreator?.followerCount ?: 1200000L).toString()) }
    var videosInput by remember { mutableStateOf((initialCreator?.videoCount ?: 45).toString()) }
    var likesInput by remember { mutableStateOf((initialCreator?.totalLikes ?: 82500000L).toString()) }
    var categoryInput by remember { mutableStateOf(initialCreator?.category ?: "Comedy") }
    var notes by remember { mutableStateOf(initialCreator?.notes ?: "") }
    var rating by remember { mutableStateOf(initialCreator?.rating ?: 5.0f) }

    Scaffold(
        topBar = {
            OptAppBar(
                title = if (initialCreator != null) "Edit Track Profile" else "Track Creator Handle",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Creator Monitor Registry",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = handle,
                onValueChange = { handle = it },
                label = { Text("Creator TikTok Handle") },
                placeholder = { Text("e.g. @khaby.lame") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_creator_handle"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Label Name") },
                placeholder = { Text("e.g., Khaby Lame") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_creator_name"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = followersInput,
                onValueChange = { followersInput = it },
                label = { Text("Followers Count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("form_creator_followers"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = videosInput,
                    onValueChange = { videosInput = it },
                    label = { Text("Videos Post Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
                )

                OutlinedTextField(
                    value = likesInput,
                    onValueChange = { likesInput = it },
                    label = { Text("Simulated Total Likes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selection Row
            Text(
                text = "Creator Track Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            val catOptions = listOf("Comedy", "Inspiration", "Tutorials", "Music & Dance")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                catOptions.forEach { opt ->
                    val isSel = categoryInput == opt
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) SecondaryTeal else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (isSel) SecondaryTeal else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { categoryInput = opt }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Rating Selector Slider (Rating strategy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Perform Audit Index",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "${String.format("%.1f", rating)} ★",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = AccentYellow
                )
            }

            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 1.0f..5.0f,
                steps = 80,
                colors = SliderDefaults.colors(
                    thumbColor = SecondaryTeal,
                    activeTrackColor = SecondaryTeal,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Private Custom Track Notes") },
                placeholder = { Text("e.g. Master level performance pacing techniques...") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryTeal)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val followers = followersInput.toLongOrNull() ?: 1200000L
                    val videos = videosInput.toIntOrNull() ?: 45
                    val totalLikes = likesInput.toLongOrNull() ?: 82500000L

                    viewModel.saveTrackedCreator(
                        handle = handle,
                        name = name,
                        followers = followers,
                        videos = videos,
                        totalLikes = totalLikes,
                        category = categoryInput,
                        notes = notes,
                        rating = rating
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_creator_form_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
            ) {
                Text(
                    text = if (initialCreator != null) "Update Audit Info" else "Establish Track Anchor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


// ---------------- SETTINGS CONTROLLER (ENGINE MODIFIERS) ----------------
@Composable
fun SettingsController(viewModel: PuretikViewModel) {
    val videos by viewModel.videosFlow.collectAsStateWithLifecycle()
    val creators by viewModel.creatorsFlow.collectAsStateWithLifecycle()
    var openConfirmationWipe by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OptAppBar(
                title = "Engine Settings",
                onBack = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Puretik core configurations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fast actions (Mock load & Wipe)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management Quickboard",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Feed starter data button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.populatePremiumStarterMetrics() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Quick Feed Injection",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Immediately load 5 mock video graphs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Inject",
                            tint = PuretikRed
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    // Clear cache button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openConfirmationWipe = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Heavy Cache Wipe",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Delete all database, videos and collections.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Wipe",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About Puretik",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Puretik is an industry-grade, lightweight and stable TikTok catalog and custom metrics analyzer. Download and parse watermark-free video streams directly from live gateways, build library watchlists and customize strategic profile details with ease.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Engine Version: stable-1.0.r02\nDatabase: SQLite (androidx.room)\nCompatible: low/mid-range Android Devices",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (openConfirmationWipe) {
        AlertDialog(
            onDismissRequest = { openConfirmationWipe = false },
            title = { Text("Confirm Wipe Cache?") },
            text = { Text("This will delete all local data inside SQLite completely. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.performHeavyCacheWipe()
                        openConfirmationWipe = false
                    }
                ) {
                    Text("Wipe Storage", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { openConfirmationWipe = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


// ---------------- GENERAL SHARED WIDGETS (BOTTOM BAR, APP BAR) ----------------
@Composable
fun PuretikBottomBar(activeScreen: PuretikScreen, viewModel: PuretikViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val navItems = listOf(
            Triple(PuretikScreen.Home, "Home", Icons.Default.Home),
            Triple(PuretikScreen.Library, "Library", Icons.Default.List),
            Triple(PuretikScreen.Trackers, "Trackers", Icons.Default.FavoriteBorder),
            Triple(PuretikScreen.Analytics, "Analytics", Icons.Default.CheckCircle)
        )

        navItems.forEach { item ->
            val isSelected = activeScreen == item.first
            val actColor = if (item.first == PuretikScreen.Trackers) SecondaryTeal else PuretikRed
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        // Reset backstack and push target screen
                        viewModel.navigationStack.clear()
                        viewModel.navigationStack.add(item.first)
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.third,
                        contentDescription = item.second,
                        tint = if (isSelected) actColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
                    )
                },
                label = {
                    Text(
                        text = item.second,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                        fontSize = 11.sp,
                        color = if (isSelected) actColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = actColor.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("nav_item_${item.second.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptAppBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

// ---------------- LOCAL ADAPTER CONVERTERS ----------------
fun formatNumber(number: Long): String {
    return try {
        if (number >= 1_000000000L) {
            String.format(Locale.US, "%.1fB", number.toDouble() / 1_000000000.0)
        } else if (number >= 1_000000L) {
            String.format(Locale.US, "%.1fM", number.toDouble() / 1_000000.0)
        } else if (number >= 1_000L) {
            String.format(Locale.US, "%.1fK", number.toDouble() / 1_000.0)
        } else {
            NumberFormat.getNumberInstance(Locale.US).format(number)
        }
    } catch (e: Exception) {
        number.toString()
    }
}
