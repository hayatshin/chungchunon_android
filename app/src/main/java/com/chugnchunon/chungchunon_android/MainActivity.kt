package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.kakao.auth.AuthType
import com.kakao.auth.Session
import com.kakao.sdk.common.util.Utility
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val auth = FirebaseAuth.getInstance()
    private lateinit var callback: SessionCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = getColor(R.color.main_color)
        }

        callback = SessionCallback(this) // Initialize Session

        // 일반 자동 로그인
        val currentUser = auth.currentUser
        Log.d("로그인", "$currentUser")

        if (currentUser != null) {
            val goDiaryActivity = Intent(this, DiaryActivity::class.java)
            startActivity(goDiaryActivity)
            finish()
        }

        // 카톡 로그인 버튼 클릭
        binding.kakaoLoginBtn.setOnClickListener {
            Log.d("카톡로그인", "클릭")
            val keyHash = Utility.getKeyHash(this) // keyHash 발급

            Session.getCurrentSession().addCallback(callback)
            Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, this)
        }

        // 일반 회원가입
        binding.registerBtn.setOnClickListener {
            val goRegisterUser = Intent(this, RegisterActivity::class.java)
            startActivity(goRegisterUser)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("카톡로그인", "LoginActivity - onActivityResult() called")

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            Log.i("카톡로그인", "Session get current session")
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        Session.getCurrentSession().removeCallback(callback)
    }


    // FireAuth 로그아웃
    private fun firebaseLogout() {
        FirebaseAuth.getInstance().signOut();
    }


    open fun getFirebaseJwt(kakaoAccessToken: String): Task<String>? {
        val source = TaskCompletionSource<String>()
        val queue = Volley.newRequestQueue(this)
        val url = "http://71fa-218-147-138-163.ngrok.io/verifyToken"
        val validationObject: HashMap<String?, String?> = HashMap()
        validationObject["token"] = kakaoAccessToken
        val request: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, JSONObject(validationObject as Map<*, *>?),
            Response.Listener { response ->
                try {
                    val firebaseToken = response.getString("firebase_token")
                    source.setResult(firebaseToken)
                } catch (e: Exception) {
                    source.setException(e)
                }
            },
            Response.ErrorListener { error ->
                Log.e("토큰토큰", error.toString())
                source.setException(error)
            }) {
            override fun getParams(): Map<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["token"] = kakaoAccessToken
                return params
            }
        }
        queue.add(request)
        return source.task
    }

}




