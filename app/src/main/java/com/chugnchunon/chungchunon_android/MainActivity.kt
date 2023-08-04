package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.chugnchunon.chungchunon_android.KakaoLogin.PartnerSessionCallback
import com.chugnchunon.chungchunon_android.KakaoLogin.SessionCallback
import com.chugnchunon.chungchunon_android.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.kakao.auth.AuthType
import com.kakao.auth.Session
import com.kakao.sdk.common.util.Utility
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var callback: SessionCallback
    private lateinit var partnerCallback: PartnerSessionCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, ex -> ex.printStackTrace() }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // 오늘도청춘 아이콘 애니메이션
        onlccIconUpAnimation()

        // 상태바 화이트
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        // 카카오로그인 세션 콜백
        callback = SessionCallback(this)
        partnerCallback = PartnerSessionCallback(this)

        // 카톡 로그인 버튼 클릭
        binding.kakaoLoginBtn.setOnClickListener {
            binding.kakaoLoginTextView.visibility = View.GONE
            binding.kakaoActivityIndicator.visibility = View.VISIBLE

            val keyHash = Utility.getKeyHash(this) // keyHash 발급

            Session.getCurrentSession().addCallback(callback)
            Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, this)
        }

        var registerShown: Boolean = false

        // 일반 회원가입
        binding.registerDetailLayout.alpha = 0f
        binding.registerDetailLayout.y = -5f
        binding.registerDetailLayout.visibility = View.GONE

        binding.registerBtn.setOnClickListener {
            if(!registerShown) {
                binding.registerDetailLayout.visibility = View.VISIBLE

                Handler().postDelayed({
                    binding.registerDetailLayout.animate()
                        .translationY(0f)
                        .setInterpolator(BounceInterpolator())
                        .setDuration(500)
                }, 200)

                binding.registerDetailLayout.animate()
                    .alpha(1f)
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(600)

            } else {

                Handler().postDelayed({
                    binding.registerDetailLayout.animate()
                        .translationY(-5f)
                        .setInterpolator(BounceInterpolator())
                        .setDuration(500)
                }, 200)

                binding.registerDetailLayout.animate()
                    .alpha(0f)
                    .setInterpolator(AccelerateInterpolator())
                    .setDuration(500)

                Handler().postDelayed({
                    binding.registerDetailLayout.visibility = View.GONE
                }, 500)
            }

            registerShown = !registerShown
        }

        binding.regularLoginProcess.setOnClickListener {
            val goOriginLogin = Intent(this, OriginLoginActivity::class.java)
            startActivity(goOriginLogin)
        }

        binding.regularRegisterProcess.setOnClickListener {
            val goRegisterUser = Intent(this, AgreementActivity::class.java)
            goRegisterUser.putExtra("userType", "사용자")
            startActivity(goRegisterUser)
        }


        // 파트너 회원가입
        binding.partnerBtn.setOnClickListener {
            binding.loginLayout.visibility = View.GONE
            binding.partnerLayout.visibility = View.VISIBLE
            binding.partnerLayout.alpha = 0f

            var upAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_enter)
            binding.partnerLayout.startAnimation(upAnimation)

            binding.partnerLayout.animate()
                .alpha(1f)
                .setDuration(500)

            // 파트너 - 카톡 로그인 버튼 클릭
            binding.partnerKakaoLoginBtn.setOnClickListener {

                val goRegisterUser = Intent(this, DefaultDiaryWarningActivity::class.java)
                goRegisterUser.putExtra("warningType", "partnerRegister")
                startActivity(goRegisterUser)

//                binding.partnerKakaoTextView.visibility = View.GONE
//                binding.partnerKakaoAcitivityIndicator.visibility = View.VISIBLE
//
//                val keyHash = Utility.getKeyHash(this) // keyHash 발급
//
//                Session.getCurrentSession().addCallback(partnerCallback)
//                Session.getCurrentSession().open(AuthType.KAKAO_LOGIN_ALL, this)
            }

            // 파트너 - 일반 회원가입
            binding.partnerRegisterBtn.setOnClickListener {

                val goRegisterUser = Intent(this, DefaultDiaryWarningActivity::class.java)
                goRegisterUser.putExtra("warningType", "partnerRegister")
                startActivity(goRegisterUser)
            }

            binding.partnerBack.setOnClickListener {

                Handler().postDelayed({
                    binding.loginLayout.visibility = View.VISIBLE
                    binding.partnerLayout.visibility = View.GONE
                }, 500)


                val downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
                binding.partnerLayout.startAnimation(downAnimation)

                binding.partnerLayout.animate()
                    .alpha(0f)
                    .setDuration(500)
            }

            binding.partnerBackArrow.setOnClickListener {

                Handler().postDelayed({
                    binding.loginLayout.visibility = View.VISIBLE
                    binding.partnerLayout.visibility = View.GONE

                }, 500)


                var downAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_enter)
                binding.partnerLayout.startAnimation(downAnimation)

                binding.partnerLayout.animate()
                    .alpha(0f)
                    .setDuration(500)
            }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
            if(resultCode == -1) {
                // 세션 요청이 안 됐을 경우
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        Session.getCurrentSession().removeCallback(callback)
    }

    override fun onResume() {
        super.onResume()
        onlccIconUpAnimation()
    }

    private fun onlccIconUpAnimation() {
        binding.iconImage.y = 100f
        binding.iconImage.animate()
            .translationY(0f)
            .setDuration(700)

        binding.iconImage.alpha = 0f
        binding.iconImage.animate()
            .alpha(1f)
            .setDuration(1000)
    }

    // FireAuth 로그아웃
    private fun firebaseLogout() {
        FirebaseAuth.getInstance().signOut();
    }

    open fun getFirebaseJwt(kakaoAccessToken: String): Task<String>? {
        val source = TaskCompletionSource<String>()
        val queue = Volley.newRequestQueue(this)
        val url = "https://ochungchun-kakaologin.herokuapp.com/verifyToken"
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




