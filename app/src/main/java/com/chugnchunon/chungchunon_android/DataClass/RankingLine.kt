package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri

data class RankingLine(
    var index: Int? = null,
    var userId: String,
    var username: String,
    var userAvatar: String?,
    var point: Int? = null,
    )