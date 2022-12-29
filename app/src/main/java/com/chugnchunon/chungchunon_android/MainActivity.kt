package com.chugnchunon.chungchunon_android

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private val registerBtn: TextView by lazy {
        findViewById(R.id.registerBtn)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        registerBtn.setOnClickListener {
            val goRegisterUser = Intent(this, RegisterActivity::class.java)
            startActivity(goRegisterUser)
        }
    }
}