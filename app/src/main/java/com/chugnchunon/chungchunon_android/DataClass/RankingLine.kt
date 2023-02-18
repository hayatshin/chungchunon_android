package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri

data class RankingLine(
    var index: Int,
    var userId: String,
    var username: String,
    var userAvatar: String?,
    var region: String
    )