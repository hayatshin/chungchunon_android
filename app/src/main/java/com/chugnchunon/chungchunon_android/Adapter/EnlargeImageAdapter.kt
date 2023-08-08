package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.DataClass.Mission
import com.chugnchunon.chungchunon_android.EnlargeImageActivity
import com.chugnchunon.chungchunon_android.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_image_enlarge.view.*
import kotlinx.android.synthetic.main.enlarge_image_card.view.*

class EnlargeImageAdapter(
    val context: Context,
    private val imageList: ArrayList<String>,
) :
    RecyclerView.Adapter<EnlargeImageAdapter.ViewHolder>() {
    private var player: SimpleExoPlayer? = null

    inner class ViewHolder(view: View?) : RecyclerView.ViewHolder(view!!) {
        val enlargeImageView = view?.findViewById<ImageView>(R.id.enlargeImageView)
        val enlargeVideoView = view!!.findViewById<PlayerView>(R.id.enlargeVideoView)
        val videoLoadingView = view!!.findViewById<LinearLayout>(R.id.videoLoading)

        fun bind(context: Context, position: Int) {
            if(imageList[position].contains("video")) {
                enlargeImageView?.visibility = View.GONE
                enlargeVideoView?.visibility = View.GONE
                videoLoadingView?.visibility = View.VISIBLE

                player = SimpleExoPlayer.Builder(context).build()
                enlargeVideoView.player = player

                val userAgent = Util.getUserAgent(context, "com.chugnchunon.chungchunon_android")
                val dataSourceFactory = DefaultDataSourceFactory(context, userAgent)
                val source = Uri.parse(imageList[position])
                val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(source))
                player?.setMediaSource(mediaSource)

                player?.addListener(object : Player.Listener {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        super.onPlayerStateChanged(playWhenReady, playbackState)
                        if(playbackState == Player.STATE_READY) {
                            videoLoadingView?.visibility = View.GONE
                            enlargeVideoView?.visibility = View.VISIBLE
                        }
                    }
                })

                player?.prepare()
                player?.playWhenReady = true

//                val mediaController = MediaController(context)
//                mediaController.setAnchorView(enlargeVideoView)
//
//                val videoUri: Uri = Uri.parse(imageList[position])
//                enlargeVideoView!!.setMediaController(mediaController)
//
//                enlargeVideoView?.setVideoURI(videoUri)
//                enlargeVideoView.requestFocus()
//
//                enlargeVideoView.setOnPreparedListener {
//                    Toast.makeText(context, "동영상 재생 준비 완료", Toast.LENGTH_LONG).show()
//
//                    enlargeVideoView.start()
//                }
            } else {
                enlargeImageView?.visibility = View.VISIBLE
                enlargeVideoView?.visibility = View.GONE
                videoLoadingView?.visibility = View.GONE

                Glide.with(context)
                    .load(imageList[position])
                    .into(enlargeImageView!!)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EnlargeImageAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.enlarge_image_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: EnlargeImageAdapter.ViewHolder, position: Int) {
        holder.bind(context, position)


    }

    override fun getItemCount(): Int = imageList.size

    fun notifyBackButtonPressed() {
        Log.d("비디오2", "중단")
        if(player != null) {
            player?.stop()
        }
    }

}