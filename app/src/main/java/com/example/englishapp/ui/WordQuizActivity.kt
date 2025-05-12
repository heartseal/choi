package com.example.englishapp.ui

import android.app.Activity
import android.content.Intent
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
import com.example.englishapp.R
import com.example.englishapp.model.QuizQuestion
import com.example.englishapp.model.Word // Word 모델 import
import com.example.englishapp.network.ApiServicePool
import com.example.englishapp.network.ReviewResultItem
import com.example.englishapp.network.StagedReviewResultRequest
import com.example.englishapp.viewmodel.ReviewViewModel
import com.example.englishapp.viewmodel.StudyViewModel // 사용자님의 StudyViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

// 단어 퀴즈 화면 (10분 후 복습 또는 누적 복습)
class WordQuizActivity : AppCompatActivity() {

    // ViewModels
    private val reviewViewModel: ReviewViewModel by viewModels()
    private val studyViewModel: StudyViewModel by viewModels() // 사용자님의 StudyViewModel 사용

    // UI 요소
    private lateinit var quizWordText: TextView
    private lateinit var quizProgressText: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var closeQuizButton: ImageButton
    private lateinit var feedbackImage: ImageView
    private lateinit var quizLoadingBar: ProgressBar
    private lateinit var rootQuizView: View

    // 상태 변수
    private var authToken: String? = null
    private var quizSessionId: String? = null
    private val feedbackDurationMs = 700L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_quiz)

        rootQuizView = findViewById(android.R.id.content)
        initUi()

        authToken = intent.getStringExtra("token")
        // quizSessionId = intent.getStringExtra("sessionId")

        if (authToken.isNullOrBlank()) {
            showMsg(getString(R.string.login_info_not_valid))
            finish()
            return
        }

        observeReviewViewModel() // ReviewViewModel 관찰
        observeStudyViewModel()  // StudyViewModel 관찰

        // "10분 후 복습" 단어 목록 로드 (이 Activity가 해당 역할을 한다고 가정)
        fetchPostLearningReviewWords()

        closeQuizButton.setOnClickListener { finish() }
    }

    // UI 요소 참조 초기화
    private fun initUi() {
        quizWordText = findViewById(R.id.text_quiz_word)
        quizProgressText = findViewById(R.id.text_quiz_progress)
        val choice1: Button = findViewById(R.id.button_choice1)
        val choice2: Button = findViewById(R.id.button_choice2)
        val choice3: Button = findViewById(R.id.button_choice3)
        val choice4: Button = findViewById(R.id.button_choice4)
        optionButtons = listOf(choice1, choice2, choice3, choice4)
        closeQuizButton = findViewById(R.id.button_close)
        feedbackImage = findViewById(R.id.image_feedback_quiz) // XML에 image_feedback_quiz ID 필요
        quizLoadingBar = findViewById(R.id.progress_bar_quiz_loading) // XML에 progress_bar_quiz_loading ID 필요
    }

    // "10분 후 복습" 단어 목록 로드 요청
    private fun fetchPostLearningReviewWords() {
        setLoadingState(true)
        authToken?.let { reviewViewModel.loadPostLearningReviewWords(it) }
    }

    // 단어 목록 로드 결과 관찰 (ReviewViewModel)
    private fun observeReviewViewModel() {
        reviewViewModel.reviewableWords.observe(this) { words: List<Word>? -> // 타입 명시
            setLoadingState(false)
            if (!words.isNullOrEmpty()) {
                studyViewModel.startQuiz(words) // StudyViewModel의 startQuiz 호출
            }
        }

        reviewViewModel.errorMessage.observe(this) { errorMessage: String? -> // 타입 명시
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

    // 퀴즈 진행 상태 관찰 (StudyViewModel - 사용자님이 제공한 버전의 멤버 이름 사용)
    private fun observeStudyViewModel() {
        // currentQuestion LiveData 관찰
        studyViewModel.currentQuestion.observe(this) { quizQuestion: QuizQuestion? -> // 타입 명시
            renderQuizQuestion(quizQuestion)
        }

        // progress LiveData 관찰
        studyViewModel.progress.observe(this) { progress: Pair<Int, Int>? -> // 타입 명시
            progress?.let { quizProgressText.text = "${it.first} / ${it.second}" }
        }

        // isQuizFinished LiveData 관찰
        studyViewModel.isQuizFinished.observe(this) { isFinished: Boolean? -> // 타입 명시
            if (isFinished == true && studyViewModel.currentQuestion.value == null) {
                // generateQuizResults 함수 호출
                val results = studyViewModel.generateQuizResults()
                if (results.isNotEmpty()) {
                    reportQuizResults(results)
                } else {
                    showMsg(getString(R.string.quiz_no_results_to_report))
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }

        // quizMessage LiveData 관찰
        studyViewModel.quizMessage.observe(this) { message: String? -> // 타입 명시
            message?.let {
                showMsg(it)
                studyViewModel.consumeQuizMessage() // consumeQuizMessage 함수 호출
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
                    // nextQuestion 함수 호출 (사용자 제공 StudyViewModel의 이름)
                    studyViewModel.nextQuestion(isCorrect)
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

    // 퀴즈 결과를 서버에 전송
    private fun reportQuizResults(results: List<ReviewResultItem>) {
        if (authToken.isNullOrBlank()) {
            showMsg(getString(R.string.quiz_report_token_missing))
            finish()
            return
        }
        setLoadingState(true)

        val request = StagedReviewResultRequest(sessionId = quizSessionId, results = results)

        lifecycleScope.launch {
            try {
                // "10분 후 복습" 결과 전송 API
                val response = ApiServicePool.reviewApi.sendPostLearningResults("Bearer $authToken", request)
                setLoadingState(false)

                if (response.success == true) {
                    showMsg(getString(R.string.quiz_post_learning_report_success))
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