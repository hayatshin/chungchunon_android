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
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.*
import android.util.Log
import android.view.*
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent.ACTION_MOVE
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.text.bold
import androidx.core.text.color
import androidx.fragment.app.Fragment
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.R
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
import com.chugnchunon.chungchunon_android.Adapter.UploadPhotosAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.MonthDate
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.LockDiaryActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_diary_two.*
import org.apache.commons.lang3.StringUtils
import kotlin.collections.ArrayList

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

    companion object {
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

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepAuthGrantedReceiver,
            IntentFilter("STEP_AUTH_UPDATE")
        );

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

        // StepCount Notification Receiver: 변경된 걸음수 UI 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepCountUpdateReceiver,
            IntentFilter(MyService.ACTION_STEP_COUNTER_NOTIFICATION)
        )

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


        // 일기 초기 세팅
        binding.diaryBtn.isEnabled = false
        binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)

        // 필드별 작성 시 변화

        diaryFillCheck.secretFill.observe(requireActivity(), Observer
        { value ->
            if (diaryFillCheck.secretFill.value!!) {
                binding.secretButton.text = "함께 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unlock,
                    0,
                    0,
                    0
                )

            } else {
                binding.secretButton.text = "나만 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock,
                    0,
                    0,
                    0
                )

            }
        })

        diaryFillCheck.diaryFill.observe(requireActivity(), Observer
        { value ->
            if (diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.diaryFill.value!!) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            binding.diaryBtn.isEnabled = diaryFillCheck.diaryFill.value!!
        })

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            secretOrNotFunction,
            IntentFilter("SECRET_OR_NOT")
        );

        // 수정 시 필드별 변화
        diaryEditCheck.diaryEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        diaryEditCheck.moodEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.moodEdit.value == true) {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        diaryEditCheck.photoEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        diaryEditCheck.secretEdit.observe(requireActivity(), Observer
        { value ->
            if (diaryEditCheck.secretEdit.value == true) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true || diaryEditCheck.secretEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        // 이미지
        newImageViewModel.newImageList.observe(requireActivity(), Observer
        { value ->
            if (newImageViewModel.newImageList.value!!.size == 0) {
                binding.photoRecyclerView.visibility = View.GONE
            }
        })

        newImageViewModel.uploadFirebaseComplete.observe(requireActivity(), Observer
        { value ->

            if (newImageViewModel.uploadFirebaseComplete.value == true) {

                val currentMilliseconds = System.currentTimeMillis()
                val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
                val writeTime = LocalDateTime.now()
                val diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        val username = document.data?.getValue("name")
                        val stepCount = document.data?.getValue("todayStepCount")
                        val region = document.data?.getValue("region")
                        val smallRegion = document.data?.getValue("smallRegion")
                        val regionGroup = "${region} ${smallRegion}"
                        val secretStatus = diaryFillCheck.secretFill.value

                        val diarySet = hashMapOf(
                            "regionGroup" to regionGroup,
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
                                fragment.beginTransaction()
                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu
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

        binding.todayDate.text = "${monthUI}월 ${dateUI}일 (${DateFormat().doDayOfWeek()})"

        diaryDB
            .document("${userId}_${writeTime}")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null) {
                        if (document.exists()) {
                            // 일기 작성을 한 상태
                            val originalDiary = document.data?.getValue("todayDiary").toString()

                            binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                            binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)


                            // 이미지 보여주기
                            if (document.data?.contains("images") == true) {
                                oldImageList =
                                    document.data?.getValue("images") as ArrayList<String>

                                if (oldImageList.size != 0) {
//                                    binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)


                                    for (i in 0 until oldImageList.size) {
                                        var uriParseImage = Uri.parse(oldImageList[i])
                                        newImageViewModel.addImage(uriParseImage)
                                    }
                                    photoAdapter = UploadPhotosAdapter(mcontext, oldImageList)
                                    binding.photoRecyclerView.adapter = photoAdapter
                                    photoAdapter.notifyItemInserted(oldImageList.size -1)
//                                    photoAdapter.notifyDataSetChanged()
                                    binding.photoRecyclerView.visibility = View.VISIBLE
                                }


                            } else {
                                // null
                                binding.photoRecyclerView.visibility = View.GONE
                            }

                            editDiary = true
                            binding.diaryBtn.isEnabled = false
                            binding.diaryBtn.text = "일기 수정"

                            // 숨기기 보여주기
                            val secretStatus = document.data?.getValue("secret") as Boolean
                            diaryFillCheck.secretFill.value = secretStatus

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
                            editDiary = false
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
            } else {
                binding.stepCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

        }

        // 사진 업로드
        fun selectGallery() {
            val readPermission = ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            if (readPermission == PackageManager.PERMISSION_DENIED) {
                // 권한 요청
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQ_GALLERY)
            } else {
                val intent = Intent(Intent.ACTION_PICK)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                startActivityForResult(intent, REQ_MULTI_PHOTO)
            }
        }

        // 사진 가져오기 권한 체크
        binding.photoButton.setOnClickListener {
            selectGallery()
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
                                R.color.main_color
                            )
                        ) { append(calendarThisMonthCount) }
                    }
                    .append(" / ${currentMonthDate}일")

                binding.thisMonth.text = "${removeZeroCurrentMonth}월 작성일"
                binding.diaryCount.text = spanText
            }
        }

        // 기분 스니퍼
        binding.todayMood.adapter = activity?.applicationContext?.let {
            MoodArrayAdapter(
                it,
                listOf(
                    Mood(R.drawable.ic_joy, "기뻐요", 0),
                    Mood(R.drawable.ic_shalom, "평온해요", 1),
                    Mood(R.drawable.ic_throb, "설레요", 2),
                    Mood(R.drawable.ic_soso, "그냥 그래요", 3),
                    Mood(R.drawable.ic_anxious, "걱정돼요", 4),
                    Mood(R.drawable.ic_sad, "슬퍼요", 5),
                    Mood(R.drawable.ic_gloomy, "우울해요", 6),
                    Mood(R.drawable.ic_angry, "화나요", 7),
                )
            )
        }

        binding.todayMood.setOnTouchListener(
            object : View.OnTouchListener {
                override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                    binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                    return false
                }
            })

        // 글 숨기기
        binding.secretButton.setOnClickListener {
            val goLockDiary = Intent(requireActivity(), LockDiaryActivity::class.java)
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


        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {
            binding.diaryBtn.text = ""
            binding.diaryProgressBar.visibility = View.VISIBLE

            val currentMilliseconds = System.currentTimeMillis()
            val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
            val writeTime = LocalDateTime.now()
            val diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

            if (diaryFillCheck.photoFill.value == true || diaryEditCheck.photoEdit.value == true) {

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

                if (newImageViewModel.newImageList.value!!.all { it ->
                        it.toString().startsWith("http://")
                    }) {
                    newImageViewModel.uploadFirebaseComplete.value = true
                }

            } else {
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var username = document.data?.getValue("name")
                        var stepCount = document.data?.getValue("todayStepCount")
                        val region = document.data?.getValue("region")
                        val smallRegion = document.data?.getValue("smallRegion")
                        val regionGroup = "${region} ${smallRegion}"
                        val secretStatus = diaryFillCheck.secretFill.value

                        val diarySet = hashMapOf(
                            "regionGroup" to regionGroup,
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
                                fragment.beginTransaction()
                                    .replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
                                requireActivity().bottomNav.selectedItemId = R.id.ourTodayMenu
                            }
                    }
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
                if (data?.clipData != null) {
                    val count = data!!.clipData!!.itemCount

                    for (i in 0 until count) {
                        val imageUri = data?.clipData!!.getItemAt(i).uri
                        newImageViewModel.addImage(imageUri)
                        if (!editDiary) diaryFillCheck.photoFill.value =
                            true else diaryEditCheck.photoEdit.value = true
                        photoAdapter = UploadPhotosAdapter(
                            mcontext,
                            newImageViewModel.newImageList.value!!
                        )
                        binding.photoRecyclerView.adapter = photoAdapter
                        photoAdapter.notifyItemInserted(i)
                    }
//                    photoAdapter.notifyDataSetChanged()
                    binding.photoRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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

        // 사진 삭제
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            deleteImageFunction,
            IntentFilter("DELETE_IMAGE")
        );

        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                val userType = document.data?.getValue("userType").toString()
                if (userType == "파트너") {
                    binding.partnerBlock.visibility = View.VISIBLE

                    binding.todayMood.isEnabled = false
                    binding.todayDiary.isEnabled = false
                    binding.recordBtn.isEnabled = false
                    binding.photoButton.isEnabled = false
                    binding.diaryBtn.isEnabled = false

                    val activity = activity
                    if (activity != null) {
                        val window = requireActivity().window
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                        window.setStatusBarColor(Color.parseColor("#B3000000"))
                    }
                } else binding.partnerBlock.visibility = View.GONE
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
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(secretOrNotFunction)
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(stepCountUpdateReceiver)
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

            val secretOrNot = intent?.getBooleanExtra("secretOrNot", false)

            if (!editDiary) {
                diaryFillCheck.secretFill.value = secretOrNot
            } else {
                diaryEditCheck.secretEdit.value = secretOrNot
            }
        }

    }

    private var deleteImageFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val deleteImagePosition = intent?.getIntExtra("deleteImagePosition", 0)
            newImageViewModel.removeImage(deleteImagePosition!!.toInt())
            photoAdapter.notifyItemRemoved(deleteImagePosition!!.toInt())

            if (!editDiary) diaryFillCheck.photoFill.value =
                true else diaryEditCheck.photoEdit.value = true
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

class DiaryFillClass : ViewModel() {
    val diaryFill by lazy { MutableLiveData<Boolean>(false) }
    val moodFill by lazy { MutableLiveData<Boolean>(false) }
    val photoFill by lazy { MutableLiveData<Boolean>(false) }
    val secretFill by lazy { MutableLiveData<Boolean>(false) }
}

class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
    val photoEdit by lazy { MutableLiveData<Boolean>(false) }
    val secretEdit by lazy { MutableLiveData<Boolean>(false) }
}

