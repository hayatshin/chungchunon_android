package com.chugnchunon.chungchunon_android.DataClass

data class Comment(
    var diaryId: String,
    var diaryPosition: Int,
    val commentId: String,
    val commentUserId: String,
    val commentUserAvatar: String,
    val commentUserName: String,
    val commentUserType: String,
    val commentTimestamp: String,
    var commentDescription: String,
)
