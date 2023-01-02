package com.chugnchunon.chungchunon_android

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterBinding.inflate(layoutInflater)
    }

    private val userDB = Firebase.firestore.collection("users")
    private val auth = Firebase.auth
    private lateinit var userId: String

    private var verificationId = ""

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
                birthScreenInput = "${year}년 ${monthOfYear+1}월 ${dayOfMonth}일"
                birthDBInput = "$year/${monthOfYear+1}/$dayOfMonth"
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

        // 휴대폰 인증 보내기

        binding.phoneAuthBtn.setOnClickListener {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                var authEditTextView = EditText(applicationContext)

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("인증번호", "성공")
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    Log.d("인증번호", "실호")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    this@RegisterActivity.verificationId = verificationId
                    binding.phoneAuthLayout.removeView(binding.phoneAuthBtn)

                    authEditTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER)
                    authEditTextView.hint = "인증번호 입력하기"

                    var authBtn = TextView(applicationContext)
                    authBtn.setTag("verificationBtn")
                    authBtn.text = "확인"
                    authBtn.setOnClickListener {
                        val credential = PhoneAuthProvider.getCredential(
                            verificationId,
                            authEditTextView.text.toString()
                        )
                        signInWithPhoneAuthCredential(credential)
                    }

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    authEditTextView.layoutParams = layoutParams
                    authBtn.layoutParams = layoutParams
                    binding.phoneAuthLayout.addView(authEditTextView)
                    binding.phoneAuthLayout.addView(authBtn)

                }
            }

            val authphone =
                "+82 10${binding.phoneInput1.text.toString()}-${binding.phoneInput2.text.toString()}"

            Log.d("번호", authphone)

            val optionCompat = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(authphone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(optionCompat)
            auth.setLanguageCode("kr")
        }


        // 회원가입 버튼 클릭
        binding.registerBtn.setOnClickListener {

            userId = Firebase.auth.currentUser?.uid.toString()

            val phoneNumber =
                "010-${binding.phoneInput1.text.toString()}-${binding.phoneInput2.text.toString()}"

            val userSet = hashMapOf(
                "loginType" to "app",
                "userId" to userId,
                "createdTime" to FieldValue.serverTimestamp(),
                "name" to (binding.nameInput.text.toString()),
                "gender" to (binding.genderInput.selectedItem.toString()),
                "phone" to phoneNumber,
                "birth" to (birthDBInput),
                "community" to (binding.communityInput.selectedItem.toString())
            )

            userDB
                .document(userId)
                .set(userSet, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("파이어베이스2", "$userId")
                    var goDiary = Intent(this, DiaryActivity::class.java)
                    startActivity(goDiary)
                }
                .addOnFailureListener { error ->
                    Log.d("회원가입 실패", "$error")
                }
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }


    // 휴대폰 인증 확인

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 인증 성공
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)

                    var successTextView = TextView(applicationContext)
                    successTextView.text = "인증에 성공했습니다."
                    binding.phoneLayout.addView(successTextView)

                } else {
                    // 인증 실패
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)

                    var successTextView = TextView(applicationContext)
                    successTextView.text = "인증에 실패했습니다."
                    binding.phoneLayout.addView(successTextView)
                }
            }
    }

}




