package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chugnchunon.chungchunon_android.KakaoLogin.PartnerSessionCallback
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.auth.AuthType
import com.kakao.auth.Session
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val auth = FirebaseAuth.getInstance()
    private val userDB = Firebase.firestore.collection("users")
    private lateinit var callback: SessionCallback
    private lateinit var partnerCallback: PartnerSessionCallback

    var timer : Timer? = null
    var deltaTime = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = getColor(R.color.main_color)
        }

        callback = SessionCallback(this) // Initialize Session
        partnerCallback = PartnerSessionCallback(this)


        // 카톡 로그인 버튼 클릭
        binding.kakaoLoginBtn.setOnClickListener {
//            TimerFun()


            val keyHash = Utility.getKeyHash(this) // keyHash 발급
            Log.d("키", "${keyHash}")

            Session.getCurrentSession().addCallback(callback)
            Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, this)
        }

        // 일반 회원가입
        binding.registerBtn.setOnClickListener {
            val goRegisterUser = Intent(this, AgreementActivity::class.java)
            goRegisterUser.putExtra("userType", "치매예방자")
            startActivity(goRegisterUser)
        }


        // 파트너 회원가입
        binding.partnerBtn.setOnClickListener {
            binding.loginLayout.visibility = View.GONE
            binding.partnerLayout.visibility = View.VISIBLE

            // 파트너 - 카톡 로그인 버튼 클릭
            binding.partnerKakaoLoginBtn.setOnClickListener {
//                TimerFun()

                val keyHash = Utility.getKeyHash(this) // keyHash 발급

                Session.getCurrentSession().addCallback(partnerCallback)
                Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, this)
            }

            // 파트너 - 일반 회원가입
            binding.partnerRegisterBtn.setOnClickListener {

                val goRegisterUser = Intent(this, AgreementActivity::class.java)
                goRegisterUser.putExtra("userType", "파트너")
                startActivity(goRegisterUser)
            }

            binding.partnerBack.setOnClickListener {
                binding.loginLayout.visibility = View.VISIBLE
                binding.partnerLayout.visibility = View.GONE
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("카톡로그인", "LoginActivity - onActivityResult() called")

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            if(resultCode == -1) {
                // null
            }
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
        val url = "http://4fd9-218-147-138-163.ngrok.io/verifyToken"
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

//    fun TimerFun() {
//        // 0.1초에 1%씩 증가, 시작 버튼 누른 후 3초 뒤 시작
//        timer = timer(period = 50, initialDelay = 500) {
//            if(deltaTime > 100) cancel()
//            binding.kakaoProgressBar.setProgress(++deltaTime)
//        }
//    }

}




