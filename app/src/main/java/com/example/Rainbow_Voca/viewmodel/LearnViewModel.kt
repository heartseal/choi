package com.example.Rainbow_Voca.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Rainbow_Voca.manager.StudyManager
import com.example.Rainbow_Voca.model.Word
import com.example.Rainbow_Voca.network.common.WordListResponse
import com.example.Rainbow_Voca.network.ApiServicePool
import com.example.Rainbow_Voca.network.TodayCompleteRequest
import kotlinx.coroutines.launch

// 오늘의 학습 로직 및 데이터 관리
class LearnViewModel : ViewModel() {

    private var studyManager: StudyManager? = null // 학습 세션 관리자

    // LiveData: 현재 화면에 표시될 학습 단어
    private val _currentWord = MutableLiveData<Word?>()
    val currentWord: LiveData<Word?> = _currentWord

    // LiveData: 현재 학습 세션의 완료 여부
    private val _isStudyFinished = MutableLiveData(false) // 초기값 false
    val isStudyFinished: LiveData<Boolean> = _isStudyFinished

    // LiveData: 학습 완료 보고의 성공/실패 결과
    private val _studyReportResult = MutableLiveData<Boolean?>()
    val studyReportResult: LiveData<Boolean?> = _studyReportResult

    // LiveData: 오류 발생 시 사용자에게 표시할 메시지
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 서버에서 오늘의 학습 단어 목록을 가져와 학습 세션을 준비
    fun loadTodayWords(token: String, dailyGoal: Int) { // 함수명 변경: fetchTodayWords -> loadTodayWords
        viewModelScope.launch {
            try {
                val response: WordListResponse = ApiServicePool.learnApi.getTodayWords("Bearer $token")
                if (response.words != null && response.words.isNotEmpty()) {
                    // 단어 목록과 학습 목표량을 전달하여 학습 세션 시작
                    startStudySession(response.words, dailyGoal)
                } else {
                    _errorMessage.value = response.message ?: "오늘 학습할 단어가 없습니다."
                    _isStudyFinished.value = true // 학습할 단어가 없으므로 바로 완료 처리
                }
            } catch (e: Exception) {
                _errorMessage.value = "단어 목록을 가져오는 중 오류가 발생했습니다: ${e.message}"
                _isStudyFinished.value = true // 오류 발생 시에도 우선 완료 처리 (재시도 로직 등 추가 가능)
            }
        }
    }

    // StudyManager를 초기화하고 첫 단어를 로드하여 학습 세션 시작
    private fun startStudySession(words: List<Word>, dailyGoal: Int) {
        if (words.isEmpty()) { // words가 비어있으면 학습 불가
            _currentWord.value = null
            _isStudyFinished.value = true
            _errorMessage.value = "학습을 시작할 단어가 없습니다."
            return
        }
        studyManager = StudyManager(words, dailyGoal)
        _currentWord.value = studyManager?.getCurrentWord()
        // studyManager 생성 후 즉시 완료 여부 확인
        _isStudyFinished.value = studyManager?.isStudyFinished() ?: true
    }

    // 다음 학습 단어로 이동 요청
    fun requestNextWord() {
        studyManager?.let { manager ->
            if (!manager.isStudyFinished()) { // 현재 세션이 끝나지 않았을 경우에만 다음 단어로 이동
                manager.moveToNext() // StudyManager 내부적으로 다음 단어로 이동 시도
                _currentWord.value = if (!manager.isStudyFinished()) manager.getCurrentWord() else null
            }
            // 다음 단어로 이동 후 세션 완료 여부 업데이트
            if (manager.isStudyFinished()) {
                _isStudyFinished.value = true
            }
        }
    }

    // 오늘의 학습 결과를 서버에 보고
    fun reportStudyCompletion(token: String) { // 함수명 변경: reportTodayComplete -> reportStudyCompletion
        val learnedWords = studyManager?.getStudyList()

        if (learnedWords.isNullOrEmpty()) {
            _studyReportResult.value = false // 보고할 학습 내용 없음
            _errorMessage.value = "보고할 학습 내용이 없습니다." // 이 메시지는 UI에 표시될 수 있음
            return
        }

        val completedWordIds = learnedWords.map { it.id }
        if (completedWordIds.isEmpty()) {
            _studyReportResult.value = true
            return
        }

        viewModelScope.launch {
            try {
                val request = TodayCompleteRequest(completedWordIds)
                val response = ApiServicePool.learnApi.completeTodaySession("Bearer $token", request)
                _studyReportResult.value = response.success
                if (response.success != true) {
                    _errorMessage.value = response.message ?: "학습 결과 보고에 실패했습니다."
                }
            } catch (e: Exception) {
                _studyReportResult.value = false
                _errorMessage.value = "학습 결과 보고 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    // 학습 세션에서 실제로 학습한 단어 목록 반환
    fun getActualLearnedWords(): List<Word> {
        return studyManager?.getStudyList() ?: emptyList()
    }

    // 오류 메시지 LiveData 초기화
    fun consumeErrorMessage() { // 함수명 추가
        _errorMessage.value = null
    }
}