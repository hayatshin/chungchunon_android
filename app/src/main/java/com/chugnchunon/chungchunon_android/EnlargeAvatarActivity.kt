package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.doOnPreDraw
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.databinding.ActivityAvatarEnlargeBinding

class EnlargeAvatarActivity: Activity() {
    private val binding by lazy {
        ActivityAvatarEnlargeBinding.inflate(layoutInflater)
    }

    override fun onBackPressed() {
        AllDiaryFragmentTwo.resumePause = true

        val downAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_small)
        binding.enlargeAvatarView.startAnimation(downAnimation)

        downAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                finish()
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        // 뷰페이저 애니메이션
        val biggerAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_big)
        binding.enlargeAvatarView.startAnimation(biggerAnimation)

        // 뒤로가기
        binding.enlargeGoBackBtn.setOnClickListener {
            AllDiaryFragmentTwo.resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_small)
            binding.enlargeAvatarView.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 300)
        }


        val userAvatarUrl = intent.getStringExtra("userAvatar")

        Glide.with(this)
            .load(userAvatarUrl)
            .into(binding.enlargeAvatarView)

    }
}