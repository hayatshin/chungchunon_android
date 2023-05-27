package com.chugnchunon.chungchunon_android.MoneyActivity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.PhoneFillClass
import com.chugnchunon.chungchunon_android.databinding.ActivityMoneyFullBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*

class MoneyFullActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMoneyFullBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid
    private var userName = ""
    private var userRegion = ""
    private var userSmallRegion = ""

    lateinit var phoneTxtCheck: PhoneFillClass

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var enrollOrNot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Handler().postDelayed({
            val goDiaryTwo = Intent(this, DiaryTwoActivity::class.java)
            startActivity(goDiaryTwo)
        }, 2000)
    }
}

