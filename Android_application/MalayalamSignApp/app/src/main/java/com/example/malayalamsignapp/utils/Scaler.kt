package com.example.malayalamsignapp.utils

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class Scaler(context: Context) {
    private val mean: FloatArray
    private val scale: FloatArray

    init {
        val inputStream = context.assets.open("scaler_params.json")
        val reader = BufferedReader(InputStreamReader(inputStream))
        val json = JSONObject(reader.readText())

        mean = json.getJSONArray("mean").let { array ->
            FloatArray(array.length()) { array.getDouble(it).toFloat() }
        }
        scale = json.getJSONArray("scale").let { array ->
            FloatArray(array.length()) { array.getDouble(it).toFloat() }
        }
    }

    fun transform(input: FloatArray): FloatArray {
        return input.indices.map { i ->
            (input[i] - mean[i]) / scale[i]
        }.toFloatArray()
    }
}
