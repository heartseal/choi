package com.example.englishapp.ui

import android.os.Build
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
import com.example.englishapp.model.Word
import com.example.englishapp.network.ApiServicePool
import com.example.englishapp.network.ReviewResultItem
import com.example.englishapp.network.StagedReviewResultRequest
import com.example.englishapp.viewmodel.StudyViewModel
import kotlinx.coroutines.launch

class WordQuizActivity : AppCompatActivity() {

    private lateinit var textQuestion: TextView
    private lateinit var textProgress: TextView
    private lateinit var optionButtons: List<Button>
    private lateinit var buttonClose: ImageButton

    private var quizQuestions: List<QuizQuestion> = emptyList()
    private var currentIndex = 0

    // 결과 리스트 (모든 단어: 맞았는지 여부 포함)
    private val reviewResults = mutableListOf<ReviewResultItem>()

    private lateinit var token: String
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_quiz)

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

        // 토큰, 세션ID 등 받기 (필요시)
        token = intent.getStringExtra("token") ?: ""
        sessionId = intent.getStringExtra("sessionId")

        // 단어 리스트 수신
        val wordList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("quizWords", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Word>("quizWords")
        } ?: emptyList()

        if (wordList.isEmpty()) {
            Toast.makeText(this, "퀴즈 단어가 없습니다!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 퀴즈 문제 생성 (한 번씩만 출제)
        quizQuestions = wordList.map { word ->
            val wrongOptions = (wordList - word).shuffled().take(3).map { it.meaning }
            val options = (wrongOptions + word.meaning).shuffled()
            val correctIndex = options.indexOf(word.meaning)
            QuizQuestion(word, options, correctIndex)
        }
        showCurrentQuestion()
    }

    private fun showCurrentQuestion() {
        if (currentIndex >= quizQuestions.size) {
            // 퀴즈 끝! 결과 전송
            sendReviewResultsToServer()
            return
        }
        val question = quizQuestions[currentIndex]
        textQuestion.text = question.word.word
        optionButtons.forEachIndexed { idx, btn ->
            btn.text = question.options[idx]
            btn.isEnabled = true // 새로운 문제마다 버튼 활성화
        }
        textProgress.text = "${currentIndex + 1}/${quizQuestions.size}"
        setupOptionClickListeners(question)
    }

    private fun setupOptionClickListeners(question: QuizQuestion) {
        optionButtons.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                // 모든 버튼 비활성화 (한 번만 시도)
                optionButtons.forEach { it.isEnabled = false }

                val isCorrect = idx == question.correctIndex
                if (isCorrect) {
                    Toast.makeText(this, "정답!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "오답!", Toast.LENGTH_SHORT).show()
                }
                // 결과 저장 (맞았는지 여부 포함)
                reviewResults.add(
                    ReviewResultItem(
                        wordId = question.word.id,
                        isCorrect = isCorrect
                    )
                )
                // 딜레이 없이 바로 다음 문제로 이동
                currentIndex++
                showCurrentQuestion()
            }
        }
    }

    // 10분 후 복습 결과를 서버에 전송하기
    private fun sendReviewResultsToServer() {
        // 실제 서버 전송 코드 주석 처리
        /*
        val request = StagedReviewResultRequest(
            sessionId = sessionId,
            results = reviewResults
        )

        lifecycleScope.launch {
            try {
                val response = ApiServicePool.reviewApi.sendPostLearningResults(
                    "Bearer $token",
                    request
                )

                if (response.success == true) {
                    setResult(RESULT_OK)
                    Toast.makeText(
                        this@WordQuizActivity,
                        "10분 후 복습 결과 전송 완료!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val errorMsg = response.message ?: "서버 오류"
                    Toast.makeText(
                        this@WordQuizActivity,
                        "전송 실패: $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WordQuizActivity,
                    "네트워크 오류: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                finish()
            }
        }
        */

        // 임시 완료 처리
        setResult(RESULT_OK)
        Toast.makeText(
            this@WordQuizActivity,
            "10분 후 복습 결과 전송 완료!",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

}

