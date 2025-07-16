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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.malayalamsignapp.databinding.ActivityWordPredictionBinding
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
import android.content.Context
import android.os.Vibrator

class WordPredictionActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityWordPredictionBinding
    private lateinit var handLandmarker: HandLandmarker
    private lateinit var signClassifier: SignClassifier
    private lateinit var wordPredictor: WordPredictor
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech
    private lateinit var wordSuggestionsAdapter: WordSuggestionsAdapter

    private var isSpeakingEnabled = true
    private var lastPredictionTime = 0L
    private val predictionInterval = 800L // Slightly longer for word building
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    private var latestBitmapWidth = 0
    private var latestBitmapHeight = 0
    private var currentAlphabetSequence = mutableListOf<String>()
    private var lastDetectedAlphabet = ""
    private var lastAlphabetTime = 0L
    private val alphabetHoldTime = 1500L // Hold gesture for 1.5 seconds to confirm

    private var countdownTimer = 0L
    private val countdownDuration = 1500L // 1.5 seconds to hold gesture
    private val cooldownDuration = 1000L // 1 second cooldown after adding alphabet
    private var isInCooldown = false
    private var cooldownStartTime = 0L

    private fun validateRequiredAssets(): Boolean {
        val validation = ModelValidator.validateAssets(this)
        if (!validation.isValid) {
            val missingFiles = validation.missingFiles.joinToString(", ")
            Toast.makeText(this, "Missing required files: $missingFiles", Toast.LENGTH_LONG).show()
            Log.e("WordPredictionActivity", "Missing files: $missingFiles")
            return false
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordPredictionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
        tts = TextToSpeech(this, this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        wordPredictor = WordPredictor(this)

        checkCameraPermission()
    }

    private fun initializeUI() {
        // Setup RecyclerView for word suggestions
        wordSuggestionsAdapter = WordSuggestionsAdapter { word ->
            speakWord(word)
        }
        binding.recyclerViewSuggestions.apply {
            layoutManager = LinearLayoutManager(this@WordPredictionActivity)
            adapter = wordSuggestionsAdapter
        }

        // Setup buttons
        binding.speakerButton.setOnClickListener {
            toggleSpeaker()
        }

        binding.clearButton.setOnClickListener {
            clearCurrentSequence()
        }

        binding.backspaceButton.setOnClickListener {
            removeLastAlphabet()
        }

        // Update UI initially
        updateSequenceDisplay()
    }

    private fun toggleSpeaker() {
        isSpeakingEnabled = !isSpeakingEnabled
        val icon = if (isSpeakingEnabled) {
            android.R.drawable.ic_lock_silent_mode_off
        } else {
            android.R.drawable.ic_lock_silent_mode
        }
        binding.speakerButton.setImageResource(icon)

        val message = if (isSpeakingEnabled) "Speech enabled" else "Speech disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearCurrentSequence() {
        currentAlphabetSequence.clear()
        updateSequenceDisplay()
        updateWordSuggestions()
    }

    private fun removeLastAlphabet() {
        if (currentAlphabetSequence.isNotEmpty()) {
            currentAlphabetSequence.removeAt(currentAlphabetSequence.size - 1)
            updateSequenceDisplay()
            updateWordSuggestions()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("ml", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault())
                Toast.makeText(this, "Malayalam TTS not available, using default", Toast.LENGTH_LONG).show()
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
            // Validate assets first
            if (!validateRequiredAssets()) {
                Toast.makeText(this, "Required model files are missing", Toast.LENGTH_LONG).show()
                return
            }

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
            Log.e("WordPredictionActivity", "Error initializing components", e)
            ErrorHandler(this).handleModelLoadError(e)
        }
    }

    // Updated processHandLandmarks method - simplified countdown without colors
    private fun processHandLandmarks(result: HandLandmarkerResult) {
        runOnUiThread {
            val hands = result.landmarks()
            val currentTime = System.currentTimeMillis()

            if (hands.isNotEmpty()) {
                binding.textView.text = "Hands detected: ${hands.size}"

                val allLandmarks = hands.flatten()
                binding.overlay.updateLandmarks(allLandmarks, latestBitmapWidth, latestBitmapHeight)

                // Check if we're in cooldown period
                if (isInCooldown) {
                    val cooldownRemaining = cooldownDuration - (currentTime - cooldownStartTime)
                    if (cooldownRemaining > 0) {
                        binding.currentGestureText.text = "Next gesture: ${cooldownRemaining.toInt()}ms"
                        return@runOnUiThread
                    } else {
                        // Cooldown finished
                        isInCooldown = false
                        lastDetectedAlphabet = "" // Reset last detected alphabet after cooldown
                    }
                }

                // Process prediction only if not in cooldown and enough time has passed since last prediction
                if (!isInCooldown && currentTime - lastPredictionTime > predictionInterval) {
                    lastPredictionTime = currentTime

                    // Convert landmarks to features (You MUST review and potentially adjust this function)
                    val features = convertLandmarksToFeatures(hands)
                    val (prediction, confidence) = signClassifier.classify(features)

                    if (prediction.isNotEmpty() && confidence > 0.75f) { // Lowered confidence threshold

                        if (prediction == lastDetectedAlphabet) {
                            // Same alphabet detected - show countdown
                            val holdDuration = currentTime - lastAlphabetTime
                            val remainingTime = countdownDuration - holdDuration

                            if (holdDuration < countdownDuration) {
                                val remainingMs = remainingTime.toInt()
                                binding.currentGestureText.text = "$prediction - Hold: ${remainingMs}ms"
                            } else {
                                // Held long enough, add to sequence
                                // Modified condition: add if sequence is empty or the last alphabet is different
                                if (currentAlphabetSequence.isEmpty() || currentAlphabetSequence.last() != prediction) {
                                    addAlphabetToSequence(prediction)
                                    startCooldownPeriod(currentTime)
                                } else {
                                    // If it's the same as the last one and we are not in cooldown,
                                    // it means the user is holding the same sign again.
                                    // Reset the hold time to allow adding it again after cooldown
                                    lastAlphabetTime = currentTime
                                }
                            }
                        } else {
                            // New alphabet detected
                            if (!isInCooldown) {
                                lastDetectedAlphabet = prediction
                                lastAlphabetTime = currentTime
                                binding.currentGestureText.text = "$prediction - Hold steady..."
                            }
                        }
                    } else {
                        // Low confidence or no prediction
                        if (!isInCooldown) {
                            binding.currentGestureText.text = "Hold gesture steady and clear..."
                            lastDetectedAlphabet = "" // Reset last detected alphabet
                        }
                    }
                }
            } else {
                // No hands detected
                binding.textView.text = "Show your hand to camera"
                binding.currentGestureText.text = ""
                binding.overlay.updateLandmarks(emptyList(), latestBitmapWidth, latestBitmapHeight)
                lastDetectedAlphabet = ""
                isInCooldown = false
            }
        }
    }

    // Function to convert MediaPipe landmarks to a FloatArray for the TFLite model.
    // YOU MUST VERIFY AND ADJUST THIS FUNCTION BASED ON YOUR MODEL'S INPUT FORMAT.
    // This is a basic example assuming 2 hands and 21 landmarks per hand (x, y, z).
    private fun convertLandmarksToFeatures(hands: List<List<NormalizedLandmark>>): FloatArray {
        val features = mutableListOf<Float>()
        // Assuming your model expects features for up to two hands.
        // If your model only uses one hand, adjust this loop.
        for (i in 0 until 2) {
            if (i < hands.size) {
                val handLandmarks = hands[i]
                for (landmark in handLandmarks) {
                    features.add(landmark.x())
                    features.add(landmark.y())
                    features.add(landmark.z())
                }
            } else {
                // Add zeros for missing hands or landmarks to maintain consistent feature size.
                // The total size should match what your TFLite model expects (e.g., 126 for 2 hands * 21 landmarks * 3 coords).
                repeat(21 * 3) { // Assuming 21 landmarks and 3 coordinates (x, y, z) per landmark
                    features.add(0f)
                }
            }
        }
        // Ensure the feature array has the exact size expected by your TFLite model.
        // Adjust the expected size (126) if your model is different.
        val expectedFeatureSize = 126
        if (features.size != expectedFeatureSize) {
            Log.e("WordPredictionActivity", "Feature size mismatch: Expected $expectedFeatureSize, got ${features.size}")
            // Pad or truncate the features to match the expected size
            // This approach pads with zeros if needed
            val paddedFeatures = FloatArray(expectedFeatureSize) { 0f }
            features.take(expectedFeatureSize).toFloatArray().copyInto(paddedFeatures)
            return paddedFeatures

        }
        return features.toFloatArray()
    }


    private fun startCooldownPeriod(currentTime: Long) {
        isInCooldown = true
        cooldownStartTime = currentTime
    }

    private fun addAlphabetToSequence(alphabet: String) {
        currentAlphabetSequence.add(alphabet)
        updateSequenceDisplay()
        updateWordSuggestions()

        // Enhanced visual feedback
        binding.currentGestureText.text = "✓ Added: $alphabet"
        binding.currentGestureText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

        // Safe haptic feedback implementation
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Android 12+ approach
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                // Older Android versions
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore vibrator errors - not critical for app functionality
            Log.d("WordPredictionActivity", "Vibration not available: ${e.message}")
        }
    }

    private fun updateSequenceDisplay() {
        val sequence = currentAlphabetSequence.joinToString(" + ")
        binding.alphabetSequenceText.text = if (sequence.isEmpty()) {
            "Alphabet sequence will appear here..."
        } else {
            sequence
        }
    }

    private fun updateWordSuggestions() {
        if (currentAlphabetSequence.isNotEmpty()) {
            val suggestions = wordPredictor.predictWords(currentAlphabetSequence)
            wordSuggestionsAdapter.updateSuggestions(suggestions)

            binding.suggestionCountText.text = "${suggestions.size} word suggestions"

            // Speaking logic - speak if speaking enabled, suggestions exist, not in cooldown,
            // and either it's an exact match or a high-scoring suggestion with at least 2 alphabets (adjusted)
            if (isSpeakingEnabled && suggestions.isNotEmpty() && !isInCooldown) {
                val topSuggestion = suggestions.first()
                if (topSuggestion.isExactMatch ||
                    (currentAlphabetSequence.size >= 2 && topSuggestion.matchScore > 40)) { // Changed >=3 to >=2
                    speakWord(topSuggestion.word)
                }
            }
        } else {
            wordSuggestionsAdapter.updateSuggestions(emptyList())
            binding.suggestionCountText.text = "Start making gestures to see suggestions"
        }
    }



    private fun speakWord(word: String) {
        if (isSpeakingEnabled) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
        }
        Toast.makeText(this, "Speaking: $word", Toast.LENGTH_SHORT).show()
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
