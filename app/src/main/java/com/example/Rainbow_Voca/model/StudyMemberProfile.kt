package com.example.Rainbow_Voca.model

// 스터디룸 멤버의 프로필 및 학습 진행도 정보
data class StudyMemberProfile(
    val userId: Int,
    val nickname: String,
    val profileImage: String?,
    val isAttendedToday: Boolean = false,
    val totalWordCount: Int = 100,
    val studiedWordCount: Int = 0,
    val wrongAnswerCount: Int = 0
)