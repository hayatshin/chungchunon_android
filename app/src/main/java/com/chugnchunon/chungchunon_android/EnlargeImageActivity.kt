package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.ScaleAnimation
import androidx.core.text.color
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.EnlargeImageAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.databinding.ActivityImageEnlargeBinding

class EnlargeImageActivity : Activity() {

    private val binding by lazy {
        ActivityImageEnlargeBinding.inflate(layoutInflater)
    }
    lateinit var imageArray: ArrayList<String>
    private var imagePosition: Int = 0
    lateinit var enlargeImageAdapter: EnlargeImageAdapter

    override fun onBackPressed() {
        resumePause = true

        val downAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_small)
        binding.enlargeImageViewPager.startAnimation(downAnimation)

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
        binding.enlargeImageViewPager.startAnimation(biggerAnimation)

        // 뒤로가기
        binding.enlargeGoBackBtn.setOnClickListener {
            resumePause = true

            val downAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_small)
            binding.enlargeImageViewPager.startAnimation(downAnimation)

            Handler().postDelayed({
                finish()
            }, 300)
        }

        imageArray =
            intent.getStringArrayListExtra("imageArray") as ArrayList<String> /* = java.util.ArrayList<kotlin.String> */
        imagePosition = intent.getIntExtra("imagePosition", 0)

        enlargeImageAdapter = EnlargeImageAdapter(this, imageArray)
        binding.enlargeImageViewPager.adapter = enlargeImageAdapter
        binding.enlargeImageViewPager.isUserInputEnabled = false

        binding.enlargeImageViewPager.setCurrentItem(imagePosition)

        val spanText = SpannableStringBuilder()
            .color(Color.WHITE) { append("${imagePosition + 1}") }
            .append(" / ${imageArray.size}")
        binding.enlargeImageIndex.text = spanText

        binding.enlargeLeftArrow.visibility = View.VISIBLE

        binding.enlargeImageViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                var spanText = SpannableStringBuilder()
                    .color(Color.WHITE) { append("${position + 1}") }
                    .append(" / ${imageArray.size}")
                binding.enlargeImageIndex.text = spanText

//               if(position == 0) {
//                   binding.enlargeLeftArrow.visibility = View.GONE
//               } else {
//                   binding.enlargeLeftArrow.visibility = View.VISIBLE
//               }

                if (position == imageArray.size - 1) {
                    binding.enlargeRightArrow.visibility = View.GONE
                } else {
                    binding.enlargeRightArrow!!.visibility = View.VISIBLE
                }

                binding.enlargeLeftArrow.setOnClickListener {
                    binding.enlargeImageViewPager.setCurrentItem(position - 1)
                }
                binding.enlargeRightArrow.setOnClickListener {
                    binding.enlargeImageViewPager.setCurrentItem(position + 1)
                }
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                if (position == 0) {
                    binding.enlargeLeftArrow.visibility = View.GONE
                } else {
                    binding.enlargeLeftArrow.visibility = View.VISIBLE
                }
                if (position == imageArray.size - 1) {
                    binding.enlargeRightArrow.visibility = View.GONE
                } else {
                    binding.enlargeRightArrow!!.visibility = View.VISIBLE
                }

                binding.enlargeLeftArrow.setOnClickListener {
                    binding.enlargeImageViewPager.setCurrentItem(position - 1)
                }
                binding.enlargeRightArrow.setOnClickListener {
                    binding.enlargeImageViewPager.setCurrentItem(position + 1)
                }
            }


        })
    }
}