package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.model.Word
import com.example.englishapp.network.ApiServicePool
import kotlinx.coroutines.launch

class ReviewViewModel : ViewModel() {

    private val _postLearningWords = MutableLiveData<List<Word>>()
    val postLearningWords: LiveData<List<Word>> get() = _postLearningWords

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchPostLearningWords(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.reviewApi.getPostLearningReviewWords("Bearer $token")
                _postLearningWords.value = response.words ?: emptyList()
            } catch (e: Exception) {
                _postLearningWords.value = emptyList()
                _errorMessage.value = "단어 목록을 불러오지 못했습니다."
            }
        }
    }
}
