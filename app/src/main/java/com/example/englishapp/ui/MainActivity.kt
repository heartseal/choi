package com.example.englishapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar // UI 요소로 ProgressBar 사용 시
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.englishapp.R
import com.example.englishapp.datastore.TokenDataStore
import com.example.englishapp.viewmodel.MainPageViewModel
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

// 앱의 메인 대시보드 화면
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    private val mainPageViewModel: MainPageViewModel by viewModels()

    // UI 요소 참조 변수
    private lateinit var btnLearning: Button
    private lateinit var btnReviewPostLearning: Button // 10분 후 복습 (기존 btn_reviewLearning)
    private lateinit var btnLearningSessionDone: Button  // 오늘의 학습 모두 완료 시 (기존 btn_reviewLearning_END)
    private lateinit var btnReviewStaged: Button       // 누적 복습 (기존 btn_review)
    private lateinit var btnPassageStudy: Button       // 맞춤 독해 (기존 btn_passage)
    private lateinit var loadingIndicator: ProgressBar // 데이터 로딩 시 보여줄 프로그레스바 (XML에 ID 추가 가정)

    private var currentToken: String? = null // 현재 사용자 인증 토큰

    // "오늘의 학습" Activity (WordStudyActivity) 결과 처리
    private val studyActivityResultLauncher = registerForActivityResult( // 이름 변경
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 학습 완료 후 메인 화면 데이터 및 버튼 상태 새로고침
            currentToken?.let { token -> mainPageViewModel.loadMainPageData(token) }
        }
    }

    // "10분 후 복습" Activity (WordQuizActivity) 결과 처리
    private val postLearningReviewActivityResultLauncher = registerForActivityResult( // 이름 변경
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 10분 후 복습 완료 후 메인 화면 데이터 및 버튼 상태 새로고침
            currentToken?.let { token -> mainPageViewModel.loadMainPageData(token) }
        }
    }

    // "누적 복습" Activity (ReviewActivity) 결과 처리
    private val stagedReviewActivityResultLauncher = registerForActivityResult( // 이름 변경
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 누적 복습 완료 후 메인 화면 데이터 및 버튼 상태 새로고침
            currentToken?.let { token -> mainPageViewModel.loadMainPageData(token) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupToolbarAndNavigationDrawer()
        initializeUiReferences()
        setupButtonClickListeners()
        observeViewModelData()

        checkUserLoginStateAndLoadData() // 사용자 로그인 상태 확인 및 데이터 로드
    }

    // 툴바 및 네비게이션 드로어 초기 설정
    private fun setupToolbarAndNavigationDrawer() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)
    }

    // XML 레이아웃의 UI 요소 참조 초기화
    private fun initializeUiReferences() {
        btnLearning = findViewById(R.id.btn_learning)
        btnReviewPostLearning = findViewById(R.id.btn_reviewLearning)
        btnLearningSessionDone = findViewById(R.id.btn_reviewLearning_END)
        btnReviewStaged = findViewById(R.id.btn_review)
        btnPassageStudy = findViewById(R.id.btn_passage)
        // loadingIndicator = findViewById(R.id.progress_bar_main_loading) // XML에 해당 ID 추가 필요
    }

    // 버튼 클릭 이벤트 리스너 설정
    private fun setupButtonClickListeners() {
        btnLearning.setOnClickListener {
            navigateToActivityWithToken(WordStudyActivity::class.java, studyActivityResultLauncher,
                mapOf("dailyGoal" to (mainPageViewModel.todayLearningGoal.value ?: 10))
            )
        }
        btnReviewPostLearning.setOnClickListener {
            navigateToActivityWithToken(WordQuizActivity::class.java, postLearningReviewActivityResultLauncher)
        }
        btnReviewStaged.setOnClickListener {
            navigateToActivityWithToken(ReviewActivity::class.java, stagedReviewActivityResultLauncher)
        }
        btnPassageStudy.setOnClickListener {
            navigateToActivityWithToken(PassageActivity::class.java)
        }
    }

    // 특정 Activity로 토큰 및 추가 데이터를 전달하며 이동하는 함수
    private fun navigateToActivityWithToken(
        activityClass: Class<*>,
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>? = null,
        extras: Map<String, Any>? = null
    ) {
        currentToken?.let { token ->
            val intent = Intent(this, activityClass).apply {
                putExtra("token", token)
                extras?.forEach { (key, value) ->
                    when (value) {
                        is Int -> putExtra(key, value)
                        is String -> putExtra(key, value)
                        is Boolean -> putExtra(key, value)
                        // 필요한 다른 타입들 추가
                    }
                }
            }
            launcher?.launch(intent) ?: startActivity(intent)
        } ?: showToast("로그인 정보가 필요합니다. 다시 로그인해주세요.")
    }


    // ViewModel의 LiveData 변경 사항 관찰
    private fun observeViewModelData() {
        mainPageViewModel.isLoading.observe(this) { isLoading ->
            // loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        mainPageViewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showToast(it)
                mainPageViewModel.consumeErrorMessage() // 오류 메시지 소비
            }
        }

        // 대시보드 정보 업데이트
        mainPageViewModel.todayLearningGoal.observe(this) { findViewById<TextView>(R.id.today_learning_goal).text = "${it}개" }
        mainPageViewModel.todayReviewGoal.observe(this) { findViewById<TextView>(R.id.today_review_count).text = "${it}개" }
        mainPageViewModel.estimatedCompletionDate.observe(this) { findViewById<TextView>(R.id.expected_finish_date).text = it }
        mainPageViewModel.monthlyAttendanceRate.observe(this) { findViewById<TextView>(R.id.month_diligence).text = "%.1f%%".format(it) }
        mainPageViewModel.totalWordCount.observe(this) { toolbar.title = "영단어 학습 (${it}단어)" }

        // 단계별 단어 진행률 ProgressBar 업데이트
        mainPageViewModel.stageProgressPercent.observe(this) { progressList ->
            if (progressList.size == 7) { // Red, Orange, ..., Violet 순서
                findViewById<ProgressBar>(R.id.progressBar_red_vertical).progress = progressList[0]
                findViewById<ProgressBar>(R.id.progressBar_orange_vertical).progress = progressList[1]
                findViewById<ProgressBar>(R.id.progressBar_yellow_vertical).progress = progressList[2]
                findViewById<ProgressBar>(R.id.progressBar_green_vertical).progress = progressList[3]
                findViewById<ProgressBar>(R.id.progressBar_blue_vertical).progress = progressList[4]
                findViewById<ProgressBar>(R.id.progressBar_navy_vertical).progress = progressList[5]
                findViewById<ProgressBar>(R.id.progressBar_purple_vertical).progress = progressList[6]
            }
        }

        // 오늘의 학습 완료 상태에 따른 버튼 UI 업데이트
        mainPageViewModel.isTodayLearningCompleted.observe(this) { isCompleted ->
            updateLearningButtonStates(isCompleted, mainPageViewModel.isPostLearningReviewReady.value ?: false)
        }

        // 10분 후 복습 준비 상태에 따른 버튼 UI 업데이트
        mainPageViewModel.isPostLearningReviewReady.observe(this) { isReady ->
            updateLearningButtonStates(mainPageViewModel.isTodayLearningCompleted.value ?: false, isReady)
        }
    }

    // 학습 관련 버튼들의 상태(가시성, 활성화)를 업데이트
    private fun updateLearningButtonStates(isTodayLearningDone: Boolean, isPostReviewReady: Boolean) {
        if (!isTodayLearningDone) { // 오늘의 학습 미완료
            btnLearning.visibility = View.VISIBLE
            btnReviewPostLearning.visibility = View.GONE
            btnLearningSessionDone.visibility = View.GONE
        } else { // 오늘의 학습 완료
            btnLearning.visibility = View.GONE
            if (isPostReviewReady) { // 10분 후 복습 준비됨
                btnReviewPostLearning.visibility = View.VISIBLE
                btnLearningSessionDone.visibility = View.GONE
            } else { // 10분 후 복습이 준비 안됐거나 이미 완료됨
                btnReviewPostLearning.visibility = View.GONE
                btnLearningSessionDone.visibility = View.VISIBLE
                btnLearningSessionDone.isEnabled = false // 일반적으로 상태 표시용
                btnLearningSessionDone.alpha = 0.5f      // 비활성화 시각적 표현
            }
        }
    }

    // 사용자 로그인 상태 확인 및 초기 데이터 로드
    private fun checkUserLoginStateAndLoadData() {
        lifecycleScope.launch {
            currentToken = TokenDataStore.getToken(applicationContext)
            if (currentToken.isNullOrBlank()) {
                // 토큰 없음: 로그인 화면으로 이동 및 현재 화면 종료
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                // 토큰 있음: 메인 페이지 데이터 로드
                mainPageViewModel.loadMainPageData(currentToken!!)
            }
        }
    }

    // 네비게이션 드로어 아이템 선택 이벤트 처리
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_words -> showToast("전체 단어 보기 (구현 예정)")
            R.id.nav_progress -> showToast("학습 현황 보기 (구현 예정)")
            R.id.nav_diligence -> showToast("성실도 보기 (구현 예정)")
            R.id.nav_settings -> showToast("설정 (구현 예정)") // 설정 Activity로 이동 구현 필요
            R.id.nav_admin -> navigateToActivityWithToken(AdminActivity::class.java)
            // R.id.nav_logout -> performLogout() // 로그아웃 처리 후에 추가 고려하기
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // 로그아웃 수행
    private fun performLogout() {
        lifecycleScope.launch {
            TokenDataStore.clearToken(applicationContext) // 저장된 토큰 삭제
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent) // 로그인 화면으로 이동
        }
    }

    // 뒤로가기 버튼 처리 (드로어가 열려있으면 먼저 닫음)
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // 간단한 Toast 메시지 표시 유틸리티
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}