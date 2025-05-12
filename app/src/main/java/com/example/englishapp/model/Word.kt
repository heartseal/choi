package com.example.englishapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Word(
    val id: Int,
    val word: String,
    val meaning: String,
    val priority: Int = 0 // 숫자가 클수록 높은 우선순위
) : Parcelable