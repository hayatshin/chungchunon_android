package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton

class ApplicationRuleActivity: AppCompatActivity() {

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_rule)

        var arrow = findViewById<AppCompatButton>(R.id.appRuleGoBack)
        arrow.setOnClickListener {
            finish()
        }

        var rulepart = findViewById<TextView>(R.id.ruleScroll)
        rulepart.movementMethod = ScrollingMovementMethod()

    }
}