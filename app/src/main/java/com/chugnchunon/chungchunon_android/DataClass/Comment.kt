package com.chugnchunon.chungchunon_android.DataClass

data class Comment(
    var diaryId: String,
    var diaryPosition: Int,
    val commentId: String,
    val commentName: String,
    val commentTimestamp: String,
    var commentDescription: String
)
