package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.Fragment.MoreFragment
import com.chugnchunon.chungchunon_android.Fragment.RegionRegisterFragment
import com.chugnchunon.chungchunon_android.databinding.ActivityEditProfileBinding
import com.chugnchunon.chungchunon_android.databinding.ActivityEditProfileTwoBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.profileEditBtn.isEnabled = false

        intentRegion = intent.getStringExtra("region").toString()
        intentSmallRegion = intent.getStringExtra("smallRegion").toString()


        editFillClass = ViewModelProvider(this).get(EditCheckClass::class.java)
        editFillClass.nameFill.observe(this, Observer { value ->
            if(value) {
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.birthFill.observe(this, Observer { value ->
            if(value) {
                binding.profileEditBtn.isEnabled = true
            }
        })
        editFillClass.regionFill.observe(this, Observer { value ->
            if(value) {
                binding.profileEditBtn.isEnabled = true
            }
        })


        // ?????? ??????

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

        // ?????? ??????
        binding.editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // null
            }

            override fun onTextChanged(char: CharSequence?, p1: Int, p2: Int, p3: Int) {

                if(char.toString() != newName) {
                    editFillClass.nameFill.value = true
                    newName = char.toString()
                }
            }

            override fun afterTextChanged(p0: Editable?) {
                // null
            }

        })

        // ???????????? ??????

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


        // ?????? ??????
        binding.editRegion.setOnClickListener {
            var goEditRegion = Intent(this, EditRegionRegisterActivity::class.java)
            startActivityForResult(goEditRegion, 1)
        }

        Log.d("??????", "${intentRegion} // ${intentSmallRegion} // ${newRegion} // ${newSmallRegion}")


        // ?????? ?????? ??????

        binding.profileEditBtn.setOnClickListener {
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


                    Log.d("????????????1", "${newRegion} ${newSmallRegion}")

                    var intent = Intent(this, MoreFragment::class.java)
                    intent.setAction("EDIT_PROFILE")
                    intent.putExtra("newName", newName)
                    intent.putExtra("newUserAge", newUserAge)
                    intent.putExtra("newRegionSmallRegion", "${newRegion} ${newSmallRegion}")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                    finish()
                }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                var region = data!!.getStringExtra("region")
                var smallRegion = data!!.getStringExtra("smallRegion")
                binding.editRegion.text = "$region $smallRegion"
                newRegion = region.toString()
                newSmallRegion = smallRegion.toString()
                editFillClass.regionFill.value = true
            }
        }
    }


}

class EditCheckClass : ViewModel() {
    val nameFill by lazy { MutableLiveData<Boolean>(false) }
    val birthFill by lazy { MutableLiveData<Boolean>(false) }
    val regionFill by lazy { MutableLiveData<Boolean>(false) }
}