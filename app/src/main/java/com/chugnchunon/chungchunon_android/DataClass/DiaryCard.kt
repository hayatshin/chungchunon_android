package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class DiaryCard(
    var userId: String = "default_user",
    var username: String = "탈퇴자",
    var userAvatar: String? = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png",
    var diaryId: String,
    var writeTime: Long,
    var stepCount: Long,
    var mood: Long? = 2131230873,
    var diary: String? = "",
    var numLikes: Long? = 0,
    var numComments: Long? = 0,
    var images: ArrayList<String>? = null,
    var secret: Boolean,
    var forceSecret: Boolean,
    )