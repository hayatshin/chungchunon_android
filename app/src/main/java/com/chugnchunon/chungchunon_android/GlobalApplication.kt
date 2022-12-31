package com.chugnchunon.chungchunon_android

import android.app.Application
import com.kakao.sdk.common.KakaoSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, getString(R.string.kakao_native_key))

    }
}