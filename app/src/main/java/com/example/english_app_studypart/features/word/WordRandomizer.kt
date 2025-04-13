package com.example.english_app_studypart.features.word

import kotlin.collections.filter
import kotlin.collections.shuffled
import kotlin.collections.sortedBy
import kotlin.collections.take
import kotlin.collections.toMutableList
import com.example.english_app_studypart.datas.Word

object WordRandomizer {
    // 파라미터로 전체 단어 리스트와 todayStudySize를 받음
    fun generateTodayStudyList(wholeList: List<Word>, todayStudySize: Int): List<Word> {
        // 1. id > 1000인 단어를 ID 오름차순으로 정렬 (선입선출을 위함)
        val highPriorityWords = wholeList.filter { it.id > 1000 }.sortedBy { it.id }

        // 2. highPriorityWords에서 최대 todayStudySize 개수를 선입선출로 선택
        val selectedWords = highPriorityWords.take(todayStudySize).toMutableList()

        // 3. 부족한 경우, id <= 1000인 단어들 중 랜덤으로 추가
        if (selectedWords.size < todayStudySize) {
            val remainingCount = todayStudySize - selectedWords.size
            val lowPriorityWords = wholeList.filter { it.id <= 1000 }
            selectedWords.addAll(generateRandomWords(lowPriorityWords, remainingCount))
        }

        return selectedWords
    }

    private fun generateRandomWords(wholeList: List<Word>, count: Int): List<Word> {
        return wholeList.shuffled().take(count)
    }
}