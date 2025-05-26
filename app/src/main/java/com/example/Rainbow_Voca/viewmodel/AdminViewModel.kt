package com.example.Rainbow_Voca.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.data.StudyRoomRepository
import com.example.Rainbow_Voca.datastore.TokenDataStore
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.network.RetrofitInstance
import com.example.Rainbow_Voca.network.StudyRoomApiService
import com.example.Rainbow_Voca.util.ApiResult
import kotlinx.coroutines.launch

/**
 * 관리자 페이지(AdminActivity)를 위한 ViewModel.
 * 스터디룸 상세 정보 로드, 멤버 강퇴 등의 로직을 처리하기.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val service: StudyRoomApiService = RetrofitInstance.retrofit.create(StudyRoomApiService::class.java)
    private val repository: StudyRoomRepository = StudyRoomRepository(service)

    private var localUserNickname: String = ""
    private var localUserId: Int = -1

    val currentNickname: String get() = localUserNickname
    val currentUserId: Int get() = localUserId

    private val _roomDetails = MutableLiveData<ApiResult<StudyRoom>>()
    val roomDetails: LiveData<ApiResult<StudyRoom>> get() = _roomDetails

    private val _kickResult = MutableLiveData<ApiResult<Unit>>(ApiResult.Idle)
    val kickResult: LiveData<ApiResult<Unit>> get() = _kickResult

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> get() = _message

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    /** ViewModel 초기화 시 현재 사용자 정보 설정하기. (토큰 제외) */
    fun initCurrentUser(nickname: String, userId: Int) {
        localUserNickname = nickname
        localUserId = userId
    }

    /** 특정 스터디룸의 상세 정보를 로드하기. */
    fun loadRoomDetails(roomTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _roomDetails.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "User auth info missing for admin actions."
                _roomDetails.value = ApiResult.Error("Admin Auth or token Error")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val result = repository.getRoomDetails(bearerToken, roomTitle, localUserId, "")
            _roomDetails.value = result
            if (result is ApiResult.Error) {
                _message.value = "Load '$roomTitle' details failed: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    /** 스터디룸에서 특정 멤버를 강퇴하기. */
    fun kickMemberFromRoom(roomTitle: String, memberKickId: Int) {
        viewModelScope.launch {
            if (_kickResult.value is ApiResult.Loading) {
                _message.value = "Kick operation already in progress."
                return@launch
            }

            _isLoading.value = true
            _kickResult.value = ApiResult.Loading
            val token = TokenDataStore.getToken(getApplication())

            if (token == null || localUserId == -1) {
                _message.value = "User auth info missing for admin actions."
                _kickResult.value = ApiResult.Error("Admin Auth or token Error")
                _isLoading.value = false
                return@launch
            }
            val bearerToken = "Bearer $token"
            val kickAttemptResult = repository.kickMember(bearerToken, roomTitle, memberKickId, localUserId, localUserNickname)

            if (kickAttemptResult is ApiResult.Success) {
                _message.value = "Member kicked successfully."
                val detailsResult = repository.getRoomDetails(bearerToken, roomTitle, localUserId, "")
                _roomDetails.value = detailsResult
                _kickResult.value = ApiResult.Success(Unit)
            } else if (kickAttemptResult is ApiResult.Error) {
                _message.value = "Kick member failed: ${kickAttemptResult.message}"
                _kickResult.value = kickAttemptResult
            }
            _isLoading.value = false
        }
    }

    /** UI에 표시된 메시지 상태를 초기화하기. */
    fun clearMessage() {
        _message.value = null
    }

    /** 멤버 강퇴 작업 결과 상태를 초기화 (Idle 상태로 변경)하기. */
    fun clearKickResult() {
        if (_kickResult.value !is ApiResult.Idle) {
            _kickResult.value = ApiResult.Idle
        }
    }
}