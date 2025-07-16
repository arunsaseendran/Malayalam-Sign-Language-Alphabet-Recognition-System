package com.example.malayalamsignapp.utils

import android.content.Context
import android.util.Log

object ModelValidator {

    fun validateAssets(context: Context): ValidationResult {
        val missingFiles = mutableListOf<String>()

        // Check for required files
        val requiredFiles = listOf(
            "model.tflite",
            "hand_landmarker.task",
            "label_map.json",
            "scaler_params.json",
            "1malayalam_words.txt"
        )

        for (fileName in requiredFiles) {
            try {
                context.assets.open(fileName).close()
            } catch (e: Exception) {
                missingFiles.add(fileName)
                Log.w("ModelValidator", "Missing file: $fileName")
            }
        }

        return ValidationResult(missingFiles.isEmpty(), missingFiles)
    }

    data class ValidationResult(
        val isValid: Boolean,
        val missingFiles: List<String>
    )
}