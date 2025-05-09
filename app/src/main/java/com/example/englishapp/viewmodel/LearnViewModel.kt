package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.manager.StudyManager
import com.example.englishapp.model.Word
import com.example.englishapp.model.WordListResponse
import com.example.englishapp.network.ApiServicePool
import kotlinx.coroutines.launch

// study랑 연동 오늘의 학습

class LearnViewModel : ViewModel() {
    private var studyManager: StudyManager? = null
    private val _currentWord = MutableLiveData<Word?>()
    val currentWord: LiveData<Word?> = _currentWord

    private val _studyList = MutableLiveData<List<Word>>()
    val studyList: LiveData<List<Word>> = _studyList

    private val _isFinished = MutableLiveData(false)
    val isFinished: LiveData<Boolean> = _isFinished

    private val _completeResult = MutableLiveData<Boolean?>()
    val completeResult: LiveData<Boolean?> = _completeResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage


    fun fetchTodayWords(token: String) {
        viewModelScope.launch {
            try {
                val response: WordListResponse =
                    ApiServicePool.learnApi.getTodayWords("Bearer $token")
                _studyList.value = response.words ?: emptyList()
            } catch (e: Exception) {
                _studyList.value = emptyList()
                _errorMessage.value = "단어 목록을 불러오지 못했습니다."
            }
        }
    }

    fun startStudy(words: List<Word>) {
        studyManager = StudyManager(words) // StudyManager가 내부에서 랜덤 10개 추출
        _currentWord.value = studyManager?.getCurrentWord()
        _isFinished.value = false
    }

    fun nextWord() {
        studyManager?.let {
            if (it.moveToNext()) {
                _currentWord.value = it.getCurrentWord()
            } else {
                _isFinished.value = true
            }
        }
    }

    fun reportTodayComplete(token: String) {
        val manager = studyManager
        if (manager == null) {
            _completeResult.value = false
            _errorMessage.value = "학습 데이터가 없습니다."
            return
        }
        viewModelScope.launch {
            try {
                // 임시로 완료 처리 되는 코드?
                _completeResult.value = true
                // 실제 서버 연동 시 아래 코드 사용
                /*
                val request = TodayCompleteRequest(manager.getStudyList().map { it.id })
                val response = ApiServicePool.learnApi.completeTodaySession("Bearer $token", request)
                _completeResult.value = response.success
                */
            } catch (e: Exception) {
                _completeResult.value = false
                _errorMessage.value = "학습 완료 보고 실패"
            }
        }
    }

    // 외부에서 리스트 접근 가능한 메서드
    fun getStudyList(): List<Word> = studyManager?.getStudyList() ?: emptyList()
}
