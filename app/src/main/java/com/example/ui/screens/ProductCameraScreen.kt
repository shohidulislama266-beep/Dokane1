package com.example.ui.screens

import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.util.ProductImageUtil
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProductCameraScreen(
    maxDimensionPx: Int = ProductImageUtil.SYNC_MAX_DIMENSION_PX,
    maxBytes: Int = ProductImageUtil.SYNC_MAX_BYTES,
    onPhotoConfirmed: (Uri) -> Unit,
    onPickFromGallery: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val density = LocalDensity.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var hasBackCamera by remember { mutableStateOf(true) }
    var hasFrontCamera by remember { mutableStateOf(false) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var previewWidthPx by remember { mutableIntStateOf(0) }
    var previewHeightPx by remember { mutableIntStateOf(0) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    fun cleanupUnconfirmedPhoto(uri: Uri?) {
        if (uri != null) {
            ProductImageUtil.deleteLocalPhotoIfOwned(context, uri.toString())
        }
    }

    fun handleRetake() {
        cleanupUnconfirmedPhoto(capturedPhotoUri)
        capturedPhotoUri = null
        cameraError = null
    }

    fun handleCancel() {
        cleanupUnconfirmedPhoto(capturedPhotoUri)
        capturedPhotoUri = null
        onCancel()
    }

    DisposableEffect(Unit) {
        onDispose {
            captureExecutor.shutdown()
        }
    }

    // Bind CameraX whenever live preview is visible or lensFacing changes
    if (capturedPhotoUri == null) {
        DisposableEffect(lifecycleOwner, lensFacing) {
            var boundProvider: ProcessCameraProvider? = null
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener(
                {
                    try {
                        val provider = cameraProviderFuture.get()
                        boundProvider = provider
                        hasBackCamera = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                        hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

                        val desiredSelector = if (lensFacing == CameraSelector.LENS_FACING_FRONT && hasFrontCamera) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else if (hasBackCamera) {
                            lensFacing = CameraSelector.LENS_FACING_BACK
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else if (hasFrontCamera) {
                            lensFacing = CameraSelector.LENS_FACING_FRONT
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            null
                        }

                        if (desiredSelector == null) {
                            cameraError = "No camera found on this device"
                            return@addListener
                        }

                        val preview = Preview.Builder().build().apply {
                            surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        provider.unbindAll()
                        val viewPort = previewView.viewPort
                        if (viewPort != null) {
                            val group = UseCaseGroup.Builder()
                                .setViewPort(viewPort)
                                .addUseCase(preview)
                                .addUseCase(capture)
                                .build()
                            provider.bindToLifecycle(lifecycleOwner, desiredSelector, group)
                        } else {
                            provider.bindToLifecycle(lifecycleOwner, desiredSelector, preview, capture)
                        }
                        cameraError = null
                    } catch (e: Exception) {
                        Log.e("ProductCameraScreen", "Camera startup failed", e)
                        cameraError = e.localizedMessage ?: "Unable to start camera"
                    }
                },
                mainExecutor
            )

            onDispose {
                try {
                    boundProvider?.unbindAll()
                } catch (_: Exception) {
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { handleCancel() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        com.example.ui.components.ConfigureEdgeToEdgeDialogWindow()

        BackHandler {
            if (capturedPhotoUri != null) {
                handleRetake()
            } else {
                handleCancel()
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("product_camera_screen")
        ) {
            val frameDp = min(maxWidth.value, maxHeight.value).dp * 0.80f
            val squareFramePx = with(density) { frameDp.toPx() }

            if (capturedPhotoUri != null) {
                // Review Captured Square Photo (Retake / Use Photo)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(frameDp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF101214),
                        border = BorderStroke(2.dp, VividGreen)
                    ) {
                        AsyncImage(
                            model = capturedPhotoUri,
                            contentDescription = "Captured product photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("camera_review_image")
                        )
                    }
                }
            } else if (cameraError == null) {
                // Live Camera Preview
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            previewWidthPx = size.width
                            previewHeightPx = size.height
                        }
                )

                // Dark scrim + Square Frame + 3x3 Grid Overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    val boxSize = min(size.width, size.height) * 0.80f
                    val left = (size.width - boxSize) / 2f
                    val top = (size.height - boxSize) / 2f
                    val right = left + boxSize
                    val bottom = top + boxSize

                    // Semi-transparent scrim outside square frame
                    drawRect(color = Color(0x99000000))

                    // Clear square cutout
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(boxSize, boxSize),
                        blendMode = BlendMode.Clear
                    )

                    // Square frame border
                    drawRect(
                        color = Color.White.copy(alpha = 0.85f),
                        topLeft = Offset(left, top),
                        size = Size(boxSize, boxSize),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3x3 Grid Overlay inside the square frame
                    val gridStroke = 1.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.45f)
                    val third = boxSize / 3f
                    drawLine(gridColor, Offset(left + third, top), Offset(left + third, bottom), gridStroke)
                    drawLine(gridColor, Offset(left + third * 2f, top), Offset(left + third * 2f, bottom), gridStroke)
                    drawLine(gridColor, Offset(left, top + third), Offset(right, top + third), gridStroke)
                    drawLine(gridColor, Offset(left, top + third * 2f), Offset(right, top + third * 2f), gridStroke)

                    // VividGreen corner accents
                    val cornerStroke = 3.5.dp.toPx()
                    val cornerLen = 22.dp.toPx()
                    drawLine(VividGreen, Offset(left, top), Offset(left + cornerLen, top), cornerStroke, StrokeCap.Round)
                    drawLine(VividGreen, Offset(left, top), Offset(left, top + cornerLen), cornerStroke, StrokeCap.Round)

                    drawLine(VividGreen, Offset(right, top), Offset(right - cornerLen, top), cornerStroke, StrokeCap.Round)
                    drawLine(VividGreen, Offset(right, top), Offset(right, top + cornerLen), cornerStroke, StrokeCap.Round)

                    drawLine(VividGreen, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke, StrokeCap.Round)
                    drawLine(VividGreen, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke, StrokeCap.Round)

                    drawLine(VividGreen, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke, StrokeCap.Round)
                    drawLine(VividGreen, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke, StrokeCap.Round)
                }
            } else {
                // No-camera / hardware error fallback with Gallery option
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = AlertRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = cameraError ?: "Camera unavailable",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        onClick = {
                            handleCancel()
                            onPickFromGallery()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = VividGreen,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("camera_gallery_fallback_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose from Gallery",
                                color = DarkBackground,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Top Hint Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0x99121417),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(
                        text = if (capturedPhotoUri != null) {
                            "Check square crop"
                        } else {
                            "Align product inside square"
                        },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom Controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color(0xCC0D0E10)
            ) {
                if (capturedPhotoUri != null) {
                    // Review Controls: Retake / Use Photo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { handleRetake() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x26FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("camera_retake_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retake",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                val confirmed = capturedPhotoUri
                                if (confirmed != null) {
                                    capturedPhotoUri = null
                                    onPhotoConfirmed(confirmed)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = VividGreen,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 50.dp)
                                .testTag("camera_use_photo_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Use Photo",
                                    color = DarkBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Live Camera Controls: Cancel / Capture / Flip camera
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel
                        Surface(
                            onClick = { handleCancel() },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x26FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 84.dp, minHeight = 48.dp)
                                .testTag("camera_cancel_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cancel",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Capture shutter button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(3.dp, Color.White),
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !isCapturing && imageCapture != null && cameraError == null) {
                                        val capture = imageCapture ?: return@clickable
                                        isCapturing = true
                                        cameraError = null
                                        val rawFile = ProductImageUtil.createTempCameraPhotoFile(context)
                                        val metadata = ImageCapture.Metadata().apply {
                                            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                        }
                                        val outputOptions = ImageCapture.OutputFileOptions.Builder(rawFile)
                                            .setMetadata(metadata)
                                            .build()
                                        capture.takePicture(
                                            outputOptions,
                                            captureExecutor,
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    coroutineScope.launch {
                                                        try {
                                                            val croppedUri = withContext(Dispatchers.IO) {
                                                                ProductImageUtil.cropSquareAndSaveCameraPhoto(
                                                                    context = context,
                                                                    rawCaptureFile = rawFile,
                                                                    previewWidthPx = previewWidthPx,
                                                                    previewHeightPx = previewHeightPx,
                                                                    squareFrameSizePx = squareFramePx,
                                                                    maxDimensionPx = maxDimensionPx,
                                                                    maxBytes = maxBytes
                                                                )
                                                            }
                                                            runCatching { rawFile.delete() }
                                                            capturedPhotoUri = croppedUri
                                                        } catch (e: Exception) {
                                                            runCatching { rawFile.delete() }
                                                            Log.e("ProductCameraScreen", "Crop failed", e)
                                                            cameraError = e.localizedMessage ?: "Could not process captured photo"
                                                        } finally {
                                                            isCapturing = false
                                                        }
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    runCatching { rawFile.delete() }
                                                    Log.e("ProductCameraScreen", "Capture failed", exception)
                                                    coroutineScope.launch {
                                                        isCapturing = false
                                                        cameraError = exception.localizedMessage ?: "Photo capture failed"
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    .testTag("camera_capture_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isCapturing) TextSecondary else VividGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            color = DarkBackground,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Capture",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Flip camera
                        val canFlip = hasBackCamera && hasFrontCamera
                        Surface(
                            onClick = {
                                if (canFlip) {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                }
                            },
                            enabled = canFlip && !isCapturing,
                            shape = RoundedCornerShape(14.dp),
                            color = if (canFlip) Color(0x26FFFFFF) else Color(0x14FFFFFF),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                                .testTag("camera_flip_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = null,
                                    tint = if (canFlip) TextPrimary else TextSecondary.copy(alpha = 0.45f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Flip camera",
                                    color = if (canFlip) TextPrimary else TextSecondary.copy(alpha = 0.45f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
