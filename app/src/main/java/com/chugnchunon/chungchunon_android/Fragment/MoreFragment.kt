package com.chugnchunon.chungchunon_android.Fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.databinding.FragmentMoreTwoBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class MoreFragment: Fragment() {

    private var _binding: FragmentMoreTwoBinding? = null
    private val binding get() = _binding!!

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    lateinit var mcontext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    @SuppressLint("Range", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMoreTwoBinding.inflate(inflater, container, false)
        val view = binding.root
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // 초기 셋업
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                val userName = document.data?.getValue("name").toString()
                val userAvatar = document.data?.getValue("avatar").toString()
                val userAge = document.data?.getValue("userAge").toString().toInt()
                val dbRegion = document.data?.getValue("region")
                val dbSmallRegion = document.data?.getValue("smallRegion")
                val userRegion = "${dbRegion} ${dbSmallRegion}"

                Glide.with(mcontext)
                    .load(userAvatar)
                    .into(binding.profileAvatar)

                binding.profileName.text = userName
                binding.profileAge.text = "${userAge}세"
                binding.profileRegion.text = userRegion
            }

        // 개인정보 수정 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            editProfileWithNewInfo,
            IntentFilter("EDIT_PROFILE")
        )

        binding.profileEditBtn.setOnClickListener {
            var goProfileEdit = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(goProfileEdit)
        }

        binding.application.setOnClickListener {
            var app_intent = Intent(activity, ApplicationRuleActivity::class.java)
            startActivity(app_intent)
        }

        binding.personalInfo.setOnClickListener {
            var personal_info_intent = Intent(activity, PersonalInfoRuleActivity::class.java)
            startActivity(personal_info_intent)
        }

        binding.blockUserBtn.setOnClickListener {
            var block_user_list_intent = Intent(activity, BlockUserListActivity::class.java)
            startActivity(block_user_list_intent)
        }

        binding.exitAppBtn.setOnClickListener {
            var exit_intent = Intent(activity, ExitActivity::class.java)
            startActivity(exit_intent)
        }

        return view
    }


    var editProfileWithNewInfo: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var newAvatar = intent?.getStringExtra("newAvatar")
            var newName = intent?.getStringExtra("newName")
            var newUserAge = intent?.getIntExtra("newUserAge", 0)
            var newRegionSmallRegion = intent?.getStringExtra("newRegionSmallRegion")

            Glide.with(context!!)
                .load(newAvatar)
                .into(binding.profileAvatar)

            binding.profileName.text = newName
            binding.profileAge.text = "${newUserAge}세"
            binding.profileRegion.text = newRegionSmallRegion
        }
    }

}

