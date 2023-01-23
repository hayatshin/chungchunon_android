package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.view.WindowManager
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StartActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userDB = Firebase.firestore.collection("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        Handler(Looper.getMainLooper()).postDelayed({


            // 일반 자동 로그인
            val currentUser = auth.currentUser
            Log.d("로그인", "$currentUser")

            if (currentUser != null) {
                val userId = Firebase.auth.currentUser?.uid
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var userType = document.data?.getValue("userType")
                        if(userType == "치매예방자" || userType == "마스터") {
                            val goDiaryActivity = Intent(this, DiaryActivity::class.java)
                            startActivity(goDiaryActivity)
                            finish()
                        } else if (userType == "파트너") {
                            val goPartnerDiaryActivity = Intent(this, PartnerDiaryActivity::class.java)
                            startActivity(goPartnerDiaryActivity)
                            finish()
                        }
                    }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

        }, 0)
    }
}