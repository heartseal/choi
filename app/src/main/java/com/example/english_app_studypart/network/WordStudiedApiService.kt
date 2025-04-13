package com.example.english_app_studypart.network

import com.example.english_app_studypart.model.TodayCompleteResponse
import com.example.english_app_studypart.model.WordTest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

//10분후 복습 결과 전송
interface WordStudiedApiService {
    @POST("/api/review/post-learning/results")
    fun postTodaySessionComplete(
        @Body wordList: List<WordTest>
    ): Call<TodayCompleteResponse>
}