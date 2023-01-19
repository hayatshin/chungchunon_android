package com.chugnchunon.chungchunon_android

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.MutableLiveData
import com.chugnchunon.chungchunon_android.databinding.ActivityAgreementBinding

class AgreementActivity: AppCompatActivity() {

    private val binding by lazy {
        ActivityAgreementBinding.inflate(layoutInflater)
    }

    private var userType: String = ""
    private var agreementCheck = MutableLiveData<Boolean>(false)

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.nextBtn.isEnabled = false

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        var mainDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_check_agreement_main)
        var grayDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_check_agreement)

        binding.totalAgreementBtn.setOnClickListener {
            if(!agreementCheck.value!!) {
                agreementCheck.value = true
                binding.nextBtn.isEnabled = true
                binding.totalAgreementBtn.setCompoundDrawablesWithIntrinsicBounds(mainDrawable, null, null, null)
            } else {
                agreementCheck.value = false
                binding.nextBtn.isEnabled = false
                binding.totalAgreementBtn.setCompoundDrawablesWithIntrinsicBounds(grayDrawable, null, null, null)
            }
        }

        userType = intent.getStringExtra("userType").toString()

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.appRuleBtn.setOnClickListener {
            var goAppRule = Intent(this, ApplicationRuleActivity::class.java)
            startActivity(goAppRule)
        }

        binding.personalInfoBtn.setOnClickListener {
            var goPersonalInfo = Intent(this, PersonalInfoRuleActivity::class.java)
            startActivity(goPersonalInfo)
        }

        binding.nextBtn.setOnClickListener {
            var goRegister = Intent(this, RegisterActivity::class.java)
            goRegister.putExtra("userType", userType)
            startActivity(goRegister)
        }
    }
}