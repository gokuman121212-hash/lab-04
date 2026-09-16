package com.example.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun HandOverlayView(handsPoints: List<List<Offset>>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Conexiones estructurales clásicas del modelo anterior
        val connections = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 4),       // Pulgar
            intArrayOf(0, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 8),       // Índice
            intArrayOf(5, 9), intArrayOf(9, 10), intArrayOf(10, 11), intArrayOf(11, 12),   // Medio
            intArrayOf(9, 13), intArrayOf(13, 14), intArrayOf(14, 15), intArrayOf(15, 16), // Anular
            intArrayOf(13, 17), intArrayOf(17, 18), intArrayOf(18, 19), intArrayOf(19, 20),// Meñique
            intArrayOf(0, 17)                                                          // Base de palma
        )

        for (idx in handsPoints.indices) {
            val points = handsPoints[idx]
            if (points.size == 21) {
                // Colores brillantes y definidos del modelo anterior
                val boneColor = Color(0xFF00FFCC) // Cian brillante de alta visibilidad
                val jointColor = Color(0xFFFF0055) // Puntos articulares destacados

                // 1. Dibujar líneas esqueléticas con renderizado directo (sin demoras)
                for (conn in connections) {
                    val p1 = points[conn[0]]
                    val p2 = points[conn[1]]
                    drawLine(
                        color = boneColor,
                        start = p1,
                        end = p2,
                        strokeWidth = 6f // Grosor óptimo para velocidad y visibilidad fluida
                    )
                }

                // 2. Dibujar nodos / articulaciones clásicos
                for (i in points.indices) {
                    val pt = points[i]
                    drawCircle(
                        color = jointColor,
                        radius = 8f,
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = pt
                    )
                }

                // 3. Puntero dinámico rápido en la punta del dedo índice (Articulación 8)
                val indexTip = points[8]
                drawCircle(
                    color = Color.Yellow,
                    radius = 16f,
                    center = indexTip,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color.Green,
                    radius = 6f,
                    center = indexTip
                )
            }
        }
    }
}
