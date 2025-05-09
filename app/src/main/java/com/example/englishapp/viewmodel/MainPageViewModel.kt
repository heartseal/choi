package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.network.ApiServicePool
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.launch

class MainPageViewModel : ViewModel() {
    // 오늘의 복습, 학습, 예상완료일, 성실도
    private val _todayReviewGoal = MutableLiveData<Int>()
    val todayReviewGoal: LiveData<Int> = _todayReviewGoal

    private val _todayLearningGoal = MutableLiveData<Int>()
    val todayLearningGoal: LiveData<Int> = _todayLearningGoal

    private val _estimatedCompletionDate = MutableLiveData<String>()
    val estimatedCompletionDate: LiveData<String> = _estimatedCompletionDate

    private val _monthlyAttendanceRate = MutableLiveData<Double>()
    val monthlyAttendanceRate: LiveData<Double> = _monthlyAttendanceRate

    private val _totalWordCount = MutableLiveData<Int>()
    val totalWordCount: LiveData<Int> = _totalWordCount

    // 색상별 단어 수 → ProgressBar용 데이터 (red, orange, yellow, green, blue, indigo, violet)
    private val _stageProgress = MutableLiveData<List<Int>>()
    val stageProgress: LiveData<List<Int>> = _stageProgress

    // 로딩/에러
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun fetchMainPage(token: String) {
        _loading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val response = ApiServicePool.mainPageApi.getMainPageInfo("Bearer $token")
                if (response.error == null) {
                    // 기본 정보
                    _todayReviewGoal.value = response.todayReviewGoal
                    _todayLearningGoal.value = response.todayLearningGoal
                    _estimatedCompletionDate.value = response.estimatedCompletionDate
                    _monthlyAttendanceRate.value = response.monthlyAttendanceRate
                    _totalWordCount.value = response.totalWordCount

                    // 색상별 단어 수 → ProgressBar 퍼센트 계산
                    val total = response.totalWordCount.toFloat()
                    val counts = response.stageCounts
                    val progressValues = listOf(
                        if (total == 0f) 0 else ((counts.red / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.orange / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.yellow / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.green / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.blue / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.indigo / total) * 100).toInt(),
                        if (total == 0f) 0 else ((counts.violet / total) * 100).toInt()
                    )
                    _stageProgress.value = progressValues
                } else {
                    _errorMessage.value = response.message ?: "서버 오류가 발생했습니다."
                }
            } catch (e: Exception) {
                _errorMessage.value = "네트워크 오류: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }
}
