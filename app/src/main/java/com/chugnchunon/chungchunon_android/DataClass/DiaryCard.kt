package com.chugnchunon.chungchunon_android.DataClass

data class DiaryCard(
    var diaryId: String,
    var writeTime: String,
    var name: String,
    var stepCount: String,
    var mood: Long? = 2131230873,
    var diary: String? = "",
    var numLikes: Long? = 0,
    var numComments: Long? = 0,
)