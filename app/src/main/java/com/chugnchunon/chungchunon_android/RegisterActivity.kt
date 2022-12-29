package com.chugnchunon.chungchunon_android

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore

    private val calendar = Calendar.getInstance()
    private var birthDBInput = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        binding.genderInput.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.genderList,
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.communityInput.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.communityList,
            android.R.layout.simple_spinner_dropdown_item
        )

        // 생년월일 설정
        val birthDatePicker =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.birthLayout.removeView(binding.birthInput)
                var birthTextView = TextView(applicationContext)
                var birthScreenInput = ""
                birthScreenInput = "${year}년 ${monthOfYear}월 ${dayOfMonth}일"
                birthDBInput = "$year/$monthOfYear/$dayOfMonth"
                birthTextView.text = birthScreenInput

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                birthTextView.layoutParams = layoutParams
                birthTextView.gravity = Gravity.END
                binding.birthLayout.addView(birthTextView)
            }
        binding.birthInput.setOnClickListener {
            DatePickerDialog(
                this@RegisterActivity,
                birthDatePicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 회원가입 버튼 클릭
        binding.registerBtn.setOnClickListener {
            val phoneNumber =
                "010-${binding.phoneInput1.text.toString()}-${binding.phoneInput2.text.toString()}"

            val user = hashMapOf(
                "name" to (binding.nameInput.text.toString()),
                "gender" to (binding.genderInput.selectedItem.toString()),
                "phone" to phoneNumber,
                "birth" to (birthDBInput),
                "community" to (binding.communityInput.selectedItem.toString())

            )

            db.collection("users")
                .add(user)
                .addOnSuccessListener {
                    startActivity(Intent(this@RegisterActivity, DiaryActivity::class.java))
                }
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }

}


