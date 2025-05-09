package com.example.englishapp.manager

import com.example.englishapp.model.Word

class StudyManager(private val words: List<Word>) {
    private val studyList = words.shuffled().take(10) // 랜덤 10개 추출
    private var currentIndex = 0

    fun getStudyList() = studyList // 외부에서 리스트 접근 가능
    fun getCurrentWord() = studyList.getOrNull(currentIndex)
    fun moveToNext() = ++currentIndex < studyList.size
    fun hasNext() = currentIndex < studyList.size
    fun reset() { currentIndex = 0 }
}

