package com.example.englishapp.model


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.englishapp.R

class StudentAdapter(private val studentList: List<Student>) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProfile: ImageView = itemView.findViewById(R.id.image_profile)
        val textName: TextView = itemView.findViewById(R.id.text_name)
        val textAttendance: TextView = itemView.findViewById(R.id.text_attendance)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        val textProgress: TextView = itemView.findViewById(R.id.text_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = studentList[position]

        // 이름
        holder.textName.text = student.name

        // 출석
        holder.textAttendance.text = "출석: ${if (student.attendedToday) "O" else "X"}"

        // 진도율 계산
        val percentage = if (student.totalWords != 0)
            (student.memorizedWords * 100 / student.totalWords)
        else 0

        holder.progressBar.max = 100
        holder.progressBar.progress = percentage
        holder.textProgress.text = "${student.memorizedWords} / ${student.totalWords}"

        // 이미지 불러오기 (동그랗게)
        Glide.with(holder.itemView.context)
            .load(student.profileResId)
            .circleCrop()
            .into(holder.imageProfile)

        // TODO: 이후 출석 여부에 따라 테두리 색상 조절 기능 넣을 수 있음
    }

    override fun getItemCount(): Int = studentList.size
}
