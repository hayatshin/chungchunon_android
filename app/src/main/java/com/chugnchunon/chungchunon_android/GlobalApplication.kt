package com.chugnchunon.chungchunon_android

import android.app.Application
import com.chugnchunon.chungchunon_android.Adapter.KakaoSDKAdapter
import com.kakao.auth.*
import com.kakao.sdk.common.KakaoSdk


class GlobalApplication : Application() {
    companion object {
        var instance: GlobalApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        KakaoSDK.init(KakaoSDKAdapter(getAppContext()))
    }

    override fun onTerminate() {
        super.onTerminate()
        instance = null
    }

    fun getAppContext(): GlobalApplication {
        checkNotNull(instance) {
            "This Application does not inherit com.example.App"
        }
        return instance!!
    }
}
