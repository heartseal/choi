package com.example.Rainbow_Voca.data

import com.example.Rainbow_Voca.model.StudyMemberProfile
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.util.ApiResult

/**
 * 로컬 메모리 캐시를 사용하여 스터디룸 데이터를 관리하는 데이터 소스.
 */
object LocalStudyRoomDataSource {

    private var roomCache: MutableMap<String, StudyRoom>? = null
    private val dummyProvider = DummyStudyRoomProvider

    /** 로컬 캐시를 초기화합니다. 캐시가 null일 경우 더미 데이터를 로드합니다. */
    private fun initCacheIfNeeded(nickname: String, userId: Int) {
        if (roomCache == null) {
            roomCache = dummyProvider.getPredefinedRooms(nickname, userId)
        }
    }

    /** 새로운 스터디룸을 로컬 캐시에 생성합니다. */
    fun createRoom(
        title: String,
        password: String,
        ownerName: String,
        ownerId: Int,
        ownerProfileImage: String,
        currentUserIdForInit: Int
    ): ApiResult<StudyRoom> {
        initCacheIfNeeded(ownerName, currentUserIdForInit)
        val currentCache = roomCache ?: return ApiResult.Error("Local cache not initialized")

        return if (currentCache.containsKey(title)) {
            ApiResult.Error("Room title exists")
        } else {
            val ownerProf = StudyMemberProfile(
                userId = ownerId,
                nickname = ownerName,
                profileImage = ownerProfileImage,
                isAttendedToday = false,
                totalWordCount = 0,
                studiedWordCount = 0,
            )
            val newMembers = mutableListOf(ownerProf)
            val newRoom = StudyRoom( // isLocked 필드 제거
                title = title,
                password = password,
                ownerNickname = ownerName,
                ownerId = ownerId,
                members = newMembers,
                isAdminForCurrentUser = true,
                memberCount = newMembers.size
            )
            currentCache[title] = newRoom
            ApiResult.Success(newRoom)
        }
    }

    /** 기존 스터디룸에 사용자를 참여시킵니다. (로컬 캐시) */
    fun joinRoom(
        title: String,
        password: String,
        nickname: String,
        userId: Int,
        userProfileImage: String // 이 파라미터는 현재 사용되지 않음, 필요시 StudyMemberProfile 생성에 사용
    ): ApiResult<StudyRoom> {
        initCacheIfNeeded(nickname, userId)
        val currentCache = roomCache ?: return ApiResult.Error("Local cache not initialized")

        val room = currentCache[title]
        return if (room == null) {
            ApiResult.Error("Room '$title' not found")
        } else if (room.password != password) {
            ApiResult.Error("Incorrect password")
        } else if (room.members.any { it.userId == userId }) {
            ApiResult.Error("Already in '$title'")
        } else {
            room.members.add(
                StudyMemberProfile(
                    userId = userId,
                    nickname = nickname,
                    profileImage = "logo", // 참여자 프로필은 일단 "logo"로 고정
                    isAttendedToday = false,
                    totalWordCount = 0,
                    studiedWordCount = 0,
                )
            )
            room.memberCount = room.members.size
            ApiResult.Success(room)
        }
    }

    /** 특정 사용자가 참여한 모든 스터디룸 목록을 로컬 캐시에서 가져옵니다. */
    fun getMyRooms(userId: Int, nicknameForInitIfNeeded: String): ApiResult<List<StudyRoom>> {
        initCacheIfNeeded(nicknameForInitIfNeeded, userId)
        val currentCache = roomCache ?: return ApiResult.Success(emptyList())

        val joinedRms = currentCache.values.filter { room ->
            room.members.any { it.userId == userId }
        }.map { room ->
            room.copy(isAdminForCurrentUser = (room.ownerId == userId))
        }
        return ApiResult.Success(joinedRms)
    }

    /** 특정 스터디룸의 상세 정보를 로컬 캐시에서 가져옵니다. */
    fun getRoomDetails(title: String, userIdForInitIfNeeded: Int): ApiResult<StudyRoom> {
        initCacheIfNeeded("", userIdForInitIfNeeded)
        val currentCache = roomCache ?: return ApiResult.Error("Local cache not initialized")

        val room = currentCache[title]
        return if (room != null) {
            ApiResult.Success(room.copy(isAdminForCurrentUser = (room.ownerId == userIdForInitIfNeeded)))
        } else {
            ApiResult.Error("Local room not found: $title")
        }
    }

    /** 스터디룸에서 특정 멤버를 강퇴합니다. (로컬 캐시) */
    fun kickMember(roomTitle: String, kickId: Int, currentUid: Int, currentNickname: String): ApiResult<Unit> {
        initCacheIfNeeded(currentNickname, currentUid)
        val currentCache = roomCache ?: return ApiResult.Error("Local cache not initialized")

        val room = currentCache[roomTitle]
        if (room == null) {
            return ApiResult.Error("Room not found")
        }
        val toKick = room.members.find { it.userId == kickId }
        if (toKick == null) {
            return ApiResult.Error("Member ID '$kickId' not found")
        }
        if (room.ownerId != currentUid) {
            return ApiResult.Error("Only owner can kick")
        }
        if (kickId == currentUid || kickId == room.ownerId) {
            return ApiResult.Error("Cannot kick self or owner")
        }
        val removed = room.members.removeAll { it.userId == kickId }
        if (removed) {
            room.memberCount = room.members.size
            return ApiResult.Success(Unit)
        } else {
            return ApiResult.Error("Local kick '${toKick.nickname}' failed")
        }
    }

    /** 제목으로 스터디룸을 검색합니다. (로컬) */
    fun searchRooms(query: String, userId: Int): ApiResult<List<StudyRoomSearchResultItem>> {
        initCacheIfNeeded("", userId)
        val currentCache = roomCache ?: return ApiResult.Success(emptyList())

        if (query.isBlank()) return ApiResult.Success(emptyList())

        val localResults = currentCache.values.filter {
            it.title.contains(query, ignoreCase = true) && !it.members.any { member -> member.userId == userId }
        }.map {
            StudyRoomSearchResultItem(
                title = it.title,
                ownerNickname = it.ownerNickname,
                memberCount = it.members.size
            )
        }
        return ApiResult.Success(localResults)
    }

    /** 사용자가 스터디룸에서 나갑니다. (로컬 캐시) */
    fun leaveRoom(roomTitle: String, userId: Int, nickname: String): ApiResult<Unit> {
        initCacheIfNeeded(nickname, userId)
        val currentCache = roomCache ?: return ApiResult.Error("Local cache not initialized")

        val room = currentCache[roomTitle]
        if (room == null) {
            return ApiResult.Error("Local room not found (leave)")
        }
        val removed = room.members.removeAll { it.userId == userId }
        if (removed) {
            room.memberCount = room.members.size
            if (room.ownerId == userId && room.members.isEmpty()) {
                currentCache.remove(roomTitle)
            }
            return ApiResult.Success(Unit)
        } else {
            return ApiResult.Error("Not a member (local leave)")
        }
    }
}

data class StudyRoomSearchResultItem(
    val title: String,
    val ownerNickname: String,
    val memberCount: Int
)