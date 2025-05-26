package com.example.Rainbow_Voca.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.Rainbow_Voca.model.Word
//import com.example.Rainbow_Voca.util.AllWordsAdapter
import com.example.Rainbow_Voca.R

/*

class AllWordsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllWordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_words)

        recyclerView = findViewById(R.id.recyclerView_all_words)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 예시 데이터 - 나중에 Room DB에서 가져오게 바꾸면 됨
        val wordList = listOf(
            Word('01', "사과"),
            Word('02', "바나나"),
            Word('03', "당근")
        )

        adapter = AllWordsAdapter(wordList)
        recyclerView.adapter = adapter

        findViewById<ImageButton>(R.id.btn_close).setOnClickListener {
            finish()
        }
    }
}
*/
