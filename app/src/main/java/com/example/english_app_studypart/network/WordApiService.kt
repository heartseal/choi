package com.example.english_app_studypart.network


import retrofit2.http.GET
import retrofit2.Call
import com.example.english_app_studypart.datas.Word
import com.example.english_app_studypart.model.TodayCompleteResponse
import com.example.english_app_studypart.model.WordTest
import retrofit2.http.Body
import retrofit2.http.POST

/*
interface WordApiService {
    @GET("words") // 백엔드에서 제공하는 엔드포인트
    fun getAllWords(): Call<List<Word>> // 전체 영단어 리스트를 반환
    @POST("words/test-results") // 테스트 결과를 전송하는 API
    fun submitTestResults(@Body wordTests: List<Word>): Call<Void>  // 응답이 필요 없다면 Void 사용
}
*/

//학습 단어 목록 조회
interface WordApiService {
    @GET("/api/learn/today-words")
    fun getAllWords(): Call<List<Word>>
}