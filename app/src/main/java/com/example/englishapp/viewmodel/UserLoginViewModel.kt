package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishapp.network.ApiServicePool
import com.example.englishapp.network.SignUpRequest
import kotlinx.coroutines.launch

class UserLoginViewModel : ViewModel() {
    private val _token = MutableLiveData<String?>()
    val token: LiveData<String?> = _token

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 회원가입(닉네임 등록)
    fun registerNickname(nickname: String) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.userApi.signUp(SignUpRequest(nickname))
                if (response.success == true && response.token != null) {
                    _token.value = response.token
                } else {
                    _error.value = response.message ?: "닉네임 등록 실패"
                }
            } catch (e: Exception) {
                _error.value = "네트워크 오류: ${e.message}"
            }

            // 향후 중복 닉네임일시 "이미 있는 닉네임입니다" 알림을 띄우면서 거절하는 기능 추가
        }
    }

    // 저장된 토큰으로 자동 로그인
    fun loginWithToken(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiServicePool.userApi.autoLogin("Bearer $token")
                if (response.user != null) {
                    _token.value = token
                } else {
                    _error.value = response.message ?: "토큰 만료"
                }
            } catch (e: Exception) {
                _error.value = "네트워크 오류: ${e.message}"
            }
        }
    }

    fun clearToken() {
        _token.value = null
    }
}
