package com.example.malayalamsignapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var landmarks = listOf<NormalizedLandmark>()
    private var imageWidth = 0
    private var imageHeight = 0

    private val landmarkPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Hand landmark connections (MediaPipe hand model)
    private val handConnections = listOf(
        // Thumb
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
        // Index finger
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
        // Middle finger
        Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
        // Ring finger
        Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
        // Pinky
        Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
        // Palm connections
        Pair(5, 9), Pair(9, 13), Pair(13, 17)
    )

    fun updateLandmarks(newLandmarks: List<NormalizedLandmark>, imageWidth: Int, imageHeight: Int) {
        this.landmarks = newLandmarks
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (landmarks.isEmpty() || imageWidth == 0 || imageHeight == 0) return

        // Process landmarks in groups of 21 (one hand = 21 landmarks)
        var landmarkIndex = 0
        while (landmarkIndex + 20 < landmarks.size) {
            val handLandmarks = landmarks.subList(landmarkIndex, landmarkIndex + 21)

            // Draw connections first (so they appear behind landmarks)
            drawHandConnections(canvas, handLandmarks)

            // Draw landmarks
            drawHandLandmarks(canvas, handLandmarks)

            landmarkIndex += 21
        }
    }

    private fun drawHandLandmarks(canvas: Canvas, handLandmarks: List<NormalizedLandmark>) {
        for (landmark in handLandmarks) {
            val x = landmark.x() * width
            val y = landmark.y() * height
            canvas.drawCircle(x, y, 6f, landmarkPaint)
        }
    }

    private fun drawHandConnections(canvas: Canvas, handLandmarks: List<NormalizedLandmark>) {
        if (handLandmarks.size < 21) return

        for (connection in handConnections) {
            val startIdx = connection.first
            val endIdx = connection.second

            if (startIdx < handLandmarks.size && endIdx < handLandmarks.size) {
                val startLandmark = handLandmarks[startIdx]
                val endLandmark = handLandmarks[endIdx]

                val startX = startLandmark.x() * width
                val startY = startLandmark.y() * height
                val endX = endLandmark.x() * width
                val endY = endLandmark.y() * height

                canvas.drawLine(startX, startY, endX, endY, connectionPaint)
            }
        }
    }
}