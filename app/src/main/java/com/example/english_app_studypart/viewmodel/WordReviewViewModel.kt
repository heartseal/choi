package com.example.english_app_studypart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.english_app_studypart.model.TodayReviewWord
import com.example.english_app_studypart.model.Word
import com.example.english_app_studypart.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 오늘의 복습 단어들을 정리한 뷰모델.

class WordReviewViewModel : ViewModel() {
    private val _reviewWords = MutableLiveData<List<Word>>()
    val reviewWords: LiveData<List<Word>> get() = _reviewWords

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun fetchTodayReviewWords() {
        RetrofitClient.WordReviewApiService.getTodayReviewWords()
            .enqueue(object : Callback<TodayReviewWord> {
                override fun onResponse(
                    call: Call<TodayReviewWord>,
                    response: Response<TodayReviewWord>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { data ->
                            _reviewWords.value = data.words // 복습 단어 목록 업데이트
                        }
                    } else {
                        _errorMessage.value = "오류 발생: ${response.code()} - ${response.message()}"
                    }
                }

                override fun onFailure(call: Call<TodayReviewWord>, t: Throwable) {
                    _errorMessage.value = "네트워크 오류 발생: ${t.message}"
                }
            })
    }
}
