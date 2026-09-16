package com.example.myapplication.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

enum class SystemAction {
    NONE, EXIT_APP, GO_HOME, CLICK
}

object GestureRecognizerEngine {

    data class AnalysisResult(
        val smoothedPointsList: List<List<Offset>>,
        val statusText: String,
        val triggeredAction: SystemAction
    )

    private val smoothers = listOf(HandSmoother(), HandSmoother())

    fun analyze(
        landmarksList: List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>,
        viewW: Float,
        viewH: Float,
        timestamp: Long
    ): AnalysisResult {
        if (landmarksList.isEmpty()) {
            smoothers[0].reset()
            smoothers[1].reset()
            return AnalysisResult(emptyList(), "Buscando manos...", SystemAction.NONE)
        }

        val allPoints = ArrayList<List<Offset>>(landmarksList.size)
        val statuses = ArrayList<String>(landmarksList.size)
        var detectedAction = SystemAction.NONE

        for (index in landmarksList.indices) {
            if (index >= 2) break
            val landmarks = landmarksList[index]
            
            val rawPoints = List(landmarks.size) { i ->
                val pt = landmarks[i]
                Offset((1f - pt.x()) * viewW, pt.y() * viewH)
            }

            val smoothed = smoothers[index].smooth(rawPoints, timestamp)
            allPoints.add(smoothed)

            val wrist = smoothed[0]
            val mcpMiddle = smoothed[9]
            val handScale = hypot(mcpMiddle.x - wrist.x, mcpMiddle.y - wrist.y).coerceAtLeast(1.0f)

            fun dist(p1: Offset, p2: Offset) = hypot(p1.x - p2.x, p1.y - p2.y) / handScale

            val thumbTip = smoothed[4]
            val indexTip = smoothed[8]
            val middleTip = smoothed[12]
            val ringTip = smoothed[16]
            val pinkyTip = smoothed[20]

            val indexPip = smoothed[6]
            val middlePip = smoothed[10]
            val ringPip = smoothed[14]
            val pinkyPip = smoothed[18]

            val isIndexExt = dist(indexTip, wrist) > dist(indexPip, wrist) * 1.08f
            val isMiddleExt = dist(middleTip, wrist) > dist(middlePip, wrist) * 1.08f
            val isRingExt = dist(ringTip, wrist) > dist(ringPip, wrist) * 1.08f
            val isPinkyExt = dist(pinkyTip, wrist) > dist(pinkyPip, wrist) * 1.08f
            val isThumbExt = dist(thumbTip, wrist) > 0.60f

            val isPinch = dist(thumbTip, indexTip) < 0.28f
            val isFist = !isIndexExt && !isMiddleExt && !isRingExt && !isPinkyExt
            val isVictory = isIndexExt && isMiddleExt && !isRingExt && !isPinkyExt

            val currentGesture = when {
                isFist -> {
                    detectedAction = SystemAction.EXIT_APP
                    "✊ PUÑO (SALIR)"
                }
                isVictory -> {
                    detectedAction = SystemAction.GO_HOME
                    "✌️ VICTORIA (HOME)"
                }
                isPinch -> {
                    detectedAction = SystemAction.CLICK
                    "🎯 PELLIZCO (CLIC)"
                }
                else -> "🖐️ MANO ACTIVA"
            }

            val handPrefix = if (landmarksList.size > 1) "Mano : " else ""
            statuses.add("")
        }

        return AnalysisResult(allPoints, statuses.joinToString(" | "), detectedAction)
    }
}
