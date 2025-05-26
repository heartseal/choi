package com.example.Rainbow_Voca.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.Rainbow_Voca.R
import com.example.Rainbow_Voca.model.QuizQuestion
import com.example.Rainbow_Voca.model.Word // Word 모델 import
import com.example.Rainbow_Voca.network.ApiServicePool
import com.example.Rainbow_Voca.network.ReviewResultItem
import com.example.Rainbow_Voca.network.StagedReviewResultRequest
import com.example.Rainbow_Voca.viewmodel.ReviewViewModel
import com.example.Rainbow_Voca.viewmodel.StudyViewModel // 사용자님의 StudyViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// "오늘의 누적 복습" 퀴즈 화면
class ReviewActivity : AppCompatActivity() {

    // ViewModels
    private val reviewViewModel: ReviewViewModel by viewModels()
    private val studyViewModel: StudyViewModel by viewModels() // 사용자님의 StudyViewModel 사용

    // UI 요소 (activity_quiz.xml 재사용)
    private lateinit var quizWordText: TextView
    private lateinit var quizProgressText: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var closeQuizButton: ImageButton
    private lateinit var feedbackImage: ImageView
    private lateinit var quizLoadingBar: ProgressBar
    private lateinit var rootQuizView: View

    // 상태 변수
    private var authToken: String? = null
    private var quizSessionId: String? = null // 누적 복습 세션 ID (필요시 사용)
    private val feedbackDurationMs = 700L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_quiz) // activity_quiz.xml 재사용

        rootQuizView = findViewById(android.R.id.content)
        initUi()

        authToken = intent.getStringExtra("token")
        // quizSessionId = intent.getStringExtra("sessionId") // 필요시 MainActivity에서 전달

        if (authToken.isNullOrBlank()) {
            showMsg(getString(R.string.login_info_not_valid))
            finish()
            return
        }

        observeReviewViewModel() // 단어 로드 ViewModel 관찰
        observeStudyViewModel()  // 퀴즈 진행 ViewModel 관찰

        // "오늘의 누적 복습" 단어 목록 로드
        fetchStagedReviewWords()

        closeQuizButton.setOnClickListener { finish() }
    }

    // UI 요소 참조 초기화 (activity_quiz.xml 기준)
    private fun initUi() {
        quizWordText = findViewById(R.id.text_quiz_word)
        quizProgressText = findViewById(R.id.text_quiz_progress)
        val choice1: Button = findViewById(R.id.button_choice1)
        val choice2: Button = findViewById(R.id.button_choice2)
        val choice3: Button = findViewById(R.id.button_choice3)
        val choice4: Button = findViewById(R.id.button_choice4)
        optionButtons = listOf(choice1, choice2, choice3, choice4)
        closeQuizButton = findViewById(R.id.button_close)
        feedbackImage = findViewById(R.id.image_feedback_quiz)
        quizLoadingBar = findViewById(R.id.progress_bar_quiz_loading)
    }

    // "오늘의 누적 복습" 단어 목록 로드 요청
    private fun fetchStagedReviewWords() { // 함수명 변경
        setLoadingState(true)
        authToken?.let { reviewViewModel.loadStagedReviewWords(it) }
    }

    // 단어 목록 로드 결과 관찰 (ReviewViewModel)
    private fun observeReviewViewModel() {
        reviewViewModel.reviewableWords.observe(this) { words: List<Word>? ->
            setLoadingState(false)
            if (!words.isNullOrEmpty()) {
                studyViewModel.startQuiz(words) // StudyViewModel의 startQuiz 호출
            }
        }

        reviewViewModel.errorMessage.observe(this) { errorMessage: String? ->
            errorMessage?.let {
                setLoadingState(false)
                showMsg(it)
                reviewViewModel.consumeErrorMessage()
                if (reviewViewModel.reviewableWords.value.isNullOrEmpty()) {
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
                }
            }
        }
    }

    // 퀴즈 진행 상태 관찰 (StudyViewModel - 사용자 제공 버전 기준)
    private fun observeStudyViewModel() {
        studyViewModel.currentQuestion.observe(this) { quizQuestion: QuizQuestion? ->
            renderQuizQuestion(quizQuestion)
        }

        studyViewModel.progress.observe(this) { progress: Pair<Int, Int>? ->
            progress?.let { quizProgressText.text = "${it.first} / ${it.second}" }
        }

        studyViewModel.isQuizFinished.observe(this) { isFinished: Boolean? ->
            if (isFinished == true && studyViewModel.currentQuestion.value == null) {
                val results = studyViewModel.generateQuizResults()
                if (results.isNotEmpty()) {
                    reportStagedQuizResults(results) // 누적 복습 결과 전송 함수 호출
                } else {
                    showMsg(getString(R.string.quiz_no_results_to_report))
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }

        studyViewModel.quizMessage.observe(this) { message: String? ->
            message?.let {
                showMsg(it)
                studyViewModel.consumeQuizMessage()
                if (studyViewModel.isQuizFinished.value == true) {
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
                }
            }
        }
    }

    // 현재 퀴즈 문제로 UI 업데이트 및 선택지 리스너 설정
    private fun renderQuizQuestion(quizQuestion: QuizQuestion?) {
        if (quizQuestion == null) {
            quizWordText.text = getString(R.string.quiz_completed_message)
            optionButtons.forEach { it.visibility = View.INVISIBLE }
            return
        }

        quizWordText.text = quizQuestion.word.word
        optionButtons.forEachIndexed { index, button ->
            button.visibility = View.VISIBLE
            button.text = quizQuestion.options.getOrNull(index) ?: ""
            button.isEnabled = true
            button.setOnClickListener {
                optionButtons.forEach { btn -> btn.isEnabled = false }
                val isCorrect = index == quizQuestion.correctIndex
                showFeedbackImage(isCorrect)
                Handler(Looper.getMainLooper()).postDelayed({
                    studyViewModel.nextQuestion(isCorrect) // StudyViewModel의 nextQuestion 호출
                }, feedbackDurationMs)
            }
        }
    }

    // 정답/오답 이미지 피드백 표시
    private fun showFeedbackImage(isCorrect: Boolean) {
        feedbackImage.setImageResource(
            if (isCorrect) R.drawable.blue_ring else R.drawable.red_x
        )
        feedbackImage.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            feedbackImage.visibility = View.GONE
        }, feedbackDurationMs)
    }

    // "오늘의 누적 복습" 퀴즈 결과를 서버에 전송
    private fun reportStagedQuizResults(results: List<ReviewResultItem>) { // 함수명 변경
        if (authToken.isNullOrBlank()) {
            showMsg(getString(R.string.quiz_report_token_missing))
            finish()
            return
        }
        setLoadingState(true)

        val request = StagedReviewResultRequest(sessionId = quizSessionId, results = results)

        lifecycleScope.launch {
            try {
                // "오늘의 누적 복습" 결과는 ReviewApiService의 sendReviewResults API 사용
                val response = ApiServicePool.reviewApi.sendReviewResults("Bearer $authToken", request)
                setLoadingState(false)

                if (response.success == true) {
                    showMsg(getString(R.string.quiz_staged_report_success)) // 성공 메시지 변경
                    setResult(Activity.RESULT_OK)
                } else {
                    showMsg(response.message ?: getString(R.string.quiz_report_failed_default))
                    setResult(Activity.RESULT_CANCELED)
                }
            } catch (e: Exception) {
                setLoadingState(false)
                showMsg(getString(R.string.quiz_report_network_error, e.localizedMessage ?: "알 수 없는 오류"))
                setResult(Activity.RESULT_CANCELED)
            } finally {
                finish()
            }
        }
    }

    // 로딩 인디케이터(ProgressBar) 표시/숨김
    private fun setLoadingState(isLoading: Boolean) {
        quizLoadingBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    // Snackbar 메시지 표시 유틸리티
    private fun showMsg(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(rootQuizView, message, duration).show()
    }
}