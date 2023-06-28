package com.chugnchunon.chungchunon_android.DataClass

data class Mission(
    var documentId: String,
    var community: String,
    var communityLogo: String,
    var missionImage: String,
    var title: String,
    var startPeriod: String,
    var endPeriod: String,
    var description: String,
    var state: String,
    var goalScore: Int,
    var autoProgress: Boolean,
    var prizeWinners: Int,
)