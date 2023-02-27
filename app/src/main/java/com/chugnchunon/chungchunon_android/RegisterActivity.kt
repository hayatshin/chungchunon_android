package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chugnchunon.chungchunon_android.Partner.PartnerDiaryTwoActivity
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.util.*
import java.util.concurrent.TimeUnit


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
    private var progressInt = 0

    lateinit var phoneTxtCheck: PhoneFillClass
    lateinit var totalTxtCheck: FillCheckClass

    private val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    var timer: Timer? = null
    var deltaTime = 0

    lateinit var authEditTextView: EditText
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var avatarUri: Uri

    private val REQUEST_PHOTO_PERMISSION = 300
    lateinit var metrics : DisplayMetrics

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        openGalleryForImages()

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        var userType = intent.getStringExtra("userType")

//        binding.nameInput.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        binding.innerScrollView.setOnTouchListener(OnTouchListener { v, event ->
            if (event != null && event.action == MotionEvent.ACTION_MOVE) {
                val imm = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                val isKeyboardUp = imm.isAcceptingText
                if (isKeyboardUp) {
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
            false
        })

        binding.authProgressBar.progress = 0

        binding.authProgressBar.visibility = View.GONE
        binding.phoneAuthBtn.isEnabled = false
        // 변경
        binding.registerBtn.isEnabled = true

        totalTxtCheck = ViewModelProvider(this).get(FillCheckClass::class.java)
        phoneTxtCheck = ViewModelProvider(this).get(PhoneFillClass::class.java)


        totalTxtCheck.avatarFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled =
                totalTxtCheck.avatarFill.value == true && totalTxtCheck.nameFill.value == true &&
                        totalTxtCheck.genderFill.value == true &&
                        totalTxtCheck.birthFill.value == true &&
                        totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.nameFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled =
                totalTxtCheck.avatarFill.value == true && totalTxtCheck.nameFill.value == true &&
                        totalTxtCheck.genderFill.value == true &&
                        totalTxtCheck.birthFill.value == true &&
                        totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.genderFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled =
                totalTxtCheck.avatarFill.value == true && totalTxtCheck.nameFill.value == true &&
                        totalTxtCheck.genderFill.value == true &&
                        totalTxtCheck.birthFill.value == true &&
                        totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.birthFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled =
                totalTxtCheck.avatarFill.value == true && totalTxtCheck.nameFill.value == true &&
                        totalTxtCheck.genderFill.value == true &&
                        totalTxtCheck.birthFill.value == true &&
                        totalTxtCheck.phoneFill.value == true
        })

        totalTxtCheck.phoneFill.observe(this, Observer { value ->
            binding.registerBtn.isEnabled =
                totalTxtCheck.avatarFill.value == true && totalTxtCheck.nameFill.value == true &&
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


        // 사진 업로드
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    avatarUri = it.data?.data!!
                    binding.avatarImage.setImageURI(avatarUri)
                    totalTxtCheck.avatarFill.value = true
                }
            }


        binding.avatarButton.setOnClickListener {
            // 권한 질문

            val readPermission =
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            if (readPermission == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PHOTO_PERMISSION
                )
            } else {
                // 권한 수락
                openGalleryForImages()
            }
        }

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
            R.layout.item_spinner_gender
        )


        binding.genderInput.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                totalTxtCheck.genderFill.value = binding.genderInput.selectedItem != null
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
                birthDay =
                    "${String.format("%02d", monthOfYear + 1)}${String.format("%02d", dayOfMonth)}"
                birthTextView.text = birthScreenInput

                val birthTextViewLayoutParams = RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                birthTextViewLayoutParams.setMargins(0, 10, 0, 0)

                birthTextView.layoutParams = birthTextViewLayoutParams
                birthTextView.setTypeface(null, Typeface.BOLD)
                birthTextView.setTextSize(dpTextSize(12f))
                birthTextView.setTextColor(ContextCompat.getColor(this, R.color.main_color))
                binding.birthResultBox.addView(birthTextView)

                // 나이 50세 이하
                var ageTextView = TextView(applicationContext)
                ageTextView.layoutParams = birthTextViewLayoutParams
                ageTextView.setTextColor(ContextCompat.getColor(this, R.color.light_gray))
                ageTextView.text = "50세 미만은 일기 쓰기가 제한됩니다."
                ageTextView.setTextSize(dpTextSize(10f))


                if (userAge < 50) {
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


            var veriProgress = ProgressBar(applicationContext)
            veriProgress.layoutParams = layoutParams
            var colorResouce = ContextCompat.getColor(this, R.color.main_color)
            veriProgress.getIndeterminateDrawable()
                .setColorFilter(colorResouce, PorterDuff.Mode.MULTIPLY)

            val resultparams = RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            resultparams.setMargins(0, 15, 0, 0)


            // 인증 확인 클릭
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {


                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("로그인", "성공")
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    Log.d("로그인 중복", "$exception")

                    binding.authProgressBar.progress = 0
                    binding.authProgressBar.visibility = View.GONE
                    binding.phoneAuthLayout.addView(binding.phoneAuthBtn)

                    var failTextView = TextView(applicationContext)
                    failTextView.text = "인증에 실패했습니다."
                    failTextView.setTextSize(dpTextSize(12f))
                    failTextView.gravity = Gravity.END
                    failTextView.layoutParams = resultparams
                    failTextView.setTextColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.main_color
                        )
                    )
                    failTextView.setTypeface(null, Typeface.BOLD)
                    binding.phoneLayout.addView(failTextView)

                    if (exception is FirebaseTooManyRequestsException) {
                        var tooManyTextView = TextView(applicationContext)
                        tooManyTextView.text = "인증요청 제한 횟수를 초과했습니다.\n추후에 다시 시도해주세요."
                        tooManyTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
                        tooManyTextView.gravity = Gravity.END
                        tooManyTextView.layoutParams = resultparams
                        tooManyTextView.setTextColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.dark_main_color
                            )
                        )
                        binding.phoneLayout.addView(tooManyTextView)
                    }

                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    super.onCodeSent(verificationId, token)
                    this@RegisterActivity.verificationId = verificationId


                    binding.phoneAuthLayout.removeAllViews()
                    binding.authProgressBar.visibility = View.GONE

                    // 인증 입력
                    authEditTextView = EditText(applicationContext)
                    authEditTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER)
                    var drawableBox =
                        ResourcesCompat.getDrawable(resources, R.drawable.register_fill_box, null)

                    val authParams = RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    authParams.setMargins(0, 0, 20, 0)
                    authEditTextView.layoutParams = authParams
                    authEditTextView.setPadding(25, 25, 25, 25)
                    authEditTextView.background = drawableBox
                    authEditTextView.hint = "인증번호 입력하기"
                    authEditTextView.setTextSize(dpTextSize(10f))
                    authEditTextView.gravity = Gravity.CENTER

                    // 인증 확인

                    val params = RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    var authBtn = Button(applicationContext)
                    authBtn.setBackgroundResource(R.drawable.sub_button)
                    authBtn.layoutParams = params
                    authBtn.setTextSize(dpTextSize(11f))
                    authBtn.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                    authBtn.setTag("verificationBtn")
                    authBtn.text = "확인"

                    binding.phoneAuthLayout.removeView(binding.phoneAuthBtn)
                    binding.phoneAuthLayout.addView(authEditTextView)
                    binding.phoneAuthLayout.addView(authBtn)


                    // 인증번호 입력창 -> 인증 확인 버튼 활성화
                    authEditTextView.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(
                            p0: CharSequence?,
                            p1: Int,
                            p2: Int,
                            p3: Int
                        ) {
                            // null
                        }

                        override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                            authBtn.isEnabled = char?.length != 0
                        }

                        override fun afterTextChanged(p0: Editable?) {
                            // null
                        }

                    })


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


            val optionCompat = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(authphone)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(optionCompat)
            auth.setLanguageCode("kr")
        }


        // 회원가입 버튼 클릭
        binding.registerBtn.setOnClickListener {

            if(avatarUri != null) {
                val fileName = UUID.randomUUID().toString()+".jpg"
                val database = FirebaseDatabase.getInstance()
                val refStorage = FirebaseStorage.getInstance().reference.child("avatars/$fileName")

                refStorage.putFile(avatarUri)
                    .addOnSuccessListener(
                        OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                                val imageUrl = it.toString()
                                val phoneNumber =
                                    "010-${binding.phoneInput1.text.toString()}-${binding.phoneInput2.text.toString()}"

                                val userId = Firebase.auth.currentUser?.uid
                                val updateUserType = if (userType == "사용자" && userAge < 50) "파트너"
                                else if (userType == "사용자" && userAge >= 50) "사용자"
                                else "파트너"

                                val userSet = hashMapOf(
                                    "userType" to updateUserType,
                                    "loginType" to "일반",
                                    "userId" to userId,
                                    "timestamp" to FieldValue.serverTimestamp(),
                                    "name" to (binding.nameInput.text.toString()),
                                    "avatar" to imageUrl,
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
                                        intent.putExtra("userType", updateUserType)
                                        intent.putExtra("userAge", userAge)
                                        startActivity(intent)
                                    }
                                    .addOnFailureListener { error ->
                                        Log.d("회원가입 실패", "$error")
                                    }
                            }
                        }
                    )
                    ?.addOnFailureListener(OnFailureListener { e ->
                        print(e.message)
                    })
            }

        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        hideKeyBoard()
        return true
    }

    private fun hideKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun TimerFun() {
        binding.authProgressBar.progress = 0

        object : CountDownTimer(9000L, 50L) {
            override fun onTick(p0: Long) {
                progressInt = 100 - ((p0.toFloat() / 9000) * 100f).toInt()
                binding.authProgressBar.setProgress(progressInt)
            }

            override fun onFinish() {
                binding.authProgressBar.progress = 100
            }
        }.start()

    }


    private fun openGalleryForImages() {
        var intent = Intent(Intent.ACTION_PICK)
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        intent.type = "image/*"

        activityResultLauncher.launch(intent)
    }


    // 휴대폰 인증 확인
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        val phoneResultparams = RelativeLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        phoneResultparams.setMargins(0, 20, 0, 0)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = Firebase.auth.currentUser?.uid

                    userDB.document("$userId").get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                var document = task.result
                                if (document.exists()) {
                                    var userAge =
                                        (document.data?.getValue("userAge") as Long).toInt()
                                    var userType = document.data?.getValue("userType")

                                    if (userType == "마스터" || (userAge >= 50 && userType == "사용자")) {
                                        var goDiary =
                                            Intent(applicationContext, DiaryTwoActivity::class.java)
                                        startActivity(goDiary)
                                    } else if (userType == "파트너" || (userAge < 50 && userType == "사용자")) {
                                        var goDiary = Intent(
                                            applicationContext,
                                            PartnerDiaryTwoActivity::class.java
                                        )
                                        startActivity(goDiary)
                                    }

                                } else {
                                    // 인증 성공
                                    binding.phoneLayout.removeView(binding.phoneAuthLayout)
                                    binding.authProgressBar.visibility = View.GONE

                                    var successTextView = TextView(applicationContext)
                                    successTextView.text = "인증에 성공했습니다."
                                    successTextView.setTextSize(dpTextSize(12f))
                                    successTextView.gravity = Gravity.END
                                    successTextView.layoutParams = phoneResultparams
                                    successTextView.setTextColor(
                                        ContextCompat.getColor(
                                            this,
                                            R.color.main_color
                                        )
                                    )
                                    successTextView.setTypeface(null, Typeface.BOLD)
                                    binding.phoneLayout.addView(successTextView)
                                    totalTxtCheck.phoneFill.value = true
                                }
                            }
                        }
                } else {
                    // 인증 실패
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)

                    var successTextView = TextView(applicationContext)
                    successTextView.text = "인증에 실패했습니다."
                    successTextView.setTextSize(dpTextSize(12f))
                    successTextView.gravity = Gravity.END
                    successTextView.layoutParams = phoneResultparams
                    successTextView.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.dark_main_color
                        )
                    )
                    successTextView.setTypeface(null, Typeface.BOLD)
                    binding.phoneLayout.addView(successTextView)
                    totalTxtCheck.phoneFill.value = false

                }
            }
    }



    private fun dpTextSize(dp: Float) : Float {
        metrics = applicationContext.resources.displayMetrics
        var fpixels = metrics.density * dp
        var pixels = fpixels * 0.5f
        return pixels
    }
}


class FillCheckClass : ViewModel() {
    val avatarFill by lazy { MutableLiveData<Boolean>(false) }
    val nameFill by lazy { MutableLiveData<Boolean>(false) }
    val genderFill by lazy { MutableLiveData<Boolean>(false) }
    val birthFill by lazy { MutableLiveData<Boolean>(false) }
    val phoneFill by lazy { MutableLiveData<Boolean>(false) }
}

class PhoneFillClass : ViewModel() {
    val phoneEditText1 by lazy { MutableLiveData<Boolean>(false) }
    val phoneEditText2 by lazy { MutableLiveData<Boolean>(false) }
}
