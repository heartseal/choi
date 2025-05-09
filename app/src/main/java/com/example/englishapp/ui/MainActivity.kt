package com.example.englishapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.edit
import androidx.drawerlayout.widget.DrawerLayout
import com.example.englishapp.R
import com.example.englishapp.manager.QuizWordRepository
import com.example.englishapp.viewmodel.MainPageViewModel
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private val viewModel: MainPageViewModel by viewModels()

    // 오늘의 학습 결과 런처
    private val studyLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val learningFinished = result.data?.getBooleanExtra("learningFinished", false) ?: false
            if (learningFinished) {
                findViewById<Button>(R.id.btn_learning).visibility = View.GONE
                findViewById<Button>(R.id.btn_reviewLearning).visibility = View.VISIBLE
            }
        }
    }

    // 10분 후 복습 결과 런처
    private val postLearningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            findViewById<Button>(R.id.btn_reviewLearning).visibility = View.GONE
            findViewById<Button>(R.id.btn_reviewLearning_END).visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 오늘의 학습 버튼
        findViewById<Button>(R.id.btn_learning).setOnClickListener {
            val intent = Intent(this, WordStudyActivity::class.java).apply {
                putExtra("token", "your_jwt_token_here")
            }
            studyLauncher.launch(intent)
        }

        // 툴바 초기화
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 뷰 초기화
        initViews()

        // ViewModel 관찰 설정
        setupObservers()

        // 초기 데이터 로드
        viewModel.fetchMainPage("your_jwt_token_here")

        // 네비게이션 설정
        setupNavigation()
    }

    private fun initViews() {
        // 10분 후 복습 버튼
        findViewById<Button>(R.id.btn_reviewLearning).setOnClickListener {
            val quizWords = QuizWordRepository.quizWords
            if (quizWords.isEmpty()) {
                Toast.makeText(this, "복습할 단어가 없습니다", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, WordQuizActivity::class.java).apply {
                    putParcelableArrayListExtra("quizWords", ArrayList(quizWords))
                }
                postLearningLauncher.launch(intent)
            }
        }

        // 10분 후 복습 완료(END) 버튼: 비활성화
        findViewById<Button>(R.id.btn_reviewLearning_END).apply {
            isEnabled = false
            alpha = 0.5f
        }

        // 누적 복습 버튼
        findViewById<Button>(R.id.btn_review).setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }

        // 지문 학습 버튼
        findViewById<Button>(R.id.btn_passage).setOnClickListener {
            startActivity(Intent(this, PassageActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.todayReviewGoal.observe(this) { count ->
            findViewById<TextView>(R.id.today_review_count).text = "${count}개"
        }

        viewModel.todayLearningGoal.observe(this) { count ->
            findViewById<TextView>(R.id.today_learning_goal).text = "${count}개"
        }

        viewModel.estimatedCompletionDate.observe(this) { date ->
            findViewById<TextView>(R.id.expected_finish_date).text = date
        }

        viewModel.monthlyAttendanceRate.observe(this) { rate ->
            findViewById<TextView>(R.id.month_diligence).text = "%.1f%%".format(rate)
        }

        viewModel.totalWordCount.observe(this) { total ->
            toolbar.title = "영단어앱 ($total)"
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }

        // ProgressBar 색상별 데이터 반영
        viewModel.stageProgress.observe(this) { progressList ->
            if (progressList.size == 7) {
                findViewById<ProgressBar>(R.id.progressBar_red_vertical).progress = progressList[0]
                findViewById<ProgressBar>(R.id.progressBar_orange_vertical).progress = progressList[1]
                findViewById<ProgressBar>(R.id.progressBar_yellow_vertical).progress = progressList[2]
                findViewById<ProgressBar>(R.id.progressBar_green_vertical).progress = progressList[3]
                findViewById<ProgressBar>(R.id.progressBar_blue_vertical).progress = progressList[4]
                findViewById<ProgressBar>(R.id.progressBar_navy_vertical).progress = progressList[5]
                findViewById<ProgressBar>(R.id.progressBar_purple_vertical).progress = progressList[6]
            }
        }
    }

    private fun setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_all_words -> Toast.makeText(this, "전체 단어 보기", Toast.LENGTH_SHORT).show()
                R.id.nav_progress -> Toast.makeText(this, "학습 현황 보기", Toast.LENGTH_SHORT).show()
                R.id.nav_diligence -> Toast.makeText(this, "성실도 보기", Toast.LENGTH_SHORT).show()
                R.id.nav_settings -> Toast.makeText(this, "설정", Toast.LENGTH_SHORT).show()
                R.id.nav_admin -> startActivity(Intent(this, AdminActivity::class.java))
            }
            true
        }
    }
}
