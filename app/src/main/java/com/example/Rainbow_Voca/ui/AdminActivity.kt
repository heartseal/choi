package com.example.Rainbow_Voca.ui

import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.Rainbow_Voca.R
import com.example.Rainbow_Voca.model.StudyMemberProfile
import com.example.Rainbow_Voca.model.StudyRoom
import com.example.Rainbow_Voca.util.ApiResult
import com.example.Rainbow_Voca.util.StudentAdapter
import com.example.Rainbow_Voca.viewmodel.AdminViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

/**
 * 스터디룸 관리자 화면을 나타내는 Activity.
 * 방 정보 표시, 멤버 목록 관리(강퇴) 등의 기능을 제공.
 */
class AdminActivity : AppCompatActivity() {

    private val viewModel: AdminViewModel by viewModels()

    private lateinit var btnClosePage: ImageButton
    private lateinit var textAdminRoomTitle: TextView
    private lateinit var imgAdminProfileBanner: ImageView
    private lateinit var textAdminLabelBanner: TextView
    private lateinit var textAdminNicknameBanner: TextView
    private lateinit var recyclerAdminMemberList: RecyclerView
    private lateinit var textAdminEmptyMembers: TextView

    private lateinit var memberAdapter: StudentAdapter

    private var currentRoomTitleFromIntent: String? = null
    private var currentRoomData: StudyRoom? = null

    /** Activity 생성 및 초기 설정을 수행하기. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        viewModel.initCurrentUser("레인", 101)

        btnClosePage = findViewById(R.id.button_close_admin_page)
        textAdminRoomTitle = findViewById(R.id.text_admin_room_title)
        imgAdminProfileBanner = findViewById(R.id.image_admin_profile_banner)
        textAdminLabelBanner = findViewById(R.id.text_admin_label_banner)
        textAdminNicknameBanner = findViewById(R.id.text_admin_nickname_banner)
        recyclerAdminMemberList = findViewById(R.id.recycler_admin_member_list)
        textAdminEmptyMembers = findViewById(R.id.text_admin_empty_members)

        currentRoomTitleFromIntent = intent.getStringExtra("room_title")

        if (currentRoomTitleFromIntent == null) {
            Toast.makeText(this, "Room info unavailable.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()
        observeViewModel()
        currentRoomTitleFromIntent?.let { viewModel.loadRoomDetails(it) }


        btnClosePage.setOnClickListener { finish() }
    }

    /** RecyclerView 초기 설정을 수행. */
    private fun setupRecyclerView() {
        recyclerAdminMemberList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    /** 멤버 목록을 표시할 RecyclerView 어댑터를 설정하고 스와이프 기능을 연결. */
    private fun setupAdapterAndSwipe(room: StudyRoom) {
        val currentNickname = viewModel.currentNickname

        if (!::memberAdapter.isInitialized || recyclerAdminMemberList.adapter == null) {
            memberAdapter = StudentAdapter(
                room.members,
                currentNickname,
                room.ownerNickname
            )
            recyclerAdminMemberList.adapter = memberAdapter
            attachSwipeToKick()
        } else {
            memberAdapter.updateData(room.members, currentNickname, room.ownerNickname)
        }

        if (room.members.isEmpty()) {
            recyclerAdminMemberList.visibility = View.GONE
            textAdminEmptyMembers.visibility = View.VISIBLE
        } else {
            recyclerAdminMemberList.visibility = View.VISIBLE
            textAdminEmptyMembers.visibility = View.GONE
        }
    }

    /** 스와이프하여 멤버를 강퇴하는 기능을 RecyclerView에 연결. */
    private fun attachSwipeToKick() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    memberAdapter.getMemberAt(position)?.let { memberToKick ->
                        currentRoomData?.let { room ->
                            showKickConfirmDialog(room.title, memberToKick)
                        }
                    }
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return 0

                memberAdapter.getMemberAt(position)?.let { member ->
                    currentRoomData?.let { room ->
                        val currentUid = viewModel.currentUserId
                        val isCurrentUserOwner = room.ownerId == currentUid
                        val isTargetNotOwner = member.userId != room.ownerId
                        val isTargetNotSelf = member.userId != currentUid

                        return if (isCurrentUserOwner && isTargetNotOwner && isTargetNotSelf) ItemTouchHelper.LEFT else 0
                    }
                }
                return 0
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val deleteColor = Color.parseColor("#F44336")
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(deleteColor)
                    .addSwipeLeftActionIcon(R.drawable.ic_delete_sweep)
                    .addSwipeLeftLabel("강퇴")
                    .setSwipeLeftLabelColor(Color.WHITE)
                    .create()
                    .decorate()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerAdminMemberList)
    }

    /** AdminViewModel의 LiveData 변경을 관찰하여 UI를 업데이트. */
    private fun observeViewModel() {
        viewModel.roomDetails.observe(this, Observer { result ->
            when (result) {
                is ApiResult.Loading -> { /* 로딩 UI 업데이트 */ }
                is ApiResult.Success -> {
                    currentRoomData = result.data
                    updateUiWithRoom(result.data)
                    setupAdapterAndSwipe(result.data)
                }
                is ApiResult.Error -> {
                    Toast.makeText(this, "방 정보를 불러오는데 실패했습니다: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is ApiResult.Idle -> { /* 초기 상태 */ }
                null -> {}
            }
        })

        viewModel.kickResult.observe(this, Observer { result ->
            if (result is ApiResult.Error) {
                currentRoomTitleFromIntent?.let { viewModel.loadRoomDetails(it) }
            }
            if (result !is ApiResult.Loading && result !is ApiResult.Idle) {
                viewModel.clearKickResult()
            }
        })

        viewModel.isLoading.observe(this, Observer { isLoading ->
            // 로딩 상태에 따른 UI 변경
        })

        viewModel.message.observe(this, Observer { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        })
    }

    /** 스터디룸 정보로 UI 요소를 업데이트. */
    private fun updateUiWithRoom(room: StudyRoom) {
        textAdminRoomTitle.text = room.title
        textAdminNicknameBanner.text = room.ownerNickname

        val ownerProfile = room.members.find { it.userId == room.ownerId }
        ownerProfile?.profileImage?.let { imageName ->
            val imageResId = resources.getIdentifier(imageName, "drawable", packageName)
            if (imageResId != 0) {
                Glide.with(this).load(imageResId).circleCrop()
                    .placeholder(R.drawable.ic_profile_default)
                    .error(R.drawable.ic_profile_default)
                    .into(imgAdminProfileBanner)
            } else {
                Glide.with(this).load(R.drawable.ic_profile_default)
                    .circleCrop().into(imgAdminProfileBanner)
            }
        } ?: Glide.with(this).load(R.drawable.ic_profile_default)
            .circleCrop().into(imgAdminProfileBanner)
    }

    /** 멤버 강퇴 전 확인 다이얼로그를 표시. */
    private fun showKickConfirmDialog(roomTitle: String, memberToKick: StudyMemberProfile) {
        AlertDialog.Builder(this)
            .setTitle("${memberToKick.nickname}님을 탈퇴시키겠습니까?")
            .setMessage("정말로 '${memberToKick.nickname}'님을 이 방에서 내보내시겠습니까?")
            .setPositiveButton("확인") { dialog, _ ->
                viewModel.kickMemberFromRoom(roomTitle, memberToKick.userId)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                currentRoomTitleFromIntent?.let { viewModel.loadRoomDetails(it) }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}