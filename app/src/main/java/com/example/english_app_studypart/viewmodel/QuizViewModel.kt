package com.example.english_app_studypart.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.english_app_studypart.model.TodayCompleteResponse
import com.example.english_app_studypart.model.WordTest
import com.example.english_app_studypart.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


// 학습 완료 내용 전송용 뷰모델

class QuizViewModel : ViewModel() {

    fun sendTodayComplete(wordList: List<WordTest>) {
        RetrofitClient.WordStudiedApiService.postTodaySessionComplete(wordList)
            .enqueue(object : Callback<TodayCompleteResponse> {
                override fun onResponse(
                    call: Call<TodayCompleteResponse>,
                    response: Response<TodayCompleteResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.d("Review", "학습 완료 전송 성공: ${response.body()?.message}")
                    } else {
                        Log.e("Review", "응답 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<TodayCompleteResponse>, t: Throwable) {
                    Log.e("Review", "전송 실패: ${t.message}")
                }
            })
    }
}