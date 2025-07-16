package com.example.malayalamsignapp.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

data class WordSuggestion(
    val word: String,
    val matchScore: Int,
    val isExactMatch: Boolean
)

class WordPredictor(private val context: Context) {

    private val malayalamWords = mutableListOf<String>()
    private val maxSuggestions = 10

    init {
        loadMalayalamWords()
    }

    private fun loadMalayalamWords() {
        try {
            val inputStream = context.assets.open("1malayalam_words.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))

            reader.useLines { lines ->
                lines.forEach { word ->
                    val trimmed = word.trim()
                    if (trimmed.isNotEmpty()) {
                        malayalamWords.add(trimmed)
                    }
                }
            }

            Log.d("WordPredictor", "Loaded ${malayalamWords.size} Malayalam words")
        } catch (e: Exception) {
            Log.e("WordPredictor", "Error loading Malayalam words", e)
            // Fallback to some basic words
            loadFallbackWords()
        }
    }

    private fun loadFallbackWords() {
        malayalamWords.addAll(listOf(
            "അമ്മ", "അച്ഛന്‍", "വീട്"
        ))
    }

    fun predictWords(alphabetSequence: List<String>): List<WordSuggestion> {
        if (alphabetSequence.isEmpty()) return emptyList()

        val sequenceString = alphabetSequence.joinToString("")
        val suggestions = mutableListOf<WordSuggestion>()

        // Find words that match the sequence pattern
        for (word in malayalamWords) {
            val matchResult = calculateMatch(word, alphabetSequence)
            if (matchResult.matchScore > 0) {
                suggestions.add(WordSuggestion(
                    word = word,
                    matchScore = matchResult.matchScore,
                    isExactMatch = matchResult.isExactMatch
                ))
            }
        }

        // Sort by match score (higher is better) and limit results
        return suggestions
            .sortedWith(compareByDescending<WordSuggestion> { it.isExactMatch }
                .thenByDescending { it.matchScore }
                .thenBy { it.word.length })
            .take(maxSuggestions)
    }

    private fun calculateMatch(word: String, sequence: List<String>): MatchResult {
        val wordChars = word.toList().map { it.toString() }
        var matchScore = 0
        var isExactMatch = false

        // Strategy 1: Check if sequence is a prefix of the word
        if (wordChars.size >= sequence.size) {
            var prefixMatch = true
            for (i in sequence.indices) {
                if (i < wordChars.size && wordChars[i] == sequence[i]) {
                    matchScore += 10 // High score for position match
                } else {
                    prefixMatch = false
                    break
                }
            }

            if (prefixMatch) {
                if (sequence.size == wordChars.size) {
                    isExactMatch = true
                    matchScore += 50 // Bonus for exact match
                } else {
                    matchScore += 20 // Bonus for prefix match
                }
            }
        }

        // Strategy 2: Check if all sequence characters appear in word (order matters)
        if (matchScore == 0) {
            var wordIndex = 0
            var sequenceMatches = 0

            for (seqChar in sequence) {
                while (wordIndex < wordChars.size) {
                    if (wordChars[wordIndex] == seqChar) {
                        sequenceMatches++
                        wordIndex++
                        break
                    }
                    wordIndex++
                }
            }

            if (sequenceMatches == sequence.size) {
                matchScore = sequenceMatches * 5 // Medium score for subsequence match
            }
        }

        // Strategy 3: Partial character matches
        if (matchScore == 0) {
            for (seqChar in sequence) {
                if (seqChar in wordChars) {
                    matchScore += 1 // Low score for character presence
                }
            }
        }

        return MatchResult(matchScore, isExactMatch)
    }

    private data class MatchResult(val matchScore: Int, val isExactMatch: Boolean)

    // Additional utility function for advanced prediction
    fun predictWordsWithContext(alphabetSequence: List<String>, context: String = ""): List<WordSuggestion> {
        val basePredictions = predictWords(alphabetSequence)

        // If context is provided, we could boost scores for contextually relevant words
        // This is a placeholder for future enhancement
        return basePredictions
    }

    // Function to add custom words dynamically
    fun addCustomWord(word: String) {
        if (word.trim().isNotEmpty() && !malayalamWords.contains(word.trim())) {
            malayalamWords.add(word.trim())
            Log.d("WordPredictor", "Added custom word: $word")
        }
    }

    // Function to get word suggestions for partial matches
    fun getPartialMatches(partialWord: String): List<String> {
        return malayalamWords.filter { word ->
            word.startsWith(partialWord) || word.contains(partialWord)
        }.take(5)
    }
}