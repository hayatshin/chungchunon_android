package com.chugnchunon.chungchunon_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityCommunityRegisterBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CommunityRegisterActivity : AppCompatActivity() {

    private var db = Firebase.firestore
    private var rootRef = FirebaseFirestore.getInstance()
    private val binding by lazy {
        ActivityCommunityRegisterBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val userId = intent.getLongExtra("userId", 0).toString()

        binding.communityInput.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.communityList,
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.registerBtn.setOnClickListener {

            var communityMap: Map<String, String> = mapOf(
                "community" to binding.communityInput.selectedItem.toString()
            )
            db.collection("users")
                .document(userId)
                .set(communityMap, SetOptions.merge())
                .addOnSuccessListener {
                    var goDiary = Intent(this, DiaryActivity::class.java)
                    startActivity(goDiary)
                }
                .addOnFailureListener { exception ->
                    Log.d("결과", "get failed with", exception)
                }
        }
    }
}

