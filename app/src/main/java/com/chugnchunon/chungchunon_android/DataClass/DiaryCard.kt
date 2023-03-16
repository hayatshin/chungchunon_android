package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri

data class DiaryCard(
    var userId: String,
    var username: String,
    var userAvatar: String?,
    var diaryId: String,
    var writeTime: Long,
    var name: String,
    var stepCount: Long,
    var mood: Long? = 2131230873,
    var diary: String? = "",
    var numLikes: Long? = 0,
    var numComments: Long? = 0,
    var images: ArrayList<String>? = null,
    var secret: Boolean,
    )