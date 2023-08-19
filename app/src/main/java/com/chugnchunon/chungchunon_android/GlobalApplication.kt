package com.chugnchunon.chungchunon_android

import android.app.Application
import android.util.Log
import com.chugnchunon.chungchunon_android.Adapter.KakaoSDKAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.auth.*
import com.kakao.sdk.common.KakaoSdk


class GlobalApplication : Application() {
    companion object {
        var instance: GlobalApplication? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            KakaoSDK.init(KakaoSDKAdapter(getAppContext()))
        } catch (e: Exception) {
            Log.d("카카오 글로벌", "$e")
        }

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
