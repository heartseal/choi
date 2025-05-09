package com.example.englishapp.network

import kotlin.getValue
import kotlin.jvm.java

// 모든 API 서비스를 한 곳에서 관리하는 객체
object ApiServicePool {
    // User 관련
    val userApi: UserApiService by lazy {
        RetrofitInstance.retrofit.create(UserApiService::class.java)
    }

    // 전체 단어 조회 & 오늘 단어 설정
    val wordApi: WordApiService by lazy {
        RetrofitInstance.retrofit.create(WordApiService::class.java)
    }

    // 메인 페이지
    val mainPageApi: MainPageApiService by lazy {
        RetrofitInstance.retrofit.create(MainPageApiService::class.java)
    }

    // 오늘의 학습
    val learnApi: LearnApiService by lazy {
        RetrofitInstance.retrofit.create(LearnApiService::class.java)
    }

    // 10분 후 복습 + 오늘의 복습
    val reviewApi: ReviewApiService by lazy {
        RetrofitInstance.retrofit.create(ReviewApiService::class.java)
    }

    // 스터디방
    val studyRoomApi: StudyRoomApiService by lazy {
        RetrofitInstance.retrofit.create(StudyRoomApiService::class.java)
    }

    // AI 독해
    val aiReadingApi: AiReadingApiService by lazy {
        RetrofitInstance.retrofit.create(AiReadingApiService::class.java)
    }
}
