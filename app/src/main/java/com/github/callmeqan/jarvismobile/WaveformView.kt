package com.github.callmeqan.jarvismobile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val gradientColors = intArrayOf(
        Color.parseColor("#FF0077CC"), // Blue
        Color.parseColor("#FF00CC77"), // Green
        Color.parseColor("#FFFFCC00")  // Yellow
    )

    private val gradient = LinearGradient(
        0f, 0f, 0f, 1f,
        gradientColors, null, Shader.TileMode.CLAMP
    )

    private var waveform: List<Float> = emptyList()

    init {
        paint.shader = gradient
    }

    fun updateAmplitude(amplitude: Float) {
        Log.d("WaveformView", "Raw amplitude: $amplitude") // Log raw amplitude
        val normalizedAmplitude = (amplitude / 10f).coerceAtLeast(0.05f) // Normalize and set a minimum threshold
        Log.d("WaveformView", "Normalized amplitude: $normalizedAmplitude") // Log normalized amplitude
        waveform = List(50) { i -> // Generate 50 points for the waveform
            val angle = (i / 50f) * Math.PI * 2
            (sin(angle) * normalizedAmplitude).toFloat()
        }
        invalidate() // Redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveform.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        val step = width / waveform.size

        for (i in waveform.indices) {
            val x = i * step
            val y = centerY - (waveform[i] * centerY)
            val barHeight = abs(waveform[i]) * centerY
            canvas.drawRect(x - step / 4, centerY - barHeight, x + step / 4, centerY + barHeight, paint)
        }
    }
}