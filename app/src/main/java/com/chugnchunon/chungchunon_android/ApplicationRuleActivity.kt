package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ApplicationRuleActivity: AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_rule)

        var arrow = findViewById<ImageView>(R.id.appRuleGoBack)
        arrow.setOnClickListener {
            finish()
        }

        var rulepart = findViewById<TextView>(R.id.ruleScroll)
        rulepart.movementMethod = ScrollingMovementMethod()

    }
}