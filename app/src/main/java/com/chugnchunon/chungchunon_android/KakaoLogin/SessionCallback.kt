package com.chugnchunon.chungchunon_android.KakaoLogin

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.chugnchunon.chungchunon_android.CommunityRegisterActivity
import com.chugnchunon.chungchunon_android.MainActivity
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


class SessionCallback(val context: MainActivity): ISessionCallback {
    private val TAG : String = "카톡로그인"
    private val userDB = Firebase.firestore.collection("users")

    override fun onSessionOpened() {

            UserManagement.getInstance().me(object : MeV2ResponseCallback(){
            override fun onSuccess(result: MeV2Response?) {
                if(result != null){
                    Log.d(TAG, "세션 오픈")
                    val accessToken = Session.getCurrentSession().tokenInfo.accessToken
                    val phoneNumber = "010-${
                        result.kakaoAccount?.phoneNumber?.substring(7)}"

                    val userSet = hashMapOf(
                        "loginType" to "kakao",
                        "userId" to (result.id),
                        "createdTime" to FieldValue.serverTimestamp(),
                        "name" to (result.nickname),
                        "gender" to (result.kakaoAccount?.gender),
                        "phone" to phoneNumber,
                        "birth" to ("${result.kakaoAccount?.birthyear}/${result.kakaoAccount?.birthday}"),
                    )

                    context.getFirebaseJwt(accessToken)!!.continueWithTask { task ->
                        val firebaseToken = task.result
                        val auth = FirebaseAuth.getInstance()
                        auth.signInWithCustomToken(firebaseToken!!)
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "성공성공")
                            userDB
                                .document("kakao:${result.id}")
                                .set(userSet, SetOptions.merge())
                                .addOnSuccessListener {
                                    var goDiary = Intent(context, CommunityRegisterActivity::class.java)
                                    context.startActivity(goDiary)
                                }
                        }
                        else {
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

                if(errorCode == clientErrorCode){
                    Log.e(TAG, "카카오톡 서버의 네트워크가 불안정합니다. 잠시 후 다시 시도해주세요.")
                }else{
                    Log.e(TAG, "알 수 없는 오류로 카카오로그인 실패 \n${errorResult?.errorMessage}")
                }

            }

        })
    }

    override fun onSessionOpenFailed(exception: KakaoException?) {
        Log.e(TAG, "onSessionOpenFailed ${exception?.message}")
    }

}