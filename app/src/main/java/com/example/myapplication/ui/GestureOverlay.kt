package com.example.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun GestureOverlay(points: List<Offset>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size >= 21) {
            val connections = listOf(
                Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
                Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
                Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
                Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
                Pair(13, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
                Pair(0, 17)
            )

            for (conn in connections) {
                drawLine(
                    color = Color(0xFF00FF88),
                    start = points[conn.first],
                    end = points[conn.second],
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            for (point in points) {
                drawCircle(color = Color.Red, radius = 10f, center = point)
                drawCircle(color = Color.White, radius = 4f, center = point)
            }
        }
    }
}
