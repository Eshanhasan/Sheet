package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.PaperBitmapGenerator
import com.example.domain.PhysicalPaperSpec
import com.example.model.ScanMode
import com.example.ui.theme.AmberUnitAccent
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestCard
import com.example.ui.theme.ForestObsidian
import com.example.ui.theme.IvoryText
import java.nio.ByteBuffer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    selectedScanMode: ScanMode,
    isExtracting: Boolean,
    extractionProgressLabel: String,
    previewPaperSpec: PhysicalPaperSpec?,
    onScanModeChange: (ScanMode) -> Unit,
    onBitmapCaptured: (Bitmap, String) -> Unit,
    onScanSamplePaper: (PhysicalPaperSpec) -> Unit,
    onPreviewPaperSpec: (PhysicalPaperSpec?) -> Unit,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = decodeUriToBitmap(context, uri)
            if (bitmap != null) {
                onBitmapCaptured(bitmap, "Imported Paper Photo")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestObsidian)
    ) {
        if (hasCameraPermission) {
            // Live CameraX PreviewView
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                val listener = Runnable {
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        cameraProvider.unbindAll()
                        cameraControl = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.w("CameraScannerScreen", "CameraX bind warning: ${e.message}")
                    }
                }
                cameraProviderFuture.addListener(listener, executor)

                onDispose {
                    runCatching { cameraProviderFuture.get().unbindAll() }
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camerax_preview_view")
            )
        } else {
            // Permission Request Card with Visual Polish (still lets user use Photo Picker or Sample Paper Deck!)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(ForestObsidian, Color(0xFF0E2920), ForestObsidian)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestCard.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera access",
                            tint = EmeraldBright,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enable Camera for Live Paper Detection",
                            style = MaterialTheme.typography.titleLarge,
                            color = IvoryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Grant camera permission to align physical invoices, ledgers, and itemized receipts with the live table grid overlay — or pick a photo / test sheet below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("grant_camera_permission_button")
                            ) {
                                Text("Enable Camera")
                            }
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("fallback_photo_picker_button")
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import Photo")
                            }
                        }
                    }
                }
            }
        }

        // Animated Table & Itemized List Laser Alignment Overlay
        ScannerGridLaserOverlay(
            scanMode = selectedScanMode,
            modifier = Modifier.fillMaxSize()
        )

        // Top Bar: Back Button, Scan Mode Pill, Torch Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.78f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("scanner_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Vault",
                        tint = IvoryText
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ForestCard.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldBright)
                        )
                        Text(
                            text = "UNIT & GRID OCR READY",
                            style = MaterialTheme.typography.labelMedium,
                            color = IvoryText
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val nextTorch = !isTorchOn
                        isTorchOn = nextTorch
                        cameraControl?.cameraControl?.enableTorch(nextTorch)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash Torch",
                        tint = if (isTorchOn) AmberUnitAccent else IvoryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scan Mode Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                ScanMode.entries.forEach { mode ->
                    val selected = mode == selectedScanMode
                    FilterChip(
                        selected = selected,
                        onClick = { onScanModeChange(mode) },
                        label = {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (mode) {
                                    ScanMode.AUTO_DETECT -> Icons.Default.AutoAwesome
                                    ScanMode.STRUCTURED_TABLE -> Icons.Default.GridOn
                                    ScanMode.ITEMIZED_LIST -> Icons.Default.ReceiptLong
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color(0xFF041C13),
                            selectedLeadingIconColor = Color(0xFF041C13),
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            labelColor = IvoryText,
                            iconColor = EmeraldBright
                        ),
                        modifier = Modifier.testTag("scan_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // Bottom Controls: Physical Paper Test Deck + Shutter & Photo Picker Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            ForestObsidian.copy(alpha = 0.88f),
                            ForestObsidian
                        )
                    )
                )
                .padding(top = 20.dp, bottom = 16.dp)
        ) {
            // Physical Paper Test Deck (allows instant realistic testing of physical paper tables in Emulator!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PHYSICAL PAPER TEST SHEETS (TAP TO SCAN OR INSPECT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberUnitAccent
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(PaperBitmapGenerator.samplePapers, key = { it.id }) { spec ->
                    PhysicalPaperDeckCard(
                        spec = spec,
                        onScanClick = { onScanSamplePaper(spec) },
                        onPreviewClick = { onPreviewPaperSpec(spec) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Camera Action Row: Import Photo | Capture Shutter | Quick Auto-Extract
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Gallery / Photo Picker Button
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ForestCard.copy(alpha = 0.85f),
                        contentColor = IvoryText
                    ),
                    modifier = Modifier.testTag("scanner_import_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Import paper photo",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Photo")
                }

                // Main Shutter Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .border(3.dp, EmeraldBright, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                        .clickable(enabled = !isExtracting) {
                            val capture = imageCapture
                            if (capture != null && hasCameraPermission) {
                                captureCameraFrame(
                                    context = context,
                                    imageCapture = capture,
                                    onBitmapReady = { bmp ->
                                        onBitmapCaptured(bmp, "Camera Paper Scan")
                                    },
                                    onFallback = {
                                        val fallbackSpec = PaperBitmapGenerator.samplePapers.first()
                                        onScanSamplePaper(fallbackSpec)
                                    }
                                )
                            } else {
                                val fallbackSpec = PaperBitmapGenerator.samplePapers.first()
                                onScanSamplePaper(fallbackSpec)
                            }
                        }
                        .testTag("camera_shutter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture and extract table",
                        tint = Color(0xFF041C13),
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Inspect Sample Paper Button
                OutlinedButton(
                    onClick = {
                        onPreviewPaperSpec(PaperBitmapGenerator.samplePapers.first())
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AmberUnitAccent.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ForestCard.copy(alpha = 0.85f),
                        contentColor = AmberUnitAccent
                    ),
                    modifier = Modifier.testTag("inspect_sample_paper_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Inspect Paper",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paper")
                }
            }
        }

        // Loading / OCR Extraction Overlay
        if (isExtracting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ForestCard),
                    border = BorderStroke(1.dp, EmeraldBright),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = EmeraldBright)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting Rows, Columns & Units",
                            style = MaterialTheme.typography.titleMedium,
                            color = IvoryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = extractionProgressLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Physical Paper Full-Resolution Preview Modal Sheet
        if (previewPaperSpec != null) {
            val renderedBitmap = remember(previewPaperSpec.id) {
                PaperBitmapGenerator.renderPaperBitmap(previewPaperSpec)
            }
            ModalBottomSheet(
                onDismissRequest = { onPreviewPaperSpec(null) },
                containerColor = ForestCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        text = previewPaperSpec.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = IvoryText
                    )
                    Text(
                        text = "${previewPaperSpec.subtitle} • Rendered Physical Paper Target",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = renderedBitmap.asImageBitmap(),
                        contentDescription = previewPaperSpec.title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onScanSamplePaper(previewPaperSpec) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("modal_extract_paper_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color(0xFF041C13)
                        )
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Run OCR & Extract Editable Table",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicalPaperDeckCard(
    spec: PhysicalPaperSpec,
    onScanClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ForestCard.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
        modifier = Modifier
            .width(235.dp)
            .clickable(onClick = onScanClick)
            .testTag("sample_paper_card_${spec.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = EmeraldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spec.badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberUnitAccent
                )
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = IvoryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${spec.rows.size} rows • Tap to extract",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onPreviewClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Preview physical paper",
                    tint = EmeraldBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ScannerGridLaserOverlay(
    scanMode: ScanMode,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_scan")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.86f
        val frameHeight = size.height * 0.50f
        val left = (size.width - frameWidth) / 2f
        val top = size.height * 0.16f
        val right = left + frameWidth
        val bottom = top + frameHeight

        // Outer guide frame
        drawRoundRect(
            color = EmeraldPrimary.copy(alpha = 0.35f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Internal table row/column alignment guide lines
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        val rowCount = if (scanMode == ScanMode.ITEMIZED_LIST) 6 else 5
        for (r in 1 until rowCount) {
            val y = top + (frameHeight / rowCount) * r
            drawLine(
                color = EmeraldBright.copy(alpha = 0.25f),
                start = Offset(left + 12f, y),
                end = Offset(right - 12f, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = dashEffect
            )
        }

        if (scanMode != ScanMode.ITEMIZED_LIST) {
            val colOffsets = listOf(0.22f, 0.56f, 0.78f)
            for (fraction in colOffsets) {
                val x = left + frameWidth * fraction
                drawLine(
                    color = AmberUnitAccent.copy(alpha = 0.25f),
                    start = Offset(x, top + 12f),
                    end = Offset(x, bottom - 12f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }
        }

        // Bold corner viewfinder brackets
        val bracketLen = 28.dp.toPx()
        val stroke = 4.dp.toPx()
        val bracketColor = EmeraldBright

        // Top-Left
        drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), stroke)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), stroke)
        // Top-Right
        drawLine(bracketColor, Offset(right - bracketLen, top), Offset(right, top), stroke)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), stroke)
        // Bottom-Left
        drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), stroke)
        drawLine(bracketColor, Offset(left, bottom - bracketLen), Offset(left, bottom), stroke)
        // Bottom-Right
        drawLine(bracketColor, Offset(right - bracketLen, bottom), Offset(right, bottom), stroke)
        drawLine(bracketColor, Offset(right, bottom - bracketLen), Offset(right, bottom), stroke)

        // Animated Laser Scan Line
        val laserY = top + frameHeight * laserProgress
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    EmeraldBright.copy(alpha = 0.9f),
                    AmberUnitAccent,
                    EmeraldBright.copy(alpha = 0.9f),
                    Color.Transparent
                ),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8f, laserY),
            end = Offset(right - 8f, laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

private fun captureCameraFrame(
    context: Context,
    imageCapture: ImageCapture,
    onBitmapReady: (Bitmap) -> Unit,
    onFallback: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = imageProxyToBitmap(image)
                image.close()
                if (bitmap != null) {
                    onBitmapReady(bitmap)
                } else {
                    onFallback()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.w("CameraScannerScreen", "Capture fallback: ${exception.message}")
                onFallback()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return runCatching {
        val planeProxy = image.planes.firstOrNull() ?: return null
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()
}
