package com.example.malayalamsignapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.snackbar.Snackbar

class HomeActivity : AppCompatActivity() {

    private lateinit var cardAlphabetRecognition: CardView
    private lateinit var cardWordPrediction: CardView
    private lateinit var cardSentencePrediction: CardView
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        initializeViews()

        // Set up click listeners
        setupClickListeners()

        // Set up back press handling
        setupBackPressHandling()

        // Add entrance animations
        addEntranceAnimations()
    }

    private fun initializeViews() {
        cardAlphabetRecognition = findViewById(R.id.cardAlphabetRecognition)
        cardWordPrediction = findViewById(R.id.cardWordPrediction)
        cardSentencePrediction = findViewById(R.id.cardSentencePrediction)
    }

    private fun setupClickListeners() {
        // Alphabet Recognition Card Click
        cardAlphabetRecognition.setOnClickListener {
            // Add click animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            // Navigate to MainActivity (Alphabet Recognition)
                            val intent = Intent(this@HomeActivity, MainActivity::class.java)
                            startActivity(intent)
                            // Add transition animation
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        }
                }
        }

        // Word Prediction Card Click - Now fully functional
        cardWordPrediction.setOnClickListener {
            // Add click animation
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction {
                            // Navigate to WordPredictionActivity
                            val intent = Intent(this@HomeActivity, WordPredictionActivity::class.java)
                            startActivity(intent)
                            // Add transition animation
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        }
                }
        }

        // Sentence Prediction Card Click (Coming Soon)
        cardSentencePrediction.setOnClickListener {
            showComingSoonMessage("Sentence Prediction feature is coming soon!")
        }
    }

    private fun addEntranceAnimations() {
        // Load animations
        val slideInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        // Apply animations to cards with delays
        cardAlphabetRecognition.startAnimation(slideInAnimation)

        cardWordPrediction.postDelayed({
            cardWordPrediction.startAnimation(slideInAnimation)
        }, 200)

        cardSentencePrediction.postDelayed({
            cardSentencePrediction.startAnimation(slideInAnimation)
        }, 400)
    }

    private fun showComingSoonMessage(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .setAction("OK") { }
            .setActionTextColor(getColor(android.R.color.holo_orange_light))
            .show()
    }

    private fun setupBackPressHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    // If back was pressed within 2 seconds, exit the app
                    finish()
                } else {
                    // Show exit confirmation
                    showExitConfirmation()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onBackPressed() {
        // Override back button to show exit confirmation
        showExitConfirmation()
    }

    private fun showExitConfirmation() {
        Snackbar.make(findViewById(android.R.id.content), "Press back again to exit", Snackbar.LENGTH_SHORT)
            .setActionTextColor(getColor(android.R.color.holo_orange_light))
            .show()
    }
}