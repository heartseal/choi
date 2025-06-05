package com.example.Rainbow_Voca.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.data.DummyStudyRoomProvider // 추가
import com.example.Rainbow_Voca.data.StudyRoomRepository
// import com.example.Rainbow_Voca.datastore.TokenDataStore // Repository에서 토큰을 관리하므로 ViewModel에서 직접 호출 불필요
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.network.RetrofitInstance
import com.example.Rainbow_Voca.network.StudyRoomApiService
import com.example.Rainbow_Voca.util.ApiResult
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val service: StudyRoomApiService = RetrofitInstance.retrofit.create(StudyRoomApiService::class.java) //
    // 기능: Repository 초기화 수정
    private val repository: StudyRoomRepository = StudyRoomRepository(
        service,
        DummyStudyRoomProvider, // 로컬 모드용 DummyProvider
        application.applicationContext // Context 전달
    ) // 수정

    private var localUserNickname: String = "레인" // 기본값 또는 로그인 정보에서 가져오도록 수정 필요
    private var localUserId: Int = 101      // 기본값 또는 로그인 정보에서 가져오도록 수정 필요

    val currentNickname: String get() = localUserNickname //
    val currentUserId: Int get() = localUserId //

    private val _roomDetails = MutableLiveData<ApiResult<StudyRoom>>() //
    val roomDetails: LiveData<ApiResult<StudyRoom>> get() = _roomDetails //

    private val _kickResult = MutableLiveData<ApiResult<Unit>>(ApiResult.Idle) //
    val kickResult: LiveData<ApiResult<Unit>> get() = _kickResult //

    private val _message = MutableLiveData<String?>() //
    val message: LiveData<String?> get() = _message //

    private val _isLoading = MutableLiveData<Boolean>() //
    val isLoading: LiveData<Boolean> get() = _isLoading //

    // 기능: ViewModel 초기화 시 현재 사용자 정보 설정 (로그인 시스템 연동 시 대체)
    fun initCurrentUser(nickname: String, userId: Int) {
        localUserNickname = nickname
        localUserId = userId
    } //

    // 기능: 특정 스터디룸의 상세 정보를 로드
    fun loadRoomDetails(roomTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _roomDetails.value = ApiResult.Loading
            if (localUserId == -1) { // 사용자 ID 유효성 검사
                _message.value = "사용자 정보가 유효하지 않습니다."
                _roomDetails.value = ApiResult.Error("사용자 ID 오류")
                _isLoading.value = false
                return@launch
            }
            // Repository의 getStudyRoomDetails 호출 (currentUserId 전달)
            val result = repository.getStudyRoomDetails(roomTitle, "", localUserId) // 호출 방식 수정
            _roomDetails.value = result
            if (result is ApiResult.Error) {
                _message.value = "'$roomTitle' 상세 정보 로드 실패: ${result.message}"
            }
            _isLoading.value = false
        }
    } // 수정

    // 기능: 스터디룸에서 특정 멤버를 강퇴
    fun kickMemberFromRoom(roomTitle: String, memberKickId: Int) {
        viewModelScope.launch {
            if (_kickResult.value is ApiResult.Loading) {
                _message.value = "이미 강퇴 작업이 진행 중입니다."
                return@launch
            }
            _isLoading.value = true
            _kickResult.value = ApiResult.Loading
            if (localUserId == -1) { // 사용자 ID(방장 ID) 유효성 검사
                _message.value = "사용자 정보(방장)가 유효하지 않습니다."
                _kickResult.value = ApiResult.Error("방장 ID 오류")
                _isLoading.value = false
                return@launch
            }

            // Repository의 kickStudyRoomMemberById 호출 (방장 ID는 localUserId)
            val kickAttemptResult = repository.kickStudyRoomMemberById(roomTitle, memberKickId, localUserId) // 호출 방식 수정
            if (kickAttemptResult is ApiResult.Success) {
                _message.value = "멤버를 강퇴했습니다."
                loadRoomDetails(roomTitle) // 성공 시 방 정보 다시 로드
                _kickResult.value = ApiResult.Success(Unit)
            } else if (kickAttemptResult is ApiResult.Error) {
                _message.value = "멤버 강퇴 실패: ${kickAttemptResult.message}"
                loadRoomDetails(roomTitle) // 실패 시에도 방 정보 다시 로드 (예: 스와이프 원위치)
                _kickResult.value = kickAttemptResult
            }
            _isLoading.value = false
        }
    } // 수정

    // 기능: UI에 표시된 메시지 상태를 초기화
    fun clearMessage() {
        _message.value = null
    } //

    // 기능: 멤버 강퇴 작업 결과 상태를 초기화 (Idle 상태로 변경)
    fun clearKickResult() {
        if (_kickResult.value !is ApiResult.Idle) {
            _kickResult.value = ApiResult.Idle
        }
    } //
}