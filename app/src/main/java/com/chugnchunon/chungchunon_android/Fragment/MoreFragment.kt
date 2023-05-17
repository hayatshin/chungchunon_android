package com.chugnchunon.chungchunon_android.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.chugnchunon.chungchunon_android.*
import com.chugnchunon.chungchunon_android.Service.MyService
import com.chugnchunon.chungchunon_android.databinding.FragmentMoreTwoBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import java.util.*

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreTwoBinding? = null
    private val binding get() = _binding!!

    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    lateinit var mcontext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mcontext = context
    }

    override fun onResume() {
        super.onResume()

        banStepInitialSetup()
    }

    @SuppressLint("Range", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMoreTwoBinding.inflate(inflater, container, false)
        val view = binding.root
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        KakaoSdk.init(requireActivity(), getString(R.string.kakao_native_key))

        // 개인정보 수정 반영
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            editProfileWithNewInfo,
            IntentFilter("EDIT_PROFILE")
        )

        // 초기 셋업
        userDB.document("$userId").get()
            .addOnSuccessListener { document ->
                val userName = document.data?.getValue("name").toString()
                val userAvatar = document.data?.getValue("avatar").toString()
                val userAge = document.data?.getValue("userAge").toString().toInt()
                val dbRegion = document.data?.getValue("region")
                val dbSmallRegion = document.data?.getValue("smallRegion")
                val userRegion = "${dbRegion} ${dbSmallRegion}"

                Glide.with(mcontext)
                    .load(userAvatar)
                    .into(binding.profileAvatar)

                binding.profileName.text = userName
                binding.profileAge.text = "${userAge}세"
                binding.profileRegion.text = userRegion
            }

        binding.profileEditBtn.setOnClickListener {
            val goProfileEdit = Intent(requireActivity(), EditProfileActivity::class.java)
            startActivity(goProfileEdit)
        }

        binding.application.setOnClickListener {
            val app_intent = Intent(activity, ApplicationRuleActivity::class.java)
            startActivity(app_intent)
        }

        binding.personalInfo.setOnClickListener {
            val personal_info_intent = Intent(activity, PersonalInfoRuleActivity::class.java)
            startActivity(personal_info_intent)
        }

        binding.blockUserBtn.setOnClickListener {
            val block_user_list_intent = Intent(activity, BlockUserListActivity::class.java)
            startActivity(block_user_list_intent)
        }

        binding.exitAppBtn.setOnClickListener {
            val exit_intent = Intent(activity, DefaultCancelWarningActivity::class.java)
            exit_intent.putExtra("warningType", "exit")
            startActivity(exit_intent)
        }

        binding.invitationIcon.setOnClickListener {
            sendKakaoLink()
        }

        // 걸음수
        banStepInitialSetup()

        return view
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(mcontext).unregisterReceiver(editProfileWithNewInfo);

        super.onDestroy()
    }

    private fun banStepInitialSetup() {
        userDB.document("$userId").get()
            .addOnCompleteListener { userTask ->
                if (userTask.isSuccessful) {
                    val userResult = userTask.result
                    if (userResult != null && userResult.exists()) {
                        val userData = userResult.data
                        val isStepStatusExist = userData?.containsKey("step_status") ?: false
                        if (isStepStatusExist) {
                            // step_status 존재
                            val stepStatusValue = userData?.getValue("step_status") as Boolean
                            if (stepStatusValue) {
                                // 1. 카운트 o
                                binding.banStepCountIcon.setImageResource(R.drawable.ic_ban_walking)
                                binding.banStepCountText.text = "걸음수\n끄기"

                                binding.banStepCountIcon.setOnClickListener {

                                    val goBanStepNotification = Intent(
                                        requireActivity(),
                                        DefaultCancelWarningActivity::class.java
                                    )
                                    goBanStepNotification.putExtra("warningType", "banStep")
                                    startActivity(goBanStepNotification)
                                }

                            } else {
                                // 2. 카운트 x
                                binding.banStepCountIcon.setImageResource(R.drawable.ic_allow_walk)
                                binding.banStepCountText.text = "걸음수\n켜기"

                             binding.banStepCountIcon.setOnClickListener {
                                 val stepPermissionCheck =
                                     ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACTIVITY_RECOGNITION)

                                 if(stepPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                                     // 권한 있음
                                     binding.banStepCountIcon.setOnClickListener {

                                         val goAllowStepNotification = Intent(
                                             requireActivity(),
                                             DefaultDiaryWarningActivity::class.java
                                         )
                                         goAllowStepNotification.putExtra("warningType", "allowStep")
                                         startActivity(goAllowStepNotification)
                                     }
                                 } else {
                                     val goDefaultWarning = Intent(
                                         requireActivity(),
                                         DefaultDiaryWarningActivity::class.java
                                     )
                                     goDefaultWarning.putExtra("warningType", "authStepNo")
                                     startActivity(goDefaultWarning)
                                 }
                             }

                            }
                        } else {
                            // step_status 존재 안 함 -> auth_step 체크
                            val stepPermissionCheck =
                                ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACTIVITY_RECOGNITION)

                            if (stepPermissionCheck == PackageManager.PERMISSION_GRANTED) {
                                // 3. auth_step 존재 -> 카운트 o
                                binding.banStepCountIcon.setImageResource(R.drawable.ic_ban_walking)
                                binding.banStepCountText.text = "걸음수\n끄기"

                                binding.banStepCountIcon.setOnClickListener {
                                    val stepStatusSet = hashMapOf(
                                        "step_status" to false,
                                    )

                                    userDB.document("$userId")
                                        .set(stepStatusSet, SetOptions.merge())
                                        .addOnSuccessListener {
                                            val exitIntent = Intent(activity, MyService::class.java)
                                            exitIntent.setAction("EXIT_APP")
                                            LocalBroadcastManager.getInstance(requireActivity())
                                                .sendBroadcast(exitIntent)
                                        }
                                }
                            } else {
                                // 4. auth_step 존재 안 함 -> 카운트 x & 권한 설정 필요
                                binding.banStepCountIcon.setImageResource(R.drawable.ic_allow_walk)
                                binding.banStepCountText.text = "걸음수\n켜기"

                                binding.banStepCountIcon.setOnClickListener {
                                    val goDefaultWarning = Intent(
                                        requireActivity(),
                                        DefaultDiaryWarningActivity::class.java
                                    )
                                    goDefaultWarning.putExtra("warningType", "authStepNo")
                                    startActivity(goDefaultWarning)
                                }
                            }
                        }
                    }
                }
            }
    }

    var editProfileWithNewInfo: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var newAvatar = intent?.getStringExtra("newAvatar")
            var newName = intent?.getStringExtra("newName")
            var newUserAge = intent?.getIntExtra("newUserAge", 0)
            var newRegionSmallRegion = intent?.getStringExtra("newRegionSmallRegion")

            Glide.with(context!!)
                .load(newAvatar)
                .into(binding.profileAvatar)

            binding.profileName.text = newName
            binding.profileAge.text = "${newUserAge}세"
            binding.profileRegion.text = newRegionSmallRegion
        }
    }

    private fun sendKakaoLink() {
        val defaultFeed = FeedTemplate(
            content = Content(
                title = "오늘도청춘",
                description = "세상에서 가장 쉬운 치매 예방 플랫폼",
                imageUrl = "https://postfiles.pstatic.net/MjAyMzA1MTVfMTA0/MDAxNjg0MTU5OTgwNjI5.hETmcfk6juGW1EEqRkIcYHe6nmMxDYf_560hojOKiOog.z7obOXdYtZPP2K8jSwwjRGL2dF3BSJaE4IsdLbYaqrsg.PNG.hayat_shin/%EC%95%84%EC%9D%B4%EC%BD%98_%EC%9D%B4%EB%AF%B8%EC%A7%80_%ED%95%91%ED%81%AC2.png?type=w773",
                link = Link(
                    webUrl = "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android",
                    mobileWebUrl = "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android"
                )
            ),
            buttons = listOf(
                Button(
                    "앱 다운 받기",
//                    Link(
//                        androidExecutionParams = mapOf(
//                            "key1" to "value1",
//                            "key2" to "value2"
//                        )
//                    )
                    Link(
                        webUrl = "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android",
                        mobileWebUrl = "https://play.google.com/store/apps/details?id=com.chugnchunon.chungchunon_android"
                    )
                )
            )
        )
        if (ShareClient.instance.isKakaoTalkSharingAvailable(requireActivity())) {
            ShareClient.instance.shareDefault(
                requireActivity(),
                defaultFeed
            ) { sharingResult, error ->
                if (error != null) {
                    // 실패
                    val goWarning =
                        Intent(requireActivity(), DefaultDiaryWarningActivity::class.java)
                    goWarning.putExtra("warningType", "appInvitation")
                    startActivity(goWarning)
                } else if (sharingResult != null) {
                    Log.d("카카오", "카카오 공유 성공")
                    Log.d("카카오: Warning", "${sharingResult.warningMsg}")
                    Log.d("카카오: Argument", "${sharingResult.argumentMsg}")

                    startActivity(sharingResult.intent)
                }
            }
        } else {
            // 카카오 미설치: 웹 공유 사용 권장
            val shareUrl = WebSharerClient.instance.makeDefaultUrl(defaultFeed)
            // customTabs 로 웹 브라우저 열기

            // 1. CustomTabsServiceConnection 지원 브라우저 열기
            // ex) Chrome, 삼성 인터넷, FireFox, 웨일 등
            try {
                KakaoCustomTabsClient.openWithDefault(requireActivity(), shareUrl)
            } catch (e: UnsupportedOperationException) {
                // CustomTabsServiceConnection 지원 브라우저가 없을 때 예외 처리
                val goWarning = Intent(requireActivity(), DefaultDiaryWarningActivity::class.java)
                goWarning.putExtra("warningType", "appInvitation")
                startActivity(goWarning)
            }

            // 2. CustomTabsServiceConnection 미지원 브라우저 열기
            // ex) 다음, 네이버 등
            try {
                KakaoCustomTabsClient.open(requireActivity(), shareUrl)
            } catch (e: ActivityNotFoundException) {
                // 디바이스에 설치된 인터넷 브라우저가 없을 때 예외처리
                val goWarning = Intent(requireActivity(), DefaultDiaryWarningActivity::class.java)
                goWarning.putExtra("warningType", "appInvitation")
                startActivity(goWarning)
            }
        }
    }


}

