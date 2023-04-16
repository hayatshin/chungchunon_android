package com.chugnchunon.chungchunon_android.DataClass

data class ReComment(
    val diaryId: String,
    val commentId: String,
    val reCommentId: String,
    val reCommentUserId: String,
    val reCommentUserAvatar: String,
    val reCommentUserName: String,
    val reCommentUserType: String,
    val reCommentTimestamp: String,
    var reCommentDescription: String
)
