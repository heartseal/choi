package com.example.Rainbow_Voca.util

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Rainbow_Voca.R // R.drawable 등 사용
import com.example.Rainbow_Voca.model.StudyMemberProfile
import kotlin.math.min

/**
 * 스터디룸 멤버 목록(학생 목록)을 RecyclerView에 표시하기 위한 어댑터.
 */
class StudentAdapter(
    private var members: List<StudyMemberProfile>,
    private var currentUserNickname: String,
    private var roomOwner: String
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    /** 각 학생 아이템의 뷰를 보관하는 ViewHolder. */
    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgMemberProfile: ImageView = itemView.findViewById(R.id.image_member_profile_item)
        private val txtMemberName: TextView = itemView.findViewById(R.id.text_member_name_item)
        private val pgbStudyRatio: ProgressBar = itemView.findViewById(R.id.progress_study_ratio_item)
        private val txtStudyRatio: TextView = itemView.findViewById(R.id.text_study_ratio_item)
        private val cardAttendanceIconBg: CardView = itemView.findViewById(R.id.card_attendance_icon_bg_item)
        private val txtAttendanceIcon: TextView = itemView.findViewById(R.id.text_attendance_icon_item)
        private val cardWrongAnswerIconBg: CardView = itemView.findViewById(R.id.card_wrong_answer_icon_bg_item)
        private val txtWrongAnswerCount: TextView = itemView.findViewById(R.id.text_wrong_answer_count_item)

        /** 학생 멤버 데이터를 아이템 뷰에 바인딩합니다. */
        fun bind(member: StudyMemberProfile) {
            txtMemberName.text = member.nickname

            if (member.isAttendedToday) {
                txtAttendanceIcon.text = "출"
                cardAttendanceIconBg.setCardBackgroundColor(Color.parseColor("#4CAF50")) // 초록색 계열
            } else {
                txtAttendanceIcon.text = "미"
                cardAttendanceIconBg.setCardBackgroundColor(Color.parseColor("#BDBDBD")) // 회색 계열 (dimens.xml의 grey_absent 대체)
            }

            if (!member.isAttendedToday) {
                txtWrongAnswerCount.text = "오답\n-"
                cardWrongAnswerIconBg.setCardBackgroundColor(Color.WHITE)
                txtWrongAnswerCount.setTextColor(Color.parseColor("#BDBDBD")) // 회색 계열
            } else if (member.wrongAnswerCount == 0) {
                txtWrongAnswerCount.text = "오답\n0"
                cardWrongAnswerIconBg.setCardBackgroundColor(Color.WHITE)
                txtWrongAnswerCount.setTextColor(Color.parseColor("#4CAF50")) // 초록색 계열
            } else {
                txtWrongAnswerCount.text = "오답\n${member.wrongAnswerCount}"
                val MAX_WRONG_FOR_GRADIENT = 10f
                val ratio = min(member.wrongAnswerCount / MAX_WRONG_FOR_GRADIENT, 1.0f)

                val startColorInt = Color.parseColor("#FFCDD2") // 연한 빨강 (dimens.xml의 red_error_very_light 대체)
                val endColorInt = Color.parseColor("#D32F2F")   // 진한 빨강 (dimens.xml의 red_error_very_dark 대체)

                val r = (Color.red(startColorInt) * (1 - ratio) + Color.red(endColorInt) * ratio).toInt()
                val g = (Color.green(startColorInt) * (1 - ratio) + Color.green(endColorInt) * ratio).toInt()
                val b = (Color.blue(startColorInt) * (1 - ratio) + Color.blue(endColorInt) * ratio).toInt()
                cardWrongAnswerIconBg.setCardBackgroundColor(Color.rgb(r, g, b))
                txtWrongAnswerCount.setTextColor(Color.WHITE)
            }

            val percentage = if (member.totalWordCount > 0) {
                (member.studiedWordCount * 100) / member.totalWordCount
            } else {
                0
            }
            pgbStudyRatio.progress = percentage
            txtStudyRatio.text = "$percentage%"

            member.profileImage?.let { imageName ->
                val imageResId = itemView.context.resources.getIdentifier(imageName, "drawable", itemView.context.packageName)
                if (imageResId != 0) {
                    Glide.with(itemView.context)
                        .load(imageResId)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_default)
                        .error(R.drawable.ic_profile_default)
                        .into(imgMemberProfile)
                } else {
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_profile_default)
                        .circleCrop()
                        .into(imgMemberProfile)
                }
            } ?: Glide.with(itemView.context)
                .load(R.drawable.ic_profile_default)
                .circleCrop()
                .into(imgMemberProfile)
        }
    }

    /** 새 ViewHolder 인스턴스를 생성합니다. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    /** ViewHolder에 데이터를 실제로 표시합니다. */
    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val member = members[position]
        holder.bind(member)
    }

    /** 어댑터가 다루는 전체 아이템의 수를 반환합니다. */
    override fun getItemCount(): Int = members.size

    /** 어댑터의 데이터를 새 목록으로 갱신하고 UI를 새로고침합니다. */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newMembers: List<StudyMemberProfile>, currentNickname: String, ownerNickname: String) {
        members = newMembers
        currentUserNickname = currentNickname
        roomOwner = ownerNickname
        notifyDataSetChanged()
    }

    /** 특정 위치(position)의 멤버 객체를 반환합니다. */
    fun getMemberAt(position: Int): StudyMemberProfile? {
        return if (position >= 0 && position < members.size) {
            members[position]
        } else {
            null
        }
    }
}