package com.example.malayalamsignapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class LandmarkOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val landmarkPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    private val connectionPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var landmarks: List<NormalizedLandmark> = listOf()
    private var inputImageWidth: Int = 1  // Avoid division by zero
    private var inputImageHeight: Int = 1

    // Hand landmark connections (MediaPipe hand model)
    private val handConnections = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),           // Thumb
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),           // Index
        Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),      // Middle
        Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),    // Ring
        Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),    // Pinky
        Pair(5, 9), Pair(9, 13), Pair(13, 17)                     // Palm
    )

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>, imageWidth: Int, imageHeight: Int) {
        landmarks = newLandmarks
        inputImageWidth = imageWidth
        inputImageHeight = imageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty()) return

        var index = 0
        while (index < landmarks.size) {
            val handLandmarks = landmarks.subList(index, minOf(index + 21, landmarks.size))
            if (handLandmarks.size == 21) {
                drawHand(canvas, handLandmarks)
            }
            index += 21
        }
    }

    private fun drawHand(canvas: Canvas, handLandmarks: List<NormalizedLandmark>) {
        val scaleX = width.toFloat()
        val scaleY = height.toFloat()

        // Draw connections
        for ((startIdx, endIdx) in handConnections) {
            val start = handLandmarks[startIdx]
            val end = handLandmarks[endIdx]

            val startX = start.x() * scaleX
            val startY = start.y() * scaleY
            val endX = end.x() * scaleX
            val endY = end.y() * scaleY

            canvas.drawLine(startX, startY, endX, endY, connectionPaint)
        }

        // Draw individual points
        for (landmark in handLandmarks) {
            val x = landmark.x() * scaleX
            val y = landmark.y() * scaleY
            canvas.drawCircle(x, y, 12f, landmarkPaint)
        }
    }

}
