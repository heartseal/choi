package com.example.englishapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.englishapp.R
import com.example.englishapp.datastore.TokenDataStore
import com.example.englishapp.viewmodel.UserLoginViewModel
import kotlinx.coroutines.launch

// 닉네임 입력 및 로그인(최초 등록) 처리
class LoginActivity : AppCompatActivity() {

    private lateinit var titleTextViewLogin: TextView // XML ID: text_title_login
    private lateinit var errorTextViewLogin: TextView // XML ID: text_error_login
    private lateinit var nicknameEditTextLogin: EditText // XML ID: edit_text_nickname_login
    private lateinit var submitButtonLogin: Button // XML ID: button_submit_login
    private lateinit var progressBarLogin: ProgressBar // XML ID: progress_bar_login

    private val userLoginViewModel: UserLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // activity_login.xml 사용

        initializeUiReferences()
        setupButtonListeners()
        observeViewModelData()
    }

    // XML 레이아웃의 UI 요소 참조 초기화
    private fun initializeUiReferences() {
        titleTextViewLogin = findViewById(R.id.text_title_login)
        errorTextViewLogin = findViewById(R.id.text_error_login)
        nicknameEditTextLogin = findViewById(R.id.edit_text_nickname_login)
        submitButtonLogin = findViewById(R.id.button_submit_login)
        progressBarLogin = findViewById(R.id.progress_bar_login)
    }

    // 버튼 클릭 이벤트 리스너 설정
    private fun setupButtonListeners() {
        submitButtonLogin.setOnClickListener {
            val nickname = nicknameEditTextLogin.text.toString().trim()
            if (nickname.isNotBlank()) {
                hideLoginError() // 이전 오류 메시지 숨김
                showLoginLoading(true) // 로딩 UI 표시
                userLoginViewModel.submitNickname(nickname) // ViewModel에 닉네임 등록(로그인) 요청
            } else {
                showLoginError("닉네임을 입력해주세요.")
            }
        }
    }

    // ViewModel의 LiveData 변경 사항 관찰
    private fun observeViewModelData() {
        // 토큰 발급 성공 (로그인/등록 성공)
        userLoginViewModel.token.observe(this) { token ->
            // 로딩 UI 숨김 (토큰이 null일 때도 로딩은 끝난 상태일 수 있음)
            showLoginLoading(false)
            if (token != null) {
                // 토큰을 DataStore에 비동기 저장
                lifecycleScope.launch {
                    TokenDataStore.saveToken(applicationContext, token)
                    Toast.makeText(this@LoginActivity, "로그인 되었습니다.", Toast.LENGTH_SHORT).show()
                    // 메인 액티비티로 이동하고 현재 화면 스택 정리
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            }
        }

        // 닉네임 등록/로그인 실패 또는 기타 오류 발생
        userLoginViewModel.error.observe(this) { errorMessage ->
            showLoginLoading(false) // 로딩 UI 숨김
            if (errorMessage != null) {
                showLoginError(errorMessage) // 오류 메시지 표시
                userLoginViewModel.consumeError() // ViewModel의 오류 상태 초기화
            }
        }
    }

    // 오류 메시지를 UI에 표시
    private fun showLoginError(message: String) {
        errorTextViewLogin.text = message
        errorTextViewLogin.visibility = View.VISIBLE
    }

    // UI의 오류 메시지를 숨김
    private fun hideLoginError() {
        errorTextViewLogin.visibility = View.GONE
    }

    // 로딩 인디케이터(ProgressBar) 및 버튼 활성화 상태 제어
    private fun showLoginLoading(isLoading: Boolean) {
        progressBarLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        submitButtonLogin.isEnabled = !isLoading
        nicknameEditTextLogin.isEnabled = !isLoading
    }
}