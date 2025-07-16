package com.example.malayalamsignapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast

class ErrorHandler(private val context: Context) {

    fun handleModelLoadError(error: Exception) {
        Log.e("WordPrediction", "Model loading failed", error)
        showToast("Model loading failed. Please check if model.tflite exists in assets folder.")
    }

    fun handleScalerLoadError(error: Exception) {
        Log.e("WordPrediction", "Scaler loading failed", error)
        showToast("Scaling parameters not found. Using default values.")
    }

    fun handleLabelMapError(error: Exception) {
        Log.e("WordPrediction", "Label map loading failed", error)
        showToast("Label mapping failed. Check label_map.json in assets folder.")
    }

    fun handleCameraError(error: Exception) {
        Log.e("WordPrediction", "Camera error", error)
        showToast("Camera initialization failed: ${error.message}")
    }

    fun handleInferenceError(error: Exception) {
        Log.e("WordPrediction", "Inference error", error)
        showToast("Sign recognition failed. Please try again.")
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}