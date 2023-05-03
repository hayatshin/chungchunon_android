package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StartActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userDB = Firebase.firestore.collection("users")
    private var lastBackPressTime: Long = 0L

    override fun onBackPressed() {
        super.onBackPressed()

        val currentTime = System.currentTimeMillis()
        val interval = 2000L

        if (currentTime - lastBackPressTime < interval) {
            super.onBackPressed()
            finishAffinity()
            finish()
        } else {
            lastBackPressTime = currentTime
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun onResume() {
        super.onResume()

        // 상태바 화이트
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser

            val userId = Firebase.auth.currentUser?.uid

            if (userId != null) {
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            // userDB에 있음
                            if (document.data!!.containsKey("smallRegion")) {
                                // 정상 기록
                                val goDiaryTwoActivity = Intent(this, DiaryTwoActivity::class.java)
                                startActivity(goDiaryTwoActivity)
                            } else {
                                // 정상기록 x

                                FirebaseAuth.getInstance().signOut()

                                userDB.document("$userId")
                                    .delete().addOnSuccessListener {
                                        // 정상 기록 x
                                        val intent = Intent(this, MainActivity::class.java)
                                        startActivity(intent)
                                    }
                            }

                        } else {

                            // userDB에 없음
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                    }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }


        }, 1000)
    }
}