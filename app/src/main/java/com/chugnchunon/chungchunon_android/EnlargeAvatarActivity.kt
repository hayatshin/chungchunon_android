package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.databinding.ActivityAvatarEnlargeBinding

class EnlargeAvatarActivity: Activity() {
    private val binding by lazy {
        ActivityAvatarEnlargeBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AllDiaryFragmentTwo.resumePause = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.enlargeGoBackBtn.setOnClickListener {
            AllDiaryFragmentTwo.resumePause = true
            Handler().postDelayed({
                finish()
            }, 100)
        }

        var userAvatarUrl = intent.getStringExtra("userAvatar")

        Glide.with(this)
            .load(userAvatarUrl)
            .into(binding.enlargeAvatarView)
    }
}