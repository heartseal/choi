package com.example.englishapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.englishapp.manager.QuizManager
import com.example.englishapp.model.QuizQuestion
import com.example.englishapp.model.Word
import com.example.englishapp.network.ReviewResultItem

// 단어 퀴즈 세션 진행 관리 (ReviewActivity에서 사용하는 이름 기준)
class StudyViewModel : ViewModel() {

    private var quizManager: QuizManager? = null
    private var originalQuizWords: List<Word> = emptyList() // 현재 퀴즈 세션의 원본 단어 목록

    // LiveData: 현재 화면에 표시될 퀴즈 문제 객체
    private val _currentQuestion = MutableLiveData<QuizQuestion?>()
    val currentQuestion: LiveData<QuizQuestion?> = _currentQuestion

    // LiveData: 퀴즈 진행률 (현재 문제 번호 / 전체 문제 수)
    private val _progress = MutableLiveData<Pair<Int, Int>>()
    val progress: LiveData<Pair<Int, Int>> = _progress

    // LiveData: 현재 퀴즈 세션이 완전히 종료되었는지 여부
    private val _isQuizFinished = MutableLiveData<Boolean>(false)
    val isQuizFinished: LiveData<Boolean> = _isQuizFinished

    // LiveData: 퀴즈 진행 중 사용자에게 알릴 메시지
    private val _quizMessage = MutableLiveData<String?>()
    val quizMessage: LiveData<String?> = _quizMessage

    // 퀴즈 세션 시작 (Activity로부터 퀴즈 대상 단어 목록을 받음)
    fun startQuiz(wordsForQuiz: List<Word>) {
        if (wordsForQuiz.isEmpty()) {
            _quizMessage.value = "퀴즈를 진행할 단어가 없습니다." // 문자열 리소스 사용 권장
            _currentQuestion.value = null
            _progress.value = 0 to 0
            _isQuizFinished.value = true // 단어가 없으면 즉시 종료 상태
            return
        }

        this.originalQuizWords = wordsForQuiz // 원본 단어 목록 저장
        quizManager = QuizManager(wordsForQuiz) // 새 QuizManager 인스턴스 생성
        _isQuizFinished.value = false          // 퀴즈 시작 시 완료 상태 초기화
        _quizMessage.value = null              // 이전 메시지 초기화
        generateNextQuestion()                 // 첫 문제 로드 (내부 함수 호출)
        updateProgress()                       // 진행률 업데이트 (내부 함수 호출)
    }

    // 사용자가 선택한 답변을 처리하고, 다음 문제가 있으면 로드
    fun nextQuestion(isCorrect: Boolean) { // ReviewActivity에서 사용하는 함수명
        quizManager?.handleAnswer(isCorrect) // QuizManager에 정답 여부 전달
        generateNextQuestion()               // 다음 문제 로드 (내부 함수 호출)
        updateProgress()                     // 진행률 업데이트 (내부 함수 호출)
    }

    // 다음 퀴즈 문제를 QuizManager로부터 가져와 LiveData에 설정 (내부 private 함수)
    private fun generateNextQuestion() {
        val nextQuestion = quizManager?.generateQuestion()
        _currentQuestion.value = nextQuestion

        // 더 이상 생성할 문제가 없으면 (퀴즈의 끝에 도달하면) 세션 종료 상태로 설정
        if (nextQuestion == null && quizManager != null) {
            _isQuizFinished.value = true
        }
    }

    // 현재 퀴즈 진행 상태(완료된 문제 수 / 전체 문제 수)를 업데이트 (내부 private 함수)
    private fun updateProgress() {
        quizManager?.let { manager ->
            _progress.value = manager.getCompletedCount() to manager.getTotalCount()
        }
    }

    // 현재 퀴즈 세션의 결과를 서버 보고용 DTO 리스트로 생성
    fun generateQuizResults(): List<ReviewResultItem> { // ReviewActivity에서 사용하는 함수명
        val manager = quizManager
        // QuizManager가 없거나, 원본 단어 목록이 비어있으면 빈 결과 반환
        if (manager == null || originalQuizWords.isEmpty()) {
            return emptyList()
        }

        // 원본 단어 목록(originalQuizWords)을 기준으로 각 단어의 정답 여부 확인
        return originalQuizWords.map { word ->
            ReviewResultItem(
                wordId = word.id,
                // getCurrentAttempts는 해당 단어에 대해 정답을 맞힌 횟수를 반환
                isCorrect = manager.getCurrentAttempts(word.id) >= 1
            )
        }
    }

    // UI에서 사용자 메시지를 확인한 후 호출하여 LiveData 초기화
    fun consumeQuizMessage() { // ReviewActivity에서 사용하는 함수명
        _quizMessage.value = null
    }
}