package com.example.Rainbow_Voca.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.Rainbow_Voca.R
import com.example.Rainbow_Voca.manager.QuizWordRepository // 학습 완료 단어 전달용
import com.example.Rainbow_Voca.viewmodel.LearnViewModel

// "오늘의 학습" 진행 화면
class WordStudyActivity : AppCompatActivity() {

    // UI 요소 참조 변수
    private lateinit var textEnglishWord: TextView // XML ID: text_english_word
    private lateinit var textKoreanMeaning: TextView // XML ID: text_korean_meaning
    private lateinit var buttonNextWord: Button      // XML ID: button_next
    private lateinit var buttonCloseStudy: ImageButton // XML ID: button_close

    private val learnViewModel: LearnViewModel by viewModels() // 이름 명확화: viewModel -> learnViewModel
    private lateinit var currentToken: String // MainActivity에서 전달받는 토큰
    private var currentDailyGoal: Int = 10    // MainActivity에서 전달받는 학습 목표량

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // 액션바 숨김
        setContentView(R.layout.activity_study) // activity_study.xml 사용

        initializeUiReferences()

        // Intent로부터 토큰 및 학습 목표량 가져오기
        currentToken = intent.getStringExtra("token") ?: run {
            showToast("로그인 정보가 유효하지 않습니다.")
            finish() // 토큰 없으면 Activity 종료
            return
        }
        currentDailyGoal = intent.getIntExtra("dailyGoal", 10) // 기본값 10

        // ViewModel 데이터 로드 요청 및 LiveData 관찰 설정
        learnViewModel.loadTodayWords(currentToken, currentDailyGoal)
        observeViewModelData()

        // 버튼 클릭 리스너 설정
        buttonNextWord.setOnClickListener { learnViewModel.requestNextWord() }
        buttonCloseStudy.setOnClickListener { finish() } // 단순 닫기 기능
    }

    // XML 레이아웃의 UI 요소 참조 초기화
    private fun initializeUiReferences() {
        textEnglishWord = findViewById(R.id.text_english_word)
        textKoreanMeaning = findViewById(R.id.text_korean_meaning)
        buttonNextWord = findViewById(R.id.button_next)
        buttonCloseStudy = findViewById(R.id.button_close)
    }

    // ViewModel의 LiveData 변경 사항 관찰 및 UI 업데이트
    private fun observeViewModelData() {
        // 현재 학습 단어 변경 시 UI 업데이트
        learnViewModel.currentWord.observe(this) { word ->
            if (word != null) {
                textEnglishWord.text = word.word
                textKoreanMeaning.text = word.meaning
                buttonNextWord.isEnabled = true // 다음 단어가 있으면 '다음' 버튼 활성화
            } else {
                // 모든 단어 학습 완료 후 currentWord가 null이 되면 (isStudyFinished도 true가 됨)
                // "학습 완료!" 등의 메시지를 표시하거나 버튼 상태 변경 가능
                // textEnglishWord.text = getString(R.string.study_all_words_viewed)
                // textKoreanMeaning.text = ""
                // buttonNextWord.isEnabled = false // 마지막 단어 후 '다음' 버튼 비활성화
            }
        }

        // 학습 세션 완료 여부 관찰
        learnViewModel.isStudyFinished.observe(this) { isFinished ->
            if (isFinished) {
                // '다음' 버튼 비활성화 (더 이상 넘길 단어 없음)
                buttonNextWord.isEnabled = false
                // 학습이 완료되면 자동으로 서버에 보고 (최초 1회)
                if (learnViewModel.studyReportResult.value == null) { // 아직 보고 전일 때만
                    learnViewModel.reportStudyCompletion(currentToken)
                }
            }
        }

        // 서버 보고 결과 관찰
        learnViewModel.studyReportResult.observe(this) { success ->
            if (success != null) { // 보고 시도 후 결과가 있을 때만 처리
                if (success) {
                    // 보고 성공 시: 10분 후 복습 퀴즈용 단어 저장 및 MainActivity로 결과 전달 후 종료
                    QuizWordRepository.quizWords = learnViewModel.getActualLearnedWords()
                    showToast(getString(R.string.study_report_success_message)) // "학습 완료! 서버에 보고되었습니다."
                    val resultIntent = Intent().apply {
                        putExtra("learningFinished", true) // MainActivity에 학습 완료 알림
                    }
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish() // 현재 학습 Activity 종료
                } else {
                    // 보고 실패 시: 사용자에게 알림
                    showToast(learnViewModel.errorMessage.value ?: getString(R.string.study_report_failed_message)) // "서버 보고에 실패했습니다."
                    // 실패 시에도 Activity를 종료할지, 재시도 옵션을 줄지 정책 결정 필요
                    // 현재는 Toast만 보여주고 Activity 유지
                    // setResult(Activity.RESULT_CANCELED)
                    // finish()
                }
            }
        }

        // 기타 오류 메시지 관찰 (예: 단어 로딩 실패)
        learnViewModel.errorMessage.observe(this) { errorMessage ->
            // studyReportResult가 false일 때의 메시지와 중복을 피하기 위한 조건
            if (learnViewModel.studyReportResult.value != false && errorMessage != null) {
                showToast(errorMessage)
                learnViewModel.consumeErrorMessage() // 오류 메시지 소비
            }
        }
    }

    // 간단한 Toast 메시지 표시 유틸리티
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}