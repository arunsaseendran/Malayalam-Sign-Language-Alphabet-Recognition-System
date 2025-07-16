package com.example.malayalamsignapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.Executors
import com.google.mediapipe.framework.image.BitmapImageBuilder

class HandLandmarkHelper(
    context: Context,
    private val onResult: (HandLandmarkerResult) -> Unit
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val handLandmarker: HandLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .build()

        val options = HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE) // Or LIVE_STREAM / VIDEO based on usage
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    fun detectAsync(bitmap: Bitmap, timestamp: Long) {
        executor.execute {
            try {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = handLandmarker.detect(mpImage)
                Log.d("HandLandmarkHelper", "Detected hands: ${result.landmarks().size}")
                onResult(result)
            } catch (e: Exception) {
                Log.e("HandLandmarkHelper", "Detection failed", e)
            }
        }
    }
}
