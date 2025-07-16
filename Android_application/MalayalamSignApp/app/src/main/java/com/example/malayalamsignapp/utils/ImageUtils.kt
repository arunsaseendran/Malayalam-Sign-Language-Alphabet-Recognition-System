package com.example.malayalamsignapp.utils

import android.graphics.Bitmap
import android.graphics.Matrix

fun Bitmap.mirrorHorizontally(): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
