package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PersonalInfoRuleActivity: AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personalinfo_rule)

        var arrow = findViewById<ImageView>(R.id.personalGoBack)
        arrow.setOnClickListener {
            finish()
        }

        var rulepart = findViewById<TextView>(R.id.personalScroll)
        rulepart.movementMethod = ScrollingMovementMethod()
    }
}