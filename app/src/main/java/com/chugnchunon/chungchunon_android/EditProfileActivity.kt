package com.chugnchunon.chungchunon_android

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.Adapter.UploadPhotosAdapter
import com.chugnchunon.chungchunon_android.Fragment.MoreFragment
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityEditProfileBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityEditProfileTwoBinding
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityEditProfileTwoBinding.inflate(layoutInflater)
    }

    private val db = Firebase.firestore
    private val userDB = db.collection("users")
    private val userId = Firebase.auth.currentUser?.uid

    private val calendar = Calendar.getInstance()
    private var birthYear = ""
    private var birthMonth = ""
    private var birthDate = ""

    lateinit var editFillClass: EditCheckClass

    private var newAvatar = ""
    private var newName = ""
    private var newBirthYear = ""
    private var newBirthDay = ""
    private var newRegion = ""
    private var newSmallRegion = ""
    private var intentRegion = ""
    private var intentSmallRegion = ""

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var selectedAvatarURI: Uri

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.editProfileScrollView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, ev: MotionEvent?): Boolean {
                when(ev?.action) {
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

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.profileEditBtn.isEnabled = false

        intentRegion = intent.getStringExtra("region").toString()
        intentSmallRegion = intent.getStringExtra("smallRegion").toString()


        editFillClass = ViewModelProvider(this).get(EditCheckClass::class.java)

        editFillClass.avatarFill.observe(this, Observer {value ->
            if(value) {
                binding.profileEditBtn.isEnabled = true
            }
        })

        editFillClass.nameFill.observe(this, Observer { value ->
            if (value) {
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.birthFill.observe(this, Observer { value ->
            if (value) {
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.regionFill.observe(this, Observer { value ->
            if (value) {
                binding.profileEditBtn.isEnabled = true
            }
        })

        // 초기 셋업
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                newAvatar = document.data?.getValue("avatar").toString()
                newName = document.data?.getValue("name").toString()
                var gender = document.data?.getValue("gender").toString()
                newBirthYear = document.data?.getValue("birthYear").toString()
                newBirthDay = document.data?.getValue("birthDay").toString()
                var showBirth =
                    "${newBirthYear}-${newBirthDay.substring(0, 2)}-${newBirthDay.substring(2, 4)}"
                birthMonth = newBirthDay.substring(0, 2)
                birthDate = newBirthDay.substring(2, 4)
                newRegion = document.data?.getValue("region").toString()
                newSmallRegion = document.data?.getValue("smallRegion").toString()

                Glide.with(this)
                    .load(newAvatar)
                    .into(binding.avatarImage)

                binding.editName.setText(newName)
                binding.editBirth.setText(showBirth)
                binding.editRegion.setText("${newRegion} ${newSmallRegion}")
            }


        // 이미지 수정
        fun selectGallery() {
            val readPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            // 권한 확인
            if (readPermission == PackageManager.PERMISSION_DENIED) {
                // 권한 요청
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    100
                )
            } else {

                var intent = Intent(Intent.ACTION_PICK)
                intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                intent.type = "image/*"

                startActivityForResult(intent, 2000)
            }
        }


        binding.editAvatar.setOnClickListener {
            selectGallery()
        }

        // 이름 수정
        binding.editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if (char.toString() != newName) {
                    editFillClass.nameFill.value = true
                    newName = char.toString()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }

        })

        // 생년월일 수정
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                newBirthYear = document.data?.getValue("birthYear").toString()
                newBirthDay = document.data?.getValue("birthDay").toString()
                birthMonth = newBirthDay.substring(0, 2)
                birthDate = newBirthDay.substring(2, 4)

                calendar.apply {
                    set(newBirthYear.toInt(), birthMonth.toInt() - 1, birthDate.toInt())
                }

                val birthDatePicker =
                    DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->

                        calendar.set(Calendar.YEAR, year)
                        calendar.set(Calendar.MONTH, monthOfYear)
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        var birthScreenInput = ""
                        birthScreenInput = "${year}-${
                            String.format(
                                "%02d",
                                monthOfYear + 1
                            )
                        }-${String.format("%02d", dayOfMonth)}"
                        newBirthYear = "$year"
                        newBirthDay = "${String.format("%02d", monthOfYear + 1)}${
                            String.format(
                                "%02d",
                                dayOfMonth
                            )
                        }"
                        binding.editBirth.setText(birthScreenInput)
                    }

                var datePickerResult = DatePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                    birthDatePicker,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerResult.datePicker.spinnersShown = true
                datePickerResult.datePicker.calendarViewShown = false

                binding.editBirth.setOnClickListener {
                    datePickerResult.show()
                    editFillClass.birthFill.value = true
                }
            }

        // 지역 수정
        binding.editRegion.setOnClickListener {
            var goEditRegion = Intent(this, EditRegionRegisterActivity::class.java)
            startActivityForResult(goEditRegion, 1)
        }

        // 수정 버튼 클릭
        binding.profileEditBtn.setOnClickListener {
            binding.profileEditBtn.text = ""
            binding.profileProgressBtn.visibility = View.VISIBLE

            if(editFillClass.avatarFill.value == false) {
                // 이미지 없는 경우

                var newPersonalInfoSet = hashMapOf(
                    "name" to newName,
                    "birthYear" to newBirthYear,
                    "birthDay" to newBirthDay,
                    "region" to newRegion,
                    "smallRegion" to newSmallRegion,
                )

                userDB.document("$userId").set(newPersonalInfoSet, SetOptions.merge())
                    .addOnSuccessListener {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        var newUserAge = currentYear - newBirthYear.toInt() + 1


                        Log.d("지역수정1", "${newRegion} ${newSmallRegion}")

                        var intent = Intent(this, MoreFragment::class.java)
                        intent.setAction("EDIT_PROFILE")
                        intent.putExtra("newName", newName)
                        intent.putExtra("newUserAge", newUserAge)
                        intent.putExtra("newRegionSmallRegion", "${newRegion} ${newSmallRegion}")
                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                        finish()
                    }
            } else {
                // 이미지 수정

                val fileName = UUID.randomUUID().toString() + ".jpg"
                val refStorage = FirebaseStorage.getInstance().reference.child("avatars/${fileName}")

                refStorage.putFile(selectedAvatarURI)
                    .addOnSuccessListener (
                        OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                                val avatarUrl = it.toString()

                                var newPersonalInfoSet = hashMapOf(
                                    "avatar" to avatarUrl,
                                    "name" to newName,
                                    "birthYear" to newBirthYear,
                                    "birthDay" to newBirthDay,
                                    "region" to newRegion,
                                    "smallRegion" to newSmallRegion,
                                )

                                userDB.document("$userId").set(newPersonalInfoSet, SetOptions.merge())
                                    .addOnSuccessListener {
                                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                        var newUserAge = currentYear - newBirthYear.toInt() + 1


                                        var intent = Intent(this, MoreFragment::class.java)
                                        intent.setAction("EDIT_PROFILE")
                                        intent.putExtra("newAvatar", avatarUrl)
                                        intent.putExtra("newName", newName)
                                        intent.putExtra("newUserAge", newUserAge)
                                        intent.putExtra("newRegionSmallRegion", "${newRegion} ${newSmallRegion}")
                                        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                                        finish()
                                    }

                            }
                    })
            }


        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            var intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"

            startActivityForResult(intent, 2000)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d("터치터치터치", "${event}")
        hideKeyBoard()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1 -> {
                // 지역
                if (resultCode == RESULT_OK) {
                    var region = data!!.getStringExtra("region")
                    var smallRegion = data!!.getStringExtra("smallRegion")
                    binding.editRegion.text = "$region $smallRegion"
                    newRegion = region.toString()
                    newSmallRegion = smallRegion.toString()
                    editFillClass.regionFill.value = true
                }
            }
            2000 -> {
                // 사진 가져오기
                selectedAvatarURI = data?.data!!

                Glide.with(this)
                    .load(selectedAvatarURI)
                    .into(binding.avatarImage)

                editFillClass.avatarFill.value = true

            }
        }
    }

    private fun hideKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}

class EditCheckClass : ViewModel() {
    val avatarFill by lazy { MutableLiveData<Boolean>(false) }
    val nameFill by lazy { MutableLiveData<Boolean>(false) }
    val birthFill by lazy { MutableLiveData<Boolean>(false) }
    val regionFill by lazy { MutableLiveData<Boolean>(false) }
}