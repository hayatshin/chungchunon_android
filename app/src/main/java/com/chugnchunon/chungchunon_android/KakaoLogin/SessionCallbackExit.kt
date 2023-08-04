package com.chugnchunon.chungchunon_android.KakaoLogin

import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.Service.MyService
import com.kakao.auth.ISessionCallback
import com.kakao.auth.Session
import com.kakao.network.ErrorResult
import com.kakao.usermgmt.UserManagement
import com.kakao.usermgmt.callback.MeV2ResponseCallback
import com.kakao.usermgmt.response.MeV2Response
import com.kakao.util.exception.KakaoException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.tasks.await
import java.util.*


class SessionCallbackExit(val context: DefaultCancelWarningActivity) : ISessionCallback {
    private val TAG: String = "카톡로그인"
    private val userDB = Firebase.firestore.collection("users")

    override fun onSessionOpened() {

        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, ex -> ex.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        UserManagement.getInstance().me(object : MeV2ResponseCallback() {
            override fun onSuccess(result: MeV2Response?) {
                try {
                    if (result != null) {
                        val accessToken = Session.getCurrentSession().tokenInfo.accessToken

                        context.getFirebaseJwt(accessToken)!!.continueWithTask { task ->
                            val firebaseToken = task.result
                            val auth = FirebaseAuth.getInstance()

                            auth.signInWithCustomToken(firebaseToken!!)
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val currentUser = Firebase.auth.currentUser!!
                                val currentUserId = currentUser.uid
                                // firestore 삭제
                                userDB.document(currentUserId).delete()
                                    .addOnCompleteListener { task ->
                                        if(task.isSuccessful) {
                                            // auth 삭제
                                            currentUser.delete()
                                                .addOnCompleteListener { task ->
                                                    if(task.isSuccessful) {
                                                        val exitIntent =
                                                            Intent(context, MyService::class.java)
                                                        exitIntent.setAction("EXIT_APP")
                                                        LocalBroadcastManager.getInstance(context)
                                                            .sendBroadcast(exitIntent)

                                                        val goMain =
                                                            Intent(
                                                                context,
                                                                MainActivity::class.java
                                                            )
                                                        goMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        context.startActivity(goMain)
                                                    }
                                                }
                                        }
                                    }

                            } else {
                                // 카톡 로그인 에러
                                Log.d("탈퇴", "에러")

                            }
                        }
                    }
                } catch (e: Exception) {
                    // 에러
                    Log.d("탈퇴", "에러: $e")
                }
            }

            override fun onSessionClosed(errorResult: ErrorResult?) {
                Log.e(TAG, "세션 종료")
                Toast.makeText(context, "가입 종료", Toast.LENGTH_LONG).show()

                context.kakaoActivityIndicator.visibility = View.GONE
                context.kakaoLoginTextView.visibility = View.VISIBLE
            }

            override fun onFailure(errorResult: ErrorResult?) {
                val errorCode = errorResult?.errorCode
                val clientErrorCode = -777

                if (errorCode == clientErrorCode) {
                    Log.e(TAG, "카카오톡 서버의 네트워크가 불안정합니다. 잠시 후 다시 시도해주세요.")
                    Toast.makeText(context, "onFailure: 서버 네트워크 불안정", Toast.LENGTH_LONG).show()

                    context.kakaoActivityIndicator.visibility = View.GONE
                    context.kakaoLoginTextView.visibility = View.VISIBLE
                } else {
                    Log.e(TAG, "알 수 없는 오류로 카카오로그인 실패 \n${errorResult?.errorMessage}")
                    Toast.makeText(
                        context,
                        "onFailure: ${errorResult?.errorMessage}",
                        Toast.LENGTH_LONG
                    ).show()

                    context.kakaoActivityIndicator.visibility = View.GONE
                    context.kakaoLoginTextView.visibility = View.VISIBLE
                }

            }

        })
    }

    override fun onSessionOpenFailed(exception: KakaoException?) {
        Log.e(TAG, "가입 실패: ${exception?.message}")
        Toast.makeText(context, "다시 한번 클릭해주세요", Toast.LENGTH_LONG).show()
        context.kakaoActivityIndicator.visibility = View.GONE
        context.kakaoLoginTextView.visibility = View.VISIBLE
    }


}