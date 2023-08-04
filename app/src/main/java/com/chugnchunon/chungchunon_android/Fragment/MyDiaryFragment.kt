package com.chugnchunon.chungchunon_android.Fragment

import com.chugnchunon.chungchunon_android.Service.MyService
import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import android.util.Base64
import android.util.Log
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent.ACTION_MOVE
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.bumptech.glide.Glide
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
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_diary_two.*
import kotlinx.android.synthetic.main.fragment_more_two.*
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
    private var itemListItemsString: ArrayList<String> = ArrayList()

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    private var userPoint: Int = 0
    private var certainUserId: String = "kakao:2788585526"
    private var certainUserPoint: Int = 0

    private var userStepPoint: Int = 0

    private var contractRegionExists: Boolean = false
    private var participateMissionExists: Boolean = false

    private var userSmallRegion: String = ""
    private var userFullRegion: String = ""

    private var participateMissionGoalScore: Int = 0
    private var participatePrizeWinners: Int = 0
    private var participateSmallDescription: String = ""

    private var contractRegionLogo: String = ""
    private var participateCommunityLogo: String = ""
    private var participateCommunityName: String = ""

    private var formatPeriodStart: String = ""
    private var formatPeriodEnd: String = ""
    lateinit var mdDateFormatStartPeriod: Date
    lateinit var mdDateFormatEndPeriod: Date

    private var removeZeroCurrentMonth = ""

    companion object {
        private var secretStatus: Boolean = false

        private var photoResume: Boolean = false
        private var recordResume: Boolean = false

        private var partnerOrNot: Boolean = false
        private var fulfilledOrNot: Boolean = false
        private var editOrNot: Boolean = false
        private var editTodayDiaryEmpty: Boolean = false

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

        val currentDateTime = LocalDate.now().toString()
        val userPref = mcontext.getSharedPreferences(
            "diary_${userId}_${currentDateTime}",
            Context.MODE_PRIVATE
        )

        val moodDonePref = userPref.getBoolean("moodDone", false)
        val recognitionDonePref = userPref.getBoolean("recognitionDone", false)
        val diaryDonePref = userPref.getBoolean("diaryDone", false)
        val secretStatusPref = userPref.getBoolean("secretStatus", false)
        val imageDonePref = userPref.getBoolean("imageDone", false)

        fulfilledOrNot = recognitionDonePref && moodDonePref && diaryDonePref

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepAuthGrantedReceiver,
            IntentFilter("STEP_AUTH_UPDATE")
        )

        // StepCount Notification Receiver: 변경된 걸음수 UI 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepCountUpdateReceiver,
            IntentFilter(MyService.ACTION_STEP_COUNTER_NOTIFICATION)
        )

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            secretOrNotFunction,
            IntentFilter("SECRET_OR_NOT")
        )

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            myDiaryWarningFeedbackFunction,
            IntentFilter("MY_DIARY_WARNING_FEEDBACK")
        )

        // 로딩 코인
        binding.loadingCoinLayout.visibility = View.GONE
        binding.writeCountLayout.visibility = View.GONE
        binding.noContractIconLayout.visibility = View.GONE
        binding.contractIconLayout.visibility = View.GONE

        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f)
        val rotationY = PropertyValuesHolder.ofFloat("rotationY", 0f, 180f)
        val scaleXBack = PropertyValuesHolder.ofFloat("scaleX", 0f, 1f)

        val flipAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.loadingCoinIcon,
            scaleX,
            rotationY,
            scaleXBack
        )
        flipAnimator.duration = 500 // Adjust the duration as needed
        flipAnimator.repeatCount = ObjectAnimator.INFINITE
        flipAnimator.interpolator = AccelerateDecelerateInterpolator()
        flipAnimator.start()

        // 특정 유저 점수
//        uiScope.launch(Dispatchers.IO) {
//            listOf(
//                launch { certainUserStepCountToArrayFun() },
//                launch { certainUserDiaryToArrayFun() },
//                launch { certainUserCommentToArrayFun() },
//            ).joinAll()
//            Log.d("certainUserPoint" , "$certainUserId: ${certainUserPoint}")
//        }

        // 점수(원) 데이터 불러오기

        uiScope.launch(Dispatchers.IO) {
            listOf(
                launch { contractOrNotCheck() },
                launch { missionParticipateDate() },
                launch { thisMonthWriteCount() }
            ).joinAll()
            withContext(Dispatchers.Main) {

                if (participateMissionExists) {
                    // 지역 계약 & 참여 이벤트 존재
                    withContext(Dispatchers.IO) {
//                        launch { appParticipateDate() }.join()
                        listOf(
                            launch { stepCountToArrayFun() },
                            launch { diaryToArrayFun() },
                            launch { commentToArrayFun() },
                        ).joinAll()

                        withContext(Dispatchers.Main) {
                            launch {
                                binding.loadingCoinLayout.visibility = View.GONE
                                binding.writeCountLayout.visibility = View.VISIBLE
                                val spanText: SpannableStringBuilder
                                val decimal = DecimalFormat("#,###")

                                if (userPoint < participateMissionGoalScore) {
                                    binding.coinTextMoney.text = "${decimal.format(userPoint)}원"
                                    binding.coinTextExplanation.visibility = View.GONE
                                } else {
                                    binding.coinTextMoney.text =
                                        "${decimal.format(participateMissionGoalScore)}"
                                    binding.coinTextExplanation.text = "달성"
                                }
                                val userPointSet = hashMapOf(
                                    "userPoint" to userPoint
                                )
                                userDB.document("$userId").set(userPointSet, SetOptions.merge())

                                Glide.with(mcontext)
                                    .load(participateCommunityLogo)
                                    .into(binding.contractRegionImageView)
                                binding.contractRegionTextView.text =
                                    participateCommunityName

                                binding.noContractIconLayout.visibility =
                                    View.GONE
                                binding.contractIconLayout.visibility =
                                    View.VISIBLE
                                binding.coinLayout.visibility = View.VISIBLE
                            }
                        }
                    }
                } else {
                    // 참여 이벤트 존재 x
                    if (contractRegionExists) {
                        // 지역 계약 O
                        if (activity?.isDestroyed == false) {
                            Glide.with(mcontext)
                                .load(contractRegionLogo)
                                .into(binding.contractRegionImageView)
                        }
                        binding.contractRegionTextView.text =
                            userSmallRegion

                        binding.contractRegionImageView.scaleType =
                            ImageView.ScaleType.CENTER_CROP
                        val contractImageLayoutParams = binding.contractRegionImageView.layoutParams
                        contractImageLayoutParams.width = 140
                        contractImageLayoutParams.height = 120

                        binding.noContractIconLayout.visibility =
                            View.GONE
                        binding.contractIconLayout.visibility =
                            View.VISIBLE
                        binding.coinLayout.visibility = View.GONE
                    } else {
                        // 지역 계약 X
                        binding.loadingCoinLayout.visibility = View.GONE
                        binding.coinLayout.visibility = View.GONE
                        binding.writeCountLayout.visibility = View.VISIBLE
                        binding.noContractIconLayout.visibility = View.VISIBLE
                        binding.contractIconLayout.visibility = View.GONE
                    }

                }
            }
        }

        binding.coinLayout.setOnClickListener {
            val goMoneyDetail = Intent(requireActivity(), MoneyDetailActivity::class.java)
            goMoneyDetail.putExtra("missionGoalScore", participateMissionGoalScore)
            goMoneyDetail.putExtra("missionPrizeWinners", participatePrizeWinners)
            goMoneyDetail.putExtra("missionSmallDescription", participateSmallDescription)
            startActivity(goMoneyDetail)
        }

        // 파트너 체크
//        userDB.document("$userId")
//            .get()
//            .addOnSuccessListener { userData ->
//                val userType = userData.data?.getValue("userType")
//                partnerOrNot = userType == "파트너"
//            }

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

            val currentDateTime = LocalDate.now().toString()
            val userPref = mcontext.getSharedPreferences(
                "diary_${userId}_${currentDateTime}",
                Context.MODE_PRIVATE
            )
            val userPrefEdit = userPref.edit()
            val secretStatusPref = userPref.getBoolean("secretStatus", false)

            if (secretStatusPref) {
                binding.secretButton.text = "함께 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unlock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.VISIBLE
                userPrefEdit.putBoolean("secretStatus", true).apply()

            } else {
                binding.secretButton.text = "나만 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock,
                    0,
                    0,
                    0
                )
                binding.secretInfoText.visibility = View.GONE
                userPrefEdit.putBoolean("secretStatus", false).apply()

            }
        })

        diaryFillCheck.moodFill.observe(requireActivity(), Observer { value ->
            if (diaryFillCheck.moodFill.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            } else if (!diaryFillCheck.moodFill.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

//            if (!partnerOrNot) {
            if (!editDiary) {
                if (diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                } else {
                    binding.diaryBtn.alpha = 0.4f
                    fulfilledOrNot = false
                }
//            }
            }

        })

        diaryFillCheck.recognitionFill.observe(requireActivity(), Observer { value ->
            if (diaryFillCheck.recognitionFill.value!!) {
                binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.recognitionFill.value!!) {
                binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

//            if (!partnerOrNot) {
            if (!editDiary) {
                if (diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                } else {
                    binding.diaryBtn.alpha = 0.4f
                    fulfilledOrNot = false
                }
            }
//            }
        })

        diaryFillCheck.diaryFill.observe(requireActivity(), Observer
        { value ->
            if (diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            } else if (!diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

//            if (!partnerOrNot) {
            if (!editDiary) {
                if (diaryFillCheck.diaryFill.value!! && diaryFillCheck.recognitionFill.value!! && diaryFillCheck.moodFill.value!!) {
                    binding.diaryBtn.alpha = 1f
                    fulfilledOrNot = true
                } else {
                    binding.diaryBtn.alpha = 0.4f
                    fulfilledOrNot = false
                }
            }
//            }
        })

        // 수정 시 필드별 변화
        diaryEditCheck.diaryEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryEditCheck.diaryEdit.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            } else {
                editOrNot = false
                binding.diaryBtn.alpha = 0.4f
            }
        })

        diaryEditCheck.moodEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.moodEdit.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryEditCheck.moodEdit.value!!) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            } else {
                editOrNot = false
                binding.diaryBtn.alpha = 0.4f
            }
        })

        diaryEditCheck.photoEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                editOrNot = true
                binding.diaryBtn.alpha = 1f
            } else {
                editOrNot = false
                binding.diaryBtn.alpha = 0.4f
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

        // 이미지
        newImageViewModel.newImageList.observe(requireActivity(), Observer
        { value ->
            if (newImageViewModel.newImageList.value!!.size == 0) {
//                binding.photoRecyclerView.visibility = View.GONE

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

                if (!editDiary) {
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

                            editDiary = true

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

                            val lineCount = binding.todayDiary.lineCount
                            val lineHeight = binding.todayDiary.lineHeight
                            val desiredHeight = lineCount * lineHeight

                            binding.todayDiary.height = desiredHeight
                            binding.todayDiary.setSelection(binding.todayDiary.text.length)

                            val currentDateTime = LocalDate.now().toString()
                            val userPref = mcontext.getSharedPreferences(
                                "diary_${userId}_${currentDateTime}",
                                Context.MODE_PRIVATE
                            )
                        } else {
                            // 일기 작성 x
//                            binding.diaryBtn.alpha = 0.4f
//                            binding.secretInfoText.visibility = View.GONE

//                            if (!partnerOrNot) {
                            editDiary = false
//                            fulfilledOrNot = false
                            binding.diaryBtn.text = "일기 작성"
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
            if(document.contains("todayStepCount")) {
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
            } else {
                binding.todayStepCount.text = "0 보"
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

                    val currentDateTime = LocalDate.now().toString()
                    val userPref = mcontext.getSharedPreferences(
                        "diary_${userId}_${currentDateTime}",
                        Context.MODE_PRIVATE
                    )
                    val userPrefEdit = userPref.edit()
                    val recognitionDonePref = userPref.getBoolean("recognitionDone", false)

                    if (char?.length == 1 && !editDiary && !recognitionDonePref) {

                        val currentDateTime = LocalDate.now().toString()
                        val userPref = mcontext.getSharedPreferences(
                            "diary_${userId}_${currentDateTime}",
                            Context.MODE_PRIVATE
                        )
                        val userPrefEdit = userPref.edit()
                        userPrefEdit.putBoolean("recognitionDone", true).apply()

                        diaryFillCheck.recognitionFill.value = true

                        val inputMethodManager =
                            context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

                        val realAnswer = calculationResult(firstNumber, secondNumber, operator)
                        val userAnswer = binding.userRecognitionText.text.trim()

                        dbRealAnswer = realAnswer.toString()
                        dbUserAnswer = userAnswer.toString()

                        userPrefEdit.putString("recognitionQuestion", recognitionQuestion).apply()
                        userPrefEdit.putString("realAnswer", dbRealAnswer).apply()
                        userPrefEdit.putString("writingRecognition", dbUserAnswer).apply()

                        if (realAnswer.toString() == userAnswer.toString()) {
                            userPrefEdit.putBoolean("recognitionResult", true).apply()
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
                            userPrefEdit.putBoolean("recognitionResult", false).apply()
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

                    val currentDateTime = LocalDate.now().toString()
                    val userPref = mcontext.getSharedPreferences(
                        "diary_${userId}_${currentDateTime}",
                        Context.MODE_PRIVATE
                    )
                    val userPrefEdit = userPref.edit()
                    userPrefEdit.putBoolean("moodDone", true).apply()
                    val writingMoodPref = userPref.getInt("writingMood", 0)

                    if (!editDiary) {
                        diaryFillCheck.moodFill.value = true
                    } else {
//                        diaryEditCheck.moodEdit.value = true

                        val nowMood = (binding.todayMood.selectedItem as Mood).position
                        if (nowMood != writingMoodPref) {
                            diaryEditCheck.moodEdit.value = true
                        }
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
        binding.todayDiary.setSelection(binding.todayDiary.text.length)
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
                    val currentDateTime = LocalDate.now().toString()
                    val userPref = mcontext.getSharedPreferences(
                        "diary_${userId}_${currentDateTime}",
                        Context.MODE_PRIVATE
                    )
                    val userPrefEdit = userPref.edit()

                    if (!editDiary) {
                        val stringDiary = char?.trim().toString()
                        val notEmptyCheck = stringDiary.isNotEmpty()
//                    val checkWriting = notEmptyCheck && !editDiary

                        //writingDiary
                        diaryFillCheck.diaryFill.value = notEmptyCheck
                        userPrefEdit.putBoolean("diaryDone", notEmptyCheck).apply()
                        userPrefEdit.putString("writingDiary", stringDiary).apply()
                    } else {

                        val writingDiaryPref = userPref.getString("writingDiary", "")

                        if (char != writingDiaryPref) {
                            diaryEditCheck.diaryEdit.value = true
                            editTodayDiaryEmpty = char?.trim().toString().isEmpty()
                        }
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                    // null
                }
            })


        // ** 종합 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {

//            val currentDateTime = LocalDate.now().toString()
//            val userPref = mcontext.getSharedPreferences("diary_${userId}_${currentDateTime}", Context.MODE_PRIVATE)
//            val userPrefEdit = userPref.edit()
//            userPrefEdit.clear().apply()

//            if (!partnerOrNot) {
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

            } else if (editDiary && editOrNot && editTodayDiaryEmpty) {
                val goMyDiaryWarning = Intent(context, MyDiaryWarningActivity::class.java)
                goMyDiaryWarning.putExtra("diary", false)
                startActivity(goMyDiaryWarning)
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
                    if (!editDiary) {
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
                                            .replace(R.id.enterFrameLayout, allFragment)
                                            .commit()

                                        requireActivity().bottomNav.selectedItemId =
                                            R.id.ourTodayMenu
                                    }
                            }
                    } else {
                        // 일기 수정
                        userDB.document("$userId").get()
                            .addOnSuccessListener { document ->
                                var username = document.data?.getValue("name")
                                var stepCount = document.data?.getValue("todayStepCount")

                                val diarySet = hashMapOf(
                                    "lastUpdate" to FieldValue.serverTimestamp(),
                                    "todayMood" to binding.todayMood.selectedItem,
                                    "todayDiary" to (binding.todayDiary.text.toString()),
                                    "images" to newImageViewModel.newImageList.value,
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
                                            .replace(R.id.enterFrameLayout, allFragment)
                                            .commit()

                                        requireActivity().bottomNav.selectedItemId =
                                            R.id.ourTodayMenu
                                    }
                            }
                    }
                }
            }
//            } else {
//                // 파트너
//                val goPartnerWarning =
//                    Intent(context, DefaultDiaryWarningActivity::class.java)
//                goPartnerWarning.putExtra("warningType", "partnerDiary")
//                startActivity(goPartnerWarning)
//            }

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

                        itemListItemsString.add(imageUri.toString())

                        val currentDateTime = LocalDate.now().toString()
                        val userPref = mcontext.getSharedPreferences(
                            "diary_${userId}_${currentDateTime}",
                            Context.MODE_PRIVATE
                        )
                        val userPrefEdit = userPref.edit()
                        userPrefEdit.putBoolean("imageDone", true)

                        val gson = Gson()
                        val arrayString = gson.toJson(itemListItemsString)

                        userPrefEdit.putString("imageArray", arrayString)
                        userPrefEdit.apply()
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
        womanIcon?.scaleType = ImageView.ScaleType.CENTER_INSIDE
        manIcon?.scaleType = ImageView.ScaleType.CENTER_INSIDE
        val womanLayoutParams = womanIcon?.layoutParams
        val manLayoutParams = manIcon?.layoutParams
        womanLayoutParams?.width = 100
        womanLayoutParams?.height = 150
        manLayoutParams?.width = 100
        manLayoutParams?.height = 150

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

        val currentDateTime = LocalDate.now().toString()
        val userPref = mcontext.getSharedPreferences(
            "diary_${userId}_${currentDateTime}",
            Context.MODE_PRIVATE
        )

        val moodDonePref = userPref.getBoolean("moodDone", false)
        val recognitionDonePref = userPref.getBoolean("recognitionDone", false)
        val diaryDonePref = userPref.getBoolean("diaryDone", false)
        val secretStatusPref = userPref.getBoolean("secretStatus", false)
        val imageDonePref = userPref.getBoolean("imageDone", false)

        fulfilledOrNot = recognitionDonePref && moodDonePref && diaryDonePref

        if (recognitionDonePref) {
            binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            val writingRecognitionPref = userPref.getString("writingRecognition", "")
            val firstNumberPref = userPref.getString("firstNumber", "")
            val secondNumberPref = userPref.getString("secondNumber", "")
            val operatorNumberPref = userPref.getString("operatorNumber", "")
            val recognitionQuestionPref = userPref.getString("recognitionQuestion", "")
            val recognitionResultPref = userPref.getBoolean("recognitionResult", false)
            val dbRealAnswerPref = userPref.getString("realAnswer", "")

            binding.firstNumber.text = firstNumberPref
            binding.secondNumber.text = secondNumberPref
            binding.operatorNumber.text = operatorNumberPref
            binding.userRecognitionText.setText(writingRecognitionPref)

            binding.userRecognitionText.isFocusable = false
            binding.userRecognitionText.isClickable = true

            recognitionResult = recognitionResultPref
            recognitionQuestion = recognitionQuestionPref!!
            firstNumber = firstNumberPref!!.toInt()
            secondNumber = secondNumberPref!!.toInt()
            operator = operatorNumberPref!!
            dbRealAnswer = dbRealAnswerPref!!
            dbUserAnswer = writingRecognitionPref!!

            if (!editDiary) {
                diaryFillCheck.recognitionFill.value = true
            } else {
                // no
            }
        }

        if (moodDonePref) {
            val writingMood = userPref.getInt("writingMood", 0)
            binding.todayMood.setSelection(writingMood)

            if (!editDiary) {
                diaryFillCheck.moodFill.value = true
            } else {
                diaryEditCheck.moodEdit.value = true
            }
        }

        if (diaryDonePref && !recordResume) {
            val writingDiary = userPref.getString("writingDiary", "")
            binding.todayDiary.setText(writingDiary)
            binding.todayDiary.setSelection(binding.todayDiary.text.length)

            if (!editDiary) {
                diaryFillCheck.diaryFill.value = true
            } else {
                diaryEditCheck.diaryEdit.value = true
            }
        }

        // 비밀 업데이트
        secretStatus = secretStatusPref

        if (!editDiary) {
            diaryFillCheck.secretFill.value = true
        } else {
            diaryEditCheck.secretEdit.value = true
        }

        if (imageDonePref && !photoResume && !recordResume) {
            val writingImage = userPref.getString("imageArray", "")
            val jsonString = """${writingImage}"""
            val gson = Gson()
            val array = gson.fromJson(jsonString, Array<String>::class.java)
            itemListItemsString = ArrayList(array.toList())

            for (item in array) {
                val uriItem = Uri.parse(item)
                newImageViewModel.addImage(uriItem)
                itemListItems.add(uriItem)
                photoAdapter.notifyItemInserted(itemListItems.size - 1)
            }

            if (!editDiary) diaryFillCheck.photoFill.value =
                true else diaryEditCheck.photoEdit.value = true
        }
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

        val currentDateTime = LocalDate.now().toString()
        val userPref = mcontext.getSharedPreferences(
            "diary_${userId}_${currentDateTime}",
            Context.MODE_PRIVATE
        )
        val userPrefEdit = userPref.edit()

        userPrefEdit.putString("writingDiary", binding.todayDiary.text.toString()).apply()

        val nowMood = (binding.todayMood.selectedItem as Mood).position
        userPrefEdit.putInt("writingMood", nowMood).apply()

        // 인지
        val recognitionDonePref = userPref.getBoolean("recognitionDone", false)
        userPrefEdit.putBoolean("recognitionDone", recognitionDonePref).apply()
        val writingRecognitionPref = userPref.getString("writingRecognition", "")
        userPrefEdit.putString("writingRecognition", writingRecognitionPref).apply()

        val firstNumber = binding.firstNumber.text.toString()
        val secondNumber = binding.secondNumber.text.toString()
        val operatorNumber = binding.operatorNumber.text.toString()

        userPrefEdit.putString("firstNumber", firstNumber).apply()
        userPrefEdit.putString("secondNumber", secondNumber).apply()
        userPrefEdit.putString("operatorNumber", operatorNumber).apply()
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

            val currentDateTime = LocalDate.now().toString()
            val userPref = mcontext.getSharedPreferences(
                "diary_${userId}_${currentDateTime}",
                Context.MODE_PRIVATE
            )
            val userPrefEdit = userPref.edit()
            userPrefEdit.putBoolean("secretStatus", newSecretStatus).apply()

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

            itemListItemsString.removeAt(deleteImagePosition)
            val currentDateTime = LocalDate.now().toString()
            val userPref = mcontext.getSharedPreferences(
                "diary_${userId}_${currentDateTime}",
                Context.MODE_PRIVATE
            )
            val userPrefEdit = userPref.edit()
            userPrefEdit.putBoolean("imageDone", true)

            val gson = Gson()
            val arrayString = gson.toJson(itemListItemsString)

            userPrefEdit.putString("imageArray", arrayString)
            userPrefEdit.apply()
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

    suspend fun thisMonthWriteCount() {
        currentMonth = LocalDateTime.now().toString().substring(0, 7)

        val currentdate = System.currentTimeMillis()
        val currentYearMonth = yearMonthDateFormat.format(currentdate)
        val currentMonth = SimpleDateFormat("MM").format(currentdate)
        removeZeroCurrentMonth = StringUtils.stripStart(currentMonth, "0");
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

//                binding.thisMonth.text = "${removeZeroCurrentMonth}월 작성일:"
                binding.diaryCount.text = spanText
            }
        }
    }

    suspend fun contractOrNotCheck() {
        val userDocument = userDB.document("$userId").get().await()

        val userRegion = userDocument.data?.getValue("region").toString()
        userSmallRegion = userDocument.data?.getValue("smallRegion").toString()
        userFullRegion = "${userRegion} ${userSmallRegion}"

        val contractRegionDocument =
            db.collection("contract_region").document(userFullRegion).get().await()
        contractRegionExists = contractRegionDocument.exists()

        contractRegionLogo = contractRegionDocument.data?.getValue("regionImage").toString()
    }

    suspend fun missionParticipateDate() {
        val userDocument = userDB.document("$userId").get().await()

        try {
            // 참여 이벤트 존재
            val participateMissionCheck = userDocument.contains("participateEventId")
            val participateMissionDocId =
                userDocument.data?.getValue("participateEventId").toString()
            val missionRef =
                db.collection("mission").document("$participateMissionDocId").get().await()

            if (participateMissionCheck) {
                participateCommunityLogo = missionRef.data?.getValue("communityLogo").toString()
                participateCommunityName = missionRef.data?.getValue("community").toString()
                participateMissionGoalScore =
                    (missionRef.data?.getValue("goalScore") as Long).toInt()
                participatePrizeWinners =
                    (missionRef.data?.getValue("prizeWinners") as Long).toInt()
                participateSmallDescription = missionRef.data?.getValue("description").toString()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                dateFormat.timeZone = android.icu.util.TimeZone.getTimeZone("Asia/Seoul")
                val missionStartDate = missionRef.data?.getValue("startPeriod").toString()
                val missionEndDate = missionRef.data?.getValue("endPeriod").toString()
                val missionState = missionRef.data?.getValue("state").toString()
                val now = Date()

                val pattern = "yyyy-MM-dd"
                formatPeriodStart = missionStartDate.replace(".", "-")
                mdDateFormatStartPeriod =
                    java.text.SimpleDateFormat(pattern).parse(formatPeriodStart)

                if (missionState == "진행") {

                    if (!missionEndDate.contains(".")) {
                        // 무제한 진행
                        val currentDateTime = LocalDate.now().toString()
                        formatPeriodEnd = currentDateTime
                        mdDateFormatEndPeriod =
                            java.text.SimpleDateFormat(pattern).parse(currentDateTime)

                        participateMissionExists = mdDateFormatStartPeriod.before(now)
                    } else {
                        // 종료일 존재
                        formatPeriodEnd = missionEndDate.replace(".", "-")
                        mdDateFormatEndPeriod =
                            java.text.SimpleDateFormat(pattern).parse(formatPeriodEnd)

                        participateMissionExists =
                            mdDateFormatStartPeriod.before(now) && mdDateFormatEndPeriod.after(now)
                    }
                } else {
                    participateMissionExists = false
                }
            } else {
                participateMissionExists = false
            }

        } catch (e: Exception) {
            // 참여 이벤트 없음
            participateMissionExists = false
        }
    }

    suspend fun certainUserStepCountToArrayFun() {

        val dataSteps = db.collection("user_step_count")
            .document(certainUserId)
            .get()
            .await()

        dataSteps.data?.forEach { (period, dateStepCount) ->
            if (dateStepCount.toString().toInt() < 10000) {
                if (dateStepCount.toString().toInt() > 0) {
                    // 걸음수 0~만보 사이 (일반)
                    val dateStepInt = (dateStepCount as Long).toInt()
                    val dateToPoint = ((Math.floor(dateStepInt / 1000.0)) * 10).toInt()

                    certainUserPoint += dateToPoint

                } else {
                    // 걸음수 0 보다 적은 경우
                }
            } else {
                // 걸음수 만보 보다 큰 경우
                certainUserPoint += 100
            }

        }
    }


    suspend fun certainUserDiaryToArrayFun() {

        val diaryDocuments = db.collection("diary")
            .whereEqualTo("userId", certainUserId)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            certainUserPoint += 100
        }
    }

    suspend fun certainUserCommentToArrayFun() {

        val commentDocuments = db.collectionGroup("comments")
            .whereEqualTo("userId", certainUserId)
            .get()
            .await()

        for (commentDocument in commentDocuments) {
            certainUserPoint += 20
        }
    }

    suspend fun stepCountToArrayFun() {
        val startDate = LocalDate.of(
            formatPeriodStart.substring(0, 4).toInt(),
            formatPeriodStart.substring(5, 7).toInt(),
            formatPeriodStart.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatPeriodEnd.substring(0, 4).toInt(),
            formatPeriodEnd.substring(5, 7).toInt(),
            formatPeriodEnd.substring(8, 10).toInt()
        )

        val dataSteps = db.collection("user_step_count")
            .document("$userId")
            .get()
            .await()


        dataSteps.data?.forEach { (period, dateStepCount) ->
            for (stepDate in startDate..endDate) {
                if (period == stepDate.toString()) {
                    if (dateStepCount.toString().toInt() < 10000) {
                        if (dateStepCount.toString().toInt() > 0) {
                            // 걸음수 0~만보 사이 (일반)
                            val dateStepInt = (dateStepCount as Long).toInt()
                            val dateToPoint = ((Math.floor(dateStepInt / 1000.0)) * 10).toInt()

                            userPoint += dateToPoint

                        } else {
                            // 걸음수 0 보다 적은 경우
                        }
                    } else {
                        // 걸음수 만보 보다 큰 경우
                        userPoint += 100
                    }
                }

            }
        }
    }


    suspend fun diaryToArrayFun() {
        val diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", mdDateFormatStartPeriod)
            .whereLessThanOrEqualTo("timestamp", mdDateFormatEndPeriod)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            userPoint += 100
        }
    }

    suspend fun commentToArrayFun() {
        val commentDocuments = db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", mdDateFormatStartPeriod)
            .whereLessThanOrEqualTo("timestamp", mdDateFormatEndPeriod)
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

