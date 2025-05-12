package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.model.Word
import com.example.englishapp.model.WordListResponse
import com.example.englishapp.network.ApiServicePool
import kotlinx.coroutines.launch

// 다양한 복습 세션(10분 후, 누적 등)을 위한 단어 목록 관리
class ReviewViewModel : ViewModel() {

    // LiveData: 서버로부터 로드된 복습 대상 단어 목록 (Non-nullable List)
    private val _reviewableWords = MutableLiveData<List<Word>>() // 초기값은 여기서 설정 안 함 (로드 후 설정)
    val reviewableWords: LiveData<List<Word>> get() = _reviewableWords

    // LiveData: 단어 목록 로드 중 또는 기타 작업 중 발생한 오류 메시지
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    // 서버로부터 "10분 후 복습" 단어 목록을 가져옴
    fun loadPostLearningReviewWords(token: String) {
        _errorMessage.value = null // 이전 오류 메시지 초기화
        viewModelScope.launch {
            try {
                val response: WordListResponse = ApiServicePool.reviewApi.getPostLearningReviewWords("Bearer $token")
                // response.words가 null이면 emptyList()를, 아니면 response.words를 할당
                _reviewableWords.value = response.words ?: emptyList()

                // 단어가 실제로 없는 경우 (null이거나 비어있음) 에러 메시지 설정
                if (response.words.isNullOrEmpty()) {
                    _errorMessage.value = response.message ?: "10분 후 복습할 단어가 없습니다." // 문자열 리소스 사용 권장
                }
            } catch (e: Exception) {
                _reviewableWords.value = emptyList() // 오류 발생 시 빈 리스트
                _errorMessage.value = "10분 후 복습 단어 로딩 중 오류: ${e.message}" // 문자열 리소스 및 포맷팅 권장
            }
        }
    }

    // 서버로부터 "오늘의 누적 복습" 단어 목록을 가져옴
    fun loadStagedReviewWords(token: String) {
        _errorMessage.value = null // 이전 오류 메시지 초기화
        viewModelScope.launch {
            try {
                val response: WordListResponse = ApiServicePool.reviewApi.getTodayReviewWords("Bearer $token")
                // response.words가 null이면 emptyList()를, 아니면 response.words를 할당
                _reviewableWords.value = response.words ?: emptyList()

                if (response.words.isNullOrEmpty()) {
                    _errorMessage.value = response.message ?: "오늘 복습할 단어가 없습니다." // 문자열 리소스 사용 권장
                }
            } catch (e: Exception) {
                _reviewableWords.value = emptyList()
                _errorMessage.value = "오늘의 복습 단어 로딩 중 오류: ${e.message}" // 문자열 리소스 및 포맷팅 권장
            }
        }
    }

    // UI에서 오류 메시지를 확인(소비)한 후 ViewModel의 오류 상태를 초기화
    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}