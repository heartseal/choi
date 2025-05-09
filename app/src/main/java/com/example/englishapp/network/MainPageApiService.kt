package com.example.englishapp.network

import retrofit2.http.GET
import retrofit2.http.Header

// 단계별 단어 수 (색상별)
data class StageCounts(
    val red: Int,
    val orange: Int,
    val yellow: Int,
    val green: Int,
    val blue: Int,
    val indigo: Int,
    val violet: Int
)

// 메인 페이지 정보 응답
data class MainPageResponse(
    val totalWordCount: Int,
    val stageCounts: StageCounts,
    val todayReviewGoal: Int,
    val todayLearningGoal: Int,
    val estimatedCompletionDate: String,
    val monthlyAttendanceRate: Double,
    val isTodayLearningComplete: Boolean,
    val isPostLearningReviewReady: Boolean,
    val error: String? = null,
    val message: String? = null
)

interface MainPageApiService {
    // 메인 페이지 정보 조회
    @GET("372e9f65-502d-430a-80eb-44a1d26f8c37")
    //"/api/dashboard"
    suspend fun getMainPageInfo(
        @Header("Authorization") token: String
    ): MainPageResponse
}