package com.example.englishapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Word(
    val id: Int,
    val word: String,
    val meaning: String,
    // val priority: Boolean? = null // 필요하다면 추가
) : Parcelable