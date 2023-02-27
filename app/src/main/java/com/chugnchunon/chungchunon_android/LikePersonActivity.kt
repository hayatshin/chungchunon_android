package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.LikePersonAdapter
import com.chugnchunon.chungchunon_android.DataClass.LikePerson
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.LinearLayoutManagerWrapper
import com.chugnchunon.chungchunon_android.databinding.ActivityImageEnlargeBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityLikePersonBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_like_person.*

class LikePersonActivity: Activity() {

    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid
    var diaryDB = Firebase.firestore.collection("diary")

    private val binding by lazy {
        ActivityLikePersonBinding.inflate(layoutInflater)
    }
    private var diaryId = ""
    lateinit var likePersonAdapter: LikePersonAdapter
    private var likePersonList = ArrayList<LikePerson>()

    override fun onBackPressed() {
        super.onBackPressed()
        resumePause = true

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
        binding.likePersonLayout.startAnimation(upAnimation)

        binding.likePersonBackground.setOnClickListener {
            resumePause = true

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.likePersonLayout.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 500)
        }

        binding.likePersonGoBackArrow.setOnClickListener {
            resumePause = true
            finish()

            var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
            binding.likePersonLayout.startAnimation(downAnimation)
            Handler().postDelayed({
                finish()
            }, 500)
        }

        diaryId = intent.getStringExtra("diaryId").toString()
        diaryDB.document("$diaryId").collection("likes")
            .get()
            .addOnSuccessListener { likePersons ->
                for (likeperson in likePersons) {
                    var likePersonId = likeperson.data.getValue("id")

                    userDB.document("$likePersonId")
                        .get()
                        .addOnSuccessListener { likePersonUser ->
                            var likePersonAvatar = likePersonUser.data!!.getValue("avatar").toString()
                            var likePersonName = likePersonUser.data!!.getValue("name").toString()
                            var userRegion = likePersonUser.data!!.getValue("region").toString()
                            var userSmallRegion = likePersonUser.data!!.getValue("smallRegion").toString()
                            var likePersonRegion = "${userRegion} ${userSmallRegion}"

                            var likePersonSet = LikePerson(
                                likePersonAvatar,
                                likePersonName,
                                likePersonRegion
                            )
                            likePersonList.add(likePersonSet)

                            binding.likePersonRecyclerView.layoutManager = LinearLayoutManager(
                                this,
                                RecyclerView.VERTICAL,
                                false
                            )

                            likePersonAdapter = LikePersonAdapter(this, likePersonList)
                            binding.likePersonRecyclerView.adapter = likePersonAdapter
                            likePersonAdapter.notifyDataSetChanged()
                            binding.dataLoadingProgressBar.visibility = View.GONE
                            binding.likePersonRecyclerView.visibility = View.VISIBLE

                        }
                }

            }

    }
}