package com.chugnchunon.chungchunon_android

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.BlockUserListAdapter
import com.chugnchunon.chungchunon_android.Adapter.LikePersonAdapter
import com.chugnchunon.chungchunon_android.DataClass.BlockUser
import com.chugnchunon.chungchunon_android.DataClass.LikePerson
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.databinding.ActivityBlockUserListBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BlockUserListActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityBlockUserListBinding.inflate(layoutInflater)
    }

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")

    private var blockUserList = ArrayList<BlockUser>()


    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.blockUserRecyclerView.visibility = View.GONE
        binding.noBlockUserText.visibility = View.GONE
        binding.dataLoadingProgressBar.visibility = View.VISIBLE

        // recyclerView
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { document ->
                val blockUsers = document.data?.getValue("blockUserList") as ArrayList<String>

                if (blockUsers.size != 0) {
                    // 차단한 사람 있는 경우

                    for (blockUser in blockUsers) {
                        userDB.document("$blockUser")
                            .get()
                            .addOnSuccessListener { blockUserData ->

                                var blockUserId = blockUserData.data!!.getValue("userId").toString()
                                var blockUserAvatar =
                                    blockUserData.data!!.getValue("avatar").toString()
                                var blockUserName = blockUserData.data!!.getValue("name").toString()
                                var userRegion = blockUserData.data!!.getValue("region").toString()
                                var userSmallRegion =
                                    blockUserData.data!!.getValue("smallRegion").toString()
                                var blockUserRegion = "${userRegion} ${userSmallRegion}"

                                var blockUserSet = BlockUser(
                                    blockUserId,
                                    blockUserAvatar,
                                    blockUserName,
                                    blockUserRegion
                                )
                                blockUserList.add(blockUserSet)

                                binding.blockUserRecyclerView.layoutManager = LinearLayoutManager(
                                    this,
                                    RecyclerView.VERTICAL,
                                    false
                                )
                                binding.blockUserRecyclerView.adapter =
                                    BlockUserListAdapter(this, blockUserList)

                                binding.dataLoadingProgressBar.visibility = View.GONE
                                binding.blockUserRecyclerView.visibility = View.VISIBLE
                                binding.noBlockUserText.visibility = View.GONE

                            }
                    }
                } else {
                    // 차단한 사람 없는 경우

                    binding.dataLoadingProgressBar.visibility = View.GONE
                    binding.blockUserRecyclerView.visibility = View.GONE
                    binding.noBlockUserText.visibility = View.VISIBLE

                }

            }


    }
}