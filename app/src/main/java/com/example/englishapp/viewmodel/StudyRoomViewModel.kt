package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.network.* // ApiServicePool 및 모든 DTO 포함 가정
import com.example.englishapp.util.ApiResult // 수정된 ApiResult import
import kotlinx.coroutines.launch

// 스터디방 관련 기능 및 데이터 관리
class StudyRoomViewModel : ViewModel() {

    // LiveData: 스터디방 생성 API 호출 상태 및 결과 (초기 상태: Idle)
    private val _createRoomOperation = MutableLiveData<ApiResult<CreateRoomResponse>>(ApiResult.Idle)
    val createRoomOperation: LiveData<ApiResult<CreateRoomResponse>> = _createRoomOperation

    // LiveData: 스터디방 참여 API 호출 상태 및 결과 (초기 상태: Idle)
    private val _joinRoomOperation = MutableLiveData<ApiResult<JoinRoomResponse>>(ApiResult.Idle)
    val joinRoomOperation: LiveData<ApiResult<JoinRoomResponse>> = _joinRoomOperation

    // LiveData: 현재 조회된 스터디방의 상세 정보
    private val _currentRoomDetails = MutableLiveData<RoomDetailsResponse?>() // 상세 정보는 null일 수 있음
    val currentRoomDetails: LiveData<RoomDetailsResponse?> = _currentRoomDetails

    // LiveData: 스터디방 멤버 삭제 API 호출 상태 및 결과 (초기 상태: Idle)
    private val _deleteMemberOperation = MutableLiveData<ApiResult<BaseSuccessResponse>>(ApiResult.Idle)
    val deleteMemberOperation: LiveData<ApiResult<BaseSuccessResponse>> = _deleteMemberOperation

    // LiveData: 일반적인 오류 메시지 (주로 네트워크 오류 등 상세 정보 전달용)
    private val _errorMessage = MutableLiveData<String?>() // 이 메시지는 null일 수 있음
    val errorMessage: LiveData<String?> = _errorMessage

    // StudyRoomViewModel.kt 내의 createStudyRoom 함수

    // 스터디방 생성 요청
    fun createStudyRoom(token: String, title: String) {
        _createRoomOperation.value = ApiResult.Loading // API 호출 시작 시 로딩 상태로 변경
        viewModelScope.launch {
            try {
                // ApiServicePool을 통해 스터디방 생성 API 호출
                val response: CreateRoomResponse = ApiServicePool.studyRoomApi.createRoom(
                    "Bearer $token", // 인증 토큰
                    CreateRoomRequest(title = title, password = null) // 방 제목, 비밀번호는 서버 자동 생성
                )

                // API 응답 기반으로 성공/실패 처리
                // (성공 조건은 실제 API 명세에 따라 roomId, roomCode 등의 유효성 검사로 구체화)
                if (response.roomId != 0 && response.roomCode.isNotBlank()) {
                    _createRoomOperation.value = ApiResult.Success(response) // 성공 결과 전달
                } else {
                    // 성공적인 HTTP 응답을 받았으나, 반환된 데이터가 유효하지 않은 경우
                    _createRoomOperation.value = ApiResult.Error("방 정보가 유효하지 않습니다.")
                }
            } catch (e: Exception) {
                // 네트워크 오류 또는 서버에서 HTTP 에러 응답 시
                _createRoomOperation.value = ApiResult.Error("방 생성 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 스터디방 참여 요청
    fun joinStudyRoom(token: String, roomCode: String) {
        _joinRoomOperation.value = ApiResult.Loading // 로딩 상태로 변경
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.joinRoom(
                    "Bearer $token", JoinRoomRequest(roomCode = roomCode)
                )
                if (response.success == true && response.room != null) {
                    _joinRoomOperation.value = ApiResult.Success(response)
                } else {
                    _joinRoomOperation.value = ApiResult.Error(response.message ?: "방 참여에 실패했습니다. 일부 데이터를 받아오는데 문제가 생겼습니다.")
                }
            } catch (e: Exception) {
                _joinRoomOperation.value = ApiResult.Error("방 참여 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // 스터디방 상세 정보 조회 요청
    fun loadRoomDetails(token: String, roomId: Int) {
        _errorMessage.value = null // 이전 일반 오류 초기화
        // _currentRoomDetails.value = null // 로딩 시작 시 데이터 비우기 (선택적)
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.getRoomDetails("Bearer $token", roomId)
                _currentRoomDetails.value = response // 성공 시 데이터 업데이트
            } catch (e: Exception) {
                _currentRoomDetails.value = null // 실패 시 데이터 비우기
                _errorMessage.value = "방 정보 조회 중 오류 발생: ${e.localizedMessage}"
            }
        }
    }

    // 스터디방 멤버 삭제 요청 (방 관리자 권한)
    fun removeStudyMember(token: String, roomId: Int, memberUserId: Int) {
        _deleteMemberOperation.value = ApiResult.Loading // 로딩 상태로 변경
        viewModelScope.launch {
            try {
                val response = ApiServicePool.studyRoomApi.deleteRoomMember("Bearer $token", roomId, memberUserId)
                if (response.success == true) {
                    _deleteMemberOperation.value = ApiResult.Success(response)
                    // 멤버 삭제 성공 후, 현재 방 정보를 다시 로드하여 멤버 목록을 UI에 갱신
                    loadRoomDetails(token, roomId)
                } else {
                    _deleteMemberOperation.value = ApiResult.Error(response.message ?: "멤버 삭제에 실패했습니다.")
                }
            } catch (e: Exception) {
                _deleteMemberOperation.value = ApiResult.Error("멤버 삭제 중 오류 발생: ${e.localizedMessage}")
            }
        }
    }

    // ViewModel의 모든 ApiResult LiveData 상태 및 오류 메시지 초기화
    fun clearAllStudyRoomOperations() { // 함수명 변경: clearAllStudyRoomData -> clearAllStudyRoomOperations
        _createRoomOperation.value = ApiResult.Idle
        _joinRoomOperation.value = ApiResult.Idle
        // _currentRoomDetails는 API 호출 결과가 아니므로 별도 관리 (또는 필요시 null로 초기화)
        // _currentRoomDetails.value = null
        _deleteMemberOperation.value = ApiResult.Idle
        _errorMessage.value = null
    }

    // StudyRoomViewModel.kt 내의 consumeErrorState 함수
    // UI에서 오류 상태를 확인한 후, 관련 Operation LiveData를 초기 상태로 되돌림
    fun consumeCurrentErrorState() { // 함수명 변경
        _errorMessage.value = null // 일반 오류 메시지 초기화

        // 각 작업(Operation) LiveData의 값이 Error 상태였다면 Idle 상태로 변경
        if (_createRoomOperation.value is ApiResult.Error) {
            _createRoomOperation.value = ApiResult.Idle
        }
        if (_joinRoomOperation.value is ApiResult.Error) {
            _joinRoomOperation.value = ApiResult.Idle
        }
        if (_deleteMemberOperation.value is ApiResult.Error) {
            _deleteMemberOperation.value = ApiResult.Idle
        }
    }

    // 일반 오류 메시지 LiveData만 초기화하는 함수
    fun consumeGeneralErrorMessage() { // 함수명 변경: consumeErrorMessage -> consumeGeneralErrorMessage
        _errorMessage.value = null
    }
}