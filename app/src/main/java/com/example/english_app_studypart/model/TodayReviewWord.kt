package com.example.english_app_studypart.model

data class TodayReviewWord (
    val words: List<Word>
)

data class Word(
    val id: Int,
    val word: String,
    val meaning: String
)