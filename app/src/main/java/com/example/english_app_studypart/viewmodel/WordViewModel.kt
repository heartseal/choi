package com.example.english_app_studypart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.english_app_studypart.datas.Word
import com.example.english_app_studypart.features.word.WordRandomizer
import com.example.english_app_studypart.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// 오늘의 학습 단어들의 뷰모델.


class WordViewModel : ViewModel() {
    private val _wholeList = MutableLiveData<List<Word>>()
    val wholeList: LiveData<List<Word>> get() = _wholeList

    private val _selectedWords = MutableLiveData<List<Word>>()
    val selectedWords: LiveData<List<Word>> get() = _selectedWords

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private var isSelectedWordsGenerated = false // 학습 단어 리스트 생성 여부 추적

    // ✅ 단어 리스트 가져오기
    fun fetchAllWords(onDataLoaded: (List<Word>) -> Unit) {
        RetrofitClient.wordApiService.getAllWords().enqueue(object : Callback<List<Word>> {
            override fun onResponse(call: Call<List<Word>>, response: Response<List<Word>>) {
                if (response.isSuccessful) {
                    handleSuccess(response.body(), onDataLoaded)
                } else {
                    handleError("오류 발생: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<Word>>, t: Throwable) {
                handleError("네트워크 오류: ${t.message}")
            }
        })
    }

    // ✅ 성공적인 응답 처리
    private fun handleSuccess(words: List<Word>?, onDataLoaded: (List<Word>) -> Unit) {
        val wordList = words ?: emptyList()
        _wholeList.value = wordList

        // 기존에 생성된 것이 없을 경우 생성
        if (!isSelectedWordsGenerated) {
            generateSelectedWords(wordList)
        }

        onDataLoaded(_selectedWords.value ?: emptyList())
    }

    // ✅ 학습 단어 리스트 생성
    private fun generateSelectedWords(wordList: List<Word>) {
        val todayStudySize = 10 // 학습 단어 수 설정
        _selectedWords.value = WordRandomizer.generateTodayStudyList(wordList, todayStudySize)
        isSelectedWordsGenerated = true
    }

    // ✅ 오류 처리
    private fun handleError(message: String) {
        _errorMessage.value = message
    }
}

