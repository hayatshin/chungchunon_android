package com.chugnchunon.chungchunon_android.Fragment

import com.chugnchunon.chungchunon_android.Service.MyService
import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.*
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent.ACTION_MOVE
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.ViewModel.BaseViewModel
import com.chugnchunon.chungchunon_android.databinding.FragmentMyDiaryBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.util.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.Adapter.UploadPhotosAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.MonthDate
import com.chugnchunon.chungchunon_android.MoneyActivity.MoneyDetailActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.Timestamp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_diary_two.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import kotlin.collections.ArrayList
import kotlin.random.Random

class MyDiaryFragment : Fragment() {

    private var _binding: FragmentMyDiaryBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var currentMonth: String = ""
    private val model: BaseViewModel by viewModels()

    private var todayTotalStepCount: Int = 0
    private val yearMonthDateFormat = SimpleDateFormat("yyyy-MM")

    lateinit var diaryFillCheck: DiaryFillClass
    lateinit var diaryEditCheck: DiaryEditClass
    lateinit var newImageViewModel: NewImageViewModel

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var photoAdapter: UploadPhotosAdapter
    private var oldImageList: ArrayList<String> = ArrayList()

    lateinit var mcontext: Context
    private lateinit var callback: OnBackPressedCallback

    private var recognitionResult: Boolean = true
    private var dbRealAnswer = ""
    private var dbUserAnswer = ""
    private var recognitionQuestion = ""

    private var firstNumber = 7
    private var secondNumber = 2
    private var operator = ""
    private var itemListItems: ArrayList<Any> = ArrayList()

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    lateinit var appParticipateDate: Date
    private var nowDate: Date = Date()
    private var formatAppParticipateDate: String = ""
    private var formatNowDate: String = ""

    private var userPoint: Int = 0
    private var userStepPoint: Int = 0
//    private var userStepCountHashMap = hashMapOf<String, Int>()

    companion object {
        private var secretStatus: Boolean = false

        private var photoResume: Boolean = false
        private var recordResume: Boolean = false

        private var partnerOrNot: Boolean = false
        private var fulfilledOrNot: Boolean = false
        private var editOrNot: Boolean = false

        private var editDiary: Boolean = false
        const val REQ_GALLERY = 200
        const val REQ_MULTI_PHOTO = 2000
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentMyDiaryBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.diaryBtn.alpha = 0.4f

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepAuthGrantedReceiver,
            IntentFilter("STEP_AUTH_UPDATE")
        );

        // StepCount Notification Receiver: 변경된 걸음수 UI 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepCountUpdateReceiver,
            IntentFilter(MyService.ACTION_STEP_COUNTER_NOTIFICATION)
        )


        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            secretOrNotFunction,
            IntentFilter("SECRET_OR_NOT")
        );


        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            myDiaryWarningFeedbackFunction,
            IntentFilter("MY_DIARY_WARNING_FEEDBACK")
        );

        // 데이터 불러오기
        uiScope.launch(Dispatchers.IO) {
            launch { appParticipateDate() }.join()
            launch { stepCountToArrayFun() }.join()
            launch { diaryToArrayFun() }.join()
            launch { commentToArrayFun() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    val spanText: SpannableStringBuilder
                    val decimal = DecimalFormat("#,###")

                    if(userPoint < 10000) {
                        binding.coinTextMoney.text = "${decimal.format(userPoint)}원"
                        binding.coinTextExplanation.text = "적립"
                    } else {
                        binding.coinTextMoney.text = "10,000원"
                        binding.coinTextExplanation.text = "달성"
                    }

                    val userPointSet = hashMapOf(
                        "userPoint" to userPoint
                    )
                    userDB.document("$userId").set(userPointSet, SetOptions.merge())
                }
            }
        }

        binding.coinLayout.setOnClickListener {
            val goMoneyDetail = Intent(requireActivity(), MoneyDetailActivity::class.java)
            startActivity(goMoneyDetail)
        }

        // 파트너 체크
        userDB.document("$userId")
            .get()
            .addOnSuccessListener { userData ->
                val userType = userData.data?.getValue("userType")
                partnerOrNot = userType == "파트너"
            }

        // 인지 화면 숨기기
        binding.recognitionResultLayout.visibility = View.GONE

        // 걸음수 권한 체크 후 버튼 노출
        binding.stepCountLayout.visibility = View.VISIBLE
        binding.stepAuthLayout.visibility = View.GONE

        val stepPermissionCheck =
            ContextCompat.checkSelfPermission(mcontext, Manifest.permission.ACTIVITY_RECOGNITION)
        if (stepPermissionCheck == PackageManager.PERMISSION_GRANTED) {
            binding.stepCountLayout.visibility = View.VISIBLE
            binding.stepAuthLayout.visibility = View.GONE
        } else {
            binding.stepCountLayout.visibility = View.GONE
            binding.stepAuthLayout.visibility = View.VISIBLE
        }

        // 스크롤뷰 키보드 터치다운
        binding.myDiaryScrollView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, ev: MotionEvent?): Boolean {
                when (ev?.action) {
                    ACTION_MOVE -> {
                        return false
                    }
                    ACTION_DOWN, ACTION_UP -> {
                        val imm: InputMethodManager =
                            activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
                        return false
                    }
                }
                return false
            }
        })

        // 이미지 애니메이션
        val womanIcon = binding.womanIcon
        val manIcon = binding.manIcon

        val womananimation = ObjectAnimator.ofFloat(womanIcon, "rotation", 10F)
        womananimation.setDuration(300)
        womananimation.repeatCount = 2
        womananimation.interpolator = LinearInterpolator()
        womananimation.start()

        val manAnimation = ObjectAnimator.ofFloat(manIcon, "rotation", -10F)
        manAnimation.setDuration(300)
        manAnimation.repeatCount = 2
        manAnimation.interpolator = LinearInterpolator()
        manAnimation.start()

        diaryFillCheck = ViewModelProvider(requireActivity()).get(
            DiaryFillClass::
            class.java
        )
        diaryEditCheck = ViewModelProvider(requireActivity()).get(
            DiaryEditClass::
            class.java
        )
        newImageViewModel = ViewModelProvider(this).get(
            NewImageViewModel::
            class.java
        )

        photoAdapter = UploadPhotosAdapter(mcontext, itemListItems)
        binding.photoRecyclerView.layoutManager = LinearLayoutManagerWrapper(
            requireActivity(),
            RecyclerView.HORIZONTAL,
            false
        )
        binding.photoRecyclerView.adapter = photoAdapter

        // 일기 초기 세팅
        binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_no)
        binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)

        // 필드별 작성 시 변화
        diaryFillCheck.secretFill.observe(requireActivity(), Observer
        { value ->
            if (secretStatus) {
                binding.secretButton.text = "함께 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unlock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.VISIBLE

            } else {
                binding.secretButton.text = "나만 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.GONE
            }
        })

        diaryFillCheck.moodFill.observe(requireActivity(), Observer { value ->
            if (diaryFillCheck.moodFill.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.moodFill.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (!partnerOrNot) {
                if (!editDiary && diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                }
            }

        })

        diaryFillCheck.recognitionFill.observe(requireActivity(), Observer { value ->

            if (diaryFillCheck.recognitionFill.value!!) {
                binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.recognitionFill.value!!) {
                binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (!partnerOrNot) {
                if (!editDiary && diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                }
            }
        })

        diaryFillCheck.diaryFill.observe(requireActivity(), Observer
        { value ->
            if (diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (!partnerOrNot) {
                if (!editDiary && diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                }
            }
        })

        // 수정 시 필드별 변화
        diaryEditCheck.diaryEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            }
        })

        diaryEditCheck.moodEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            }
        })

        diaryEditCheck.photoEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            }
        })

        diaryEditCheck.secretEdit.observe(requireActivity(), Observer
        { value ->

            if (secretStatus) {
                binding.secretButton.text = "함께 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unlock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.VISIBLE
            } else {
                binding.secretButton.text = "나만 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.GONE
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            }
        })

        // 화면 변경
        if (!photoResume && !recordResume) {
            binding.diaryBtn.alpha = 0.4f
            editOrNot = false
            fulfilledOrNot = false

            if (!editDiary) {
                diaryFillCheck.recognitionFill.value = false
                diaryFillCheck.diaryFill.value = false
                diaryFillCheck.moodFill.value = false
                diaryFillCheck.photoFill.value = false
                diaryFillCheck.secretFill.value = false
            } else {
                diaryEditCheck.diaryEdit.value = false
                diaryEditCheck.moodEdit.value = false
                diaryEditCheck.photoEdit.value = false
                diaryEditCheck.secretEdit.value = false
            }
        }

        // 이미지
        newImageViewModel.newImageList.observe(requireActivity(), Observer
        { value ->
            if (newImageViewModel.newImageList.value!!.size == 0) {
                binding.photoRecyclerView.visibility = View.GONE

                if (!editDiary) {
                    diaryFillCheck.photoFill.value = false
                }

            }
        })

        newImageViewModel.uploadFirebaseComplete.observe(requireActivity(), Observer
        { value ->

            if (newImageViewModel.uploadFirebaseComplete.value == true) {

                val currentMilliseconds = System.currentTimeMillis()
                val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
                val writeTime = LocalDateTime.now()
                val diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

                // 인지
                if (!editDiary) {
                    val recognitionSet = hashMapOf(
                        "userId" to userId,
                        "diaryId" to diaryId,
                        "monthDate" to writeMonthDate,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "recognitionResult" to recognitionResult,
                        "recognitionQuestion" to recognitionQuestion,
                        "firstNumber" to firstNumber,
                        "secondNumber" to secondNumber,
                        "operator" to operator,
                        "realAnswer" to dbRealAnswer,
                        "userAnswer" to dbUserAnswer,
                    )
                    db.collection("recognition").document(diaryId)
                        .set(recognitionSet, SetOptions.merge())
                }

                if(!editDiary) {
                    userDB.document("$userId").get()
                        .addOnSuccessListener { document ->
                            val username = document.data?.getValue("name")
                            val stepCount = document.data?.getValue("todayStepCount")

                            val diarySet = hashMapOf(
                                "diaryId" to diaryId,
                                "userId" to userId.toString(),
                                "monthDate" to writeMonthDate,
                                "timestamp" to FieldValue.serverTimestamp(),
                                "todayMood" to binding.todayMood.selectedItem,
                                "todayDiary" to (binding.todayDiary.text.toString()),
                                "images" to newImageViewModel.newImageList.value,
                                "numLikes" to 0,
                                "numComments" to 0,
                                "blockedBy" to ArrayList<String>(),
                                "secret" to secretStatus,
                            )

                            diaryDB
                                .document(diaryId)
                                .set(diarySet, SetOptions.merge())
                                .addOnSuccessListener {
                                    val fragment = requireActivity().supportFragmentManager
//                                fragment.beginTransaction()
//                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
//                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu

                                    val allFragment = AllDiaryFragmentTwo()
                                    val bundle = Bundle()
                                    bundle.putString("diaryType", "my")
                                    allFragment.arguments = bundle
                                    fragment.beginTransaction()
                                        .replace(R.id.enterFrameLayout, allFragment).commit()

                                    requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu
                                }
                        }
                } else {
                    userDB.document("$userId").get()
                        .addOnSuccessListener { document ->
                            val username = document.data?.getValue("name")
                            val stepCount = document.data?.getValue("todayStepCount")

                            val diarySet = hashMapOf(
                                "diaryId" to diaryId,
                                "userId" to userId.toString(),
                                "monthDate" to writeMonthDate,
                                "lastUpdate" to FieldValue.serverTimestamp(),
                                "todayMood" to binding.todayMood.selectedItem,
                                "todayDiary" to (binding.todayDiary.text.toString()),
                                "images" to newImageViewModel.newImageList.value,
                                "numLikes" to 0,
                                "numComments" to 0,
                                "blockedBy" to ArrayList<String>(),
                                "secret" to secretStatus,
                            )

                            diaryDB
                                .document(diaryId)
                                .set(diarySet, SetOptions.merge())
                                .addOnSuccessListener {
                                    val fragment = requireActivity().supportFragmentManager
//                                fragment.beginTransaction()
//                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
//                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu

                                    val allFragment = AllDiaryFragmentTwo()
                                    val bundle = Bundle()
                                    bundle.putString("diaryType", "my")
                                    allFragment.arguments = bundle
                                    fragment.beginTransaction()
                                        .replace(R.id.enterFrameLayout, allFragment).commit()

                                    requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu
                                }
                        }
                }
            }
        })

        // 일기 작성 여부
        val writeTime = LocalDateTime.now().toString().substring(0, 10)
        val year = writeTime.substring(0, 4)
        val month = writeTime.substring(5, 7)
        val date = writeTime.substring(8, 10)
        val monthUI = StringUtils.stripStart(month, "0");
        val dateUI = StringUtils.stripStart(date, "0");

        // 상단 요일 세팅
        val sdf = java.text.SimpleDateFormat("EEE")
        val dayOfTheWeek = sdf.format(Date())

        val requestId = "${userId}_${writeTime}"

        binding.todayDate.text = "${monthUI}월 ${dateUI}일 (${DateFormat().doDayOfWeek()})"

        diaryDB
            .document(requestId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null) {
                        if (document.exists()) {
                            // 일기 작성을 한 상태

//                            binding.diaryBtn.isEnabled = true
                            binding.diaryBtn.alpha = 0.4f
                            editDiary = true
                            binding.diaryBtn.text = "일기 수정"

                            binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                            binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                            binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                            // 인지 보여주기
                            db.collection("recognition")
                                .document(requestId)
                                .get()
                                .addOnSuccessListener { recogDocument ->
                                    if (recogDocument.exists()) {
                                        // 있을 경우
                                        binding.mathLayout.visibility = View.VISIBLE
                                        binding.noRecognitionLayout.visibility = View.GONE

                                        val oldFirstNumber =
                                            recogDocument.data?.getValue("firstNumber").toString()
                                        val oldSecondNumber =
                                            recogDocument.data?.getValue("secondNumber").toString()
                                        val oldOperator =
                                            recogDocument.data?.getValue("operator").toString()
                                        val oldUserAnswer =
                                            recogDocument.data?.getValue("userAnswer").toString()

                                        binding.firstNumber.text = oldFirstNumber
                                        binding.secondNumber.text = oldSecondNumber
                                        binding.operatorNumber.text = oldOperator
                                        binding.userRecognitionText.setText(oldUserAnswer)
                                        binding.userRecognitionText.isFocusable = false
                                        binding.userRecognitionText.isClickable = true

                                        binding.userRecognitionText.setOnClickListener {
                                            if (editDiary) {
                                                binding.recognitionResultLayout.visibility =
                                                    View.VISIBLE
                                                val biggerAnimation =
                                                    AnimationUtils.loadAnimation(
                                                        mcontext,
                                                        R.anim.scale_big
                                                    )
                                                binding.recognitionResultBox.startAnimation(
                                                    biggerAnimation
                                                )

                                                binding.resultEmoji.setImageResource(R.drawable.ic_soso)
                                                binding.bigResultText.setText(null)
                                                binding.smallResultText.text =
                                                    "문제는 한번만 풀 수 있어요.\n내일 또 도전해보아요!"

                                                Handler().postDelayed({
                                                    val downAnimation =
                                                        AnimationUtils.loadAnimation(
                                                            mcontext,
                                                            R.anim.scale_small
                                                        )
                                                    binding.recognitionResultBox.startAnimation(
                                                        downAnimation
                                                    )

                                                    Handler().postDelayed({
                                                        binding.recognitionResultLayout.visibility =
                                                            View.GONE
                                                        binding.userRecognitionText.isEnabled =
                                                            false
                                                    }, 300)
                                                }, 1500)
                                            }
                                        }


                                    } else {
                                        // 없을 경우
                                        binding.mathLayout.visibility = View.GONE
                                        binding.noRecognitionLayout.visibility = View.VISIBLE
                                    }
                                }

                            val originalDiary = document.data?.getValue("todayDiary").toString()

                            // 이미지 보여주기
                            if (document.data?.contains("images") == true) {
                                oldImageList =
                                    document.data?.getValue("images") as ArrayList<String>

                                if (oldImageList.size != 0) {
                                    binding.photoRecyclerView.visibility = View.VISIBLE
//                                    binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)


                                    for (i in 0 until oldImageList.size) {
                                        val uriParseImage = Uri.parse(oldImageList[i])
                                        newImageViewModel.addImage(uriParseImage)
                                        itemListItems.add(uriParseImage)
                                        photoAdapter.notifyItemInserted(oldImageList.size - 1)
                                    }
//                                    photoAdapter = UploadPhotosAdapter(mcontext, oldImageList)
//                                    binding.photoRecyclerView.adapter = photoAdapter
//                                    photoAdapter.notifyDataSetChanged()
                                }

                            } else {
                                // null
                                binding.photoRecyclerView.visibility = View.GONE
                            }

                            // 숨기기 보여주기
                            val secretStatusFromDB = document.data?.getValue("secret") as Boolean
                            secretStatus = secretStatusFromDB

                            if (secretStatus) {
                                binding.secretButton.text = "함께 보기"
                                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_unlock,
                                    0,
                                    0,
                                    0
                                )
                                binding.secretInfoText.visibility = View.VISIBLE
                            } else {
                                binding.secretButton.text = "나만 보기"
                                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_lock,
                                    0,
                                    0,
                                    0
                                )
                                binding.secretInfoText.visibility = View.GONE
                            }

                            // 일기 보여주기
                            val oldDiary = document.data?.getValue("todayDiary").toString()
                            binding.todayDiary.setText(oldDiary)

                            // 마음 보여주기
                            var spinnerAdapter = binding.todayMood.adapter
                            val dbMoodPosition =
                                (document.data?.getValue("todayMood") as Map<*, *>)["position"].toString()
                                    .toInt()

                            binding.todayMood.setSelection(dbMoodPosition)
                            binding.todayMood.setOnItemSelectedListener(object :
                                AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    p0: AdapterView<*>?,
                                    p1: View?,
                                    p2: Int,
                                    p3: Long
                                ) {
                                    val nowMood = (binding.todayMood.selectedItem as Mood).position
                                    if (nowMood != dbMoodPosition) {
                                        diaryEditCheck.moodEdit.value = true
                                    }
                                }

                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    // null
                                }

                            })

                            // 일기 수정
                            binding.todayDiary.addTextChangedListener(
                                object : TextWatcher {
                                    override fun beforeTextChanged(
                                        char: CharSequence?,
                                        p1: Int,
                                        p2: Int,
                                        p3: Int
                                    ) {
                                        // null
                                    }

                                    override fun onTextChanged(
                                        char: CharSequence?,
                                        start: Int,
                                        before: Int,
                                        count: Int
                                    ) {
                                        if (char != originalDiary) diaryEditCheck.diaryEdit.value =
                                            true
                                    }

                                    override fun afterTextChanged(p0: Editable?) {
                                        // null
                                    }
                                })

                        } else {
                            // 일기 작성 x
                            binding.diaryBtn.alpha = 0.4f
                            binding.secretInfoText.visibility = View.GONE

                            if (!partnerOrNot) {
                                editDiary = false
                                fulfilledOrNot = false
                                binding.diaryBtn.text = "일기 작성"
                            }

//                            if(partnerOrNot) {
//                                binding.diaryBtn.isEnabled = true
//                                binding.diaryBtn.alpha = 0.4f
//                            } else {
//                                editDiary = false
//                            }
                        }
                    }
                } else {
                }
            }

        // 하루 걸음수 초기화
        val stepInitializeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                binding.todayStepCount.text = "0 보"
            }
        }

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        );

        // 걸음수 업데이트
        userDB.document("$userId").get().addOnSuccessListener { document ->
            val todayStepCountFromDB = (document.data?.getValue("todayStepCount") as Long).toInt()

            todayTotalStepCount = todayStepCountFromDB
            val decimal = DecimalFormat("#,###")
            val step = decimal.format(todayTotalStepCount)
            binding.todayStepCount.text = "$step 보"

            if (todayTotalStepCount >= 3000) {
                binding.stepCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                Handler().postDelayed({
                    binding.stepSuccessLayout.visibility = View.VISIBLE

                    val downSuccessAnimation =
                        ObjectAnimator.ofFloat(binding.stepSuccessLayout, "translationY", -80f, 0f)
                    downSuccessAnimation.duration = 1000
                    downSuccessAnimation.interpolator = DecelerateInterpolator()
                    downSuccessAnimation.start()

                    Handler().postDelayed({
                        binding.stepSuccessLayout.alpha = 1f
                        binding.stepSuccessLayout.animate()
                            .alpha(0f).duration = 1000

                        val upSuccessAnimation =
                            ObjectAnimator.ofFloat(
                                binding.stepSuccessLayout,
                                "translationY",
                                0f,
                                -120f
                            )
                        upSuccessAnimation.duration = 1200
                        upSuccessAnimation.interpolator = DecelerateInterpolator()
                        upSuccessAnimation.start()

                        Handler().postDelayed({
                            binding.stepSuccessLayout.visibility = View.GONE
                        }, 500)

                    }, 3000)
                }, 1000)


            } else {
                binding.stepCheckBox.setImageResource(R.drawable.ic_checkbox_no)
                binding.stepSuccessLayout.visibility = View.GONE

            }

        }

        // 사진 업로드
        val readGalleryPermission =
            ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        val readMediaImagesPermission = ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.READ_MEDIA_IMAGES
        )

        // 사진 가져오기 권한 체크
        binding.photoButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (readMediaImagesPermission == PackageManager.PERMISSION_DENIED) {
                    // 권한 요청
                    requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQ_GALLERY)
                } else {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                    startActivityForResult(intent, REQ_MULTI_PHOTO)
                }

            } else {

                if (readGalleryPermission == PackageManager.PERMISSION_DENIED) {
                    // 권한 요청
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQ_GALLERY
                    )
                } else {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                    startActivityForResult(intent, REQ_MULTI_PHOTO)
                }

            }
        }

        // 매달 일기 작성 카운트
        currentMonth = LocalDateTime.now().toString().substring(0, 7)

        val currentdate = System.currentTimeMillis()
        val currentYearMonth = yearMonthDateFormat.format(currentdate)
        val currentMonth = SimpleDateFormat("MM").format(currentdate)
        val removeZeroCurrentMonth = StringUtils.stripStart(currentMonth, "0");
        val currentMonthDate = MonthDate(currentMonth.toInt()).getDate

        val thisMonthCount = diaryDB
            .whereEqualTo("monthDate", currentYearMonth)
            .whereEqualTo("userId", userId)
            .count()

        thisMonthCount.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val calendarThisMonthCount = "${task.result.count}일"
                val spanText = SpannableStringBuilder()
                    .bold {
                        color(
                            ContextCompat.getColor(
                                mcontext,
                                R.color.light_gray
                            )
                        ) { append(calendarThisMonthCount) }
                    }
                    .append(" / ${currentMonthDate}일")

                binding.thisMonth.text = "${removeZeroCurrentMonth}월 작성일:"
                binding.diaryCount.text = spanText
            }
        }

        // 인지
        val optOption1 = "+"
        val optOption2 = "-"
        operator = if (Random.nextBoolean()) optOption1 else optOption2

        if (operator == optOption2) {
            firstNumber = Random.nextInt(5, 11)
            secondNumber = Random.nextInt(1, 4)
        } else {
            firstNumber = Random.nextInt(1, 9)
            secondNumber = Random.nextInt(1, 10 - firstNumber)
        }

        binding.firstNumber.text = firstNumber.toString()
        binding.secondNumber.text = secondNumber.toString()
        binding.operatorNumber.text = operator

        recognitionQuestion = "${firstNumber} ${operator} ${secondNumber}"

        binding.userRecognitionText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // null
                }

                override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    if (char?.length == 1 && !editDiary) {
                        val inputMethodManager =
                            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

                        val realAnswer = calculationResult(firstNumber, secondNumber, operator)
                        val userAnswer = binding.userRecognitionText.text.trim()

                        diaryFillCheck.recognitionFill.value = true

                        dbRealAnswer = realAnswer.toString()
                        dbUserAnswer = userAnswer.toString()

                        if (realAnswer.toString() == userAnswer.toString()) {
                            recognitionResult = true
                            binding.recognitionResultLayout.visibility = View.VISIBLE
                            val biggerAnimation =
                                AnimationUtils.loadAnimation(mcontext, R.anim.scale_big)
                            binding.recognitionResultBox.startAnimation(biggerAnimation)

                            binding.resultEmoji.setImageResource(R.drawable.ic_throb)
                            binding.bigResultText.text = "정답입니다!"
                            binding.smallResultText.text = "대단해요~ 정말 똑똑하세요!"

                            Handler().postDelayed({
                                val downAnimation =
                                    AnimationUtils.loadAnimation(mcontext, R.anim.scale_small)
                                binding.recognitionResultBox.startAnimation(downAnimation)

                                Handler().postDelayed({
                                    binding.recognitionResultLayout.visibility = View.GONE
                                }, 300)
                            }, 1500)

                        } else {
                            recognitionResult = false
                            binding.recognitionResultLayout.visibility = View.VISIBLE
                            val biggerAnimation =
                                AnimationUtils.loadAnimation(mcontext, R.anim.scale_big)
                            binding.recognitionResultBox.startAnimation(biggerAnimation)

                            binding.resultEmoji.setImageResource(R.drawable.ic_gloomy)
                            binding.bigResultText.text = "틀렸습니다!"
                            binding.smallResultText.text = "정답은 ${realAnswer}입니다.\n꾸준히 지속하면 돼요 :)"


                            Handler().postDelayed({
                                val downAnimation =
                                    AnimationUtils.loadAnimation(mcontext, R.anim.scale_small)
                                binding.recognitionResultBox.startAnimation(downAnimation)

                                Handler().postDelayed({
                                    binding.recognitionResultLayout.visibility = View.GONE
                                }, 300)
                            }, 1500)
                        }

                        binding.userRecognitionText.isEnabled = false
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                    // null
                }
            })

        binding.recognitionResultLayout.setOnClickListener { view ->

            val downAnimation = AnimationUtils.loadAnimation(mcontext, R.anim.scale_small)
            binding.recognitionResultBox.startAnimation(downAnimation)

            Handler().postDelayed({
                binding.recognitionResultLayout.visibility = View.GONE
            }, 300)
        }


        // 기분 스니퍼
        binding.todayMood.adapter = activity?.applicationContext?.let {
            MoodArrayAdapter(
                it,
//                listOf(
//                    Mood(R.drawable.ic_joy, "기뻐요", 0),
//                    Mood(R.drawable.ic_shalom, "평온해요", 1),
//                    Mood(R.drawable.ic_throb, "설레요", 2),
//                    Mood(R.drawable.ic_soso, "그냥 그래요", 3),
//                    Mood(R.drawable.ic_anxious, "걱정돼요", 4),
//                    Mood(R.drawable.ic_sad, "슬퍼요", 5),
//                    Mood(R.drawable.ic_gloomy, "우울해요", 6),
//                    Mood(R.drawable.ic_angry, "화나요", 7),
//                )
                listOf(
                    Mood(R.drawable.ic_joy, "기뻐요", 0),
                    Mood(R.drawable.ic_throb, "설레요", 1),
                    Mood(R.drawable.ic_thanksful, "감사해요", 2),
                    Mood(R.drawable.ic_shalom, "평온해요", 3),
                    Mood(R.drawable.ic_soso, "그냥 그래요", 4),
                    Mood(R.drawable.ic_lonely, "외로워요", 5),
                    Mood(R.drawable.ic_anxious, "불안해요", 6),
                    Mood(R.drawable.ic_gloomy, "우울해요", 7),
                    Mood(R.drawable.ic_sad, "슬퍼요", 8),
                    Mood(R.drawable.ic_angry, "화나요", 9),
                )
            )
        }

        binding.todayMood.setOnTouchListener(
            object : View.OnTouchListener {
                override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                    if (!editDiary) {
                        diaryFillCheck.moodFill.value = true
                    } else {
                        diaryEditCheck.moodEdit.value = true
                    }
                    return false
                }
            })

        // 글 숨기기
        binding.secretButton.setOnClickListener {
            val goLockDiary = Intent(requireActivity(), LockDiaryActivity::class.java)
            goLockDiary.putExtra("secretStatus", secretStatus)
            startActivity(goLockDiary)
        }

        // 말로 쓰기
        model.initial(textToSpeechEngine, startForResult)

        binding.recordBtn.setOnClickListener {
            model.displaySpeechRecognizer()
//            val text = todayDiary.text?.trim().toString()
//            model.speak(if (text.isNotEmpty()) text else "일기를 써보세요")
        }

        // 다이어리 작성
        binding.todayDiary.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    // null
                }

                override fun onTextChanged(
                    char: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (!editDiary) {
                        diaryFillCheck.diaryFill.value = char?.length != 0
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                    // null
                }
            })


        // ** 종합 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {
            if (!partnerOrNot) {
                if (!editDiary && !fulfilledOrNot) {
                    // 작성인데 빈칸이 있음
                    val goMyDiaryWarning = Intent(context, MyDiaryWarningActivity::class.java)
                    goMyDiaryWarning.putExtra("recognition", diaryFillCheck.recognitionFill.value)
                    goMyDiaryWarning.putExtra("mood", diaryFillCheck.moodFill.value)
                    goMyDiaryWarning.putExtra("diary", diaryFillCheck.diaryFill.value)
                    startActivity(goMyDiaryWarning)
                } else if (editDiary && !editOrNot) {
                    // 수정인데 수정 안함
                    val goEditWarning = Intent(context, DefaultDiaryWarningActivity::class.java)
                    goEditWarning.putExtra("warningType", "editDiary")
                    startActivity(goEditWarning)
                } else {
                    // 문제 없이 진행
                    binding.diaryBtn.text = ""
                    binding.diaryProgressBar.visibility = View.VISIBLE

                    val currentMilliseconds = System.currentTimeMillis()
                    val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
                    val writeTime = LocalDateTime.now()
                    val diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

                    if (diaryFillCheck.photoFill.value == true || diaryEditCheck.photoEdit.value == true) {
                        // 이미지 있는 경우

                        val allStartsWithHttps = newImageViewModel.newImageList.value!!.all {
                            it.toString().startsWith("https://")
                        }

                        if (allStartsWithHttps) {

                            newImageViewModel.uploadFirebaseComplete.value = true
                        } else {
                            for (i in 0 until newImageViewModel.newImageList.value!!.size) {
                                if (!newImageViewModel.newImageList.value!![i].toString()
                                        .startsWith("https://")
                                ) {
                                    uploadImageToFirebase(
                                        newImageViewModel.newImageList.value!![i] as Uri,
                                        i,
                                    )
                                }
                            }
                        }

                    } else {
                        // 이미지 없는 경우

                        // 인지
                        if (!editDiary) {
                            val recognitionSet = hashMapOf(
                                "userId" to userId,
                                "diaryId" to diaryId,
                                "monthDate" to writeMonthDate,
                                "timestamp" to FieldValue.serverTimestamp(),
                                "recognitionResult" to recognitionResult,
                                "recognitionQuestion" to recognitionQuestion,
                                "firstNumber" to firstNumber,
                                "secondNumber" to secondNumber,
                                "operator" to operator,
                                "realAnswer" to dbRealAnswer,
                                "userAnswer" to dbUserAnswer,
                            )
                            db.collection("recognition").document(diaryId)
                                .set(recognitionSet, SetOptions.merge())
                        }

                        // 일기 업로드
                        if(!editDiary) {
                            userDB.document("$userId").get()
                                .addOnSuccessListener { document ->
                                    var username = document.data?.getValue("name")
                                    var stepCount = document.data?.getValue("todayStepCount")

                                    val diarySet = hashMapOf(
                                        "diaryId" to diaryId,
                                        "userId" to userId.toString(),
                                        "monthDate" to writeMonthDate,
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "todayMood" to binding.todayMood.selectedItem,
                                        "todayDiary" to (binding.todayDiary.text.toString()),
                                        "images" to newImageViewModel.newImageList.value,
                                        "numLikes" to 0,
                                        "numComments" to 0,
                                        "blockedBy" to ArrayList<String>(),
                                        "secret" to secretStatus
                                    )

                                    diaryDB
                                        .document(diaryId)
                                        .set(diarySet, SetOptions.merge())
                                        .addOnSuccessListener {
                                            val fragment = requireActivity().supportFragmentManager
//                                fragment.beginTransaction()
//                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
//                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu

                                            val allFragment = AllDiaryFragmentTwo()
                                            val bundle = Bundle()
                                            bundle.putString("diaryType", "my")
                                            allFragment.arguments = bundle
                                            fragment.beginTransaction()
                                                .replace(R.id.enterFrameLayout, allFragment).commit()

                                            requireActivity().bottomNav.selectedItemId =
                                                R.id.ourTodayMenu
                                        }
                                }
                        } else {
                            userDB.document("$userId").get()
                                .addOnSuccessListener { document ->
                                    var username = document.data?.getValue("name")
                                    var stepCount = document.data?.getValue("todayStepCount")

                                    val diarySet = hashMapOf(
                                        "diaryId" to diaryId,
                                        "userId" to userId.toString(),
                                        "monthDate" to writeMonthDate,
                                        "lastUpdate" to FieldValue.serverTimestamp(),
                                        "todayMood" to binding.todayMood.selectedItem,
                                        "todayDiary" to (binding.todayDiary.text.toString()),
                                        "images" to newImageViewModel.newImageList.value,
                                        "numLikes" to 0,
                                        "numComments" to 0,
                                        "blockedBy" to ArrayList<String>(),
                                        "secret" to secretStatus
                                    )

                                    diaryDB
                                        .document(diaryId)
                                        .set(diarySet, SetOptions.merge())
                                        .addOnSuccessListener {
                                            val fragment = requireActivity().supportFragmentManager
//                                fragment.beginTransaction()
//                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
//                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu

                                            val allFragment = AllDiaryFragmentTwo()
                                            val bundle = Bundle()
                                            bundle.putString("diaryType", "my")
                                            allFragment.arguments = bundle
                                            fragment.beginTransaction()
                                                .replace(R.id.enterFrameLayout, allFragment).commit()

                                            requireActivity().bottomNav.selectedItemId =
                                                R.id.ourTodayMenu
                                        }
                                }
                        }
                    }
                }
            } else {
                // 파트너
                val goPartnerWarning =
                    Intent(context, DefaultDiaryWarningActivity::class.java)
                goPartnerWarning.putExtra("warningType", "partnerDiary")
                startActivity(goPartnerWarning)
            }

        }


        view.setOnTouchListener(
            object : View.OnTouchListener {
                override fun onTouch(view: View?, event: MotionEvent?): Boolean {

                    val imm: InputMethodManager =
                        activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view!!.windowToken, 0)
                    return false
                }
            })

        return view
    }

    @SuppressLint("SetTextI18n")
    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        recordResume = true

        // 말로 쓰기 결과
        if (result.resultCode == RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    .let { text -> text?.get(0) }
            val recordDiaryText =
                if ("${binding.todayDiary.text}" == "") "${spokenText}" else "${binding.todayDiary.text} ${spokenText}"
            binding.todayDiary.setText(recordDiaryText)
        }
    }

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) textToSpeechEngine.language = Locale("in_ID")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 사진 보기 권한 요청
        if (requestCode == REQ_GALLERY && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            val intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            startActivityForResult(intent, REQ_MULTI_PHOTO)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQ_MULTI_PHOTO -> {
                photoResume = true

                if (data?.clipData != null) {

                    val count = data!!.clipData!!.itemCount

                    for (i in 0 until count) {

                        val imageUri = data?.clipData!!.getItemAt(i).uri
                        newImageViewModel.addImage(imageUri)
                        itemListItems.add(imageUri)
                        photoAdapter.notifyItemInserted(itemListItems.size - 1)

                        if (!editDiary) diaryFillCheck.photoFill.value =
                            true else diaryEditCheck.photoEdit.value = true
                    }
                    binding.photoRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 사진 삭제
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            deleteImageFunction,
            IntentFilter("DELETE_IMAGE")
        );


        // 이미지 애니메이션
        val womanIcon = view?.findViewById<ImageView>(R.id.womanIcon)
        val manIcon = view?.findViewById<ImageView>(R.id.manIcon)

        val womananimation = ObjectAnimator.ofFloat(womanIcon, "rotation", 0f, 20f, 0f)
        womananimation.setDuration(500)
        womananimation.repeatCount = 2
        womananimation.interpolator = LinearInterpolator()
        womananimation.start()

        val manAnimation = ObjectAnimator.ofFloat(manIcon, "rotation", 0f, -20F, 0f)
        manAnimation.setDuration(500)
        manAnimation.repeatCount = 2
        manAnimation.interpolator = LinearInterpolator()
        manAnimation.start()
    }

    override fun onPause() {
        super.onPause()

        val activity = activity
        if (activity != null) {
            val window = requireActivity().window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);
        }

        LocalBroadcastManager.getInstance(requireActivity())
            .unregisterReceiver(deleteImageFunction);
    }

    override fun onDestroy() {
        super.onDestroy()

        photoResume = false
        recordResume = false


        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(secretOrNotFunction)
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(stepCountUpdateReceiver)
    }

    private fun calculationResult(firstNum: Int, secondNum: Int, opt: String): Int {
        var result: Int = 0
        if (opt == "+") {
            result = firstNum + secondNum
        } else {
            result = firstNum - secondNum
        }
        return result
    }


    private fun uploadImageToFirebase(fileUri: Uri, position: Int) {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        val database = FirebaseDatabase.getInstance()
        val refStorage = FirebaseStorage.getInstance().reference.child("images/$fileName")

        refStorage.putFile(fileUri)
            .addOnSuccessListener(
                OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                        val imageUrl = it.toString()
                        newImageViewModel.changeImage(imageUrl, position)

                        if (newImageViewModel.newImageList.value!!.all { it ->
                                it.toString().startsWith("https://")
                            }) {
                            newImageViewModel.uploadFirebaseComplete.value = true
                        }
                    }
                }
            )
            ?.addOnFailureListener(OnFailureListener { e ->
                print(e.message)
            })
    }

    private var stepAuthGrantedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val stepAuth = intent?.getBooleanExtra("StepAuth", true)
            if (stepAuth!!) {
                binding.stepCountLayout.visibility = View.VISIBLE
                binding.stepAuthLayout.visibility = View.GONE
            }
        }
    }

    private var stepCountUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                val todayTotalStepCount = it.getIntExtra("todayTotalStepCount", 0)
                val decimal = DecimalFormat("#,###")
                val step = decimal.format(todayTotalStepCount)
                binding.todayStepCount.text = "$step 보"

                if (todayTotalStepCount >= 3000) {
                    binding.stepCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                }
            }
        }
    }

    private var secretOrNotFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newSecretStatus = intent?.getBooleanExtra("newSecretStatus", false) as Boolean
            secretStatus = newSecretStatus

            if (!editDiary) {
                diaryFillCheck.secretFill.value = true
            } else {
                diaryEditCheck.secretEdit.value = true
            }
        }

    }

    private var deleteImageFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val deleteImagePosition = intent?.getIntExtra("deleteImagePosition", 0)
            newImageViewModel.removeImage(deleteImagePosition!!)
            itemListItems.removeAt(deleteImagePosition)
            photoAdapter.notifyItemRemoved(deleteImagePosition!!)

            if (editDiary) diaryEditCheck.photoEdit.value = true

        }
    }


    private var myDiaryWarningFeedbackFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val recognitionBoolean = intent?.getBooleanExtra("recognitionBoolean", true)
            val moodBoolean = intent?.getBooleanExtra("moodBoolean", true)
            val diaryBoolean = intent?.getBooleanExtra("diaryBoolean", true)

            if (!recognitionBoolean!!) binding.recognitionHeader.setText(spanTextFn("인지"))
            if (!moodBoolean!!) binding.moodHeader.setText(spanTextFn("마음"))
            if (!diaryBoolean!!) binding.diaryHeader.setText(spanTextFn("쓰기"))
        }
    }

    private fun spanTextFn(text: String): Spannable {
        val spanText = Spannable.Factory.getInstance().newSpannable(text)
        val color = ContextCompat.getColor(mcontext, R.color.yellow_highlight)
        spanText.setSpan(
            BackgroundColorSpan(color),
            0,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spanText
    }


    // 점수 계산
    suspend fun appParticipateDate() {
        val currentDate = Calendar.getInstance()
        val userData = userDB.document("$userId").get().await()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        dateFormat.timeZone = android.icu.util.TimeZone.getTimeZone("Asia/Seoul")
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val now = Calendar.getInstance(timeZone).time

        val appTimestamp = userData.data?.getValue("timestamp") as Timestamp
        val getString = DateFormat().convertTimeStampToDate(appTimestamp)
        appParticipateDate = dateFormat.parse(getString)

        formatAppParticipateDate = dateFormat.format(appParticipateDate)
        formatNowDate = dateFormat.format(now)

        Log.d("날짜", "${formatAppParticipateDate} / ${formatNowDate}")

    }

    suspend fun stepCountToArrayFun() {
        val startDate = LocalDate.of(
            formatAppParticipateDate.substring(0, 4).toInt(),
            formatAppParticipateDate.substring(5, 7).toInt(),
            formatAppParticipateDate.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatNowDate.substring(0, 4).toInt(),
            formatNowDate.substring(5, 7).toInt(),
            formatNowDate.substring(8, 10).toInt()
        )

        for (stepDate in startDate..endDate) {
            val dataSteps = db.collection("user_step_count")
                .document("$userId")
                .get()
                .await()

            dataSteps.data?.forEach { (stepPeriod, dateStepCount) ->

                if(stepPeriod == stepDate.toString()) {
                    if(dateStepCount.toString().toInt() < 10000) {
                        userStepPoint += dateStepCount.toString().toInt()
                    } else {
                        userStepPoint += 10000
                    }
                }
            }
        }
        userPoint = ((Math.floor(userStepPoint / 1000.0)) * 10).toInt()
    }


    suspend fun diaryToArrayFun() {

        val diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", appParticipateDate)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            userPoint += 100
        }
    }

    suspend fun commentToArrayFun() {
        val commentDocuments = db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", appParticipateDate)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (commentDocument in commentDocuments) {
            userPoint += 20
        }
    }


    class NewImageViewModel : ViewModel() {
        var uploadFirebaseComplete = MutableLiveData<Boolean>().apply {
            postValue(false)
        }

        var newImageList = MutableLiveData<List<Any>>().apply {
            postValue(ArrayList())
        }
        var newImageListValue = newImageList.value
        var templateList = mutableListOf<Any>()

        fun addImage(addImage: Any) {
            newImageListValue?.forEach { data ->
                templateList.add(data)
            }
            templateList.add(addImage)
            newImageList.value = templateList
        }

        fun changeImage(changeImage: String, position: Int) {
            templateList = newImageList.value!!.toMutableList()
            templateList[position] = changeImage
            newImageList.value = templateList
        }

        fun removeImage(removePosition: Int) {
            templateList.removeAt(removePosition)
            newImageList.value = templateList
        }
    }
}

@Keep
class DiaryFillClass : ViewModel() {
    val recognitionFill by lazy { MutableLiveData<Boolean>(false) }
    val diaryFill by lazy { MutableLiveData<Boolean>(false) }
    val moodFill by lazy { MutableLiveData<Boolean>(false) }
    val photoFill by lazy { MutableLiveData<Boolean>(false) }
    val secretFill by lazy { MutableLiveData<Boolean>(false) }
}

@Keep
class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
    val photoEdit by lazy { MutableLiveData<Boolean>(false) }
    val secretEdit by lazy { MutableLiveData<Boolean>(false) }
}

