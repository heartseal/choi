package com.example.english_app_studypart.model

data class WholeWordResponse(
    val totalCount: Int,
    val totalPages: Int,
    val currentPage: Int,
    val words: List<WholeWord>
)

data class WholeWord(
    val id: Int,
    val word: String,
    val meaning: String
)
