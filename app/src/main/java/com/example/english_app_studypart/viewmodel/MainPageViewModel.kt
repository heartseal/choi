package com.example.english_app_studypart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.english_app_studypart.model.MainPageStudyGraph
import com.example.english_app_studypart.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// 그래프를 제외한 데이터들입니다.

class MainPageViewModel : ViewModel() {
    private val _totalWordCount = MutableLiveData<Int>() // 총 단어
    val totalWordCount: LiveData<Int> get() = _totalWordCount

    private val _todayReviewGoal = MutableLiveData<Int>() // 오늘 복습 단어 목표
    val todayReviewGoal: LiveData<Int> get() = _todayReviewGoal

    private val _todayLearningGoal = MutableLiveData<Int>() // 오늘 학습 단어 목표
    val todayLearningGoal: LiveData<Int> get() = _todayLearningGoal

    private val _estimatedCompletionDate = MutableLiveData<String>() // 예상 학습 완료 날짜??
    val estimatedCompletionDate: LiveData<String> get() = _estimatedCompletionDate

    private val _monthlyAttendanceRate = MutableLiveData<Double>() // 이 달의 성실도 퍼센트
    val monthlyAttendanceRate: LiveData<Double> get() = _monthlyAttendanceRate

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    fun fetchMainPageData() {
        RetrofitClient.mainPageGraphApiService.getMainPageGraphData()
            .enqueue(object : Callback<MainPageStudyGraph> {
                override fun onResponse(
                    call: Call<MainPageStudyGraph>,
                    response: Response<MainPageStudyGraph>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { data ->
                            // 각각의 필드 업데이트
                            _totalWordCount.value = data.totalWordCount
                            _todayReviewGoal.value = data.todayReviewGoal
                            _todayLearningGoal.value = data.todayLearningGoal
                            _estimatedCompletionDate.value = data.estimatedCompletionDate
                            _monthlyAttendanceRate.value = data.monthlyAttendanceRate
                        }
                    } else {
                        _errorMessage.value = "오류 발생: ${response.code()} - ${response.message()}"
                    }
                }

                override fun onFailure(call: Call<MainPageStudyGraph>, t: Throwable) {
                    _errorMessage.value = "네트워크 오류 발생: ${t.message}"
                }
            })
    }
}
