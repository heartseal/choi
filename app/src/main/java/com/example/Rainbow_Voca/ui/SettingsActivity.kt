package com.example.Rainbow_Voca.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.Rainbow_Voca.R

/*
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        val buttons = listOf(
            Pair(findViewById<Button>(R.id.btn_10), 10),
            Pair(findViewById<Button>(R.id.btn_20), 20),
            Pair(findViewById<Button>(R.id.btn_30), 30),
            Pair(findViewById<Button>(R.id.btn_40), 40),
            Pair(findViewById<Button>(R.id.btn_50), 50)
        )

        for ((button, value) in buttons) {
            button.setOnClickListener {
                prefs.edit().putInt("daily_word_goal", value).apply()
                Toast.makeText(this, "${value}개로 설정되었습니다", Toast.LENGTH_SHORT).show()
                finish() // 설정 완료 후 화면 종료
            }
        }
    }
}
*/
