package com.example.Rainbow_Voca.network

import com.example.Rainbow_Voca.network.common.BaseSuccessResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Header

// --- 요청(Request) 데이터 클래스 ---

// 스터디룸 생성을 위한 요청 데이터 모델
data class CreateStudyRoomRequest(
    val title: String, // 생성할 스터디룸의 제목 (PK)
    val password: String // 생성할 스터디룸의 비밀번호
)

// 스터디룸 참여를 위한 요청 데이터 모델
data class JoinStudyRoomRequest(
    val title: String, // 참여할 스터디룸의 제목 (PK)
    val password: String // 참여할 스터디룸의 비밀번호
)

// --- 응답(Response) 데이터 클래스 ---
// (서버 API 명세에서 사용하는 DTO 이름과 구조를 정확히 따라야 함)

// 스터디룸 생성 API의 응답 데이터 모델
data class CreateStudyRoomResponse(
    val title: String, // 생성된 스터디룸의 제목 (PK)
    val ownerUserId: Int, // 생성된 스터디룸 방장의 사용자 ID
    val ownerNickname: String, // 생성된 스터디룸 방장의 닉네임
    val message: String? = null, // API 응답 메시지
    val success: Boolean? = null // API 호출 성공 여부
)

// 스터디룸 멤버 정보
data class StudyRoomMember(
    val userId: Int, // 멤버의 고유 사용자 ID
    val nickname: String, // 멤버의 닉네임
    val profileImageUrl: String? = null, // 멤버의 프로필 이미지 URL
    val isAttendedToday: Boolean = false, // 멤버의 오늘 학습(출석) 완료 여부
    val totalWordCount: Int = 0, // 멤버의 전체 학습 대상 단어 수 * studiedWordCount와 함께 진도율 표기 담당
    val studiedWordCount: Int = 0, // 멤버가 학습 완료한 단어 수
    val wrongAnswerCount: Int = 0 // 멤버의 오늘 학습 중 틀린 단어 수
)

// 스터디룸 상세 정보 및 '내 스터디룸 목록'의 각 아이템
data class StudyRoomDetailsResponse(
    val title: String, // 스터디룸 제목 (PK)
    val ownerUserId: Int, // 방장의 사용자 ID
    val ownerNickname: String, // 방장의 닉네임
    val isAdmin: Boolean, // 현재 API를 요청한 사용자가 이 방의 관리자인지 여부 (그냥 admin이 누군지만 보내기로 결정하면 자체적으로 admin인지 아닌지 확인하는 로직 추가)
    val members: List<StudyRoomMember>, // 스터디룸에 속한 전체 멤버 목록
    val memberCount: Int, // 현재 멤버 수
)

// 스터디룸 참여 API의 응답
data class JoinStudyRoomResponse(
    val success: Boolean?, // API 호출 성공 여부
    val message: String?, // API 응답 메시지
    val roomDetails: StudyRoomDetailsResponse? // 참여 성공 시 반환될 방 상세 정보
)

// 스터디룸 검색 결과의 각 아이템을 나타내는
data class StudyRoomSearchResultItem(
    val title: String, // 검색된 스터디룸의 제목 (PK)
    val ownerNickname: String, // 검색된 스터디룸의 방장 닉네임
    val memberCount: Int, // 검색된 스터디룸의 현재 멤버 수
)

// 스터디룸 관련 기능을 제공하는 API 서비스 인터페이스
interface StudyRoomApiService {

    // 새 스터디룸을 서버에 생성
    @POST("api/studyrooms")
    suspend fun createStudyRoom(
        @Header("Authorization") token: String,
        @Body request: CreateStudyRoomRequest
    ): CreateStudyRoomResponse

    // 기존 스터디룸에 사용자가 참여
    @POST("api/studyrooms/join")
    suspend fun joinStudyRoom(
        @Header("Authorization") token: String,
        @Body request: JoinStudyRoomRequest
    ): JoinStudyRoomResponse

    // 현재 사용자가 참여하고 있는 모든 스터디룸의 상세 목록을 조회
    @GET("api/users/me/studyrooms")
    suspend fun getMyStudyRooms(
        @Header("Authorization") token: String
    ): List<StudyRoomDetailsResponse>

    // (선택적) 특정 스터디룸 하나의 상세 정보를 조회
    @GET("api/studyrooms/details")
    suspend fun getStudyRoomDetails(
        @Header("Authorization") token: String,
        @Query("title") roomTitle: String // PK
    ): StudyRoomDetailsResponse

    // 스터디룸에서 특정 멤버를 삭제 (방장 권한)
    @DELETE("api/studyrooms/members")
    suspend fun deleteStudyRoomMember(
        @Header("Authorization") token: String,
        @Query("roomTitle") roomTitle: String, // PK
        @Query("memberUserId") memberUserId: Int
    ): BaseSuccessResponse

    // 사용자가 특정 스터디룸에서 나감
    @POST("api/studyrooms/leave")
    suspend fun leaveStudyRoom(
        @Header("Authorization") token: String,
        @Query("roomTitle") roomTitle: String // PK
    ): BaseSuccessResponse

    // 제목으로 스터디룸을 검색
    @GET("api/studyrooms/search")
    suspend fun searchStudyRooms(
        @Header("Authorization") token: String,
        @Query("query") searchQuery: String
    ): List<StudyRoomSearchResultItem>
}