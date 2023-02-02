package com.chugnchunon.chungchunon_android.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.chugnchunon.chungchunon_android.databinding.BigImageCardBinding

class DisplayImageAdapter(val context: Context,  private val imageUrlList: ArrayList<String>?):
    RecyclerView.Adapter<DisplayImageAdapter.ViewPagerViewHolder>(){

    inner class ViewPagerViewHolder(val binding: BigImageCardBinding):
        RecyclerView.ViewHolder(binding.root){

            fun setData(imageUrl: String, position: Int) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.bigImageCard)

                // 화살표 핸들링
                if(imageUrlList!!.size == 1) {
                    binding.backImageBtn.visibility = View.GONE
                    binding.forwardImageBtn.visibility = View.GONE
                } else if (position == 0) {
                    binding.backImageBtn.visibility = View.GONE
                    binding.forwardImageBtn.visibility = View.VISIBLE
                } else if (position == imageUrlList!!.size-1) {
                    binding.backImageBtn.visibility = View.VISIBLE
                    binding.forwardImageBtn.visibility = View.GONE
                } else {
                    binding.backImageBtn.visibility = View.VISIBLE
                    binding.forwardImageBtn.visibility = View.VISIBLE
                }

                // 화살표 함수
                binding.backImageBtn.setOnClickListener {
                    setData(imageUrlList!![position-1], position-1)
                }
                binding.forwardImageBtn.setOnClickListener {
                    setData(imageUrlList!![position+1], position+1)
                }
            }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DisplayImageAdapter.ViewPagerViewHolder {
        val binding = BigImageCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewPagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DisplayImageAdapter.ViewPagerViewHolder, position: Int) {
        holder.setData(imageUrlList!![position], position)
    }

    override fun getItemCount(): Int = imageUrlList!!.size


}