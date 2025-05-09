package com.example.englishapp.viewmodel

// quiz activity랑 연동 10분 후 복습

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.englishapp.manager.QuizManager
import com.example.englishapp.manager.QuizWordRepository
import com.example.englishapp.model.QuizQuestion
import com.example.englishapp.model.Word
import com.example.englishapp.network.ReviewResultItem

class StudyViewModel : ViewModel() {
    private lateinit var quizManager: QuizManager
    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> = _currentQuestion

    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>> = _progress

    fun startQuiz(words: List<Word>) {
        quizManager = QuizManager(words)
        updateProgress()
        generateNextQuestion()
    }

    fun nextQuestion(isCorrect: Boolean) {
        quizManager.handleAnswer(isCorrect)
        generateNextQuestion()
        updateProgress()
    }

    private fun generateNextQuestion() {
        _currentQuestion.value = quizManager.generateQuestion()
    }

    private fun updateProgress() {
        _progress.value = quizManager.getCompletedCount() to quizManager.getTotalCount()
    }

    // ✅ QuizWordRepository에서 단어 리스트를 가져와 결과 생성
    fun getReviewResults(): List<ReviewResultItem> {
        return QuizWordRepository.quizWords.map { word ->
            ReviewResultItem(
                wordId = word.id,
                isCorrect = quizManager.getCurrentAttempts(word.id) >= 1
            )
        }
    }
}
