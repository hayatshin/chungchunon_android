package com.chugnchunon.chungchunon_android.DataClass

import android.net.Uri
import androidx.annotation.Keep

@Keep
data class AppUpdate(
    var app_version: String?,
    var force_update: Boolean?,
)