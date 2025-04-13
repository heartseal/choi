package com.example.english_app_studypart.model

// 메인 페이지 데이터 구조
data class MainPageStudyGraph(
    val totalWordCount: Int,
    val stageCounts: StageCounts,
    val todayReviewGoal: Int,
    val todayLearningGoal: Int,
    val estimatedCompletionDate: String,
    val monthlyAttendanceRate: Double
)

data class StageCounts(
    val waiting: Int,
    val seed: Int,
    val sprout: Int,
    val leaf: Int,
    val branch: Int,
    val fruit: Int,
    val tree: Int
)
