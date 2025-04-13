package com.example.english_app_studypart.network

import com.example.english_app_studypart.model.TodayReviewWord
import retrofit2.Call
import retrofit2.http.GET

interface WordReviewApiService {
    @GET("/api/review/staged/today-words")
    fun getTodayReviewWords(): Call<TodayReviewWord>
}