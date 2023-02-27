package com.chugnchunon.chungchunon_android.Fragment

import com.chugnchunon.chungchunon_android.MyService
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
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.*
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.color
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
import com.chugnchunon.chungchunon_android.BroadcastReceiver.DiaryUpdateBroadcastReceiver
import com.chugnchunon.chungchunon_android.BroadcastReceiver.StepCountBroadcastReceiver
import com.chugnchunon.chungchunon_android.DataClass.MonthDate
import com.chugnchunon.chungchunon_android.DiaryTwoActivity
import com.chugnchunon.chungchunon_android.NewImageViewModel
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

//    private lateinit var sensorManager: SensorManager
//    private lateinit var step_sensor: Sensor

//    lateinit var diaryUpdateBroadcastReceiver: DiaryUpdateBroadcastReceiver

    private var currentMonth: String = ""
    private val model: BaseViewModel by viewModels()

    private var todayTotalStepCount: Int = 0
    private val yearMonthDateFormat = SimpleDateFormat("yyyy-MM")

    lateinit var diaryFillCheck: DiaryFillClass
    lateinit var diaryEditCheck: DiaryEditClass
    lateinit var newImageViewModel: NewImageViewModel


    // 갤러리 사진 열람
    companion object {
        private var editDiary: Boolean = false
        const val REQ_GALLERY = 200
    }

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var photoAdapter: UploadPhotosAdapter
    private var oldImageList: ArrayList<String> = ArrayList()

    lateinit var mcontext: Context


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

//        userDB.document("$userId").get()
//            .addOnSuccessListener { document ->
//                var userType = document.data?.getValue("userType").toString()
//                if(userType == "파트너") {
//                    binding.partnerBlock.visibility = View.VISIBLE

//                    var activity = activity
//                    if(activity != null) {
//                        var window = requireActivity().window
//                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//                        window.setStatusBarColor(Color.parseColor("#99000000"));
//                    }
//                } else binding.partnerBlock.visibility = View.GONE
//            }

        // DiaryUpdateBroadcastReceiver : 다이어리 업데이트
//        diaryUpdateBroadcastReceiver = DiaryUpdateBroadcastReceiver()

//        val diaryChangeIntent = IntentFilter()
//        diaryChangeIntent.addAction(Intent.ACTION_TIME_TICK)
//        activity?.registerReceiver(diaryUpdateBroadcastReceiver, diaryChangeIntent)

        // StepCount Notification Receiver: 변경된 걸음수 UI 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            receiver,
            IntentFilter(MyService.ACTION_STEP_COUNTER_NOTIFICATION)
        )

        // 이미지 애니메이션
        var womanIcon = binding.womanIcon
        var manIcon = binding.manIcon

        var womananimation = ObjectAnimator.ofFloat(womanIcon, "rotation", 10F)
        womananimation.setDuration(300)
        womananimation.repeatCount = 2
        womananimation.interpolator = LinearInterpolator()
        womananimation.start()

        var manAnimation = ObjectAnimator.ofFloat(manIcon, "rotation", -10F)
        manAnimation.setDuration(300)
        manAnimation.repeatCount = 2
        manAnimation.interpolator = LinearInterpolator()
        manAnimation.start()

        diaryFillCheck = ViewModelProvider(requireActivity()).get(DiaryFillClass::class.java)
        diaryEditCheck = ViewModelProvider(requireActivity()).get(DiaryEditClass::class.java)
        newImageViewModel = ViewModelProvider(this).get(NewImageViewModel::class.java)


        // 이미지 삭제 로컬 브로드캐스트


//        var startService = Intent(activity, MyService::class.java)
//        activity?.let { ContextCompat.startForegroundService(it, startService) }


        binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)

//        if(!editDiary) {
//            binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
//            binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
//        } else {
//            binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
//            binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
//        }


        binding.diaryBtn.isEnabled = false

        diaryFillCheck.diaryFill.observe(requireActivity(), Observer { value ->
            if (diaryFillCheck.diaryFill.value!! && !editDiary) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
            } else if (!diaryFillCheck.diaryFill.value!! && !editDiary) {
                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
            }

            binding.diaryBtn.isEnabled = diaryFillCheck.diaryFill.value!! && !editDiary
        })

        // 수정
        diaryEditCheck.diaryEdit.observe(requireActivity(), Observer { value ->

            if (diaryEditCheck.diaryEdit.value == true) binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })

        diaryEditCheck.moodEdit.observe(requireActivity(), Observer { value ->

            if (diaryEditCheck.moodEdit.value == true) binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true) {
                binding.diaryBtn.isEnabled = true

                var nowMood = (binding.todayMood.selectedItem as Mood).image
                Log.d("체크", "$nowMood")
            }
        })

        diaryEditCheck.photoEdit.observe(requireActivity(), Observer { value ->
            Log.d("수정확인 edit", "${diaryEditCheck.photoEdit.value}")
            if (diaryEditCheck.photoEdit.value == true) binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            if (diaryEditCheck.diaryEdit.value == true || diaryEditCheck.moodEdit.value == true || diaryEditCheck.photoEdit.value == true) {
                binding.diaryBtn.isEnabled = true
            }
        })


        newImageViewModel.newImageList.observe(requireActivity(), Observer { value ->
            binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            if (newImageViewModel.newImageList.value!!.size == 0) {
                binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_no)
                binding.photoRecyclerView.visibility = View.GONE
            }
        })

        newImageViewModel.uploadFirebaseComplete.observe(requireActivity(), Observer { value ->

            if (newImageViewModel.uploadFirebaseComplete.value == true) {

                var currentMilliseconds = System.currentTimeMillis()
                val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
                val writeTime = LocalDateTime.now()
                var diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"


                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var username = document.data?.getValue("name")
                        var stepCount = document.data?.getValue("todayStepCount")
                        var region = document.data?.getValue("region")
                        var smallRegion = document.data?.getValue("smallRegion")
                        var regionGroup = "${region} ${smallRegion}"
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
                        )

                        diaryDB
                            .document(diaryId)
                            .set(diarySet, SetOptions.merge())
                            .addOnSuccessListener {
                               var fragment = requireActivity().supportFragmentManager
                                fragment.beginTransaction().replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
                                requireActivity().bottomNav.selectedItemId =  R.id.ourTodayMenu
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

        // 요일
        val sdf = java.text.SimpleDateFormat("EEE")
        val dayOfTheWeek = sdf.format(Date())

        binding.todayDate.text = "${monthUI}월 ${dateUI}일 (${dayOfTheWeek})"


        diaryDB
            .document("${userId}_${writeTime}")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null) {
                        if (document.exists()) {
                            // 일기 작성을 한 상태

                            // 이미지 보여주기
                            if (document.data?.contains("images") == true) {
                                oldImageList =
                                    document.data?.getValue("images") as ArrayList<String>

                                if(oldImageList.size != 0) {
                                    binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)


                                    for (i in 0 until oldImageList.size) {
                                        var uriParseImage = Uri.parse(oldImageList[i])
                                        newImageViewModel.addImage(uriParseImage)
                                    }
                                    photoAdapter = UploadPhotosAdapter(mcontext, oldImageList)
                                    binding.photoRecyclerView.adapter = photoAdapter
                                    photoAdapter.notifyDataSetChanged()
                                    binding.photoRecyclerView.visibility = View.VISIBLE
                                }


                            } else {
                                // null
                                binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
                                binding.photoRecyclerView.visibility = View.GONE
                            }

                            binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                            binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                            editDiary = true
                            binding.diaryBtn.isEnabled = false
                            binding.diaryBtn.text = "일기 수정"

                            // 일기 보여주기
                            var oldDiary = document.data?.getValue("todayDiary").toString()
                            binding.todayDiary.setText(oldDiary)

                            // 마음 보여주기
                            var spinnerAdapter = binding.todayMood.adapter
                            var dbMoodPosition =
                                (document.data?.getValue("todayMood") as Map<*, *>)["position"].toString()
                                    .toInt()
                            binding.todayMood.setSelection(dbMoodPosition)
//                            binding.todayMood.setSelection(selectedPosition)

                            binding.todayMood.setOnItemSelectedListener(object :
                                AdapterView.OnItemSelectedListener {
                                override fun onItemSelected(
                                    p0: AdapterView<*>?,
                                    p1: View?,
                                    p2: Int,
                                    p3: Long
                                ) {
                                    var nowMood = (binding.todayMood.selectedItem as Mood).position
                                    if (nowMood != dbMoodPosition) {
                                        diaryEditCheck.moodEdit.value = true
                                    }
                                }

                                override fun onNothingSelected(p0: AdapterView<*>?) {
                                    // null
                                }

                            })



                            binding.todayDiary.addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(
                                    p0: CharSequence?,
                                    p1: Int,
                                    p2: Int,
                                    p3: Int
                                ) {
                                    // null
                                }

                                override fun onTextChanged(
                                    char: CharSequence?,
                                    p1: Int,
                                    p2: Int,
                                    p3: Int
                                ) {
                                    if (binding.todayDiary.text.toString() != oldDiary) {
                                        diaryEditCheck.diaryEdit.value = true
                                    }
                                }

                                override fun afterTextChanged(p0: Editable?) {
                                    // null
                                }
                            })


                        } else {
                            // 일기 작성 놉
                        }
                    }
                } else {
                    // 일기 작성 놉
                }
            }

        // 하루 걸음수 초기화

        var stepInitializeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                binding.todayStepCount.text = "0 보"
            }

        }

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            stepInitializeReceiver,
            IntentFilter("NEW_DATE_STEP_ZERO")
        );

        userDB.document("$userId").get().addOnSuccessListener { document ->
            var todayStepCountFromDB = (document.data?.getValue("todayStepCount") as Long).toInt()
            Log.d("걸음수 결과", "$todayStepCountFromDB")
            todayTotalStepCount = todayStepCountFromDB
            var decimal = DecimalFormat("#,###")
            var step = decimal.format(todayTotalStepCount)
            binding.todayStepCount.text = "$step 보"
        }

        currentMonth = LocalDateTime.now().toString().substring(0, 7)


        // 사진 업로드
        fun openGalleryForImages() {
            Log.d("이미지", "오픈갤러리")
            var intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            activityResultLauncher.launch(intent)
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    if (it.data?.clipData != null) {
                        binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                        val count = it.data!!.clipData!!.itemCount

                        for (i in 0 until count) {
                            val imageUri = it.data?.clipData!!.getItemAt(i).uri
                            newImageViewModel.addImage(imageUri)
                            if(!editDiary) diaryFillCheck.photoFill.value = true else diaryEditCheck.photoEdit.value = true
                        }

                        photoAdapter = UploadPhotosAdapter(
                            mcontext,
                            newImageViewModel.newImageList.value!!
                        )
                        binding.photoRecyclerView.adapter = photoAdapter
                        photoAdapter.notifyDataSetChanged()

                        binding.photoRecyclerView.visibility = View.VISIBLE
                        binding.photoRecyclerView.alpha = 0f
                        binding.photoRecyclerView.y = -50f

                        binding.photoRecyclerView.animate()
                            .translationY(0f)
                            .setDuration(500)
                            .setInterpolator(LinearInterpolator())
                            .start()

                        binding.photoRecyclerView.animate()
                            .alpha(1f)
                            .setDuration(600)
                            .setInterpolator(LinearInterpolator())
                            .start()
                    }
                }
            }


        fun selectGallery() {
            val writePermission = ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readPermission = ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            // 권한 확인
            if (writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    1
                )

                // 권한이 있는 경우 실행
                openGalleryForImages()
            } else {
                openGalleryForImages()
            }
        }


        // 사진 가져오기 권한 체크
        binding.photoButton.setOnClickListener {
            selectGallery()
        }




        // 매달 일기 작성
        var currentdate = System.currentTimeMillis()
        var currentYearMonth = yearMonthDateFormat.format(currentdate)
        var currentMonth = SimpleDateFormat("MM").format(currentdate)
        var removeZeroCurrentMonth = StringUtils.stripStart(currentMonth, "0");
        var currentMonthDate = MonthDate(currentMonth.toInt()).getDate


        var thisMonthCount = diaryDB
            .whereEqualTo("monthDate", currentYearMonth)
            .whereEqualTo("userId", userId)
            .count()

        thisMonthCount.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val calendarThisMonthCount = "${task.result.count}일"
                var spanText = SpannableStringBuilder()
                    .color(Color.RED) { append("${calendarThisMonthCount}") }
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


        binding.todayMood.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                return false
            }
        })

        // 음성녹음

        model.initial(textToSpeechEngine, startForResult)

        binding.recordBtn.setOnClickListener {
            model.displaySpeechRecognizer()
//            val text = todayDiary.text?.trim().toString()
//            model.speak(if (text.isNotEmpty()) text else "일기를 써보세요")
        }

        // 다이어리 작성
        binding.todayDiary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {
                diaryFillCheck.diaryFill.value = char?.length != 0
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }
        })

        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {
            binding.diaryBtn.text = ""
            binding.diaryProgressBar.visibility = View.VISIBLE

            var currentMilliseconds = System.currentTimeMillis()
            val writeMonthDate = yearMonthDateFormat.format(currentMilliseconds)
            val writeTime = LocalDateTime.now()
            var diaryId = "${userId}_${writeTime.toString().substring(0, 10)}"

            Log.d("수정수정1", "${diaryFillCheck.photoFill.value}")
            Log.d("수정수정2", "${diaryEditCheck.photoEdit.value}")

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
                    Log.d("올리기", "모두 https://")
                    newImageViewModel.uploadFirebaseComplete.value = true
                }

            } else {
                userDB.document("$userId").get()
                    .addOnSuccessListener { document ->
                        var username = document.data?.getValue("name")
                        var stepCount = document.data?.getValue("todayStepCount")
                        var region = document.data?.getValue("region")
                        var smallRegion = document.data?.getValue("smallRegion")
                        var regionGroup = "${region} ${smallRegion}"
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
                        )

                        diaryDB
                            .document(diaryId)
                            .set(diarySet, SetOptions.merge())
                            .addOnSuccessListener {
                                var fragment = requireActivity().supportFragmentManager
                                fragment.beginTransaction().replace(R.id.enterFrameLayout, AllDiaryFragmentTwo()).commit()
                                requireActivity().bottomNav.selectedItemId =  R.id.ourTodayMenu
                            }
                    }
            }


        }



        view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(view: View?, event: MotionEvent?): Boolean {
                val imm: InputMethodManager =
                    activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view!!.windowToken, 0)
                return true
            }
        })
        return view
    }

    @SuppressLint("SetTextI18n")
    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText: String? =
                result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    .let { text -> text?.get(0) }
            var recordDiaryText =
                if ("${binding.todayDiary.text}" == "") "${spokenText}" else "${binding.todayDiary.text} ${spokenText}"
            binding.todayDiary.setText(recordDiaryText)
        }
    }

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(context) {
            if (it == TextToSpeech.SUCCESS) textToSpeechEngine.language = Locale("in_ID")
        }
    }

    override fun onResume() {
        super.onResume()
        // 이미지 애니메이션
        var womanIcon = view?.findViewById<ImageView>(R.id.womanIcon)
        var manIcon = view?.findViewById<ImageView>(R.id.manIcon)

        var womananimation = ObjectAnimator.ofFloat(womanIcon, "rotation", 0f, 20f, 0f)
        womananimation.setDuration(500)
        womananimation.repeatCount = 2
        womananimation.interpolator = LinearInterpolator()
        womananimation.start()


        var manAnimation = ObjectAnimator.ofFloat(manIcon, "rotation", 0f, -20F, 0f)
        manAnimation.setDuration(500)
        manAnimation.repeatCount = 2
        manAnimation.interpolator = LinearInterpolator()
        manAnimation.start()


        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            deleteImageFunction,
            IntentFilter("DELETE_IMAGE")
        );


        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                var userType = document.data?.getValue("userType").toString()
                if(userType == "파트너") {
                    binding.partnerBlock.visibility = View.VISIBLE

                    binding.todayMood.isEnabled = false
                    binding.todayDiary.isEnabled = false
                    binding.recordBtn.isEnabled = false
                    binding.photoButton.isEnabled = false
                    binding.diaryBtn.isEnabled = false

                    var activity = activity
                    if(activity != null) {
                        var window = requireActivity().window
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(Color.parseColor("#99000000"));
                    }
                } else binding.partnerBlock.visibility = View.GONE
            }
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireActivity())
            .unregisterReceiver(deleteImageFunction);

        var activity = activity
        if(activity != null) {
            var window = requireActivity().window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.WHITE);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
        LocalBroadcastManager.getInstance(requireActivity())
            .unregisterReceiver(deleteImageFunction);
//        activity?.unregisterReceiver(diaryUpdateBroadcastReceiver)

    }


    private fun uploadImageToFirebase(fileUri: Uri, position: Int) {
        if (fileUri != null) {
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
    }

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {

            intent?.let {
                val todayTotalStepCount = it.getIntExtra("todayTotalStepCount", 0)
                var decimal = DecimalFormat("#,###")
                var step = decimal.format(todayTotalStepCount)
                binding.todayStepCount.text = "$step 보"

                Log.d("걸음수", "$todayTotalStepCount")

                if (todayTotalStepCount >= 3000) {
                    binding.stepCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
                }
            }
        }
    }


    // 갤러리
    private var deleteImageFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var deleteImagePosition = intent?.getIntExtra("deleteImagePosition", 0)

            newImageViewModel.removeImage(deleteImagePosition!!.toInt())
            photoAdapter.notifyDataSetChanged()

            if(!editDiary) diaryFillCheck.photoFill.value = true else diaryEditCheck.photoEdit.value = true

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
}

class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
    val photoEdit by lazy { MutableLiveData<Boolean>(false) }
}

