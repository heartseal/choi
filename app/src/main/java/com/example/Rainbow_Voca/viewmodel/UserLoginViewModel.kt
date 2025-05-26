package com.example.Rainbow_Voca.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.network.ApiServicePool
import com.example.Rainbow_Voca.network.SignUpRequest // UserApiService에서 사용하는 DTO
import kotlinx.coroutines.launch

// 사용자 인증 (로그인, 닉네임 등록) 관리
class UserLoginViewModel : ViewModel() {

    // LiveData: 현재 유효하거나 새로 발급된 JWT 토큰
    private val _token = MutableLiveData<String?>()
    val token: LiveData<String?> = _token

    // LiveData: 오류 발생 시 메시지
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 닉네임 등록 (최초 로그인 시 사용)
    fun submitNickname(nickname: String) { // 함수명 변경: registerNickname -> submitNickname
        viewModelScope.launch {
            try {
                val response = ApiServicePool.userApi.signUp(SignUpRequest(nickname))
                if (response.success == true && response.token != null) {
                    _token.value = response.token // 성공: 토큰 업데이트
                } else {
                    _error.value = response.message ?: "닉네임 등록에 실패했습니다." // 실패: 오류 메시지 설정
                }
            } catch (e: Exception) {
                _error.value = "네트워크 통신 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    // 저장된 토큰으로 자동 로그인 시도
    fun attemptAutoLogin(savedToken: String) { // 함수명 변경: loginWithToken -> attemptAutoLogin, 파라미터명 변경
        viewModelScope.launch {
            try {
                // "Bearer " 접두사 포함하여 API 호출
                val response = ApiServicePool.userApi.autoLogin("Bearer $savedToken")
                if (response.user != null) {
                    _token.value = savedToken // 토큰 유효: 기존 토큰으로 설정
                } else {
                    _error.value = response.message ?: "자동 로그인 실패: 유효하지 않은 토큰입니다." // 토큰 무효
                }
            } catch (e: Exception) {
                _error.value = "자동 로그인 중 네트워크 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    // 오류 메시지 LiveData 초기화 (UI에서 오류 확인 후 호출)
    fun consumeError() { // 함수명 변경: clearError -> consumeError
        _error.value = null
    }
}