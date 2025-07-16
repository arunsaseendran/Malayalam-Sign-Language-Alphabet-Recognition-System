package com.example.malayalamsignapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.malayalamsignapp.databinding.ActivityMainBinding
import com.example.malayalamsignapp.utils.*
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var signClassifier: SignClassifier
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech

    private var isSpeakingEnabled = true
    private var lastPredictionTime = 0L
    private val predictionInterval = 1500L
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private var latestBitmapWidth = 0
    private var latestBitmapHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        checkCameraPermission()

        binding.speakerButton.setOnClickListener {
            isSpeakingEnabled = !isSpeakingEnabled
            val icon = if (isSpeakingEnabled) {
                android.R.drawable.ic_lock_silent_mode_off
            } else {
                android.R.drawable.ic_lock_silent_mode
            }
            binding.speakerButton.setImageResource(icon)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("ml", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Malayalam TTS not supported", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            initializeComponents()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeComponents()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeComponents() {
        try {
            signClassifier = SignClassifier(this)

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("hand_landmarker.task")
                        .build()
                )
                .setMinHandDetectionConfidence(0.7f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(2)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result: HandLandmarkerResult, _: MPImage ->
                    processHandLandmarks(result)
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(this, options)
            startCamera()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing components", e)
            Toast.makeText(this, "Initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processHandLandmarks(result: HandLandmarkerResult) {
        runOnUiThread {
            val hands = result.landmarks()

            if (hands.isNotEmpty()) {
                binding.textView.text = "Hands detected: ${hands.size}"

                val allLandmarks = hands.flatten()
                binding.overlay.updateLandmarks(allLandmarks, latestBitmapWidth, latestBitmapHeight)

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastPredictionTime > predictionInterval) {
                    lastPredictionTime = currentTime

                    val features = convertLandmarksToFeatures(hands)
                    val (prediction, confidence) = signClassifier.classify(features)

                    if (prediction.isNotEmpty()) {
                        binding.resultText.text = "$prediction (${(confidence * 100).toInt()}%)"
                        if (isSpeakingEnabled) {
                            tts.speak(prediction, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    } else {
                        binding.resultText.text = ""
                    }
                }
            } else {
                binding.textView.text = "No hands detected"
                binding.resultText.text = ""
                binding.overlay.updateLandmarks(emptyList(), latestBitmapWidth, latestBitmapHeight)
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val bitmap = mediaImage.toBitmap(imageProxy.imageInfo.rotationDegrees).mirrorHorizontally()
                    latestBitmapWidth = bitmap.width
                    latestBitmapHeight = bitmap.height

                    val mpImage = BitmapImageBuilder(bitmap).build()
                    handLandmarker.detectAsync(mpImage, imageProxy.imageInfo.timestamp)
                }
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun Image.toBitmap(rotationDegrees: Int): Bitmap {
        val nv21 = yuv420ToNv21()
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val yuvByteArray = out.toByteArray()
        var bitmap = BitmapFactory.decodeByteArray(yuvByteArray, 0, yuvByteArray.size)

        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        return bitmap
    }

    private fun Image.yuv420ToNv21(): ByteArray {
        val ySize = planes[0].buffer.remaining()
        val uSize = planes[1].buffer.remaining()
        val vSize = planes[2].buffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        planes[0].buffer.get(nv21, 0, ySize)
        planes[2].buffer.get(nv21, ySize, vSize)
        planes[1].buffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        if (::handLandmarker.isInitialized) handLandmarker.close()
        if (::signClassifier.isInitialized) signClassifier.close()
        cameraExecutor.shutdown()
    }
}
