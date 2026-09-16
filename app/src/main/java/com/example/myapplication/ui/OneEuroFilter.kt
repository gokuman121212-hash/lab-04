package com.example.myapplication.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

class OneEuroFilter(private val minCutoff: Float = 0.5f, private val beta: Float = 0.003f) {
    private var xPrev = 0f
    private var dxPrev = 0f
    private var tPrev = 0L

    fun filter(value: Float, timestamp: Long): Float {
        if (tPrev == 0L) {
            tPrev = timestamp
            xPrev = value
            return value
        }
        val dt = ((timestamp - tPrev) / 1000f).coerceAtLeast(0.001f)
        tPrev = timestamp

        val dx = (value - xPrev) / dt
        val alphaVal = alpha(dt, 1.0f)
        val edx = alphaVal * dx + (1f - alphaVal) * dxPrev
        dxPrev = edx

        val cutoff = minCutoff + beta * abs(edx)
        val a = alpha(dt, cutoff)
        val xFiltered = a * value + (1f - a) * xPrev
        xPrev = xFiltered
        return xFiltered
    }

    private fun alpha(dt: Float, cutoff: Float): Float {
        val tau = 1.0f / (6.2831853f * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    fun reset() {
        tPrev = 0L
        xPrev = 0f
        dxPrev = 0f
    }
}

class HandSmoother {
    private val xFilters = Array(21) { OneEuroFilter() }
    private val yFilters = Array(21) { OneEuroFilter() }

    fun smooth(points: List<Offset>, timestamp: Long): List<Offset> {
        return List(points.size) { i ->
            Offset(
                xFilters[i].filter(points[i].x, timestamp),
                yFilters[i].filter(points[i].y, timestamp)
            )
        }
    }

    fun reset() {
        for (i in 0..20) {
            xFilters[i].reset()
            yFilters[i].reset()
        }
    }
}
