package com.example.Rainbow_Voca.viewmodel

import android.app.Application // AndroidViewModel 사용 시 필요
import androidx.lifecycle.AndroidViewModel // Application Context 사용 위해 변경
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.data.DummyStudyRoomProvider
import com.example.Rainbow_Voca.data.StudyRoomRepository
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.network.ApiStudyRoomSearchResultItem
import com.example.Rainbow_Voca.network.RetrofitInstance
import com.example.Rainbow_Voca.network.StudyRoomApiService
import com.example.Rainbow_Voca.util.ApiResult
import kotlinx.coroutines.launch

// Application Context를 사용하기 위해 AndroidViewModel로 변경
class StudyRoomViewModel(application: Application) : AndroidViewModel(application) {

    // 기능: Repository 인스턴스 (Hilt 등으로 주입하는 것이 이상적)
    private val repository: StudyRoomRepository = StudyRoomRepository(
        RetrofitInstance.retrofit.create(StudyRoomApiService::class.java),
        DummyStudyRoomProvider,
        application.applicationContext
    )

    // 기능: 현재 사용자 정보 (실제로는 로그인 시스템과 연동 필요)
    var currentUserId: Int = 101 // 임시 ID
    var currentUserNickname: String = "레인" // 임시 닉네임
    var currentUserProfileImage: String = "logo" // 임시 프로필 이미지

    // LiveData 정의
    private val _joinedRooms = MutableLiveData<ApiResult<List<StudyRoom>>>(ApiResult.Idle)
    val joinedRooms: LiveData<ApiResult<List<StudyRoom>>> get() = _joinedRooms

    // 검색 결과 LiveData (ApiStudyRoomSearchResultItem 사용)
    private val _foundRooms = MutableLiveData<ApiResult<List<ApiStudyRoomSearchResultItem>>>(ApiResult.Idle)
    val foundRooms: LiveData<ApiResult<List<ApiStudyRoomSearchResultItem>>> get() = _foundRooms

    private val _createOp = MutableLiveData<ApiResult<StudyRoom>>(ApiResult.Idle)
    val createOp: LiveData<ApiResult<StudyRoom>> get() = _createOp

    private val _joinOp = MutableLiveData<ApiResult<StudyRoom>>(ApiResult.Idle)
    val joinOp: LiveData<ApiResult<StudyRoom>> get() = _joinOp

    private val _leaveOp = MutableLiveData<ApiResult<Unit>>(ApiResult.Idle)
    val leaveOp: LiveData<ApiResult<Unit>> get() = _leaveOp

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _message = MutableLiveData<String?>() // 사용자에게 보여줄 메시지
    val message: LiveData<String?> get() = _message

    init {
        loadJoinedRooms() // ViewModel 생성 시 내 방 목록 로드
    }

    // 기능: 현재 사용자 정보 설정 (로그인 완료 후 호출)
    fun initUser(nickname: String, userId: Int, profileImage: String) { // 파라미터 순서 및 타입: String, Int, String
        this.currentUserNickname = nickname
        this.currentUserId = userId
        this.currentUserProfileImage = profileImage
        loadJoinedRooms() // 사용자 정보 설정 후 내 방 목록 다시 로드
    }

    // 기능: 내가 참여한 스터디룸 목록 로드
    fun loadJoinedRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            _joinedRooms.value = ApiResult.Loading // 로딩 상태 먼저 반영
            val result = repository.getMyJoinedStudyRooms(currentUserId, currentUserNickname) // currentUserId와 로컬용 닉네임 전달
            _joinedRooms.value = result
            if (result is ApiResult.Error) {
                _message.value = "내 방 목록 로드 실패: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    // 기능: 새 스터디룸 생성
    fun createNewRoom(title: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _createOp.value = ApiResult.Loading //
            val result = repository.createStudyRoom(title, password) //
            _createOp.value = result //
            if (result is ApiResult.Success) { //
                _message.value = "'${result.data.title}' 방 생성 완료!" //
                loadJoinedRooms() // 방 생성 후 내 방 목록 새로고침
            } else if (result is ApiResult.Error) { //
                _message.value = "방 생성 실패: ${result.message}" //
            }
            _isLoading.value = false //
        }
    }

    // 기능: 기존 스터디룸 참여
    fun joinExistingRoom(title: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _joinOp.value = ApiResult.Loading
            val result = repository.joinStudyRoom(title, password, currentUserId)
            _joinOp.value = result
            if (result is ApiResult.Success) {
                _message.value = "'${result.data.title}' 방 참여 완료!"
                loadJoinedRooms() // 방 참여 후 내 방 목록 새로고침
            } else if (result is ApiResult.Error) {
                _message.value = "방 참여 실패: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    // 기능: 현재 스터디룸 나가기
    fun leaveCurrentRoom(roomTitle: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _leaveOp.value = ApiResult.Loading
            val result = repository.leaveStudyRoom(roomTitle, currentUserId, currentUserNickname)
            _leaveOp.value = result
            if (result is ApiResult.Success) {
                _message.value = "'$roomTitle' 방에서 나갔습니다."
                loadJoinedRooms() // 방 나간 후 내 방 목록 새로고침
            } else if (result is ApiResult.Error) {
                _message.value = "방 나가기 실패: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    // 기능: 참여 가능한 스터디룸 검색 (서버 연동)
    fun findRooms(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _foundRooms.value = ApiResult.Loading
            if (query.isBlank()) {
                _foundRooms.value = ApiResult.Success(emptyList()) // 빈 쿼리는 빈 결과
                _isLoading.value = false
                return@launch
            }
            val result = repository.searchRooms(query, currentUserId) // ApiResult<List<ApiStudyRoomSearchResultItem>> 반환
            _foundRooms.value = result
            if (result is ApiResult.Error) {
                _message.value = "방 검색 실패: ${result.message}"
            }
            _isLoading.value = false
        }
    }

    // 기능: 메시지 소비 후 null로 초기화 (Toast 중복 방지 등)
    fun clearMessage() {
        _message.value = null
    }

    // 기능: 각 작업 상태 초기화 (필요에 따라)
    fun clearCreateOp() { _createOp.value = ApiResult.Idle }
    fun clearJoinOp() { _joinOp.value = ApiResult.Idle }
    fun clearLeaveOp() { _leaveOp.value = ApiResult.Idle }
    fun clearFoundRooms() { _foundRooms.value = ApiResult.Idle }
}