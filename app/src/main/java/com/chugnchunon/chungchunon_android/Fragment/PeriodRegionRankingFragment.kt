package com.chugnchunon.chungchunon_android.Fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.CommunityMenuAdapter
import com.chugnchunon.chungchunon_android.Adapter.RankingRecyclerAdapter
import com.chugnchunon.chungchunon_android.DataClass.Community
import com.chugnchunon.chungchunon_android.DataClass.DateFormat
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Fragment.PeriodAllRankingFragment.Companion.thisStepCheck
import com.chugnchunon.chungchunon_android.Layout.CustomBarChartRender
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingWeekBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.protobuf.Value
import com.kakao.auth.IApplicationConfig
import kotlinx.android.synthetic.main.fragment_region_data.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class PeriodRegionRankingFragment : Fragment() {

    private var _binding: FragmentRankingWeekBinding? = null
    private val binding get() = _binding!!

    lateinit var rankingAdapter: RankingRecyclerAdapter
    private var rankingItems = ArrayList<RankingLine>()

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var userPointArray = ArrayList<RankingLine>()

    lateinit var periodStart: Date
    lateinit var periodEnd: Date
    lateinit var thisNow: Date

    private var formatPeriodStart: String = ""
    private var formatPeriodEnd: String = ""
    private var formatThisNow: String = ""

    private var thisWeekMyStepCount: Int = 0
    private var thisWeekMyStepCountAvg: Int = 0

    private var thisWeekMyStepPoint: Float = 0f
    private var thisWeekMyDiaryPoint: Float = 0f

    private val mutex = Mutex()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var initialIndex: Int = 1
    lateinit var metrics: DisplayMetrics

    lateinit var regionCommunitySet: Community
    private var regionCommunityItems: ArrayList<Community> = ArrayList()
    private lateinit var communityMenuAdapter: CommunityMenuAdapter

    companion object {
        var regionQuestionClick = false
        var allLastWeekOrNot = false
        var communityOrNot = false
        var communityName: String = ""
    }

    lateinit var rankingBarChartView: BarChart

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRankingWeekBinding.inflate(inflater, container, false)
        val view = binding.root

        rankingBarChartView = view.findViewById<BarChart>(R.id.rankingBarChart)
        binding.communitySelectRecycler.visibility = View.VISIBLE

        communityMenuAdapter = CommunityMenuAdapter(requireContext(), regionCommunityItems, 0)
        binding.communitySelectRecycler.adapter = communityMenuAdapter
        binding.communitySelectRecycler.layoutManager = LinearLayoutManagerWrapper(
            requireActivity(),
            RecyclerView.HORIZONTAL,
            false
        )

        val mainLeftBox =
            ResourcesCompat.getDrawable(resources, R.drawable.mindbox_left_main_10, null)
        val grayLeftBox =
            ResourcesCompat.getDrawable(resources, R.drawable.mindbox_left_gray_10, null)
        val mainRightBox =
            ResourcesCompat.getDrawable(resources, R.drawable.mindbox_right_main_10, null)
        val grayRightBox =
            ResourcesCompat.getDrawable(resources, R.drawable.mindbox_right_gray_10, null)


        // 이번주 저번주 클릭
        binding.thisWeekBox.setOnClickListener {

            val mainBox =
                ResourcesCompat.getDrawable(resources, R.drawable.mindbox_left_main_10, null)
            val grayBox =
                ResourcesCompat.getDrawable(resources, R.drawable.mindbox_left_gray_10, null)

            binding.rankingBarChart.visibility = View.GONE
            binding.rankingBarChartProgressBar.visibility = View.VISIBLE
            binding.rankingRecyclerView.visibility = View.GONE
            binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE

            binding.thisWeekBox.background = mainLeftBox
            binding.lastWeekBox.background = grayRightBox
            binding.thisWeekText.setTextColor(Color.WHITE)
            binding.lastWeekText.setTextColor(Color.BLACK)

            allLastWeekOrNot = false

            initialIndex = 1
            userPointArray = ArrayList<RankingLine>()
            thisWeekMyStepCount = 0
            thisWeekMyStepCountAvg = 0

            thisWeekMyStepPoint = 0f
            thisWeekMyDiaryPoint = 0f

            periodSet()
            getGraph()

            if (!communityOrNot) {
                getRegionRanking()
            } else {
                getCommunityRanking(communityName)
            }
        }

        binding.lastWeekBox.setOnClickListener {

            val mainBox =
                ResourcesCompat.getDrawable(resources, R.drawable.mindbox_right_main_10, null)
            val grayBox =
                ResourcesCompat.getDrawable(resources, R.drawable.mindbox_right_main_10, null)

            binding.rankingBarChart.visibility = View.GONE
            binding.rankingBarChartProgressBar.visibility = View.VISIBLE
            binding.rankingRecyclerView.visibility = View.GONE
            binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE
            binding.thisWeekBox.background = grayLeftBox
            binding.lastWeekBox.background = mainRightBox
            binding.thisWeekText.setTextColor(Color.BLACK)
            binding.lastWeekText.setTextColor(Color.WHITE)

            allLastWeekOrNot = true

            initialIndex = 1
            userPointArray = ArrayList<RankingLine>()
            thisWeekMyStepCount = 0
            thisWeekMyStepCountAvg = 0

            thisWeekMyStepPoint = 0f
            thisWeekMyDiaryPoint = 0f

            periodSet()
            getGraph()

            if (!communityOrNot) {
                getRegionRanking()
            } else {
                getCommunityRanking(communityName)
            }

        }

        binding.pointQuestion.setOnClickListener {
            if (!regionQuestionClick) {

                binding.pointIntroduction.visibility = View.VISIBLE

                ObjectAnimator.ofFloat(binding.pointIntroduction, "translationY", -20f, 0f)
                    .apply {
                        duration = 300
                        interpolator = LinearInterpolator()
                        start()
                    }

                ObjectAnimator.ofFloat(binding.rankingRecyclerView, "translationY", -20f, 0f)
                    .apply {
                        duration = 300
                        interpolator = LinearInterpolator()
                        start()
                    }

                regionQuestionClick = true
            } else {
                binding.pointIntroduction.visibility = View.GONE

                ObjectAnimator.ofFloat(binding.pointIntroduction, "translationY", 0f, -40f)
                    .apply {
                        duration = 300
                        interpolator = LinearInterpolator()
                        start()
                    }

                ObjectAnimator.ofFloat(binding.rankingRecyclerView, "translationY", 0f, -40f)
                    .apply {
                        duration = 300
                        interpolator = LinearInterpolator()
                        start()
                    }

                regionQuestionClick = false
            }
        }

        initialIndex = 1
        regionQuestionClick = false
        allLastWeekOrNot = false
        communityOrNot = false
        communityName = ""

        getMenuData()
        periodSet()
        getGraph()
        getRegionRanking()

        binding.rankingBarChart.visibility = View.GONE
        binding.rankingBarChartProgressBar.visibility = View.VISIBLE
        binding.rankingRecyclerView.visibility = View.GONE
        binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE

        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            communityGroupSelectReceiver,
            IntentFilter("COMMUNITY_GROUP_SELECT")
        )

        return view
    }

    private fun getMenuData() {

        regionCommunityItems.clear()

        userDB.document("$userId").get()
            .addOnSuccessListener { originUserData ->
                val originUserRegion = originUserData.data?.getValue("region").toString()
                val originUserSmallRegion = originUserData.data?.getValue("smallRegion").toString()
                val userSmallRegionParts = originUserSmallRegion.split(" ")
                val userRegionLastPart = userSmallRegionParts.last()
                val originUserFullRegion = "${originUserRegion} ${originUserSmallRegion}"

                db.collection("contract_region")
                    .document(originUserFullRegion)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val regionImageDocument = task.result
                            if (regionImageDocument != null) {
                                if (regionImageDocument.exists()) {
                                    // 이미지 존재
                                    val regionImage =
                                        regionImageDocument.data?.getValue("regionImage").toString()
                                    regionCommunitySet = Community(
                                        userRegionLastPart,
                                        regionImage
                                    )
                                } else {
                                    regionCommunitySet = Community(
                                        userRegionLastPart,
                                        null
                                    )
                                }
                                regionCommunityItems.add(regionCommunitySet)
                                communityMenuAdapter.notifyDataSetChanged()
                            } else {
                                regionCommunitySet = Community(
                                    userRegionLastPart,
                                    null
                                )
                                regionCommunityItems.add(regionCommunitySet)
                                communityMenuAdapter.notifyDataSetChanged()
                            }

                            db.collection("community")
                                .get()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val communities = task.result
                                        if (communities != null) {
                                            if (!communities.isEmpty) {
                                                // 소속기관 있음
                                                for (communityData in communities) {
                                                    val communityTitle =
                                                        communityData.data.getValue("communityTitle")
                                                            .toString()
                                                    val communityImage =
                                                        communityData.data.getValue("communityImage")
                                                            .toString()
                                                    val communityUsers =
                                                        communityData.data.getValue("users") as ArrayList<String>

                                                    if (communityUsers.contains(userId)) {
                                                        regionCommunitySet = Community(
                                                            communityTitle,
                                                            communityImage
                                                        )
                                                        regionCommunityItems.add(regionCommunitySet)
                                                        communityMenuAdapter.notifyDataSetChanged()
                                                    }
                                                }
                                            } else {
                                                // 소속기관 없음
                                            }
                                        } else {
                                            // 소속기관 없음
                                        }
                                    }
                                }
                        }
                    }
            }
    }

    private fun periodSet() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.timeZone = android.icu.util.TimeZone.getTimeZone("Asia/Seoul")
        val today = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")

        if (!allLastWeekOrNot) {
            // 이번주
            periodStart = Calendar.getInstance(timeZone).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            periodEnd = Calendar.getInstance(timeZone).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 59)
            }.time
        } else {
            // 지난주
            periodStart = Calendar.getInstance(timeZone).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                add(Calendar.DAY_OF_MONTH, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            periodEnd = Calendar.getInstance(timeZone).apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                add(Calendar.DAY_OF_MONTH, -7)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 59)
            }.time
        }

        val now = Calendar.getInstance(timeZone).time

        formatPeriodStart = dateFormat.format(periodStart)
        formatPeriodEnd = dateFormat.format(periodEnd)
        formatThisNow = dateFormat.format(now)

//        thisWeekStart = dateFormat.parse(formatThisWeekStart)
//        thisWeekEnd = dateFormat.parse(formatThisWeekEnd)
        thisNow = dateFormat.parse(formatThisNow)

        val periodSubStringStart = formatPeriodStart.substring(5, 10)
        val periodSubStringEnd = formatPeriodEnd.substring(5, 10)
        val replacePeriodStart = periodSubStringStart.replace("-", "/")
        val replacePeriodEnd = periodSubStringEnd.replace("-", "/")
        binding.periodText.text = "${replacePeriodStart} ~ ${replacePeriodEnd}"
    }

    private fun getRegionRanking() {
        // 데이터 불러오기
        uiScope.launch(Dispatchers.IO) {
            launch { userIdRegionToArrayFun() }.join()
            launch { calculatePointFun() }.join()
            launch { indexArrayFun() }.join()
            launch { filterItemUpdate() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    binding.rankingRecyclerView.visibility = View.VISIBLE
                    binding.rankingRecyclerViewProgressBar.visibility = View.GONE
                }
            }
        }
    }


    private fun getCommunityRanking(communityName: String) {
        // 데이터 불러오기
        uiScope.launch(Dispatchers.IO) {
            launch { userIdCommunityToArrayFun(communityName) }.join()
            launch { calculatePointFun() }.join()
            launch { indexArrayFun() }.join()
            launch { filterItemUpdate() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    binding.rankingRecyclerView.visibility = View.VISIBLE
                    binding.rankingRecyclerViewProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun getGraph() {
        // graph
        uiScope.launch(Dispatchers.IO) {
            listOf(
                launch { graphStepFun() },
                launch { graphDiaryFun() }
            ).joinAll()
            launch { graphStepCalculateFun() }.join()
            launch { dataToGraph() }.join()
            withContext(Dispatchers.Main) {
                launch {
                    binding.rankingBarChart.visibility = View.VISIBLE
                    binding.rankingBarChartProgressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun dpTextSize(dp: Float): Float {
        metrics = context?.resources!!.displayMetrics
        val fpixels = metrics.density * dp
        val pixels = fpixels * 0.5f
        return pixels
    }

    override fun onResume() {
        super.onResume()
//        binding.rankingBarChart.animateY(1000, Easing.Linear)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            communityGroupSelectReceiver
        )
    }

    // 그래프 데이터

    suspend fun dataToGraph() = withContext(Dispatchers.Main) {

        val entryOne = ArrayList<BarEntry>()
        val entryTwo = ArrayList<BarEntry>()

        entryOne.add(BarEntry(1f, 3f))
        entryOne.add(BarEntry(2f, 4f))

        entryTwo.add(BarEntry(3f, thisWeekMyStepPoint))
        entryTwo.add(BarEntry(4f, thisWeekMyDiaryPoint))

        val bds1 = BarDataSet(entryOne, "목표")
        bds1.setColor(ContextCompat.getColor(requireActivity(), R.color.light_gray));
        bds1.valueTextSize = dpTextSize(13f)
        bds1.valueTextColor = ContextCompat.getColor(requireActivity(), R.color.custom_gray)
        bds1.valueFormatter = ThisMyValueFormatter("goal", thisWeekMyStepCountAvg)

        val bds2 = BarDataSet(entryTwo, "나")
        bds2.setColor(ContextCompat.getColor(requireActivity(), R.color.main_color));
        bds2.valueTextSize = dpTextSize(13f)
        bds2.valueTextColor = Color.BLACK
        bds2.valueFormatter = ThisMyValueFormatter("me", thisWeekMyStepCountAvg)

        val graphArr = ArrayList<IBarDataSet>()
        graphArr.add(bds1)
        graphArr.add(bds2)

        val data = BarData(graphArr)

        data.barWidth = 0.6f
        binding.rankingBarChart.data = data
        binding.rankingBarChart.groupBars(0.3f, 0.4f, 0.3f)
        binding.rankingBarChart.setFitBars(false)
        binding.rankingBarChart.setDrawValueAboveBar(true)
        binding.rankingBarChart.setNoDataText("")
        binding.rankingBarChart.legend.isEnabled = false

        binding.rankingBarChart.xAxis.setDrawGridLines(false)
        binding.rankingBarChart.axisLeft.setDrawGridLines(false)
        binding.rankingBarChart.axisRight.setDrawGridLines(false)

        binding.rankingBarChart.xAxis.isEnabled = false
        binding.rankingBarChart.axisLeft.isEnabled = false
        binding.rankingBarChart.axisRight.isEnabled = false

        binding.rankingBarChart.axisLeft.axisMinimum = 0f
        binding.rankingBarChart.axisRight.axisMinimum = 0f

        binding.rankingBarChart.setDrawBarShadow(true)
        binding.rankingBarChart.setTouchEnabled(false)

        binding.rankingBarChart.description.isEnabled = false

        val barChartRender = CustomBarChartRender(
            binding.rankingBarChart,
            binding.rankingBarChart.animator,
            binding.rankingBarChart.viewPortHandler,
        )
        barChartRender.setRadius(20f)
        barChartRender.initBuffers()
        binding.rankingBarChart.renderer = barChartRender
        binding.rankingBarChart.animateY(1000, Easing.Linear)
        binding.rankingBarChart.invalidate()
    }

    suspend fun graphStepFun() {
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

        val userSteps = db.collection("user_step_count")
            .document("$userId")
            .get()
            .await()

        for (stepDate in startDate..endDate) {
            userSteps.data?.forEach { (period, dateStepCount) ->
                if (period == stepDate.toString()) {
                    val dateStepCountToInt = (dateStepCount as Long).toInt()
                    if (dateStepCountToInt > 0) {
                        thisWeekMyStepCount += dateStepCountToInt
                    } else {
                        // null
                    }
                }
            }
        }
    }

    suspend fun graphStepCalculateFun() {
        val daysDiffTime = thisNow.time - periodStart.time
        val daysDiffDate = TimeUnit.DAYS.convert(daysDiffTime, TimeUnit.MILLISECONDS)
        thisWeekMyStepCountAvg = ((thisWeekMyStepCount / (daysDiffDate + 1).toDouble())).toInt()
        thisWeekMyStepPoint = (Math.round(thisWeekMyStepCountAvg / 1000.0)).toFloat()
    }

    suspend fun graphDiaryFun() {
        val thisWeekMyDiaryDB = diaryDB
            .whereGreaterThanOrEqualTo("timestamp", periodStart)
            .whereLessThanOrEqualTo("timestamp", periodEnd)
            .whereEqualTo("userId", userId)
            .count()

        val diaryTask = thisWeekMyDiaryDB.get(AggregateSource.SERVER).await()
        if (diaryTask != null) {
            thisWeekMyDiaryPoint = (diaryTask.count).toFloat()
        }
    }

    // 리스트 데이터
    suspend fun calculatePointFun() = coroutineScope {
        try {
            listOf(
                launch { stepCountToArrayFun() },
                launch { diaryToArrayFun() },
                launch { commentToArrayFun() },
//                launch { likeToArrayFun() }
            ).joinAll()

        } catch (e: Throwable) {
            false
        }
    }

    suspend fun userIdRegionToArrayFun() {
        val myUserDocument = userDB.document("$userId").get().await()
        val myUserRegion = myUserDocument.data?.getValue("region").toString()
        val myUserSmallRegion = myUserDocument.data?.getValue("smallRegion").toString()
        val myUserFullRegion = "${myUserRegion} ${myUserSmallRegion}"
        val userdocuments = userDB.get().await()

        for (document in userdocuments) {
            if (document.data.containsKey("userType")) {
                try {
                    val userId = document.data?.getValue("userId").toString()
                    val username = document.data.getValue("name").toString()
                    val userAvatar = document.data.getValue("avatar").toString()
                    val userRegion = document.data.getValue("region").toString()
                    val userSmallRegion = document.data.getValue("smallRegion").toString()
                    val diaryUserFullRegion = "${userRegion} ${userSmallRegion}"

                    if (diaryUserFullRegion == myUserFullRegion && userId != "kakao:2358828971") {
                        val userEntry = RankingLine(
                            0,
                            userId,
                            username,
                            userAvatar,
                            0,
                        )
                        userPointArray.add(userEntry)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun userIdCommunityToArrayFun(communityName: String) {
        val communityDocument = db.collection("community").document(communityName).get().await()
        val communityUsers = communityDocument.data?.getValue("users") as ArrayList<String>
        val userdocuments = userDB.get().await()

        for (document in userdocuments) {
            if (document.data.containsKey("userType")) {
                val userName = document.data.getValue("name").toString()
                val userType = document.data.getValue("userType").toString()

                try {
                    val userId = document.data?.getValue("userId").toString()
                    val username = document.data.getValue("name").toString()
                    val userAvatar = document.data.getValue("avatar").toString()
                    val userRegion = document.data.getValue("region").toString()
                    val userSmallRegion = document.data.getValue("smallRegion").toString()
                    val diaryUserFullRegion = "${userRegion} ${userSmallRegion}"

                    if (communityUsers.contains(userId) && userId != "kakao:2358828971") {
                        val userEntry = RankingLine(
                            0,
                            userId,
                            username,
                            userAvatar,
                            0,
                        )

                        userPointArray.add(userEntry)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

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

        for (stepDate in startDate..endDate) {
            val dataSteps = db.collection("period_step_count")
                .document("$stepDate")
                .get()
                .await()


            dataSteps.data?.forEach { (stepUserId, dateStepCount) ->
                if (dateStepCount.toString().toInt() < 10000) {
                    if (dateStepCount.toString().toInt() > 0) {
                        // 걸음수 0~만보 사이 (일반)
                        val dateStepInt = (dateStepCount as Long).toInt()
                        val dateToPoint = ((Math.floor(dateStepInt / 1000.0)) * 10).toInt()

                        userPointArray.forEach {
                            if (it.userId == stepUserId) {
                                val currentpoint = it.point as Int
                                it.point = currentpoint + dateToPoint
                            }
                        }
                    } else {
                        // 걸음수 0 보다 적은 경우
                    }
                } else {
                    // 걸음수 만보 보다 큰 경우

                    userPointArray.forEach {
                        if (it.userId == stepUserId) {
                            val currentpoint = it.point as Int
                            it.point = currentpoint + 100
                        }
                    }
                }
            }

        }
    }


    suspend fun diaryToArrayFun() {
        val diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", periodStart)
            .whereLessThanOrEqualTo("timestamp", periodEnd)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            val userId = diaryDocument.data.getValue("userId").toString()
            val diaryTimestamp = diaryDocument.data.getValue("timestamp") as Timestamp

            userPointArray.forEach {
                if (userId == it.userId) {
                    val currentpoint = it.point as Int
                    it.point = currentpoint + 100
                }
            }
        }
    }

    suspend fun commentToArrayFun() {
        val commentDocuments = db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", periodStart)
            .whereLessThanOrEqualTo("timestamp", periodEnd)
            .get()
            .await()

        for (commentDocument in commentDocuments) {
            val userId = commentDocument.data.getValue("userId").toString()

            userPointArray.forEach {
                if (userId == it.userId) {
                    val currentpoint = it.point as Int
                    it.point = currentpoint + 20
                }
            }
        }
    }

    suspend fun likeToArrayFun() {
        val likeDocuments = db.collectionGroup("likes")
            .whereGreaterThanOrEqualTo("timestamp", periodStart)
            .whereLessThanOrEqualTo("timestamp", periodEnd)
            .get()
            .await()

        for (likeDocument in likeDocuments) {
            val userId = likeDocument.data.getValue("id").toString()
            val likeTimestamp = likeDocument.data.getValue("timestamp") as Timestamp

            userPointArray.forEach {
                if (userId == it.userId) {
                    val currentpoint = it.point as Int
                    it.point = currentpoint + 10
                }
            }
        }
    }

    suspend fun indexArrayFun() {
        userPointArray.sortWith(compareBy { it.point })
        userPointArray.reverse()

        for (i in 0..userPointArray.size - 1) {
            userPointArray[i].index = initialIndex

            if (i != userPointArray.size - 1) {
                if (userPointArray[i].point != userPointArray[i + 1].point) {
                    initialIndex += 1
                }
            } else {
                // i+1 = userPointArray.size
                null
            }

        }
    }

    suspend fun filterItemUpdate() = withContext(Dispatchers.Main) {
//        rankingItems = ArrayList(userPointArray.filter { it.index!! <= 10 })
        rankingItems = ArrayList(userPointArray.filter { it.point!! != 0 })

        rankingAdapter = RankingRecyclerAdapter(requireActivity(), rankingItems)
        binding.rankingRecyclerView.adapter = rankingAdapter
        binding.rankingRecyclerView.layoutManager =
            LinearLayoutManagerWrapper(
                requireActivity(),
                RecyclerView.VERTICAL,
                false
            )
    }

    var communityGroupSelectReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onReceive(context: Context?, intent: Intent?) {
            val selectPosition = intent?.getIntExtra("selectPosition", 0)
            communityName = intent?.getStringExtra("selectCommunity").toString()

            if (selectPosition == 0) {
                binding.rankingRecyclerView.visibility = View.GONE
                binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE

                communityOrNot = false
                allLastWeekOrNot = false

                initialIndex = 1
                userPointArray = ArrayList<RankingLine>()
                thisWeekMyStepCount = 0
                thisWeekMyStepCountAvg = 0

                thisWeekMyStepPoint = 0f
                thisWeekMyDiaryPoint = 0f

//                communityMenuAdapter.notifyDataSetChanged()
                getRegionRanking()
            } else {
                binding.rankingRecyclerView.visibility = View.GONE
                binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE

                communityOrNot = true
                allLastWeekOrNot = false

                initialIndex = 1
                userPointArray = ArrayList<RankingLine>()
                thisWeekMyStepCount = 0
                thisWeekMyStepCountAvg = 0

                thisWeekMyStepPoint = 0f
                thisWeekMyDiaryPoint = 0f

//                communityMenuAdapter.notifyDataSetChanged()
                getCommunityRanking(communityName)
            }
        }
    }
}


