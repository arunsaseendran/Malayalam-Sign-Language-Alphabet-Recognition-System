package com.example.malayalamsignapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.malayalamsignapp.utils.WordSuggestion

class WordSuggestionsAdapter(
    private val onWordClick: (String) -> Unit
) : RecyclerView.Adapter<WordSuggestionsAdapter.WordViewHolder>() {

    private var suggestions = listOf<WordSuggestion>()

    fun updateSuggestions(newSuggestions: List<WordSuggestion>) {
        suggestions = newSuggestions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_suggestion, parent, false)
        return WordViewHolder(view)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(suggestions[position])
    }

    override fun getItemCount(): Int = suggestions.size

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardWordSuggestion)
        private val wordText: TextView = itemView.findViewById(R.id.textWordSuggestion)
        private val scoreText: TextView = itemView.findViewById(R.id.textMatchScore)
        private val exactMatchIndicator: View = itemView.findViewById(R.id.exactMatchIndicator)

        fun bind(suggestion: WordSuggestion) {
            wordText.text = suggestion.word
            scoreText.text = "${suggestion.matchScore}%"

            // Show exact match indicator
            exactMatchIndicator.visibility = if (suggestion.isExactMatch) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set card color based on match quality
            val cardColor = when {
                suggestion.isExactMatch -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
                suggestion.matchScore > 50 -> ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light)
                suggestion.matchScore > 20 -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_light)
                else -> ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
            }

            cardView.setCardBackgroundColor(cardColor)

            // Set click listener
            cardView.setOnClickListener {
                onWordClick(suggestion.word)

                // Add visual feedback
                cardView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        cardView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                    }
            }
        }
    }
}