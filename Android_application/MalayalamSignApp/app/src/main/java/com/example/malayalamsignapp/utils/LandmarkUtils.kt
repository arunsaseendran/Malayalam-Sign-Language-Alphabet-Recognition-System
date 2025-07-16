package com.example.malayalamsignapp.utils

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

fun convertLandmarksToFeatures(landmarksList: List<List<NormalizedLandmark>>): FloatArray {
    val features = FloatArray(126) { 0f }

    for ((handIndex, landmarks) in landmarksList.withIndex()) {
        if (handIndex >= 2 || landmarks.size < 21) continue

        val wrist = landmarks[0]
        val baseIndex = handIndex * 63

        for (i in 0 until 21) {
            val lm = landmarks[i]
            features[baseIndex + i] = lm.x() - wrist.x()
            features[baseIndex + 21 + i] = lm.y() - wrist.y()
            features[baseIndex + 42 + i] = lm.z() - wrist.z()
        }
    }

    return features
}


// Helper function to ensure we have exactly 126 features
fun normalizeFeatures(features: FloatArray): FloatArray {
    return if (features.size == 126) {
        features
    } else {
        val normalized = FloatArray(126) { 0f }
        val copySize = minOf(features.size, 126)
        System.arraycopy(features, 0, normalized, 0, copySize)
        normalized
    }
}