package com.example.englishapp.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.englishapp.R
import com.example.englishapp.model.QuizQuestion
import com.example.englishapp.network.ApiServicePool
import com.example.englishapp.network.ReviewResultItem
import com.example.englishapp.network.StagedReviewResultRequest
import com.example.englishapp.viewmodel.ReviewViewModel
import kotlinx.coroutines.launch


class ReviewActivity : AppCompatActivity() {

    private lateinit var textQuestion: TextView
    private lateinit var textProgress: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var buttonClose: ImageButton

    private var quizQuestions: List<QuizQuestion> = emptyList()
    private var currentIndex = 0
    private val reviewResults = mutableListOf<ReviewResultItem>()

    private lateinit var token: String
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_quiz)  // 기존 퀴즈 레이아웃 재사용

        buttonClose = findViewById(R.id.button_close)
        textProgress = findViewById(R.id.text_quiz_progress)
        textQuestion = findViewById(R.id.text_quiz_word)
        optionButtons = listOf(
            findViewById(R.id.button_choice1),
            findViewById(R.id.button_choice2),
            findViewById(R.id.button_choice3),
            findViewById(R.id.button_choice4)
        )

        buttonClose.setOnClickListener { finish() }

        token = intent.getStringExtra("token") ?: ""
        sessionId = intent.getStringExtra("sessionId")

        val viewModel: ReviewViewModel by viewModels()
        viewModel.postLearningWords.observe(this) { wordList ->
            if (wordList.isEmpty()) {
                Toast.makeText(this, "복습할 단어가 없습니다!", Toast.LENGTH_SHORT).show()
                finish()
                return@observe
            }

            // 백엔드에서 받은 전체 리스트로 문제 생성
            quizQuestions = wordList.map { word ->
                val wrongOptions = (wordList - word).shuffled().take(3).map { it.meaning }
                val options = (wrongOptions + word.meaning).shuffled()
                val correctIndex = options.indexOf(word.meaning)
                QuizQuestion(word, options, correctIndex)
            }
            currentIndex = 0
            showCurrentQuestion()
        }

        viewModel.errorMessage.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        viewModel.fetchPostLearningWords(token)
    }

    private fun showCurrentQuestion() {
        if (currentIndex >= quizQuestions.size) {
            sendReviewResultsToServer()
            return
        }
        val question = quizQuestions[currentIndex]
        textQuestion.text = question.word.word
        optionButtons.forEachIndexed { idx, btn ->
            btn.text = question.options[idx]
            btn.isEnabled = true
        }
        textProgress.text = "${currentIndex + 1}/${quizQuestions.size}"
        setupOptionClickListeners(question)
    }

    private fun setupOptionClickListeners(question: QuizQuestion) {
        optionButtons.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                optionButtons.forEach { it.isEnabled = false }
                val isCorrect = idx == question.correctIndex
                if (isCorrect) {
                    Toast.makeText(this, "정답!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "오답!", Toast.LENGTH_SHORT).show()
                }
                reviewResults.add(ReviewResultItem(question.word.id, isCorrect))
                currentIndex++
                showCurrentQuestion()
            }
        }
    }

    private fun sendReviewResultsToServer() {
        val request = StagedReviewResultRequest(sessionId, reviewResults)
        lifecycleScope.launch {
            try {
                val response = ApiServicePool.reviewApi.sendPostLearningResults("Bearer $token", request)
                if (response.success ?: false) {
                    Toast.makeText(this@ReviewActivity, "복습 결과 전송 완료!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ReviewActivity, "복습 결과 전송 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReviewActivity, "네트워크 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }
}
