package com.example.english_app_studypart.network

import com.example.english_app_studypart.datas.Word
import retrofit2.Call
import retrofit2.http.GET

interface WholeWordApiServiece {
    @GET("/api/words")
    fun getAllWords(): Call<List<Word>>
}