package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Adapter.MoodArrayAdapter
import com.chugnchunon.chungchunon_android.Adapter.UploadPhotosAdapter
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.Mood
import com.chugnchunon.chungchunon_android.Fragment.ImageSizeCheck
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
import java.util.*
import kotlin.collections.ArrayList

class EditDiaryActivity : AppCompatActivity() {

    private val diaryDB = Firebase.firestore.collection("diary")
    private val userDB = Firebase.firestore.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    private val binding by lazy {
        ActivityEditDiaryBinding.inflate(layoutInflater)
    }

    lateinit var diaryEditCheck: DiaryEditClass
    lateinit var imageSizeCheck: ImageSizeCheck
    lateinit var firestorageImageSizeCheck: FireStorageImageSizeCheck

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var photoAdapter: UploadPhotosAdapter
    private var photoStringList: ArrayList<String> = ArrayList()
    private var newDiarySet = hashMapOf<String, Any>()

    private val model: BaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var editDiaryId = intent.getStringExtra("editDiaryId")

        binding.moodCheckBox.setImageResource(R.drawable.ic_checkbox_no)
        binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_no)

        diaryEditCheck = ViewModelProvider(this).get(DiaryEditClass::class.java)
        imageSizeCheck = ViewModelProvider(this).get(ImageSizeCheck::class.java)
        firestorageImageSizeCheck =
            ViewModelProvider(this).get(FireStorageImageSizeCheck::class.java)

        binding.diaryBtn.isEnabled = false

        // 수정
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
            if (diaryEditCheck.photoEdit.value == true) binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_yes)
        })


        imageSizeCheck.imageList.observe(this, Observer { value ->
            Log.d("수정 55", "${imageSizeCheck.imageList.value}")

            diaryEditCheck.photoEdit.value = true

            if (imageSizeCheck.imageList.value!!.size == 0) {
                binding.photoCheckBox.setImageResource(R.drawable.ic_checkbox_no)
                binding.photoRecyclerView.visibility = View.GONE
            }
        })

        firestorageImageSizeCheck.imageList.observe(this, Observer { value ->
            if (firestorageImageSizeCheck.imageList.value?.size == imageSizeCheck.imageList.value?.size) {

                newDiarySet.put("images", firestorageImageSizeCheck.imageList.value as List<String>)

                if (diaryEditCheck.diaryEdit.value == true) {
                    var newDiary = binding.todayDiary.text
                    newDiarySet.put("todayDiary", newDiary.toString())
                }

                if (diaryEditCheck.moodEdit.value == true) {
                    newDiarySet.put("todayMood", binding.todayMood.selectedItem as Mood)
                }

                diaryDB.document("$editDiaryId")
                    .set(newDiarySet, SetOptions.merge())
                    .addOnSuccessListener {
                        var goAllDiary = Intent(this, DiaryTwoActivity::class.java)
                        goAllDiary.putExtra("from", "edit")
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

                binding.todayDate.text = diaryDate


                // 이미지 보여주기
                if (document.data?.contains("images") == true) {

                    binding.diaryCheckBox.setImageResource(R.drawable.ic_checkbox_yes)

                    var imageList =
                        document.data?.getValue("images") as ArrayList<String>

                    if (imageList.size != 0) {
                        for (i in 0 until imageList.size) {
                            var uriParseImage = Uri.parse(imageList[i])
                            imageSizeCheck.addImage(uriParseImage)
                        }

                        photoAdapter = UploadPhotosAdapter(this, imageList)
                        binding.photoRecyclerView.adapter = photoAdapter
                        photoAdapter.notifyDataSetChanged()

                        binding.photoRecyclerView.visibility = View.VISIBLE
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
            }

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
                            val imageUri = it.data?.clipData!!.getItemAt(i).uri.toString()
                            var uriParseImage = Uri.parse(imageUri)
                            imageSizeCheck.addImage(uriParseImage)
                        }

                        photoAdapter = UploadPhotosAdapter(this, imageSizeCheck.imageList.value!!)
                        binding.photoRecyclerView.adapter = photoAdapter
                        photoAdapter.notifyDataSetChanged()

                        binding.photoRecyclerView.visibility = View.VISIBLE
//                        binding.photoRecyclerView.alpha = 0f
//                        binding.photoRecyclerView.y = -50f
//
//                        binding.photoRecyclerView.animate()
//                            .translationY(0f)
//                            .setDuration(500)
//                            .setInterpolator(LinearInterpolator())
//                            .start()
//
//                        binding.photoRecyclerView.animate()
//                            .alpha(1f)
//                            .setDuration(600)
//                            .setInterpolator(LinearInterpolator())
//                            .start()
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

        // 다이어리 작성 버튼
        binding.diaryBtn.setOnClickListener {

            if (diaryEditCheck.photoEdit.value == true) {
                for (i in 0 until imageSizeCheck.imageList.value!!.size) {
                    uploadImageToFirebase(imageSizeCheck.imageList.value!![i])
                }

            } else {
                if (diaryEditCheck.diaryEdit.value == true) {
                    var newDiary = binding.todayDiary.text
                    newDiarySet.put("todayDiary", newDiary.toString())
                }

                if (diaryEditCheck.moodEdit.value == true) {
                    newDiarySet.put("todayMood", binding.todayMood.selectedItem as Mood)
                }

                diaryDB.document("$editDiaryId")
                    .set(newDiarySet, SetOptions.merge())
                    .addOnSuccessListener {
                        var goAllDiary = Intent(this, DiaryTwoActivity::class.java)
                        goAllDiary.putExtra("from", "edit")
                        startActivity(goAllDiary)
                    }
            }


//            if (diaryEditCheck.diaryEdit.value == true) {
//                var newDiary = binding.todayDiary.text
//                newDiarySet.put("todayDiary", newDiary.toString())
//            }
//
//            if (diaryEditCheck.moodEdit.value == true) {
//                newDiarySet.put("todayMood", binding.todayMood.selectedItem as Mood)
//            }
//
//
//            if (diaryEditCheck.photoEdit.value == true) {
//                Log.d("수정1", "트루")
//                for (i in 0 until imageSizeCheck.imageList.value!!.size) {
//                    uploadImageToFirebase(imageSizeCheck.imageList.value!![i])
//                }
//            }
//
//
//            diaryDB.document("$editDiaryId")
//                .set(newDiarySet, SetOptions.merge())
//                .addOnSuccessListener {
//                    var goAllDiary = Intent(this, DiaryTwoActivity::class.java)
//                    goAllDiary.putExtra("from", "edit")
//                    startActivity(goAllDiary)
//                }


        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }


    private fun uploadImageToFirebase(fileUri: Uri) {
        if (fileUri != null) {
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val database = FirebaseDatabase.getInstance()
            val refStorage = FirebaseStorage.getInstance().reference.child("images/$fileName")

            refStorage.putFile(fileUri)
                .addOnSuccessListener(
                    OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                            val imageUrl = it.toString()
                            photoStringList.add(imageUrl)
//                            firestorageImageSizeCheck.addImage(imageUrl)
                        }
                    }
                )
                ?.addOnFailureListener(OnFailureListener { e ->
                    print(e.message)
                })
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            deleteImageFunction,
            IntentFilter("DELETE_IMAGE")
        );
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(deleteImageFunction);
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(deleteImageFunction);
    }

    private var deleteImageFunction: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var deleteImagePosition = intent?.getIntExtra("deleteImagePosition", 0)

            imageSizeCheck.removeImage(deleteImagePosition!!.toInt())
            photoAdapter.notifyDataSetChanged()

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


class DiaryEditClass : ViewModel() {
    val diaryEdit by lazy { MutableLiveData<Boolean>(false) }
    val moodEdit by lazy { MutableLiveData<Boolean>(false) }
    val photoEdit by lazy { MutableLiveData<Boolean>(false) }
}


class ImageSizeCheck : ViewModel() {
    var imageList = MutableLiveData<List<Any>>()
    var imageListValue = imageList.value
    var templateList = mutableListOf<Any>()

    fun addImage(addImage: Any) {
        imageListValue?.forEach { data ->
            templateList.add(data)
        }
        templateList.add(addImage)
        imageList.value = templateList
    }

    fun removeImage(removePosition: Int) {
        templateList.removeAt(removePosition)
        imageList.value = templateList
    }
}

class FireStorageImageSizeCheck : ViewModel() {
    var imageList = MutableLiveData<List<String>>()
    var imageListValue = imageList.value
    var templateList = mutableListOf<String>()

    fun addImage(addImage: String) {
        imageListValue?.forEach { data ->
            templateList.add(data)
        }
        templateList.add(addImage)
        imageList.value = templateList
    }

    fun removeImage(removePosition: Int) {
        templateList.removeAt(removePosition)
        imageList.value = templateList
    }
}