package com.chugnchunon.chungchunon_android

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.DiaryFillClass
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityBlockBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityLockDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*


class LockDiaryActivity : FragmentActivity() {

    private val binding by lazy {
        ActivityLockDiaryBinding.inflate(layoutInflater)
    }

    companion object {
        var secretOrNot = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setStatusBarColor(Color.parseColor("#CC000000"));

        binding.secretCancelBox.setOnClickListener {
            finish()
        }


        if (secretOrNot) {

            binding.secreteNotificationText.text =
                "${this.getString(R.string.secret_unhide_notification)}"

        } else {

            binding.secreteNotificationText.text =
                "${this.getString(R.string.secret_hide_notification)}"

        }

        binding.secretConfirmBox.setOnClickListener {
            secretOrNot = !secretOrNot

            var intent = Intent(this, MyDiaryFragment::class.java)
            intent.setAction("SECRET_OR_NOT")
            intent.putExtra("secretOrNot", secretOrNot)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            finish()
        }

    }
}

