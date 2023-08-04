package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.PhoneAuthLogin.PhoneAuthLoginCallback
import com.chugnchunon.chungchunon_android.databinding.ActivityOriginLoginBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityRegisterBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
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
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class OriginLoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityOriginLoginBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val auth = Firebase.auth

    private var verificationId = ""

    private val calendar = Calendar.getInstance()
    private var birthYear = ""
    private var birthDay = ""
    private var userAge = 0
    private var progressInt = 0

    private var genderFillValue = "여성"

    lateinit var phoneTxtCheck: OriginPhoneFillViewModel
//    lateinit var totalTxtCheck: FillCheckClass

    private val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    lateinit var avatarUri: Uri

    lateinit var metrics: DisplayMetrics
    lateinit var authEditTextView: EditText

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.innerScrollView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, ev: MotionEvent?): Boolean {
                when (ev?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        return false
                    }
                    KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP -> {
                        val imm: InputMethodManager =
                            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                        return false
                    }
                }
                return false
            }
        })


        binding.authProgressBar.progress = 0
        binding.authProgressBar.visibility = View.GONE
        binding.phoneAuthBtn.isEnabled = false
        binding.registerBtn.isEnabled = false

        phoneTxtCheck = ViewModelProvider(this).get(OriginPhoneFillViewModel::class.java)

        phoneTxtCheck.phoneEditText1.observe(this, Observer { value ->
            if (phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true) {
                binding.phoneAuthBtn.isEnabled = true
            }
        })

        phoneTxtCheck.phoneEditText2.observe(this, Observer { value ->
            if (phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true) {
                binding.phoneAuthBtn.isEnabled = true
            }
        })

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


        // 회원가입 버튼 클릭
        binding.phoneAuthBtn.setOnClickListener {
            val authphone =
                "+8210${binding.phoneInput1.text}${binding.phoneInput2.text}"
//            binding.registerBtn.visibility = View.GONE
//            binding.registerProgressBar.visibility = View.VISIBLE

            binding.phoneAuthLayout.removeView(binding.phoneAuthBtn)
            binding.authProgressBar.visibility = View.VISIBLE
            TimerFun()

            val veriProgress = ProgressBar(applicationContext)
            veriProgress.layoutParams = layoutParams
            val colorResouce = ContextCompat.getColor(this, R.color.main_color)
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

                    val failTextView = TextView(applicationContext)
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
                        val tooManyTextView = TextView(applicationContext)
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
                    this@OriginLoginActivity.verificationId = verificationId

                    binding.phoneAuthLayout.removeAllViews()
                    binding.authProgressBar.visibility = View.GONE

                    // 인증 입력
                    authEditTextView = EditText(applicationContext)
                    authEditTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER)
                    val drawableBox =
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
                    authEditTextView.setTextSize(dpTextSize(13f))
                    authEditTextView.gravity = Gravity.CENTER

                    // 인증 확인
                    val params = RelativeLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    val authBtn = Button(applicationContext)
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

                        // user 확인
                        val credential = PhoneAuthProvider.getCredential(
                            verificationId,
                            authEditTextView.text.toString()
                        )

                        signInWithPhoneAuthCredential(credential)
                    }
                }
            }

            val optionCompat = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(authphone)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(optionCompat)
            auth.setLanguageCode("kr")


//            PhoneAuthLoginCallback().phoneAuthLoginCallBack(this, authphone)
        }
    }

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

                    // 인증 성공
                    binding.phoneLayout.removeView(binding.phoneAuthLayout)
                    binding.authProgressBar.visibility = View.GONE

                    val successTextView = TextView(applicationContext)
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

                    binding.registerBtn.isEnabled = true
                    binding.registerBtn.setOnClickListener {
                        binding.registerTextBar.visibility = View.GONE
                        binding.registerProgressBar.visibility = View.VISIBLE
                        val goDiaryTwoActivity = Intent(this, DiaryTwoActivity::class.java)
                        startActivity(goDiaryTwoActivity)
                    }
                } else {
                    // 인증 실패
                    val goWarning = Intent(this, DefaultDiaryWarningActivity::class.java)
                    goWarning.putExtra("warningType", "originLoginFail")
                    startActivity(goWarning)
                }
            }
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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        hideKeyBoard()
        return true
    }

    private fun hideKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    private fun dpTextSize(dp: Float): Float {
        metrics = applicationContext.resources.displayMetrics
        val fpixels = metrics.density * dp
        val pixels = fpixels * 0.5f
        return pixels
    }
}


class OriginPhoneFillViewModel : ViewModel() {
    val phoneEditText1 by lazy { MutableLiveData<Boolean>(false) }
    val phoneEditText2 by lazy { MutableLiveData<Boolean>(false) }
}
