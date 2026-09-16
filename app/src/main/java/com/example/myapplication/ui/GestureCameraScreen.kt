package com.example.myapplication.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.example.myapplication.service.HandGestureAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.hypot

@Composable
fun GestureCameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var rawHandsListPoints by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var smoothHandsListPoints by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    
    var isHandDetected by remember { mutableStateOf(false) }
    var detectedCount by remember { mutableStateOf(0) }
    var isAccessibilityActive by remember { mutableStateOf(false) }
    var lastClickTime by remember { mutableStateOf(0L) }
    var isPinchedVisual by remember { mutableStateOf(false) }

    val analysisChannel = remember { Channel<ImageProxy>(Channel.CONFLATED) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityActive = HandGestureAccessibilityService.instance != null
            delay(1000L)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val currentRaw: List<List<Offset>> = rawHandsListPoints
            if (currentRaw.isNotEmpty()) {
                smoothHandsListPoints = currentRaw.mapIndexed { hIdx: Int, rawHand: List<Offset> ->
                    if (hIdx < smoothHandsListPoints.size) {
                        val smoothHand: List<Offset> = smoothHandsListPoints[hIdx]
                        rawHand.mapIndexed { pIdx: Int, rawPt: Offset ->
                            if (pIdx < smoothHand.size) {
                                val smoothPt: Offset = smoothHand[pIdx]
                                Offset(
                                    smoothPt.x + (rawPt.x - smoothPt.x) * 0.75f,
                                    smoothPt.y + (rawPt.y - smoothPt.y) * 0.75f
                                )
                            } else rawPt
                        }
                    } else rawHand
                }

                val primaryHand: List<Offset>? = smoothHandsListPoints.firstOrNull()
                if (primaryHand != null && primaryHand.size >= 9) {
                    val thumbTip: Offset = primaryHand[4]
                    val indexTip: Offset = primaryHand[8]
                    val distance: Double = hypot((thumbTip.x - indexTip.x).toDouble(), (thumbTip.y - indexTip.y).toDouble())

                    if (distance < 45.0) {
                        isPinchedVisual = true
                        val currentTime: Long = System.currentTimeMillis()
                        if (currentTime - lastClickTime > 1200) {
                            lastClickTime = currentTime
                            HandGestureAccessibilityService.instance?.simulateTap(indexTip.x, indexTip.y) { success: Boolean ->
                                // Callback completado
                            }
                        }
                    } else {
                        isPinchedVisual = false
                    }
                }
            } else {
                smoothHandsListPoints = emptyList()
                isPinchedVisual = false
            }
            delay(3L) 
        }
    }

    LaunchedEffect(Unit) {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .setDelegate(Delegate.GPU)
            .build()

        val options = HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.15f)
            .setMinHandPresenceConfidence(0.15f)
            .setMinTrackingConfidence(0.15f)
            .build()

        val landmarker = HandLandmarker.createFromOptions(context, options)

        coroutineScope.launch(Dispatchers.Default) {
            analysisChannel.consumeAsFlow().collect { imageProxy ->
                val bitmap = imageProxy.toRgbaBitmap()
                if (bitmap != null) {
                    try {
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, false)
                        val mpImage = BitmapImageBuilder(scaledBitmap).build()
                        val result = landmarker.detectForVideo(mpImage, System.currentTimeMillis())

                        if (result.landmarks().isNotEmpty()) {
                            isHandDetected = true
                            val viewWidth = imageProxy.width.toFloat()
                            val viewHeight = imageProxy.height.toFloat()

                            rawHandsListPoints = result.landmarks().map { rawHand ->
                                rawHand.map { landmark ->
                                    Offset(landmark.x() * viewWidth, landmark.y() * viewHeight)
                                }
                            }
                            detectedCount = rawHandsListPoints.size
                        } else {
                            isHandDetected = false
                            rawHandsListPoints = emptyList()
                            detectedCount = 0
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                imageProxy.close()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        analysisChannel.trySend(imageProxy)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val connections = arrayOf(
                intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 4),
                intArrayOf(0, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 8),
                intArrayOf(5, 9), intArrayOf(9, 10), intArrayOf(10, 11), intArrayOf(11, 12),
                intArrayOf(9, 13), intArrayOf(13, 14), intArrayOf(14, 15), intArrayOf(15, 16),
                intArrayOf(13, 17), intArrayOf(17, 18), intArrayOf(18, 19), intArrayOf(19, 20),
                intArrayOf(0, 17)
            )

            val hyperMeshLinks = arrayOf(
                intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7), intArrayOf(4, 8),
                intArrayOf(5, 10), intArrayOf(6, 11), intArrayOf(7, 12),
                intArrayOf(9, 14), intArrayOf(10, 15), intArrayOf(11, 16),
                intArrayOf(13, 18), intArrayOf(14, 19), intArrayOf(15, 20),
                intArrayOf(0, 2), intArrayOf(0, 6), intArrayOf(0, 10), intArrayOf(0, 14), intArrayOf(0, 18)
            )

            for (handPoints in smoothHandsListPoints) {
                if (handPoints.size == 21) {
                    val palmPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(handPoints[0].x, handPoints[0].y)
                        lineTo(handPoints[1].x, handPoints[1].y)
                        lineTo(handPoints[5].x, handPoints[5].y)
                        lineTo(handPoints[9].x, handPoints[9].y)
                        lineTo(handPoints[13].x, handPoints[13].y)
                        lineTo(handPoints[17].x, handPoints[17].y)
                        close()
                    }
                    drawPath(path = palmPath, color = Color(0xFF00E676).copy(alpha = 0.35f))

                    for (link in hyperMeshLinks) {
                        drawLine(
                            color = Color(0xFF00E676).copy(alpha = 0.65f),
                            start = handPoints[link[0]],
                            end = handPoints[link[1]],
                            strokeWidth = 3.5f
                        )
                    }

                    for (conn in connections) {
                        drawLine(
                            color = Color(0xFFB9F6CA).copy(alpha = 0.55f),
                            start = handPoints[conn[0]],
                            end = handPoints[conn[1]],
                            strokeWidth = 11f
                        )
                        drawLine(
                            color = Color.White,
                            start = handPoints[conn[0]],
                            end = handPoints[conn[1]],
                            strokeWidth = 5f
                        )
                    }

                    for ((idx, pt) in handPoints.withIndex()) {
                        val ptColor = if (idx == 8 && isPinchedVisual) Color(0xFFFFD700) else Color(0xFF69F0AE)
                        val outerRadius = if (idx == 8 && isPinchedVisual) 22f else 13f
                        
                        drawCircle(color = ptColor, radius = outerRadius, center = pt)
                        drawCircle(color = Color.White, radius = 7f, center = pt)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isPinchedVisual) Color(0xFFFFD700) else Color(0xFF69F0AE).copy(alpha = pulseAlpha))
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPinchedVisual) "⚡ ¡CLIC DETECTADO (PINZA)!" else "QUANTUM GESTURE MOUSE",
                            color = if (isPinchedVisual) Color(0xFFFFD700) else Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAccessibilityActive) "ESTADO: CONTROL TOTAL EXTERNO" else "VINCULA LA ACCESIBILIDAD",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isAccessibilityActive) Color(0xFF00E676) else Color(0xFFFF5252),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isAccessibilityActive) "ACTIVO" else "VINCULAR",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun ImageProxy.toRgbaBitmap(): Bitmap? {
    val plane = planes[0]
    val buffer = plane.buffer
    buffer.rewind()
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    
    val matrix = Matrix().apply {
        postRotate(imageInfo.rotationDegrees.toFloat())
        postScale(-1f, 1f, width / 2f, height / 2f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
}
