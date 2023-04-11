package com.chugnchunon.chungchunon_android

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.Adapter.AttractionDetailAdapter
import com.chugnchunon.chungchunon_android.Layout.CenterZoomLayoutManager
import com.chugnchunon.chungchunon_android.databinding.ActivityAttractionDetailBinding

class AttractionDetailActivity: AppCompatActivity() {

    private val binding by lazy {
        ActivityAttractionDetailBinding.inflate(layoutInflater)
    }

    lateinit var attractionDetailAdapter: AttractionDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.adGoBack.setOnClickListener {
            finish()
        }

        var adName = intent.getStringExtra("adName")
        var adDescription = intent.getStringExtra("adDescription")
        var adLocation = intent.getStringExtra("adLocation")
        var adSubImage = intent.getSerializableExtra("adSubImage") as ArrayList<String>

        Log.d("행사", "${adSubImage.toString()}")

        binding.adAttractionName.text = adName
        binding.adAttractionDescription.text = adDescription
        binding.adAttractionLocation.text = adLocation

        attractionDetailAdapter = AttractionDetailAdapter(this, adSubImage)
        binding.adAttractionRecyclerView.adapter = attractionDetailAdapter
        binding.adAttractionRecyclerView.layoutManager = CenterZoomLayoutManager(this)

    }

}