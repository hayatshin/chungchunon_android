package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.CommunityAdapter
import com.chugnchunon.chungchunon_android.Adapter.CommunityResultAdapter
import com.chugnchunon.chungchunon_android.Adapter.RegionPagerAdapter
import com.chugnchunon.chungchunon_android.DataClass.Community
import com.chugnchunon.chungchunon_android.DataClass.DiaryCard
import com.chugnchunon.chungchunon_android.Fragment.LinearLayoutManagerWrapper
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.databinding.ActivityCommunityBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityRegionBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_region_data.view.*


class EditCommunityRegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityCommunityBinding.inflate(layoutInflater)
    }

    lateinit var adapter: CommunityAdapter
    lateinit var communitySet: Community
    private var communityItems: ArrayList<Community> = ArrayList()

    lateinit var resultAdapter: CommunityResultAdapter
    private var selectedCommunityItems: ArrayList<String> = ArrayList()

    var db = Firebase.firestore
    var userDB = Firebase.firestore.collection("users")
    var userId = Firebase.auth.currentUser?.uid

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.communityRegisterBtn.text = "소속기관 수정하기"

//        val selectedFullRegion = intent.getStringExtra("fullRegion").toString()
        val userCommunities = intent.getStringArrayListExtra("userCommunities") as ArrayList<String>
        selectedCommunityItems = ArrayList<String>(userCommunities)

        LocalBroadcastManager.getInstance(this).registerReceiver(
            communitySelectReceiver,
            IntentFilter("COMMUNITY_SELECT")
        )

        LocalBroadcastManager.getInstance(this).registerReceiver(
            communityDeleteReceiver,
            IntentFilter("COMMUNITY_DELETE")
        )

        adapter = CommunityAdapter(this, communityItems)
        binding.communityRecycler.adapter = adapter
        binding.communityRecycler.layoutManager = LinearLayoutManagerWrapper(
            this,
            RecyclerView.VERTICAL,
            false
        )

        resultAdapter = CommunityResultAdapter(this, selectedCommunityItems)
        binding.communityResultRecycler.adapter = resultAdapter
        binding.communityResultRecycler.layoutManager = LinearLayoutManagerWrapper(
            this,
            RecyclerView.HORIZONTAL,
            false
        )

        binding.backBtn.setOnClickListener {
            finish()
        }

        db.collection("community")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val communities = task.result
                    if (communities != null) {
                        if (!communities.isEmpty) {
                            // 소속기관 있음
                            binding.noCommunity.visibility = View.GONE
                            binding.communityRecycler.visibility = View.VISIBLE

                            for (communityData in communities) {
                                val communityTitle =
                                    communityData.data.getValue("communityTitle").toString()
                                val communityImage =
                                    communityData.data.getValue("communityImage").toString()

                                communitySet = Community(
                                    communityTitle,
                                    communityImage
                                )

                                communityItems.add(communitySet)
                                adapter.notifyDataSetChanged()
                            }
                        } else {
                            // 소속기관 없음
                            binding.noCommunity.visibility = View.VISIBLE
                            binding.communityRecycler.visibility = View.GONE
                        }
                    } else {
                        // 소속기관 없음

                        binding.noCommunity.visibility = View.VISIBLE
                        binding.communityRecycler.visibility = View.GONE
                    }
                }
            }

        binding.communityRegisterBtn.setOnClickListener {

            val intent = Intent(this, EditProfileActivity::class.java)
            intent.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            intent.putExtra("newCommunityList", selectedCommunityItems)
            setResult(RESULT_OK, intent)
            finish()

        }
    }

    var communitySelectReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val communityTitle = intent?.getStringExtra("communityTitle").toString()
            selectedCommunityItems.add(communityTitle)
            resultAdapter.notifyDataSetChanged()
        }
    }

    var communityDeleteReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val communityDeletePosition = intent?.getIntExtra("deletePosition", 0)
            selectedCommunityItems.removeAt(communityDeletePosition!!.toInt())
            resultAdapter.notifyDataSetChanged()
        }
    }
}
