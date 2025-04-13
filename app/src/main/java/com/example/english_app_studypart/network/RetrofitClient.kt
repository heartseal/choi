package com.example.english_app_studypart.network


import com.example.english_app_studypart.viewmodel.MainPageViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "--" //실주소로 수정

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // JSON 데이터 변환
            .build()
    }

    // 기존 단어 관련 API
    val wordApiService: WordApiService by lazy {
        retrofit.create(WordApiService::class.java)
    }

    // ✅ 추가: 학습 완료 관련 API
    val WordStudiedApiService: WordStudiedApiService by lazy {
        retrofit.create(WordStudiedApiService::class.java)
    }

    // ✅ 메인 페이지 관련 API 추가
    val mainPageGraphApiService: MainPageApiService by lazy {
        retrofit.create(MainPageApiService::class.java)
    }

    // ✅ 새로운 복습 단어 조회 API 추가
    val wordReviewApiService: WordReviewApiService by lazy {
        retrofit.create(WordReviewApiService::class.java)
    }

    val WordReviewApiService: WordReviewApiService by lazy {
        retrofit.create(WordReviewApiService::class.java)
    }
}