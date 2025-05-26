package com.example.Rainbow_Voca.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.data.StudyRoomRepository
import com.example.Rainbow_Voca.datastore.TokenDataStore // TokenDataStore 임포트
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.network.RetrofitInstance
import com.example.Rainbow_Voca.network.StudyRoomApiService
import com.example.Rainbow_Voca.network.StudyRoomSearchResultItem
import com.example.Rainbow_Voca.util.ApiResult
import kotlinx.coroutines.launch

/**
 * 스터디룸 관련 UI(StudyRoomActivity, CreateRoomActivity)를 위한 ViewModel.
 * 스터디룸 목록 로드, 생성, 참여, 나가기, 검색 등의 로직을 처리하기.
 */
class StudyRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val service: StudyRoomApiService = RetrofitInstance.retrofit.create(StudyRoomApiService::class.java)
    private val repository: StudyRoomRepository = StudyRoomRepository(service)

    private var localUserNickname: String = "DefaultUser"
    private var localUserId: Int = -1
    // private var localToken: String = "" // TokenDataStore에서 직접 가져오므로 제거 가능
    private var localUserProfileImage: String = "default_profile"

    val currentNickname: String get() = localUserNickname
    val currentUserId: Int get() = localUserId

    private val _joinedRooms = MutableLiveData<ApiResult<List<StudyRoom>>>()
    val joinedRooms: LiveData<ApiResult<List<StudyRoom>>> get() = _joinedRooms

    private val _createOp = MutableLiveData<ApiResult<StudyRoom>>()
    val createOp: LiveData<ApiResult<StudyRoom>> get() = _createOp

    private val _joinOp = MutableLiveData<ApiResult<StudyRoom>>()
    val joinOp: LiveData<ApiResult<StudyRoom>> get() = _joinOp

    private val _leaveOp = MutableLiveData<ApiResult<Unit>>()
    val leaveOp: LiveData<ApiResult<Unit>> get() = _leaveOp

    private val _foundRooms = MutableLiveData<ApiResult<List<StudyRoomSearchResultItem>>>()
    val foundRooms: LiveData<ApiResult<List<StudyRoomSearchResultItem>>> get() = _foundRooms

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> get() = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    /** 사용자 정보를 초기화하고, 초기화 시 참여 중인 스터디룸 목록을 로드하기. */
    fun initUser(nickname: String, id: Int, profileImage: String) { // authToken 파라미터 제거
        localUserNickname = nickname
        localUserId = id
        localUserProfileImage = profileImage
        loadJoinedRooms() // initUser 시 바로 목록 로드
    }

    /** 참여 중인 스터디룸 목록을 로드하기. */
    fun loadJoinedRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            _joinedRooms.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "User auth info missing. Login required."
                _joinedRooms.value = ApiResult.Error("Login or token required")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.getMyRooms(bearerToken, localUserId, localUserNickname)
            _joinedRooms.value = result
            if (result is ApiResult.Error) {
                _message.value = "Load joined rooms failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** 새 스터디룸을 생성하기. */
    fun createNewRoom(title: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _createOp.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "Login required to create room."
                _createOp.value = ApiResult.Error("Login or token required")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.createRoom(bearerToken, title, password, localUserNickname, localUserId, localUserProfileImage)
            _createOp.value = result
            if (result is ApiResult.Success) {
                _message.value = "'${result.data.title}' room created!"
                loadJoinedRooms()
            } else if (result is ApiResult.Error) {
                _message.value = "Create room failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** 기존 스터디룸에 참여하기. */
    fun joinExistingRoom(roomTitle: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _joinOp.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "Login required to join room."
                _joinOp.value = ApiResult.Error("Login or token required")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.joinRoom(bearerToken, roomTitle, password, localUserNickname, localUserId, localUserProfileImage)
            _joinOp.value = result
            if (result is ApiResult.Success) {
                _message.value = "Joined '${result.data.title}' room!"
                loadJoinedRooms()
            } else if (result is ApiResult.Error) {
                _message.value = "Join room failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** 현재 스터디룸에서 나가기. */
    fun leaveCurrentRoom(roomTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _leaveOp.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "Login required to leave room."
                _leaveOp.value = ApiResult.Error("Login or token required")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.leaveRoom(bearerToken, roomTitle, localUserId, localUserNickname)
            _leaveOp.value = result
            if (result is ApiResult.Success) {
                _message.value = "Left '$roomTitle' room."
                loadJoinedRooms()
            } else if (result is ApiResult.Error) {
                _message.value = "Leave room failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** 스터디룸을 검색. */
    fun findRooms(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _foundRooms.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "Login required to search rooms."
                _foundRooms.value = ApiResult.Error("Login or token required")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.searchRooms(bearerToken, query, localUserId)
            _foundRooms.value = result
            if (result is ApiResult.Error) {
                _message.value = "Search rooms failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** UI에 표시된 메시지 상태를 초기화. */
    fun clearMessage() {
        _message.value = null
    }

    /** 방 생성 작업 결과 상태를 초기화. */
    fun clearCreateOp() {
        _createOp.value = ApiResult.Idle
    }

    /** 방 참여 작업 결과 상태를 초기화. */
    fun clearJoinOp() {
        _joinOp.value = ApiResult.Idle
    }

    /** 방 나가기 작업 결과 상태를 초기화. */
    fun clearLeaveOp() {
        _leaveOp.value = ApiResult.Idle
    }

    /** 검색된 방 목록 상태를 초기화. */
    fun clearFoundRooms() {
        _foundRooms.value = ApiResult.Idle
    }
}