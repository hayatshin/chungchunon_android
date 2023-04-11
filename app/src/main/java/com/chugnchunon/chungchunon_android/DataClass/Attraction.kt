package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri

data class Attraction(
    var name: String,
    var description: String,
    var location: String,
    var mainImage: String,
    var subImage: ArrayList<String>,
    )