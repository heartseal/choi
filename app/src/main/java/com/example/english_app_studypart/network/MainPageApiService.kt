package com.example.english_app_studypart.network

import com.example.english_app_studypart.model.MainPageStudyGraph
import retrofit2.Call
import retrofit2.http.GET

// 메인 화면 정보 조회
interface MainPageApiService {
    @GET("api/dashboard")
    fun getMainPageGraphData(): Call<MainPageStudyGraph>
}