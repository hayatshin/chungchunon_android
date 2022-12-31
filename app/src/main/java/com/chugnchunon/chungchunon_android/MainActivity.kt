package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethod
import android.widget.TextView
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.auth.Session
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        kakaoLogout()
        firebaseLogout()

        // 일반 자동 로그인
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null || AuthApiClient.instance.hasToken()) {
            Log.d("유저", currentUser.toString())
            val goDiaryActivity = Intent(this, DiaryActivity::class.java)
            startActivity(goDiaryActivity)
            finish()
        }

        // 카카오 로그인
        binding.kakaoLoginBtn.setOnClickListener {
            Log.d("카카오", "클린")
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null) {
                    Log.e("LOGIN", "카카오계정으로 로그인 실패", error)
                } else if (token != null) {
                    Log.i("LOGIN", "카카오계정으로 로그인 성공 ${token.accessToken}")
                    kakaoLoginSuccess()

                }
            }

            // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context = this)) {
                UserApiClient.instance.loginWithKakaoTalk(context = this) { token, error ->
                    if (error != null) {
                        Log.e("LOGIN", "카카오톡으로 로그인 실패", error)

                        // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                        // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            return@loginWithKakaoTalk
                        }

                        // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                        UserApiClient.instance.loginWithKakaoAccount(
                            context = this,
                            callback = callback
                        )
                    } else if (token != null) {
                        Log.i("LOGIN", "카카오톡으로 로그인 성공 ${token.accessToken}")

                        kakaoLoginSuccess()
//                        val intent = Intent(this, MainActivity::class.java)
//                        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
//                        finish()
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context = this, callback = callback)
            }
        }

        // 일반 회원가입
        binding.registerBtn.setOnClickListener {
            val goRegisterUser = Intent(this, RegisterActivity::class.java)
            startActivity(goRegisterUser)
        }
    }

    // 카카오 로그인 성공
    private fun kakaoLoginSuccess() {
        val current_time = LocalDateTime.now()


        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("카카오", "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.i("카카오", "사용자 정보 요청 성공: $user")

                val userSet = hashMapOf(
                    "loginType" to "kakao",
                    "userId" to (user.id),
                    "createdTime" to FieldValue.serverTimestamp(),
                    "name" to (user.kakaoAccount?.name),
                    "gender" to (user.kakaoAccount?.name),
                    "phone" to (user.kakaoAccount?.phoneNumber),
                    "birth" to ("${user.kakaoAccount?.birthyear}/${user.kakaoAccount?.birthday}"),
                )
                db.collection("users")
                    .document(user.id.toString())
                    .set(userSet)
                    .addOnSuccessListener {
                        var goDiary = Intent(this, CommunityRegisterActivity::class.java)
                        goDiary.putExtra("userId", user.id)
                        startActivity(goDiary)
                    }
            }
        }
    }

    // 토큰까지 삭제하는 카카오 로그아웃
    private fun kakaoLogout() {
        UserApiClient.instance.unlink { error ->
            if (error != null) {
                Log.e("Hello", "연결 끊기 실패", error)
            } else {
                Log.i("Hello", "연결 끊기 성공. SDK에서 토큰 삭제 됨")
            }
        }
    }

    // 파이어베이스 로그아웃
    private fun firebaseLogout() {
        FirebaseAuth.getInstance().signOut();
    }

}




