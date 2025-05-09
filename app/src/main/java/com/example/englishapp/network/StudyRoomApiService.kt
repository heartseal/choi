package com.example.englishapp.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Header

// 방 생성 요청 (password는 서버가 자동 생성, 클라이언트에서는 title만 전송)
data class CreateRoomRequest(
    val title: String,
    val password : String? // 사용자가 password를 지정할 수도 있음
)

// 방 생성 응답
data class CreateRoomResponse(
    val roomId: Int,
    val title: String,
    val roomCode: String,
    val ownerNickname: String
)

// 방 참여 요청
data class JoinRoomRequest(
    val roomCode: String
)

// 방 참여 응답
data class JoinRoomResponse(
    val success: Boolean?,
    val message: String?,
    val room: RoomBasicInfo?
)

data class RoomBasicInfo(
    val roomId: Int,
    val title: String
)

// 멤버 진도율 정보
data class Progress(
    val totalWordCount: Int,
    val stageCounts: StageCounts
)

// 멤버 오늘 현황
data class DailyStatus(
    val isStudiedToday: Boolean
)

// 멤버 정보
data class Member(
    val userId: Int,
    val nickname: String,
    val role: String, // "admin" or "member"
    val progress: Progress,
    val dailyStatus: DailyStatus? = null // 관리자에게만 포함
)

// 방 상세 정보 응답
data class RoomDetailsResponse(
    val roomId: Int,
    val title: String,
    val roomCode: String,
    val isAdmin: Boolean,
    val members: List<Member>
)


interface StudyRoomApiService {
    // 방 생성
    @POST("/api/rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body request: CreateRoomRequest
    ): CreateRoomResponse

    // 방 참여
    @POST("/api/rooms/join")
    suspend fun joinRoom(
        @Header("Authorization") token: String,
        @Body request: JoinRoomRequest
    ): JoinRoomResponse

    // 방 상세 정보 조회
    @GET("/api/rooms/{roomId}")
    suspend fun getRoomDetails(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: Int
    ): RoomDetailsResponse

    // 방 멤버 삭제
    @DELETE("/api/rooms/{roomId}/members/{memberUserId}")
    suspend fun deleteRoomMember(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: Int,
        @Path("memberUserId") memberUserId: Int
    ): BaseSuccessResponse
}