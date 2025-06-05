package com.example.Rainbow_Voca.data

import android.content.Context
import com.example.Rainbow_Voca.datastore.TokenDataStore
import com.example.Rainbow_Voca.model.StudyMemberProfile
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.network.ApiStudyRoomSearchResultItem
import com.example.Rainbow_Voca.network.CreateStudyRoomRequest
import com.example.Rainbow_Voca.network.JoinStudyRoomRequest
import com.example.Rainbow_Voca.network.StudyRoomApiService
import com.example.Rainbow_Voca.network.StudyRoomDetailsResponse
import com.example.Rainbow_Voca.network.MyStudyRoomBasicInfo
import com.example.Rainbow_Voca.util.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudyRoomRepository(
    private val remoteDataSource: StudyRoomApiService,
    private val localDummyProvider: DummyStudyRoomProvider,
    private val context: Context
) {
    private val USE_SERVER_DATA = true

    private suspend fun getAuthToken(): String? {
        return TokenDataStore.getToken(context)
    }

    private var cachedRooms: MutableMap<String, StudyRoom>? = null

    private suspend fun initLocalCache(currentUserNickname: String, currentUserId: Int) {
        if (cachedRooms == null && !USE_SERVER_DATA) {
            cachedRooms = localDummyProvider.getPredefinedRooms(currentUserNickname, currentUserId)
        }
    }

    suspend fun createStudyRoom(title: String, password: String): ApiResult<StudyRoom> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            withContext(Dispatchers.IO) {
                try {
                    val request = CreateStudyRoomRequest(title, password)
                    val response = remoteDataSource.createStudyRoom("Bearer $token", request)
                    if (response.success == true && response.ownerId != null && response.ownerNickname != null) {
                        val newRoomOwnerProfile = StudyMemberProfile(
                            userId = response.ownerId,
                            nickname = response.ownerNickname,
                            profileImage = null
                        )
                        val newRoom = StudyRoom(
                            title = response.title,
                            password = password,
                            ownerNickname = response.ownerNickname,
                            ownerId = response.ownerId,
                            members = mutableListOf(newRoomOwnerProfile),
                            isAdminForCurrentUser = true,
                            memberCount = 1
                        )
                        cachedRooms?.put(newRoom.title, newRoom)
                        ApiResult.Success(newRoom)
                    } else {
                        ApiResult.Error(response.message ?: "서버 방 생성 실패 또는 방장 정보 누락")
                    }
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "방 생성 중 예외 발생")
                }
            }
        } else {
            val localOwnerId = -100
            val localOwnerNickname = "LocalOwner"

            initLocalCache(localOwnerNickname, localOwnerId)
            val currentLocalRooms = cachedRooms ?: mutableMapOf<String, StudyRoom>().also { cachedRooms = it }

            if (currentLocalRooms.containsKey(title)) {
                ApiResult.Error("오류: 이미 존재하는 방 제목입니다. (로컬)")
            } else {
                val ownerProfile = StudyMemberProfile(
                    userId = localOwnerId,
                    nickname = localOwnerNickname,
                    profileImage = "logo"
                )
                val newRoom = StudyRoom(
                    title = title,
                    password = password,
                    ownerNickname = localOwnerNickname,
                    ownerId = localOwnerId,
                    members = mutableListOf(ownerProfile),
                    isAdminForCurrentUser = true,
                    memberCount = 1
                )
                currentLocalRooms[title] = newRoom
                ApiResult.Success(newRoom)
            }
        }
    }

    suspend fun joinStudyRoom(title: String, password: String, currentUserId: Int): ApiResult<StudyRoom> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            withContext(Dispatchers.IO) {
                try {
                    val request = JoinStudyRoomRequest(title, password)
                    val response = remoteDataSource.joinStudyRoom("Bearer $token", request)
                    if (response.success == true && response.roomDetails != null) {
                        val joinedRoom = mapStudyRoomDetailsResponseToStudyRoom(response.roomDetails, password, currentUserId)
                        cachedRooms?.put(joinedRoom.title, joinedRoom)
                        ApiResult.Success(joinedRoom)
                    } else {
                        ApiResult.Error(response.message ?: "서버 방 참여 실패")
                    }
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "방 참여 중 예외 발생")
                }
            }
        } else {
            val localUserNickname = "User$currentUserId"
            initLocalCache(localUserNickname, currentUserId)
            val roomToJoin = cachedRooms?.get(title)
            when {
                roomToJoin == null -> ApiResult.Error("오류: '$title' 방을 찾을 수 없습니다.")
                roomToJoin.password != password -> ApiResult.Error("오류: 비밀번호가 일치하지 않습니다.")
                roomToJoin.members.any { it.userId == currentUserId } -> ApiResult.Error("알림: 이미 '${title}' 방에 참여하고 있습니다.")
                else -> {
                    val joiningUser = StudyMemberProfile(userId = currentUserId, nickname = localUserNickname, profileImage = "logo", isAttendedToday = false)
                    roomToJoin.members.add(joiningUser)
                    roomToJoin.memberCount = roomToJoin.members.size
                    ApiResult.Success(roomToJoin)
                }
            }
        }
    }

    suspend fun getMyJoinedStudyRooms(currentUserId: Int, currentUserNicknameForLocal: String): ApiResult<List<StudyRoom>> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            withContext(Dispatchers.IO) {
                try {
                    val apiResponseItems: List<MyStudyRoomBasicInfo> = remoteDataSource.getMyStudyRooms("Bearer $token")
                    val myRooms = apiResponseItems.map { basicInfo ->
                        StudyRoom(
                            title = basicInfo.title,
                            password = "",
                            ownerNickname = basicInfo.ownerNickname,
                            ownerId = basicInfo.ownerId,
                            members = mutableListOf(),
                            isAdminForCurrentUser = (basicInfo.ownerId == currentUserId),
                            memberCount = basicInfo.memberCount
                        ).also { room -> cachedRooms?.put(room.title, room) }
                    }
                    ApiResult.Success(myRooms)
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "내 방 목록 조회 중 예외 발생")
                }
            }
        } else {
            initLocalCache(currentUserNicknameForLocal, currentUserId)
            val joined = cachedRooms?.values?.filter { room ->
                room.members.any { it.userId == currentUserId }
            }?.map { room -> room.copy(isAdminForCurrentUser = (room.ownerId == currentUserId)) } ?: emptyList()
            ApiResult.Success(joined)
        }
    }

    suspend fun getStudyRoomDetails(title: String, passwordForLocalModel: String = "", currentUserId: Int): ApiResult<StudyRoom> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            withContext(Dispatchers.IO) {
                try {
                    val response = remoteDataSource.getStudyRoomDetails("Bearer $token", title)
                    val roomDetails = mapStudyRoomDetailsResponseToStudyRoom(response, passwordForLocalModel, currentUserId)
                    cachedRooms?.put(roomDetails.title, roomDetails)
                    ApiResult.Success(roomDetails)
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "방 상세 정보 조회 중 예외 발생")
                }
            }
        } else {
            val localUserNickname = "User$currentUserId"
            initLocalCache(localUserNickname, currentUserId)
            val room = cachedRooms?.get(title)
            if (room != null) {
                ApiResult.Success(room.copy(isAdminForCurrentUser = (room.ownerId == currentUserId)))
            } else {
                ApiResult.Error("로컬에서 '$title' 방을 찾을 수 없습니다.")
            }
        }
    }

    suspend fun kickStudyRoomMemberById(roomTitle: String, memberUserIdToKick: Int, currentOwnerUserId: Int): ApiResult<Unit> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            if (memberUserIdToKick == currentOwnerUserId) return ApiResult.Error("자기 자신을 강퇴할 수 없습니다.")

            withContext(Dispatchers.IO) {
                try {
                    val response = remoteDataSource.deleteStudyRoomMember("Bearer $token", roomTitle, memberUserIdToKick)
                    if (response.success == true) {
                        cachedRooms?.get(roomTitle)?.members?.removeAll { it.userId == memberUserIdToKick }
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Error(response.message ?: "서버 멤버 삭제 실패")
                    }
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "멤버 강퇴 중 예외 발생")
                }
            }
        } else {
            val localOwnerNickname = "User$currentOwnerUserId"
            initLocalCache(localOwnerNickname, currentOwnerUserId)
            val room = cachedRooms?.get(roomTitle)
            when {
                room == null -> ApiResult.Error("오류: 방 정보를 찾을 수 없습니다.")
                room.ownerId != currentOwnerUserId -> ApiResult.Error("오류: 방장만 멤버를 내보낼 수 있습니다.")
                memberUserIdToKick == currentOwnerUserId -> ApiResult.Error("오류: 방장은 자신을 내보낼 수 없습니다.")
                else -> {
                    val memberRemoved = room.members.removeAll { it.userId == memberUserIdToKick }
                    if (memberRemoved) {
                        room.memberCount = room.members.size
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Error("오류: ID '${memberUserIdToKick}' 멤버를 방에서 찾을 수 없습니다.")
                    }
                }
            }
        }
    }

    suspend fun leaveStudyRoom(title: String, userId: Int, userNicknameForLocal: String): ApiResult<Unit> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            withContext(Dispatchers.IO) {
                try {
                    val response = remoteDataSource.leaveStudyRoom("Bearer $token", mapOf("title" to title))
                    if (response.success == true) {
                        cachedRooms?.remove(title)
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Error(response.message ?: "서버 방 나가기 실패")
                    }
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "방 나가기 중 예외 발생")
                }
            }
        } else {
            initLocalCache(userNicknameForLocal, userId)
            val room = cachedRooms?.get(title)
            if (room == null || !room.members.any { it.userId == userId }) {
                return ApiResult.Error("방을 찾을 수 없거나 참여 중이 아닙니다.")
            }
            room.members.removeAll { it.userId == userId }
            room.memberCount = room.members.size
            if (room.ownerId == userId && room.members.isEmpty()) {
                cachedRooms?.remove(title)
            }
            ApiResult.Success(Unit)
        }
    }

    suspend fun searchRooms(query: String, currentUserId: Int): ApiResult<List<ApiStudyRoomSearchResultItem>> {
        return if (USE_SERVER_DATA) {
            val token = getAuthToken()
            if (token == null) return ApiResult.Error("인증 토큰이 없습니다. 로그인이 필요합니다.")
            if (query.isBlank()) return ApiResult.Success(emptyList())

            return withContext(Dispatchers.IO) {
                try {
                    val results = remoteDataSource.searchStudyRooms("Bearer $token", query)
                    ApiResult.Success(results)
                } catch (e: Exception) {
                    ApiResult.Error(e.message ?: "방 검색 중 예외 발생")
                }
            }
        } else {
            val localUserNickname = "User$currentUserId"
            initLocalCache(localUserNickname, currentUserId)
            val allLocalRooms = cachedRooms?.values ?: return ApiResult.Success(emptyList())
            if (query.isBlank()) return ApiResult.Success(emptyList())

            val localResults = allLocalRooms.filter {
                it.title.contains(query, ignoreCase = true) && !it.members.any { member -> member.userId == currentUserId }
            }.map { room ->
                ApiStudyRoomSearchResultItem(
                    title = room.title,
                    ownerNickname = room.ownerNickname,
                    memberCount = room.members.size,
                    isLocked = room.password.isNotEmpty()
                )
            }
            return ApiResult.Success(localResults)
        }
    }


    private fun mapStudyRoomDetailsResponseToStudyRoom(dto: StudyRoomDetailsResponse, passwordForModel: String, currentUserId: Int): StudyRoom {
        val members = dto.members.map { apiMember ->
            StudyMemberProfile(
                userId = apiMember.userId,
                nickname = apiMember.nickname,
                profileImage = apiMember.profileImage,
                isAttendedToday = apiMember.dailyStatus?.isStudiedToday ?: false,
                totalWordCount = apiMember.progress.totalWordCount,
                studiedWordCount = apiMember.progress.totalWordCount - apiMember.progress.redStageWordCount,
                wrongAnswerCount = apiMember.progress.redStageWordCount
            )
        }
        return StudyRoom(
            title = dto.title,
            password = passwordForModel,
            ownerNickname = dto.ownerNickname,
            ownerId = dto.ownerId,
            members = members.toMutableList(),
            isAdminForCurrentUser = (dto.ownerId == currentUserId),
            memberCount = members.size
        )
    }
}