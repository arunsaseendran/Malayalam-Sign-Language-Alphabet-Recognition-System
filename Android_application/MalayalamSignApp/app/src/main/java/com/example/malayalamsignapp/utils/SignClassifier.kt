package com.example.malayalamsignapp.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SignClassifier(context: Context) {
    private val interpreter: Interpreter
    private val scaler: Scaler
    private val labelMap: Map<Int, String>
    private val confidenceThreshold = 0.75f

    init {
        val modelBuffer = loadModelFile(context)
        interpreter = Interpreter(modelBuffer)

        scaler = Scaler(context)
        labelMap = loadLabelMap(context)

        Log.d("SignClassifier", "Initialized with ${labelMap.size} classes")
    }

    fun classify(landmarks: FloatArray): Pair<String, Float> {
        // Ensure we have exactly 126 features
        val normalizedLandmarks = if (landmarks.size == 126) {
            landmarks
        } else {
            Log.w("SignClassifier", "Expected 126 features, got ${landmarks.size}")
            val padded = FloatArray(126) { 0f }
            val copySize = minOf(landmarks.size, 126)
            System.arraycopy(landmarks, 0, padded, 0, copySize)
            padded
        }

        // Apply scaling
        val scaled = scaler.transform(normalizedLandmarks)

        // Prepare input buffer
        val inputBuffer = ByteBuffer.allocateDirect(126 * 4).apply {
            order(ByteOrder.nativeOrder())
            scaled.forEach { putFloat(it) }
        }

        // Prepare output buffer
        val numClasses = labelMap.size
        val outputBuffer = ByteBuffer.allocateDirect(numClasses * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        // Run inference
        interpreter.run(inputBuffer, outputBuffer)

        // Extract probabilities
        outputBuffer.rewind()
        val probabilities = FloatArray(numClasses)
        outputBuffer.asFloatBuffer().get(probabilities)

        // Debug: log top 3 predictions
        val top3 = probabilities.mapIndexed { idx, prob -> idx to prob }
            .sortedByDescending { it.second }
            .take(3)
        Log.d("TopPredictions", top3.joinToString { "${labelMap[it.first]}: ${(it.second * 100).toInt()}%" })

        // Pick best prediction
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIndex]

        return if (confidence > confidenceThreshold) {
            Pair(labelMap[maxIndex] ?: "Unknown", confidence)
        } else {
            Pair("", confidence)
        }
    }


    private fun FloatArray.indexOfMax(): Int {
        return this.indices.maxByOrNull { this[it] } ?: 0
    }

    private fun loadLabelMap(context: Context): Map<Int, String> {
        val inputStream = context.assets.open("label_map.json")
        val json = JSONObject(inputStream.bufferedReader().readText())
        val map = mutableMapOf<Int, String>()
        for (key in json.keys()) {
            map[key.toInt()] = json.getString(key)
        }
        return map
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter.close()
    }
}