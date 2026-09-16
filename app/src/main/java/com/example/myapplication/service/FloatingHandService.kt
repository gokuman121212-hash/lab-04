package com.example.myapplication.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner

class FloatingHandService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    companion object {
        val handsPointsState = mutableStateOf<List<List<Offset>>>(emptyList())
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingHandService)
            setViewTreeViewModelStoreOwner(this@FloatingHandService)
            setViewTreeSavedStateRegistryOwner(this@FloatingHandService)
            
            setContent {
                Hand3DOverlayView(handsPoints = handsPointsState.value)
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        store.clear()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun Hand3DOverlayView(handsPoints: List<List<Offset>>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Conexiones estructurales completas de los dedos y palma
        val connections = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 4),
            intArrayOf(0, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 8),
            intArrayOf(5, 9), intArrayOf(9, 10), intArrayOf(10, 11), intArrayOf(11, 12),
            intArrayOf(9, 13), intArrayOf(13, 14), intArrayOf(14, 15), intArrayOf(15, 16),
            intArrayOf(13, 17), intArrayOf(17, 18), intArrayOf(18, 19), intArrayOf(19, 20),
            intArrayOf(0, 17)
        )

        // Malla cruzada para dar aspecto de guante tecnológico tridimensional
        val meshLinks = arrayOf(
            intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7),
            intArrayOf(5, 9), intArrayOf(6, 10), intArrayOf(7, 11),
            intArrayOf(9, 13), intArrayOf(10, 14), intArrayOf(11, 15),
            intArrayOf(13, 17), intArrayOf(14, 18), intArrayOf(15, 19),
            intArrayOf(0, 5), intArrayOf(0, 9), intArrayOf(0, 13), intArrayOf(0, 17)
        )

        for (idx in handsPoints.indices) {
            val points = handsPoints[idx]
            if (points.size == 21) {
                // 1. Relleno translúcido de la palma
                val palmPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[5].x, points[5].y)
                    lineTo(points[9].x, points[9].y)
                    lineTo(points[13].x, points[13].y)
                    lineTo(points[17].x, points[17].y)
                    close()
                }
                drawPath(path = palmPath, color = Color.White.copy(alpha = 0.25f))

                // 2. Líneas de la malla tridimensional (con grosor visible)
                for (link in meshLinks) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = points[link[0]],
                        end = points[link[1]],
                        strokeWidth = 3f
                    )
                }

                // 3. Esqueleto principal con líneas blancas gruesas
                for (conn in connections) {
                    drawLine(
                        color = Color.White,
                        start = points[conn[0]],
                        end = points[conn[1]],
                        strokeWidth = 5f
                    )
                }

                // 4. Sensores / Articulaciones bien marcadas (B/N de alto contraste)
                for (i in points.indices) {
                    val pt = points[i]
                    drawCircle(color = Color.Black, radius = 10f, center = pt)
                    drawCircle(color = Color.White, radius = 6f, center = pt)
                }
            }
        }
    }
}
