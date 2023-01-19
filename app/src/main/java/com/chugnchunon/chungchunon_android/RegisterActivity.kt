package com.chugnchunon.chungchunon_android

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.firebase.ui.auth.ui.ProgressView
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.checkerframework.checker.units.qual.Angle
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.timer

class RegisterActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityRegisterBinding.inflate(layoutInflater)
    }

    private val userDB = Firebase.firestore.collection("users")
    private val auth = Firebase.auth

    private var verificationId = ""

    private val calendar = Calendar.getInstance()
    private var birthYear = ""
    private var birthDay = ""
    private var userAge = 0

    lateinit var phoneTxtCheck: PhoneFillClass
    lateinit var totalTxtCheck: FillCheckClass

    private val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    var timer : Timer? = null
    var deltaTime = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var userType = intent.getStringExtra("userType")

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.authProgressBar.visibility = View.GONE
        binding.phoneAuthBtn.isEnabled = false
        // 변경
        binding.registerBtn.isEnabled = true

        totalTxtCheck = ViewModelProvider(this).get(FillCheckClass::class.java)
        phoneTxtCheck = ViewModelProvider(this).get(PhoneFillClass::class.java)


        totalTxtCheck.nameFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled = totalTxtCheck.nameFill.value == true &&
                    totalTxtCheck.genderFill.value == true &&
                    totalTxtCheck.birthFill.value == true &&
                    totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.genderFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled = totalTxtCheck.nameFill.value == true &&
                    totalTxtCheck.genderFill.value == true &&
                    totalTxtCheck.birthFill.value == true &&
                    totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.birthFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled = totalTxtCheck.nameFill.value == true &&
                    totalTxtCheck.genderFill.value == true &&
                    totalTxtCheck.birthFill.value == true &&
                    totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.phoneFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled = totalTxtCheck.nameFill.value == true &&
                    totalTxtCheck.genderFill.value == true &&
                    totalTxtCheck.birthFill.value == true &&
                    totalTxtCheck.phoneFill.value == true
        })

        phoneTxtCheck.phoneEditText1.observe(this, Observer { value ->
            Log.d("9일", "$value")
            binding.phoneAuthBtn.isEnabled =
                phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true
        })

        phoneTxtCheck.phoneEditText2.observe(this, Observer { value ->
            Log.d("9일", "$value")
            binding.phoneAuthBtn.isEnabled =
                phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true
        })


        // 이름 입력창
        binding.nameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                totalTxtCheck.nameFill.value = char!!.length != 0
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }

        })

        // 성별
       binding.genderInput.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.genderList,
            android.R.layout.simple_spinner_dropdown_item
        )


        binding.genderInput.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                totalTxtCheck.genderFill.value =  binding.genderInput.selectedItem != null
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // null
            }

        })

        // 생년월일 설정
        var editBirth: Boolean = false



        calendar.apply {
            set(1960, 0, 1)
        }

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        val birthDatePicker =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                if (editBirth) {
                    binding.birthResultBox.removeAllViews()
                }

                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
//                binding.birthLayout.removeView(binding.birthInput)
                var birthTextView = TextView(applicationContext)
                var birthScreenInput = ""
                birthScreenInput = "${year}년 ${monthOfYear + 1}월 ${dayOfMonth}일"
                birthYear = "$year"
                userAge = currentYear - birthYear.toInt() + 1
                birthDay = "${String.format("%02d", monthOfYear + 1)}${String.format("%02d", dayOfMonth)}"
                birthTextView.text = birthScreenInput

                val birthTextViewLayoutParams = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                birthTextViewLayoutParams.setMargins(0, 10, 0, 0)

                birthTextView.layoutParams = birthTextViewLayoutParams
                birthTextView.setTypeface(null, Typeface.BOLD)
                birthTextView.textSize = 20f
                birthTextView.setTextColor(ContextCompat.getColor(this, R.color.main_color))
                binding.birthResultBox.addView(birthTextView)

                // 나이 50세 이하
                var ageTextView = TextView(applicationContext)
                ageTextView.layoutParams = birthTextViewLayoutParams
                ageTextView.setTypeface(null, Typeface.BOLD)
                ageTextView.textSize = 20f
                ageTextView.setTextColor(ContextCompat.getColor(this, R.color.dark_main_color))
                ageTextView.text = "50세 이하는 일기 쓰기가 제한됩니다."

                if(userAge < 50) {
                    binding.birthResultBox.addView(ageTextView)
                }


                // 수정 버튼
                editBirth = true
                binding.birthInput.text = "수정하기"
                totalTxtCheck.birthFill.value = true
            }

        var datePickerResult = DatePickerDialog(
            this@RegisterActivity,
            android.R.style.Theme_Holo_Light_Dialog_MinWidth,
            birthDatePicker,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerResult.datePicker.spinnersShown = true
        datePickerResult.datePicker.calendarViewShown = false

        binding.birthInput.setOnClickListener {
            datePickerResult.show()
        }


        // 휴대폰 글자수 제한
        binding.errorMessage.visibility = View.GONE

        binding.phoneInput1.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //null
            }

            override fun onTextChanged(char: CharSequence?, start: Int, before: Int, count: Int) {

                if (char?.length == 4) {
                    binding.errorMessage.visibility = View.GONE
                    binding.phoneInput2.requestFocus()
                    phoneTxtCheck.phoneEditText1.value = true
                } else {
                    binding.errorMessage.visibility = View.VISIBLE
                    phoneTxtCheck.phoneEditText1.value = false
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }
        })

        binding.phoneInput2.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // null
                }
                override fun onTextChanged(
                    char: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (char?.length == 4) {
                        binding.errorMessage.visibility = View.GONE
                        phoneTxtCheck.phoneEditText2.value = true
                    } else {
                        binding.errorMessage.visibility = View.VISIBLE
                        phoneTxtCheck.phoneEditText2.value = false
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                    // null
                }
            })


        // 휴대폰 인증번호 받기
        binding.phoneAuthBtn.setOnClickListener {

            binding.phoneAuthLayout.removeView(binding.phoneAuthBtn)
            binding.authProgressBar.visibility = View.VISIBLE
            TimerFun()

            // 인증 입력
            var authEditTextView = EditText(applicationContext)
            authEditTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER)
            var drawableBox = ResourcesCompat.getDrawable(resources, R.drawable.mindbox, null)

            val params = RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.setMargins(0, 0, 15, 0)
            authEditTextView.layoutParams = params
            authEditTextView.background = drawableBox
            authEditTextView.hint = "인증번호 입력하기"
            authEditTextView.paddingLeft.plus(10)
            authEditTextView.paddingRight.plus(10)
            authEditTextView.width = 400
            authEditTextView.gravity = Gravity.CENTER

            // 인증 확인
            var authBtn = Button(applicationContext)
            authBtn.setBackgroundResource(R.drawable.default_button)
            authBtn.layoutParams = layoutParams
            authBtn.textSize = 17f
            authBtn.setTextColor(ContextCompat.getColor(this, R.color.white))
            authBtn.setTag("verificationBtn")
            authBtn.text = "확인"



            // 인증번호 입력창 -> 인증 확인 버튼 활성화
            authEditTextView.addTextChangedListener ( object :TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // null
                }

                override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    authBtn.isEnabled = char?.length != 0
                }

                override fun afterTextChanged(p0: Editable?) {
                    // null
                }

            } )

            binding.phoneAuthLayout.addView(authEditTextView)
            binding.phoneAuthLayout.addView(authBtn)

            var veriProgress = ProgressBar(applicationContext)
            veriProgress.layoutParams = layoutParams
            var colorResouce = ContextCompat.getColor(this, R.color.main_color)
            veriProgress.getIndeterminateDrawable().setColorFilter(colorResouce, PorterDuff.Mode.MULTIPLY)

            // 인증 확인 클릭
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    binding.phoneAuthLayout.removeView(veriProgress)
                    binding.phoneAuthLayout.addView(authBtn)
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

                    authBtn.setOnClickListener {
                        binding.authProgressBar.visibility = View.GONE
                        binding.phoneAuthLayout.removeView(authBtn)
                        binding.phoneAuthLayout.addView(veriProgress)

                        val credential = PhoneAuthProvider.getCredential(
                            verificationId,
                            authEditTextView.text.toString()
                        )
                        signInWithPhoneAuthCredential(credential)
                    }
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



            val phoneNumber =
                "010-${binding.phoneInput1.text.toString()}-${binding.phoneInput2.text.toString()}"

            val userId = Firebase.auth.currentUser?.uid

            var intent = Intent(this, RegionRegisterActivity::class.java)
            intent.putExtra("userType", userType)
            intent.putExtra("userAge", userAge)
            startActivity(intent)

            val userSet = hashMapOf(
                "userType" to userType,
                "loginType" to "일반",
                "userId" to userId,
                "timestamp" to FieldValue.serverTimestamp(),
                "name" to (binding.nameInput.text.toString()),
                "gender" to (binding.genderInput.selectedItem.toString()),
                "phone" to phoneNumber,
                "birthYear" to birthYear,
                "birthDay" to birthDay,
                "userAge" to userAge,
                "todayStepCount" to 0,
            )

            userDB
                .document("$userId")
                .set(userSet, SetOptions.merge())
                .addOnSuccessListener {
                    var intent = Intent(this, RegionRegisterActivity::class.java)
                    intent.putExtra("userType", userType)
                    intent.putExtra("userAge", userAge)
                    startActivity(intent)
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

    private fun TimerFun() {
        // 0.1초에 1%씩 증가, 시작 버튼 누른 후 3초 뒤 시작
        timer = timer(period = 100, initialDelay = 500) {
            if(deltaTime > 100) cancel()
            binding.authProgressBar.setProgress(++deltaTime)
        }
    }


    // 휴대폰 인증 확인
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        val resultparams = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        resultparams.setMargins(0, 15, 0, 0)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 인증 성공
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)
                    binding.authProgressBar.visibility = View.GONE

                    var successTextView = TextView(applicationContext)
                    successTextView.text = "인증에 성공했습니다."
                    successTextView.textSize = 20f
                    successTextView.gravity = Gravity.END
                    successTextView.layoutParams = resultparams
                    successTextView.setTextColor(ContextCompat.getColor(this, R.color.main_color))
                    successTextView.setTypeface(null, Typeface.BOLD)
                    binding.phoneLayout.addView(successTextView)
                    totalTxtCheck.phoneFill.value = true

                } else {
                    // 인증 실패
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)

                    var successTextView = TextView(applicationContext)
                    successTextView.text = "인증에 실패했습니다."
                    successTextView.textSize = 20f
                    successTextView.gravity = Gravity.END
                    successTextView.layoutParams = resultparams
                    successTextView.setTextColor(ContextCompat.getColor(this, R.color.dark_main_color))
                    successTextView.setTypeface(null, Typeface.BOLD)
                    binding.phoneLayout.addView(successTextView)
                    totalTxtCheck.phoneFill.value = false

                }
            }
    }
}


class FillCheckClass : ViewModel() {
    val nameFill by lazy { MutableLiveData<Boolean>(false) }
    val genderFill by lazy { MutableLiveData<Boolean>(false) }
    val birthFill by lazy { MutableLiveData<Boolean>(false) }
    val phoneFill by lazy { MutableLiveData<Boolean>(false) }
}

class PhoneFillClass : ViewModel() {
    val phoneEditText1 by lazy { MutableLiveData<Boolean>(false) }
    val phoneEditText2 by lazy { MutableLiveData<Boolean>(false) }
}