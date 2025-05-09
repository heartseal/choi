package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.network.ApiServicePool
import com.example.englishapp.network.CreateRoomRequest
import com.example.englishapp.network.JoinRoomRequest
import kotlinx.coroutines.launch


// 임시로 만든 스터디룸 뷰모델. 후에 수정
class StudyRoomViewModel : ViewModel() {

    private val _createRoomResult = MutableLiveData<com.example.englishapp.network.CreateRoomResponse?>()
    val createRoomResult: LiveData<com.example.englishapp.network.CreateRoomResponse?> = _createRoomResult

    private val _joinRoomResult = MutableLiveData<com.example.englishapp.network.JoinRoomResponse?>()
    val joinRoomResult: LiveData<com.example.englishapp.network.JoinRoomResponse?> = _joinRoomResult

    private val _roomDetails = MutableLiveData<com.example.englishapp.network.RoomDetailsResponse?>()
    val roomDetails: LiveData<com.example.englishapp.network.RoomDetailsResponse?> = _roomDetails

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 스터디 방 생성
    fun createRoom(token: String, title: String) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.createRoom(
                    "Bearer $token",
                    CreateRoomRequest(title = title, password = null) // password는 서버에서 자동 생성
                )
                _createRoomResult.value = response
            } catch (e: Exception) {
                _error.value = "방 생성 실패: ${e.localizedMessage}"
            }
        }
    }

    // 스터디 방 참여
    fun joinRoom(token: String, roomCode: String) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.joinRoom(
                    "Bearer $token",
                    JoinRoomRequest(roomCode = roomCode)
                )
                _joinRoomResult.value = response
            } catch (e: Exception) {
                _error.value = "방 참여 실패: ${e.localizedMessage}"
            }
        }
    }

    // 스터디 방 상세 정보 조회
    fun getRoomDetails(token: String, roomId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.getRoomDetails("Bearer $token", roomId)
                _roomDetails.value = response
            } catch (e: Exception) {
                _error.value = "방 정보 조회 실패: ${e.localizedMessage}"
            }
        }
    }

    // 스터디 방 멤버 삭제 (관리자)
    fun deleteRoomMember(token: String, roomId: Int, memberUserId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.deleteRoomMember("Bearer $token", roomId, memberUserId)
                // 성공 여부는 LiveData로 전달하지 않고, 필요한 경우 콜백 등으로 처리
                if (response.success != true) {
                    _error.value = response.message ?: "멤버 삭제 실패"
                }
            } catch (e: Exception) {
                _error.value = "멤버 삭제 실패: ${e.localizedMessage}"
            }
        }
    }

    // 데이터 초기화 (Activity 종료 시 호출 권장)
    fun clearData() {
        _createRoomResult.value = null
        _joinRoomResult.value = null
        _roomDetails.value = null
        _error.value = null
    }
}