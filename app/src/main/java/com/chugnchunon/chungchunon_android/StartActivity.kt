package com.chugnchunon.chungchunon_android

import android.animation.ObjectAnimator
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.icu.text.AlphabeticIndex
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.algolia.search.client.ClientSearch
import com.algolia.search.model.APIKey
import com.algolia.search.model.ApplicationID
import com.algolia.search.model.IndexName
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.AllRegionDataFragment
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryTwoActivity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_region_data.view.*

class StartActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val userDB = Firebase.firestore.collection("users")
    lateinit var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE: Int = 200
    private var lastBackPressTime: Long = 0

    override fun onBackPressed() {
        super.onBackPressed()

        val currentTime = System.currentTimeMillis()
        val interval = 2000 // Define the time interval between two back presses

        if (currentTime - lastBackPressTime < interval) {
            // If the current back press is within the time interval,
            // close the app
            super.onBackPressed()
            finishAffinity()
            finish()
        } else {
            // If the current back press is not within the time interval,
            // update the timestamp of the last back press and show a toast
            lastBackPressTime = currentTime
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)


    }

    override fun onResume() {
        super.onResume()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // 일반 자동 로그인
            val currentUser = auth.currentUser

            if (currentUser != null) {
                val userId = Firebase.auth.currentUser?.uid
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var userType = document.data?.getValue("userType")
                        Log.d("userType", "$userType")

                        val goDiaryTwoActivity = Intent(this, DiaryTwoActivity::class.java)
                        startActivity(goDiaryTwoActivity)

//                        if(userType == "사용자" || userType == "마스터") {
//                            val goDiaryTwoActivity = Intent(this, DiaryTwoActivity::class.java)
//                            startActivity(goDiaryTwoActivity)
//                        } else if (userType == "파트너") {
//                            val goPartnerDiaryTwoActivity = Intent(this, PartnerDiaryTwoActivity::class.java)
//                            startActivity(goPartnerDiaryTwoActivity)
//                        }
                    }
            } else {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

        }, 1000)
    }
}