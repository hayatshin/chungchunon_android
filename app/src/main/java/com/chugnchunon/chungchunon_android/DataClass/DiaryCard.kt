package com.chugnchunon.chungchunon_android.DataClass

data class DiaryCard(
    var userId: String,
    var username: String,
    var diaryId: String,
    var writeTime: Long,
    var name: String,
    var stepCount: Long,
    var mood: Long? = 2131230873,
    var diary: String? = "",
    var numLikes: Long? = 0,
    var numComments: Long? = 0,
    )