package com.chugnchunon.chungchunon_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityYoutubeEnlargeBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener

class EnlargeYoutubeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityYoutubeEnlargeBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.enlargeGoBackBtn.setOnClickListener {
            finish()
        }

        val link = intent.getStringExtra("link").toString()
        val videoId = intent.getStringExtra("videoId").toString()

        lifecycle.addObserver(binding.youtubeView)
        // 이미지
        binding.youtubeView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                super.onReady(youTubePlayer)
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.youtubeView.release()
    }
}