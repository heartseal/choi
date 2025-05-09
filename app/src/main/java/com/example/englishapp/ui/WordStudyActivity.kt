package com.example.englishapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.englishapp.viewmodel.LearnViewModel
import com.example.englishapp.R
import com.example.englishapp.manager.QuizWordRepository

class WordStudyActivity : AppCompatActivity() {

    private lateinit var textEnglish: TextView
    private lateinit var textKorean: TextView
    private lateinit var buttonNext: Button
    private lateinit var buttonClose: ImageButton

    private val viewModel: LearnViewModel by viewModels()
    private lateinit var token: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_study)

        textEnglish = findViewById(R.id.text_english_word)
        textKorean = findViewById(R.id.text_korean_meaning)
        buttonNext = findViewById(R.id.button_next)
        buttonClose = findViewById(R.id.button_close)

        token = intent.getStringExtra("token") ?: run {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.fetchTodayWords(token)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.studyList.observe(this) { words ->
            if (words.isNullOrEmpty()) {
                Toast.makeText(this, "학습할 단어가 없습니다.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                viewModel.startStudy(words)
            }
        }

        viewModel.currentWord.observe(this) { word ->
            word?.let {
                textEnglish.text = it.word
                textKorean.text = it.meaning
            } ?: run {
                textEnglish.text = ""
                textKorean.text = ""
            }
        }

        viewModel.isFinished.observe(this) { finished ->
            if (finished == true) {
                viewModel.reportTodayComplete(token)
            }
        }

        viewModel.completeResult.observe(this) { success ->
            if (success == true) {
                // 학습 완료 시에 저장
                QuizWordRepository.quizWords = viewModel.getStudyList()
                Toast.makeText(this, "학습 완료!", Toast.LENGTH_SHORT).show()
                // 결과 전달 및 액티비티 종료
                val resultIntent = Intent().apply {
                    putExtra("learningFinished", true)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else if (success == false) {
                Toast.makeText(this, "서버 보고 실패", Toast.LENGTH_SHORT).show()
            }
        }


        buttonNext.setOnClickListener { viewModel.nextWord() }
        buttonClose.setOnClickListener { finish() }

    }
}
