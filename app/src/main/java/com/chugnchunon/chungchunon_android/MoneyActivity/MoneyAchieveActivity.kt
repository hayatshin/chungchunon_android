package com.chugnchunon.chungchunon_android.MoneyActivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.ActivityMoneyAchieveBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class MoneyAchieveActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMoneyAchieveBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid
    private var userName = ""
    private var userRegion = ""
    private var userSmallRegion = ""

    lateinit var phoneTxtCheck: PhoneFillClass

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var enrollOrNot: Boolean = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        hideKeyBoard()
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        phoneTxtCheck = ViewModelProvider(this).get(PhoneFillClass::class.java)

        binding.achieveFinishText.visibility = View.GONE
        binding.achieveFullBox.visibility = View.VISIBLE
        binding.errorMessage.visibility = View.GONE

        binding.achieveScrollBox.setOnTouchListener(object : View.OnTouchListener {
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


        uiScope.launch(Dispatchers.IO) {
            launch { enrollCheck() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    if (enrollOrNot) {
                        // 이미 등록
                        alreadyAchieve()
                    } else {
                        nowAchieve()
                    }
                }
            }
        }

    }

    class PhoneFillClass : ViewModel() {
        val phoneEditText1 by lazy { MutableLiveData<Boolean>(false) }
        val phoneEditText2 by lazy { MutableLiveData<Boolean>(false) }
    }


    private fun hideKeyBoard()  {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    suspend fun enrollCheck() {
        val document = db.collection("manwon").document("$userId").get().await()
        if (document != null) {
            enrollOrNot = document.exists()
        } else {
            enrollOrNot = false
        }
    }

    private fun alreadyAchieve() {

        db.collection("manwon").document("$userId").get()
            .addOnSuccessListener { userData ->
                val userPhone = userData.data?.getValue("userPhone").toString()
                val firstPhone = userPhone.substring(4, 8)
                val secondPhone = userPhone.substring(9, 13)
                binding.phoneInput1.setText(firstPhone)
                binding.phoneInput2.setText(secondPhone)
            }

        binding.mainAchieveText.text = "이미 신청하셨습니다."
        binding.subAchieveText.text = "곧 만원 상품권이 도착할거예요!"

        val drawableBox =
            ResourcesCompat.getDrawable(resources, R.drawable.mindbox_custom_gray, null)
        binding.achieveConfirmBtn.background = drawableBox

        Handler().postDelayed({
            val goDiaryTwo = Intent(this, DiaryTwoActivity::class.java)
            startActivity(goDiaryTwo)
        }, 2000)

    }

    private fun nowAchieve() {
        // 휴대폰 입력 값

        binding.subAchieveText.text = "만원을 달성하셨습니다.\n핸드폰 번호를 입력하시면\n상품권을 보내드려요."

        // 휴대폰 초기 값
        userDB.document("$userId").get()
            .addOnSuccessListener { userData ->
                val userPhone = userData.data?.getValue("phone").toString()
                userName = userData.data?.getValue("name").toString()
                userRegion = userData.data?.getValue("region").toString()
                userSmallRegion = userData.data?.getValue("smallRegion").toString()

                val firstPhone = userPhone.substring(4, 8)
                val secondPhone = userPhone.substring(9, 13)
                binding.phoneInput1.setText(firstPhone)
                binding.phoneInput2.setText(secondPhone)
            }

        phoneTxtCheck.phoneEditText1.observe(this, Observer { value ->
            if( phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true) {
                val drawableBox =
                    ResourcesCompat.getDrawable(resources, R.drawable.mindbox_main, null)
                binding.achieveConfirmBtn.background = drawableBox

                binding.achieveConfirmBtn.setOnClickListener {
                    val confirmPhone = "010-${binding.phoneInput1.text}-${binding.phoneInput2.text}"
                    val userFullRegion = "${userRegion} ${userSmallRegion}"

                    val manwonHashMap = hashMapOf(
                        "userId" to userId,
                        "userName" to userName,
                        "userPhone" to confirmPhone,
                        "userFullRegion" to userFullRegion,
                    )

                    db.collection("manwon").document("$userId").set(manwonHashMap, SetOptions.merge())
                        .addOnSuccessListener {
                            binding.achieveFullBox.visibility = View.GONE
                            binding.achieveFinishText.visibility = View.VISIBLE

                            Handler().postDelayed({
                                val goDiaryTwo = Intent(this, DiaryTwoActivity::class.java)
                                startActivity(goDiaryTwo)
                            }, 2000)
                        }
                }
            } else {
                val drawableBox =
                    ResourcesCompat.getDrawable(resources, R.drawable.mindbox_custom_gray, null)
                binding.achieveConfirmBtn.background = drawableBox
                binding.achieveConfirmBtn.setOnClickListener {
                    Toast.makeText(this, "핸드폰 번호를 모두 기입해주세요!", Toast.LENGTH_SHORT).show()
                }
            }
        })

        phoneTxtCheck.phoneEditText2.observe(this, Observer { value ->
            if( phoneTxtCheck.phoneEditText1.value == true && phoneTxtCheck.phoneEditText2.value == true) {
                val drawableBox =
                    ResourcesCompat.getDrawable(resources, R.drawable.mindbox_main, null)
                binding.achieveConfirmBtn.background = drawableBox

                binding.achieveConfirmBtn.setOnClickListener {
                    val confirmPhone = "010-${binding.phoneInput1.text}-${binding.phoneInput2.text}"
                    val userFullRegion = "${userRegion} ${userSmallRegion}"

                    val manwonHashMap = hashMapOf(
                        "userId" to userId,
                        "userName" to userName,
                        "userPhone" to confirmPhone,
                        "userFullRegion" to userFullRegion,
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    db.collection("manwon").document("$userId").set(manwonHashMap, SetOptions.merge())
                        .addOnSuccessListener {
                            binding.achieveFullBox.visibility = View.GONE
                            binding.achieveFinishText.visibility = View.VISIBLE

                            Handler().postDelayed({
                                val goDiaryTwo = Intent(this, DiaryTwoActivity::class.java)
                                startActivity(goDiaryTwo)
                            }, 2000)
                        }
                }
            } else {
                val drawableBox =
                    ResourcesCompat.getDrawable(resources, R.drawable.mindbox_custom_gray, null)
                binding.achieveConfirmBtn.background = drawableBox
                binding.achieveConfirmBtn.setOnClickListener {
                    Toast.makeText(this, "핸드폰 번호를 모두 기입해주세요!", Toast.LENGTH_SHORT).show()
                }
            }
        })

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
    }
}

