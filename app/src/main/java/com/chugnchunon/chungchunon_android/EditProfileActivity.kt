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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
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
import com.chugnchunon.chungchunon_android.Fragment.MoreFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment.Companion.smallRegionCheck
import com.chugnchunon.chungchunon_android.databinding.ActivityEditProfileTwoBinding
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_edit_profile_two.*
import java.util.*
import kotlin.collections.ArrayList

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
    private var newGender = ""
    private var newBirthYear = ""
    private var newBirthDay = ""
    private var newRegion = ""
    private var newSmallRegion = ""
    private var newCommunity = ""
    private var dbCommunities: ArrayList<String> = ArrayList()
    private var newCommunityList: ArrayList<String> = ArrayList()

    private var intentRegion = ""
    private var intentSmallRegion = ""

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    lateinit var selectedAvatarURI: Uri

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down_enter)
    }

    companion object {
        const val REQ_GALLERY: Int = 100
        const val REQ_MULTI_PHOTO: Int = 2000
        const val EDIT_REGION_REQ = 1
        const val EDIT_COMMUNITY_REQ = 2
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        smallRegionCheck = false

        binding.editProfileScrollView.setOnTouchListener(object : View.OnTouchListener {
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

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.profileEditBtn.isEnabled = false

        intentRegion = intent.getStringExtra("region").toString()
        intentSmallRegion = intent.getStringExtra("smallRegion").toString()


        editFillClass = ViewModelProvider(this).get(EditCheckClass::class.java)

        editFillClass.avatarFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "아바타")
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.nameFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "이름")
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.genderFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "성별")
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.birthFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "생년")
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.regionFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "지역")
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.communityFill.observe(this, Observer { value ->
            if (value) {
                Log.d("수정", "소속")
                binding.profileEditBtn.isEnabled = true
            }
        })

        // 성별
        binding.editGender.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.genderList,
            R.layout.item_spinner_gender
        )

        // 초기 셋업
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                try {
                    newAvatar = document.data?.getValue("avatar").toString()
                    newName = document.data?.getValue("name").toString()
                    newGender = document.data?.getValue("gender").toString()
                    val genderIndex: Int = if (newGender == "여성") 0 else 1
                    newBirthYear = document.data?.getValue("birthYear").toString()
                    newBirthDay = document.data?.getValue("birthDay").toString()
                    val showBirth =
                        "${newBirthYear}-${newBirthDay.substring(0, 2)}-${
                            newBirthDay.substring(
                                2,
                                4
                            )
                        }"
                    birthMonth = newBirthDay.substring(0, 2)
                    birthDate = newBirthDay.substring(2, 4)
                    newRegion = document.data?.getValue("region").toString()
                    newSmallRegion = document.data?.getValue("smallRegion").toString()

                    if (document.contains("community")) {
                        dbCommunities = document.data?.getValue("community") as ArrayList<String>

                        if (dbCommunities.size == 0) {
                            newCommunity = "없음"
                        } else {
                            newCommunity = dbCommunities.joinToString(", ")
                        }
                    } else {
                        newCommunity = "없음"
                    }
                    binding.editCommunity.setText(newCommunity)

                    Glide.with(this)
                        .load(newAvatar)
                        .into(binding.avatarImage)

                    binding.editName.setText(newName)
                    binding.editGender.setSelection(genderIndex)
                    binding.editBirth.setText(showBirth)
                    binding.editRegion.setText("${newRegion} ${newSmallRegion}")

                } catch (e: Exception) {
                    // null
                }

            }

        binding.editGender.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                newGender = binding.editGender.selectedItem.toString()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // null
            }
        })

        binding.editGender.setOnTouchListener { _, event ->
            if(event.action == MotionEvent.ACTION_DOWN) {
                editFillClass.genderFill.value = true
            }
            false
        }


        // 이미지 수정
        fun selectGallery() {
            val readGalleryPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val readMediaImagesPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            )

            // 권한 확인

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (readMediaImagesPermission == PackageManager.PERMISSION_DENIED) {
                    // 권한 요청
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                        REQ_GALLERY
                    )
                } else {
                    var intent = Intent(Intent.ACTION_PICK)
                    intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    intent.type = "image/*"

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
                    var intent = Intent(Intent.ACTION_PICK)
                    intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    intent.type = "image/*"

                    startActivityForResult(intent, REQ_MULTI_PHOTO)
                }
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
        binding.editRegion.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val editRegionLine = binding.editRegion.lineCount

                if (editRegionLine == 1) {
                    binding.editRegion.gravity = Gravity.END
                } else {
                    binding.editRegion.gravity = Gravity.START
                }
                binding.editRegion.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })


        binding.editRegion.setOnClickListener {
            val goEditRegion = Intent(this, EditRegionRegisterActivity::class.java)
            goEditRegion.putExtra("userCommunities", newCommunity)
            startActivityForResult(goEditRegion, EDIT_REGION_REQ)
        }

        // 커뮤니티 수정
        binding.editCommunity.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val editRegionLine = binding.editCommunity.lineCount

                if (editRegionLine == 1) {
                    binding.editCommunity.gravity = Gravity.END
                } else {
                    binding.editCommunity.gravity = Gravity.START
                }
                binding.editCommunity.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        binding.editCommunity.setOnClickListener {
            val goEditCommunity = Intent(this, EditCommunityRegisterActivity::class.java)
            goEditCommunity.putExtra("fullRegion", "${newRegion} ${newSmallRegion}")
            goEditCommunity.putExtra("userCommunities", dbCommunities)
            startActivityForResult(goEditCommunity, EDIT_COMMUNITY_REQ)
        }

        // 수정 버튼 클릭
        binding.profileEditBtn.setOnClickListener {
            binding.profileEditBtn.text = ""
            binding.profileProgressBtn.visibility = View.VISIBLE

            for (newCommunity in newCommunityList) {
                if (!dbCommunities.contains(newCommunity)) {
                    // 더해짐
                    db.collection("community").document(newCommunity)
                        .update("users", (FieldValue.arrayUnion("$userId")))
                }
            }

            for (dbCommunity in dbCommunities) {
                if (!newCommunityList.contains(dbCommunity)) {
                    // 지워짐
                    db.collection("community").document(dbCommunity)
                        .update("users", (FieldValue.arrayRemove("$userId")))
                }
            }

            if (editFillClass.avatarFill.value == false) {
                // 이미지 없는 경우

                val newPersonalInfoSet = hashMapOf(
                    "name" to newName,
                    "gender" to newGender,
                    "birthYear" to newBirthYear,
                    "birthDay" to newBirthDay,
                    "region" to newRegion,
                    "smallRegion" to newSmallRegion,
                    "community" to newCommunityList
                )

                userDB.document("$userId").set(newPersonalInfoSet, SetOptions.merge())
                    .addOnSuccessListener {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val newUserAge = currentYear - newBirthYear.toInt() + 1

                        val intent = Intent(this, MoreFragment::class.java)
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
                val refStorage =
                    FirebaseStorage.getInstance().reference.child("avatars/${fileName}")

                refStorage.putFile(selectedAvatarURI)
                    .addOnSuccessListener(
                        OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                                val avatarUrl = it.toString()

                                val newPersonalInfoSet = hashMapOf(
                                    "avatar" to avatarUrl,
                                    "name" to newName,
                                    "gender" to newGender,
                                    "birthYear" to newBirthYear,
                                    "birthDay" to newBirthDay,
                                    "region" to newRegion,
                                    "smallRegion" to newSmallRegion,
                                    "community" to newCommunityList
                                )

                                userDB.document("$userId")
                                    .set(newPersonalInfoSet, SetOptions.merge())
                                    .addOnSuccessListener {
                                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                        val newUserAge = currentYear - newBirthYear.toInt() + 1


                                        val intent = Intent(this, MoreFragment::class.java)
                                        intent.setAction("EDIT_PROFILE")
                                        intent.putExtra("newAvatar", avatarUrl)
                                        intent.putExtra("newName", newName)
                                        intent.putExtra("newUserAge", newUserAge)
                                        intent.putExtra(
                                            "newRegionSmallRegion",
                                            "${newRegion} ${newSmallRegion}"
                                        )
                                        LocalBroadcastManager.getInstance(this)
                                            .sendBroadcast(intent);

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

        if (requestCode == REQ_GALLERY && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            var intent = Intent(Intent.ACTION_PICK)
            intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            intent.type = "image/*"

            startActivityForResult(intent, REQ_MULTI_PHOTO)

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        hideKeyBoard()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            EDIT_REGION_REQ -> {
                // 지역
                if (resultCode == RESULT_OK) {
                    smallRegionCheck = false

                    val region = data!!.getStringExtra("region")
                    val smallRegion = data!!.getStringExtra("smallRegion")
                    binding.editRegion.text = "$region $smallRegion"
                    newRegion = region.toString()
                    newSmallRegion = smallRegion.toString()
                    editFillClass.regionFill.value = true
                }
            }
            EDIT_COMMUNITY_REQ -> {
                if (resultCode == RESULT_OK) {
                    newCommunityList =
                        data!!.getStringArrayListExtra("newCommunityList") as ArrayList<String>

                    editFillClass.communityFill.value = true

                    if (newCommunityList.size == 0) {
                        binding.editCommunity.setText("없음")

                    } else {
                        val showNewCommunity = newCommunityList.joinToString(", ")
                        binding.editCommunity.setText(showNewCommunity)
                    }
                }
            }
            REQ_MULTI_PHOTO -> {
                // 사진 가져오기
                if (data?.data != null) {
                    selectedAvatarURI = data?.data!!

                    Glide.with(this)
                        .load(selectedAvatarURI)
                        .into(binding.avatarImage)

                    editFillClass.avatarFill.value = true
                }


            }
        }
    }

    private fun hideKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}

@Keep
class EditCheckClass : ViewModel() {
    val avatarFill by lazy { MutableLiveData<Boolean>(false) }
    val nameFill by lazy { MutableLiveData<Boolean>(false) }
    val genderFill by lazy { MutableLiveData<Boolean>(false) }
    val birthFill by lazy { MutableLiveData<Boolean>(false) }
    val regionFill by lazy { MutableLiveData<Boolean>(false) }
    val communityFill by lazy { MutableLiveData<Boolean>(false) }
}