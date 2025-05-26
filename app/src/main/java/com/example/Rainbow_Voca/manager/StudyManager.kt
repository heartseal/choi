package com.example.Rainbow_Voca.manager

import com.example.Rainbow_Voca.model.Word

// 오늘의 학습 단어 목록 및 진행 관리
class StudyManager(
    allWords: List<Word>, // 전체 단어 후보군
    dailyGoal: Int        // 학습 목표 단어 수
) {
    private val studyList: List<Word> // 실제 학습할 단어 목록
    private var currentIndex = 0      // 현재 학습 중인 단어 인덱스

    init {
        // priority 내림차순, id 오름차순으로 정렬 후 목표량만큼 선택
        val sortedWords = allWords.sortedWith(
            compareByDescending<Word> { it.priority }
                .thenBy { it.id }
        )
        studyList = sortedWords.take(dailyGoal)
    }

    // 학습할 단어 목록 반환
    fun getStudyList(): List<Word> = studyList

    // 현재 학습 단어 반환
    fun getCurrentWord(): Word? = studyList.getOrNull(currentIndex)

    // 다음 단어로 이동 (성공 시 true, 더 없으면 false)
    fun moveToNext(): Boolean {
        return if (currentIndex < studyList.size - 1) {
            currentIndex++
            true
        } else {
            false
        }
    }

    // 다음 학습할 단어가 있는지 여부
    fun hasNext(): Boolean = currentIndex < studyList.size -1

    // 모든 단어 학습 완료 여부
    fun isStudyFinished(): Boolean = currentIndex >= studyList.size

    // 학습 진행 상태 초기화
    fun reset() {
        currentIndex = 0
    }
}