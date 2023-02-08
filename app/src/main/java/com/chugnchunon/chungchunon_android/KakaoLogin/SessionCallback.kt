package com.chugnchunon.chungchunon_android.KakaoLogin

import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.chugnchunon.chungchunon_android.DiaryActivity
import com.chugnchunon.chungchunon_android.RegionRegisterActivity
import com.chugnchunon.chungchunon_android.MainActivity
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryActivity
import com.chugnchunon.chungchunon_android.R
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.NonCancellable.cancel
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.timer


class SessionCallback(val context: MainActivity) : ISessionCallback {
    private val TAG: String = "카톡로그인"
    private val userDB = Firebase.firestore.collection("users")


    override fun onSessionOpened() {

        UserManagement.getInstance().me(object : MeV2ResponseCallback() {
            override fun onSuccess(result: MeV2Response?) {

                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                if (result != null) {

                    val accessToken = Session.getCurrentSession().tokenInfo.accessToken
                    val phoneNumber = "010-${
                        result.kakaoAccount?.phoneNumber?.substring(7)
                    }"
                    var birthYear = result.kakaoAccount?.birthyear
                    var userAge = currentYear - birthYear!!.toInt() + 1
                    var gender = if(result.kakaoAccount?.gender.toString() == "FEMALE") "여성" else "남성"

                    val userSet = hashMapOf(
                        "userType" to "치매예방자",
                        "loginType" to "카카오",
                        "userId" to (result.id),
                        "timestamp" to FieldValue.serverTimestamp(),
                        "name" to (result.nickname),
                        "gender" to gender,
                        "phone" to phoneNumber,
                        "birthYear" to result.kakaoAccount?.birthyear,
                        "birthDay" to result.kakaoAccount?.birthday,
                        "todayStepCount" to 0,
                        "userAge" to userAge,
                    )

                    context.getFirebaseJwt(accessToken)!!.continueWithTask { task ->
                        val firebaseToken = task.result
                        val auth = FirebaseAuth.getInstance()
                        auth.signInWithCustomToken(firebaseToken!!)
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            var userId = "kakao:${result.id}"
                            userDB
                                .document("$userId")
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        var userDocument = task.result
                                        if (userDocument.exists()) {
                                            // user 이미 존재
                                            var userAge =
                                                (userDocument.data?.getValue("userAge") as Long).toInt()
                                            var userType = userDocument.data?.getValue("userType")

                                            if(userType == "마스터" || (userAge >= 50 && userType == "치매예방자")) {
                                                var goDiary =
                                                    Intent(context, DiaryActivity::class.java)
                                                context.startActivity(goDiary)
                                            } else if (userType == "파트너"  || (userAge < 50 && userType == "치매예방자")){
                                                var goDiary = Intent(
                                                    context,
                                                    PartnerDiaryActivity::class.java
                                                )
                                                context.startActivity(goDiary)
                                            }
                                        } else {
                                            // user 존재 x
                                            userDB
                                                .document("kakao:${result.id}")
                                                .set(userSet, SetOptions.merge())
                                                .addOnSuccessListener {
                                                    var goRegionRegister = Intent(
                                                        context,
                                                        RegionRegisterActivity::class.java
                                                    )
                                                    goRegionRegister.putExtra("userType", "치매예방자")
                                                    goRegionRegister.putExtra("userAge", userAge)
                                                    context.startActivity(goRegionRegister)
                                                }
                                        }
                                    }
                                }
                        } else {
                            if (task.exception != null) {
                                Log.e(TAG, task.exception.toString())
                            }
                        }
                    }
                }
            }

            override fun onSessionClosed(errorResult: ErrorResult?) {
                Log.e(TAG, "세션 종료")
            }

            override fun onFailure(errorResult: ErrorResult?) {
                val errorCode = errorResult?.errorCode
                val clientErrorCode = -777

                if (errorCode == clientErrorCode) {
                    Log.e(TAG, "카카오톡 서버의 네트워크가 불안정합니다. 잠시 후 다시 시도해주세요.")
                } else {
                    Log.e(TAG, "알 수 없는 오류로 카카오로그인 실패 \n${errorResult?.errorMessage}")
                }

            }

        })
    }

    override fun onSessionOpenFailed(exception: KakaoException?) {
        Log.e(TAG, "onSessionOpenFailed ${exception?.message}")
    }


}