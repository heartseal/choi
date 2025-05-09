package com.example.englishapp.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.englishapp.model.StudentAdapter
import com.example.englishapp.model.Student
import com.example.englishapp.R

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // ⭕ 관리자 이미지 불러오기
        val imageView = findViewById<ImageView>(R.id.admin_image)
        Glide.with(this)
            .load(R.drawable.admin_profile)  // drawable에 있는 이미지 사용
            .circleCrop()                    // 동그랗게 잘라줌
            .into(imageView)

        // ❌ X 버튼 클릭하면 종료
        val closeBtn = findViewById<ImageButton>(R.id.button_close_admin)
        closeBtn.setOnClickListener {
            finish()
        }
        // 🔵 1. RecyclerView 연결
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_student_list)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 🧑‍🎓 2. 더미 학생 리스트 만들기
        val studentList = listOf(
            Student("김태우", R.drawable.profile1, true, 200, 1000),
            Student("이지은", R.drawable.profile2, false, 700, 1000),
            Student("홍길동", R.drawable.profile3, true, 500, 1000)
        )

        // 📦 3. 어댑터에 연결
        val adapter = StudentAdapter(studentList)
        recyclerView.adapter = adapter

    }
}
