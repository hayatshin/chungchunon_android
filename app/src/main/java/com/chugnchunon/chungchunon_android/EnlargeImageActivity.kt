package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.Adapter
import androidx.core.text.color
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.chugnchunon.chungchunon_android.Adapter.EnlargeImageAdapter
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.LinearLayoutManagerWrapper
import com.chugnchunon.chungchunon_android.databinding.ActivityImageEnlargeBinding
import kotlinx.android.synthetic.main.activity_image_enlarge.view.*
import kotlinx.android.synthetic.main.fragment_mission.view.*

class EnlargeImageActivity : Activity() {

    private val binding by lazy {
        ActivityImageEnlargeBinding.inflate(layoutInflater)
    }
    lateinit var imageArray: ArrayList<String>
    private var imagePosition: Int = 0
    lateinit var enlargeImageAdapter: EnlargeImageAdapter

    override fun onBackPressed() {
        super.onBackPressed()
        resumePause = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.enlargeGoBackBtn.setOnClickListener {
            resumePause = true
            Log.d("resumePause", "EnlargeImageActivity: ${resumePause}")
            Handler().postDelayed({
                finish()
            }, 100)
        }

        imageArray =
            intent.getStringArrayListExtra("imageArray") as ArrayList<String> /* = java.util.ArrayList<kotlin.String> */
        imagePosition = intent.getIntExtra("imagePosition", 0)

        enlargeImageAdapter = EnlargeImageAdapter(this, imageArray)
        binding.enlargeImageViewPager.adapter = enlargeImageAdapter

        binding.enlargeImageViewPager.setCurrentItem(imagePosition)

        var spanText = SpannableStringBuilder()
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