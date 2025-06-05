package com.example.Rainbow_Voca.network

import com.example.Rainbow_Voca.network.common.BaseSuccessResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Query
import retrofit2.http.Header

// --- 요청(Request) 데이터 클래스 ---

/** 스터디룸 생성 요청 (사용자 입력: 방 제목, 비밀번호) */
data class CreateStudyRoomRequest(
    val title: String,
    val password: String
) //

/** 스터디룸 참여 요청 (사용자 입력: 방 제목, 비밀번호) */
data class JoinStudyRoomRequest(
    val title: String,
    val password: String
) //

// --- 응답(Response) 데이터 클래스 ---

/** 스터디룸 생성 응답 (생성된 방 정보 포함) */
data class CreateStudyRoomResponse(
    val title: String,
    val ownerNickname: String,
    val ownerId: Int, // 기능: 방 생성 시 방장 ID 반환 (추가됨)
    val message: String? = null,
    val success: Boolean? = null
) // // ownerId 추가 제안

/** 스터디룸 참여 응답 (참여 성공 시 방 상세 정보 포함) */
data class JoinStudyRoomResponse(
    val success: Boolean?,
    val message: String?,
    val roomDetails: StudyRoomDetailsResponse?
) //

/** 내가 참여한 스터디룸 목록의 각 아이템 정보 */
data class MyStudyRoomBasicInfo(
    val title: String,
    val ownerNickname: String,
    val ownerId: Int, // 기능: 내 스터디룸 목록 조회 시 방장 ID 반환 (추가됨)
    val memberCount: Int
) // // ownerId 추가 제안

/** 스터디룸 멤버의 학습 진행도 정보 */
data class StudyRoomProgress(
    val totalWordCount: Int,
    val redStageWordCount: Int
) //

/** 스터디룸 멤버의 일일 학습 상태 */
data class StudyRoomDailyStatus(
    val isStudiedToday: Boolean
) //

/** 스터디룸 멤버 정보 (API DTO) */
data class StudyRoomMember(
    val userId: Int,
    val nickname: String,
    val role: String, // "OWNER" 또는 "MEMBER"
    val profileImage: String?, // 기능: 멤버 프로필 이미지 URL (추가/수정)
    val progress: StudyRoomProgress,
    val dailyStatus: StudyRoomDailyStatus? = null
) // // profileImage, role 추가/수정 제안

/** 스터디룸 상세 정보 (API DTO) */
data class StudyRoomDetailsResponse(
    val title: String,
    val ownerNickname: String,
    val ownerId: Int, // 기능: 상세 정보 조회 시 방장 ID 반환 (추가됨)
    val isAdmin: Boolean, // 현재 요청 사용자가 이 방의 관리자인지 여부
    val members: List<StudyRoomMember>
) // // ownerId 추가 제안

// --- 알림 발송 관련 요청(Request) 데이터 클래스 (신규 추가) ---

/** 개별 학생 독촉 알림 요청 */
data class IndividualNotificationRequest(
    val roomTitle: String,
    val targetUserId: Int
) //

/** 다수 학생 독촉 알림 일괄 요청 */
data class BatchNotificationRequest(
    val roomTitle: String,
    val targetUserIds: List<Int>
) //

// --- 스터디룸 검색 기능 추가 ---
/** 스터디룸 검색 결과 아이템 DTO (서버 응답용) */
data class ApiStudyRoomSearchResultItem(
    val title: String,
    val ownerNickname: String,
    val memberCount: Int,
    val isLocked: Boolean // 기능: 방 잠금 상태 (비밀번호 유무)
)

/** 스터디룸 관련 API 서비스 인터페이스 */
interface StudyRoomApiService {

    /** 스터디룸 생성 API */
    @POST("api/studyrooms") // 기능: 스터디룸 생성
    suspend fun createStudyRoom(
        @Header("Authorization") token: String,
        @Body request: CreateStudyRoomRequest
    ): CreateStudyRoomResponse //

    /** 스터디룸 참여 API */
    @POST("api/studyrooms/join") // 기능: 스터디룸 참여
    suspend fun joinStudyRoom(
        @Header("Authorization") token: String,
        @Body request: JoinStudyRoomRequest
    ): JoinStudyRoomResponse //

    /** 현재 사용자가 참여하고 있는 스터디룸 목록 조회 API */
    @GET("api/users/me/studyrooms") // 기능: 내 스터디룸 목록 조회
    suspend fun getMyStudyRooms(
        @Header("Authorization") token: String
    ): List<MyStudyRoomBasicInfo> //

    /** 특정 스터디룸의 상세 정보 조회 API */
    @GET("api/studyrooms/details") // 기능: 스터디룸 상세 정보 조회
    suspend fun getStudyRoomDetails(
        @Header("Authorization") token: String,
        @Query("title") roomTitle: String
    ): StudyRoomDetailsResponse //

    /** 스터디룸에서 특정 멤버 삭제 API (방장 권한) */
    @DELETE("api/studyrooms/members") // 기능: 스터디룸 멤버 강퇴
    suspend fun deleteStudyRoomMember(
        @Header("Authorization") token: String,
        @Query("roomTitle") roomTitle: String,
        @Query("memberUserId") memberUserId: Int
    ): BaseSuccessResponse //

    /** 스터디룸 나가기 API (신규 추가) */
    @POST("api/studyrooms/leave") // 기능: 스터디룸 나가기 (예시 경로)
    suspend fun leaveStudyRoom(
        @Header("Authorization") token: String,
        @Body roomTitleRequest: Map<String, String> // 예: {"title": "roomName"}
    ): BaseSuccessResponse
    /* 알림 발송 임시
    /** 개별 학생에게 학습 독촉 알림 발송 API */
    @POST("") // 기능: 개별 학습 독촉 알림 (경로 수정), 알림 기능 추가 안 할 시 주석 처리하기
    suspend fun sendIndividualNotification(
        @Header("Authorization") token: String,
        @Body request: IndividualNotificationRequest
    ): BaseSuccessResponse //

    /** 선택된 다수 학생에게 학습 독촉 알림 일괄 발송 API */
    @POST("") // 기능: 일괄 학습 독촉 알림 (경로 수정)
    suspend fun sendBatchNotification(
        @Header("Authorization") token: String,
        @Body request: BatchNotificationRequest
    ): BaseSuccessResponse //
    */
    /** 스터디룸 검색 API */
    @GET("") // 기능: 스터디룸 검색 ((경로 수정필요)
    suspend fun searchStudyRooms(
        @Header("Authorization") token: String,
        @Query("query") searchQuery: String
    ): List<ApiStudyRoomSearchResultItem>
}