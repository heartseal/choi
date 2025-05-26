package com.example.Rainbow_Voca.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.network.ApiServicePool
import com.example.Rainbow_Voca.network.MainPageResponse
import com.example.Rainbow_Voca.network.StageCounts // StageCounts DTO import
import kotlinx.coroutines.launch

// 메인 화면(대시보드) 데이터 관리
class MainPageViewModel : ViewModel() {

    // LiveData: 오늘의 복습 목표 단어 수
    private val _todayReviewGoal = MutableLiveData<Int>()
    val todayReviewGoal: LiveData<Int> = _todayReviewGoal

    // LiveData: 오늘의 학습 목표 단어 수
    private val _todayLearningGoal = MutableLiveData<Int>()
    val todayLearningGoal: LiveData<Int> = _todayLearningGoal

    // LiveData: 예상 학습 완료일
    private val _estimatedCompletionDate = MutableLiveData<String>()
    val estimatedCompletionDate: LiveData<String> = _estimatedCompletionDate

    // LiveData: 이달의 학습 성실도 (백분율)
    private val _monthlyAttendanceRate = MutableLiveData<Double>()
    val monthlyAttendanceRate: LiveData<Double> = _monthlyAttendanceRate

    // LiveData: 사용자의 전체 학습 단어 수
    private val _totalWordCount = MutableLiveData<Int>()
    val totalWordCount: LiveData<Int> = _totalWordCount

    // LiveData: 단계별 단어 수 (UI의 ProgressBar 등에 사용될 퍼센트 데이터)
    private val _stageProgressPercent = MutableLiveData<List<Int>>() // 이름 변경: stageProgress -> stageProgressPercent
    val stageProgressPercent: LiveData<List<Int>> = _stageProgressPercent

    // LiveData: 오늘의 학습 완료 여부 (버튼 상태 제어용)
    private val _isTodayLearningCompleted = MutableLiveData<Boolean>() // 이름 변경: isTodayLearningComplete -> isTodayLearningCompleted
    val isTodayLearningCompleted: LiveData<Boolean> = _isTodayLearningCompleted

    // LiveData: 10분 후 복습 가능 여부 (버튼 상태 제어용)
    private val _isPostLearningReviewReady = MutableLiveData<Boolean>()
    val isPostLearningReviewReady: LiveData<Boolean> = _isPostLearningReviewReady

    // LiveData: 데이터 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>() // 이름 변경: loading -> isLoading
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData: 오류 발생 시 사용자에게 표시할 메시지
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 서버에서 메인 페이지 정보를 가져와 LiveData 업데이트
    fun loadMainPageData(token: String) { // 함수명 변경: fetchMainPageData -> loadMainPageData
        _isLoading.value = true
        _errorMessage.value = null // 이전 오류 메시지 초기화

        viewModelScope.launch {
            try {
                val response = ApiServicePool.mainPageApi.getMainPageInfo("Bearer $token")
                if (response.error == null) {
                    // 응답 데이터를 사용하여 UI용 LiveData 업데이트
                    updateUiDataFromResponse(response)
                } else {
                    _errorMessage.value = response.message ?: "메인 화면 정보를 가져오는데 실패했습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "메인 화면 정보 로딩 중 네트워크 오류가 발생했습니다: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // MainPageResponse DTO를 사용하여 LiveData 값들을 업데이트
    private fun updateUiDataFromResponse(response: MainPageResponse) {
        _todayReviewGoal.value = response.todayReviewGoal
        _todayLearningGoal.value = response.todayLearningGoal
        _estimatedCompletionDate.value = response.estimatedCompletionDate
        _monthlyAttendanceRate.value = response.monthlyAttendanceRate
        _totalWordCount.value = response.totalWordCount
        _isTodayLearningCompleted.value = response.isTodayLearningComplete
        _isPostLearningReviewReady.value = response.isPostLearningReviewReady

        // 단계별 단어 수(StageCounts)를 퍼센트 리스트로 변환 (UI ProgressBar 용)
        _stageProgressPercent.value = calculateStageProgressPercents(response.stageCounts, response.totalWordCount)
    }

    // StageCounts와 전체 단어 수로부터 각 단계별 진행률(%) 리스트 계산
    private fun calculateStageProgressPercents(counts: StageCounts, totalWords: Int): List<Int> {
        val total = totalWords.toFloat()
        if (total == 0f) {
            // 단어가 없으면 모든 단계 진행률 0%
            return listOf(0, 0, 0, 0, 0, 0, 0)
        }
        // 순서: Red, Orange, Yellow, Green, Blue, Indigo, Violet (!!API 응답 순서와 일치하게 해줘야함!! 후에 수정 만약 하면 참고)
        return listOf(
            (counts.red / total * 100).toInt(),
            (counts.orange / total * 100).toInt(),
            (counts.yellow / total * 100).toInt(),
            (counts.green / total * 100).toInt(),
            (counts.blue / total * 100).toInt(),
            (counts.indigo / total * 100).toInt(),
            (counts.violet / total * 100).toInt()
        )
    }

    // 오류 메시지 LiveData 초기화
    fun consumeErrorMessage() {
        _errorMessage.value = null
    }
}