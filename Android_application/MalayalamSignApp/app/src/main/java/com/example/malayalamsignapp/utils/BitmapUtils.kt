package com.example.malayalamsignapp.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    // Correct NV21 format: Y + V + U order for bytes
    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    uBuffer.get(nv21, ySize, uSize)     // Swap these two buffers' order
    vBuffer.get(nv21, ySize + uSize, vSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()

    // Decode with ARGB_8888 config for MediaPipe compatibility
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

    // Ensure ARGB_8888 format
    if (bitmap.config != Bitmap.Config.ARGB_8888) {
        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        bitmap.recycle()
        bitmap = argbBitmap
    }

    // Rotate bitmap to correct orientation
    val matrix = Matrix()
    matrix.postRotate(imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}