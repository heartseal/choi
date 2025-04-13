package com.example.english_app_studypart.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.english_app_studypart.model.MainPageStudyGraph
import com.example.english_app_studypart.model.StageCounts
import com.example.english_app_studypart.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// 메인페이지에 나올 그래프 데이터들이 있습니다.

class MainPageGraphViewModel : ViewModel() {
    private val _graphData = MutableLiveData<List<Pair<String, Int>>>()
    val graphData: LiveData<List<Pair<String, Int>>> get() = _graphData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    // ✅ 메인 페이지 데이터를 가져오는 메서드
    fun fetchMainPageData() {
        RetrofitClient.mainPageGraphApiService.getMainPageGraphData()
            .enqueue(object : Callback<MainPageStudyGraph> {
                override fun onResponse(
                    call: Call<MainPageStudyGraph>,
                    response: Response<MainPageStudyGraph>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { data ->
                            handleSuccess(data.stageCounts)
                        }
                    } else {
                        handleError("오류 발생: ${response.code()} - ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<MainPageStudyGraph>, t: Throwable) {
                    handleError("네트워크 오류 발생: ${t.message}")
                }
            })
    }

    // ✅ 성공적인 응답 처리
    private fun handleSuccess(stageCounts: StageCounts?) {
        stageCounts?.let {
            _graphData.value = toGraphData(it)
        }
    }

    // ✅ 그래프 데이터 변환
    private fun toGraphData(stageCounts: StageCounts): List<Pair<String, Int>> {
        return listOf(
            "미암기" to stageCounts.waiting,
            "씨앗" to stageCounts.seed,
            "새싹" to stageCounts.sprout,
            "잎새" to stageCounts.leaf,
            "가지" to stageCounts.branch,
            "열매" to stageCounts.fruit,
            "나무" to stageCounts.tree
        )
    }

    // ✅ 에러 처리
    private fun handleError(message: String) {
        _errorMessage.value = message
    }
}
