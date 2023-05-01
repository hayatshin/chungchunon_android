package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.Adapter.UploadEditPhotosAdapter
import com.chugnchunon.chungchunon_android.Adapter.UploadPhotosAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.Fragment.AllDiaryFragmentTwo.Companion.resumePause
import com.chugnchunon.chungchunon_android.Fragment.LinearLayoutManagerWrapper
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.ViewModel.BaseViewModel
import com.chugnchunon.chungchunon_android.databinding.ActivityEditDiaryBinding
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.mutable.MutableBoolean
import java.util.*
import kotlin.collections.ArrayList

class EditDiaryActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    private val binding by lazy {
        ActivityEditDiaryBinding.inflate(layoutInflater)
    }

    lateinit var diaryEditCheck: DiaryEditClass
    lateinit var newImageViewModel: EditNewImageViewModel

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var photoAdapter: UploadEditPhotosAdapter
    private var newDiarySet = hashMapOf<String, Any>()
    private var itemListItems: ArrayList<Any> = ArrayList()

    private val model: BaseViewModel by viewModels()

    // 이미지 관리
    lateinit var oldImageList: ArrayList<String>
    private var newImageList: ArrayList<String> = ArrayList()
    private var editButtonClick: Boolean = false

//    companion object {
//        private var secretStatus : Boolean = false
//    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var diaryType = intent.getStringExtra("diaryType")

        binding.backBtn.setOnClickListener {
            finish()
        }

        newImageViewModel = ViewModelProvider(this).get(EditNewImageViewModel::class.java)

        photoAdapter = UploadEditPhotosAdapter(this, itemListItems)
        binding.photoRecyclerView.layoutManager = LinearLayoutManagerWrapper(
            this,
            RecyclerView.HORIZONTAL,
            false
        )
        binding.photoRecyclerView.adapter = photoAdapter

        var editDiaryId = intent.getStringExtra("editDiaryId")

        binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
        binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

        diaryEditCheck = ViewModelProvider(this).get(DiaryEditClass::class.java)

        binding.diaryBtn.isEnabled = false

        // 수정

        diaryEditCheck.secretEdit.observe(this, Observer { value ->
            if (diaryEditCheck.secretEdit.value == true) binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

            if (diaryEditCheck.secretEdit.value!!) {

                binding.secretButton.text = "함께 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_unlock,
                    0,
                    0,
                    0
                )
                binding.secreteNotificationText.text =
                    getString(R.string.secret_unhide_notification)

                binding.secretConfirmBox.setOnClickListener {
                    diaryEditCheck.secretEdit.value = false
                    binding.secretNotificationLayout.visibility = View.GONE
                    window.setStatusBarColor(Color.WHITE);
                    binding.diaryBtn.isEnabled = true
                }
            } else {

                binding.secretButton.text = "나만 보기"
                binding.secretButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_lock,
                    0,
                    0,
                    0
                )
                binding.secreteNotificationText.text = getString(R.string.secret_hide_notification)

                binding.secretConfirmBox.setOnClickListener {
                    diaryEditCheck.secretEdit.value = true
                    binding.secretNotificationLayout.visibility = View.GONE
                    window.setStatusBarColor(Color.WHITE);
                    binding.diaryBtn.isEnabled = true
                }
            }
        })

        diaryEditCheck.diaryEdit.observe(this, Observer { value ->
            binding.diaryBtn.isEnabled = true
            if (diaryEditCheck.diaryEdit.value == true) binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
        })

        diaryEditCheck.moodEdit.observe(this, Observer { value ->
            binding.diaryBtn.isEnabled = true
            if (diaryEditCheck.moodEdit.value == true) binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
        })

        diaryEditCheck.photoEdit.observe(this, Observer { value ->
            binding.diaryBtn.isEnabled = true
        })


        newImageViewModel.newImageList.observe(this, Observer { value ->

            if (newImageViewModel.newImageList.value!!.size == 0) {
                binding.photoRecyclerView.visibility = View.GONE
            }
        })

        newImageViewModel.uploadFirebaseComplete.observe(this, Observer { value ->

            if (newImageViewModel.uploadFirebaseComplete.value == true) {

                newDiarySet.put("images", newImageViewModel.newImageList.value!!)

                diaryDB.document("$editDiaryId")
                    .set(newDiarySet, SetOptions.merge())
                    .addOnSuccessListener {
                        val goAllDiary = Intent(this, DiaryTwoActivity::class.java)
                        goAllDiary.putExtra("from", "edit")
                        goAllDiary.putExtra("diaryType", diaryType)
                        startActivity(goAllDiary)
                    }
            }
        })

        diaryDB.document("$editDiaryId")
            .get()
            .addOnSuccessListener { document ->

                var diaryTimestamp =
                    document.data!!.getValue("timestamp") as com.google.firebase.Timestamp
                var diaryDate = DateFormat().convertTimeStampToDate(diaryTimestamp)

                val year = diaryDate.substring(0, 4)
                val month = diaryDate.substring(5, 7)
                val date = diaryDate.substring(8, 10)
                val monthUI = StringUtils.stripStart(month, "0");
                val dateUI = StringUtils.stripStart(date, "0");

                val sdf = java.text.SimpleDateFormat("EEE")
                val dateFormat = sdf.format(Date())
                val dateOfWeek = sdf.format(diaryTimestamp.nanoseconds)

                binding.todayDate.text = "${monthUI}월 ${dateUI}일 (${dateOfWeek})"

                // 인지 보여주기
                db.collection("recognition")
                    .document(editDiaryId!!)
                    .get()
                    .addOnSuccessListener { recogDocument ->
                        if (recogDocument.exists()) {
                            // 있을 경우

                            binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                            binding.mathLayout.visibility = View.VISIBLE
                            binding.noRecognitionLayout.visibility = View.GONE

                            val oldFirstNumber =
                                recogDocument.data?.getValue("firstNumber").toString()
                            val oldSecondNumber =
                                recogDocument.data?.getValue("secondNumber").toString()
                            val oldOperator = recogDocument.data?.getValue("operator").toString()
                            val oldUserAnswer =
                                recogDocument.data?.getValue("userAnswer").toString()

                            binding.firstNumber.text = oldFirstNumber
                            binding.secondNumber.text = oldSecondNumber
                            binding.operatorNumber.text = oldOperator
                            binding.userRecognitionText.setText(oldUserAnswer)
                            binding.userRecognitionText.isFocusable = false
                            binding.userRecognitionText.isClickable = true

                            binding.userRecognitionText.setOnClickListener {
                                binding.recognitionResultLayout.visibility = View.VISIBLE
                                val biggerAnimation =
                                    AnimationUtils.loadAnimation(this, R.anim.scale_big)
                                binding.recognitionResultBox.startAnimation(biggerAnimation)

                                binding.resultEmoji.setImageResource(R.drawable.ic_soso)
                                binding.bigResultText.setText(null)
                                binding.smallResultText.text = "문제는 한번만 풀 수 있어요.\n내일 또 도전해보아요!"

                                Handler().postDelayed({
                                    val downAnimation =
                                        AnimationUtils.loadAnimation(this, R.anim.scale_small)
                                    binding.recognitionResultBox.startAnimation(downAnimation)

                                    Handler().postDelayed({
                                        binding.recognitionResultLayout.visibility = View.GONE
                                        binding.userRecognitionText.isEnabled = false
                                    }, 300)
                                }, 1500)

                            }

                        } else {
                            // 없을 경우
                            binding.recognitionCheckBox.setImageResource(R.drawable.ic_checkbox_no)

                            binding.mathLayout.visibility = View.GONE
                            binding.noRecognitionLayout.visibility = View.VISIBLE
                        }
                    }


                // 이미지 초기 셋업
                if (document.data?.contains("images") == true) {
                    oldImageList =
                        document.data?.getValue("images") as ArrayList<String>

                    if (oldImageList.size != 0) {
                        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                        if (oldImageList.size != 0) {
                            for (i in 0 until oldImageList.size) {
                                binding.photoRecyclerView.visibility = View.VISIBLE

                                var uriParseImage = Uri.parse(oldImageList[i])
                                newImageViewModel.addImage(uriParseImage)
                                itemListItems.add(uriParseImage)
                                photoAdapter.notifyItemInserted(oldImageList.size-1)
                            }
                        }
                    }

                } else {
                    // null
                    binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)
                    binding.photoRecyclerView.visibility = View.GONE
                }


                // 일기 보여주기
                var oldDiary = document.data?.getValue("todayDiary").toString()
                binding.todayDiary.setText(oldDiary)

                // 마음 보여주기
                var spinnerAdapter = binding.todayMood.adapter
                var dbMoodPosition =
                    (document.data?.getValue("todayMood") as Map<*, *>)["position"].toString()
                        .toInt()
                binding.todayMood.setSelection(dbMoodPosition)

                // 숨기기 보여주기
                var DBsecretStatus = document.data?.getValue("secret") as Boolean
                diaryEditCheck.secretEdit.value = DBsecretStatus
            }

        // 사진 업로드
        fun openGalleryForImages() {
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

                        val count = it.data!!.clipData!!.itemCount
                        for (i in 0 until count) {
                            binding.photoRecyclerView.visibility = View.VISIBLE

                            val imageUri = it.data?.clipData!!.getItemAt(i).uri.toString()
                            var uriParseImage = Uri.parse(imageUri)
                            newImageViewModel.addImage(uriParseImage)
                            itemListItems.add(uriParseImage)
                            photoAdapter.notifyItemInserted(itemListItems.size-1)

                            diaryEditCheck.photoEdit.value = true
                        }

                    }
                }
            }


        fun selectGallery() {
            val writePermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val readPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            // 권한 확인
            if (writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
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

        // 기분 스니퍼
        binding.todayMood.adapter = applicationContext?.let {
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
                diaryEditCheck.moodEdit.value = true
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
                diaryEditCheck.diaryEdit.value = char?.length != 0
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }
        })


        // 숨기기
        binding.secretButton.setOnClickListener {
            binding.secretNotificationLayout.visibility = View.VISIBLE
            window.setStatusBarColor(Color.parseColor("#B3000000"))
        }

        binding.secretCancelBox.setOnClickListener {
            binding.secretNotificationLayout.visibility = View.GONE
            window.setStatusBarColor(Color.WHITE);
        }


        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {
            resumePause = false

            binding.diaryBtn.text = ""
            binding.diaryProgressBar.visibility = View.VISIBLE

            if (diaryEditCheck.photoEdit.value == true) {

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

                if (diaryEditCheck.diaryEdit.value == true) {
                    var newDiary = binding.todayDiary.text
                    newDiarySet.put("todayDiary", newDiary.toString())
                }

                if (diaryEditCheck.moodEdit.value == true) {
                    newDiarySet.put("todayMood", binding.todayMood.selectedItem as Mood)
                }

                newDiarySet.put("secret", diaryEditCheck.secretEdit.value as Boolean)

            } else {
                // 이미지 업로드 안 하는 경우
                if (diaryEditCheck.diaryEdit.value == true) {
                    var newDiary = binding.todayDiary.text
                    newDiarySet.put("todayDiary", newDiary.toString())
                }

                if (diaryEditCheck.moodEdit.value == true) {
                    newDiarySet.put("todayMood", binding.todayMood.selectedItem as Mood)
                }

                newDiarySet.put("secret", diaryEditCheck.secretEdit.value as Boolean)


                diaryDB.document("$editDiaryId")
                    .set(newDiarySet, SetOptions.merge())
                    .addOnSuccessListener {
                        var goAllDiary = Intent(this, DiaryTwoActivity::class.java)
                        goAllDiary.putExtra("from", "edit")
                        goAllDiary.putExtra("diaryType", diaryType)
                        startActivity(goAllDiary)
                    }
            }
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
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
//                            newDiarySet.put("images", newImageViewModel.newImageList.value!!)
                            newImageViewModel.uploadFirebaseComplete.value = true
                        }

                    }
                }
            )
            ?.addOnFailureListener(OnFailureListener { e ->
                print(e.message)
            })
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteImageFunction,
            IntentFilter("DELETE_IMAGE_EDIT")
        );
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(deleteImageFunction);
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(deleteImageFunction);
        super.onDestroy()
    }

    private var deleteImageFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val deleteImagePosition = intent?.getIntExtra("deleteImagePosition", 0)
            newImageViewModel.removeImage(deleteImagePosition!!)
            itemListItems.removeAt(deleteImagePosition)
            photoAdapter.notifyItemRemoved(deleteImagePosition)

            diaryEditCheck.photoEdit.value = true
        }
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
        TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) textToSpeechEngine.language = Locale("in_ID")
        }
    }
}


@Keep
class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
    val photoEdit by lazy { MutableLiveData<Boolean>(false) }
    val secretEdit by lazy { MutableLiveData<Boolean>(false) }
}

@Keep
class EditNewImageViewModel : ViewModel() {
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
