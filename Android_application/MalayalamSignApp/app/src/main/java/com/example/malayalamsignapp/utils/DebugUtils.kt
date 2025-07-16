package com.example.malayalamsignapp.utils

import android.util.Log

object DebugUtils {
    private const val TAG = "WordPrediction"

    fun logFeatures(features: FloatArray, label: String = "Features") {
        if (features.isEmpty()) {
            Log.d(TAG, "$label: Empty array")
            return
        }

        val summary = "Size: ${features.size}, Range: [${features.minOrNull()?.let { "%.3f".format(it) }}, ${features.maxOrNull()?.let { "%.3f".format(it) }}]"
        Log.d(TAG, "$label - $summary")

        // Log first few and last few values
        val preview = features.take(5).map { "%.3f".format(it) }.joinToString(", ")
        Log.d(TAG, "$label preview: [$preview...]")
    }

    fun logPrediction(prediction: String, confidence: Float, features: FloatArray) {
        Log.d(TAG, "Prediction: '$prediction' (${(confidence * 100).toInt()}%) with ${features.size} features")
    }

    fun logTimestamp(operation: String) {
        Log.d(TAG, "$operation at ${System.currentTimeMillis()}")
    }
}