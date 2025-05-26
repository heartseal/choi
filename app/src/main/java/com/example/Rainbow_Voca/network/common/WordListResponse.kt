package com.example.Rainbow_Voca.network.common

import com.example.Rainbow_Voca.model.Word

data class WordListResponse(
    val words: List<Word>? = null,
    val error: String? = null,
    val message: String? = null
)
