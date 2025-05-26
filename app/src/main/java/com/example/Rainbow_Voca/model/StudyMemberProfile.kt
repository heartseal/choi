package com.example.Rainbow_Voca.model

// 스터디룸 멤버의 프로필 및 학습 진행도 정보
data class StudyMemberProfile(
    val userId: Int,
    val nickname: String,
    val profileImage: String? = null,
    val totalWordCount: Int = 0,
    val studiedWordCount: Int = 0,
    val isAttendedToday: Boolean = false,
    val wrongAnswerCount: Int = 0
)