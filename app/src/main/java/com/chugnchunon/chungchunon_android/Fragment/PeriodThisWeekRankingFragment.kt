package com.chugnchunon.chungchunon_android.Fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.RankingRecyclerAdapter
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Fragment.PeriodThisWeekRankingFragment.Companion.thisStepCheck
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.protobuf.Value
import com.kakao.auth.IApplicationConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class PeriodThisWeekRankingFragment : Fragment() {

    private var _binding: FragmentRankingWeekBinding? = null
    private val binding get() = _binding!!

    lateinit var rankingAdapter: RankingRecyclerAdapter
    private var rankingItems = ArrayList<RankingLine>()

    private val db = Firebase.firestore
    private val userDB = Firebase.firestore.collection("users")
    private val diaryDB = Firebase.firestore.collection("diary")
    private val userId = Firebase.auth.currentUser?.uid

    private var userPointArray = ArrayList<RankingLine>()
    private var userStepCountHashMap = hashMapOf<String, Int>()

    private var formatThisWeekStart: String = ""
    private var formatThisWeekEnd: String = ""
    private var formatThisNow: String = ""

    lateinit var thisWeekStart: Date
    lateinit var thisWeekEnd: Date
    lateinit var thisNow: Date

    private var thisWeekMyStepCount: Int = 0
    private var thisWeekMyStepCountAvg: Int = 0

    private var thisWeekMyStepPoint: Float = 0f
    private var thisWeekMyDiaryPoint: Float = 0f

    private val mutex = Mutex()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var initialIndex: Int = 1
    lateinit var metrics: DisplayMetrics

    companion object {
        var questionClick = false
        var thisStepCheck = true
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

        binding.pointQuestion.setOnClickListener {
            if (!questionClick) {

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

                questionClick = true
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

                questionClick = false
            }
        }

        // 이번주 시작
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val today = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val startOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }.time

        val endOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.time

        val startOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR) - 1)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }.time

        val endOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR) - 1)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }.time

        val now = Calendar.getInstance(timeZone).time

        formatThisWeekStart = dateFormat.format(startOfWeek)
        formatThisWeekEnd = dateFormat.format(endOfWeek)
        formatThisNow = dateFormat.format(now)

        thisWeekStart = dateFormat.parse(formatThisWeekStart)
        thisWeekEnd = dateFormat.parse(formatThisWeekEnd)
        thisNow = dateFormat.parse(formatThisNow)

        binding.rankingBarChart.visibility = View.GONE
        binding.rankingBarChartProgressBar.visibility = View.VISIBLE
        binding.rankingRecyclerView.visibility = View.GONE
        binding.rankingRecyclerViewProgressBar.visibility = View.VISIBLE

        // 데이터 불러오기
        uiScope.launch(Dispatchers.IO) {
            launch { userIdToArrayFun() }.join()
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

        return view
    }

    private fun dpTextSize(dp: Float): Float {
        metrics = context?.resources!!.displayMetrics
        var fpixels = metrics.density * dp
        var pixels = fpixels * 0.5f
        return pixels
    }

    override fun onResume() {
        super.onResume()
        binding.rankingBarChart.animateY(1000, Easing.Linear)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    // 그래프 데이터

    suspend fun dataToGraph() = withContext(Dispatchers.Main) {

        var entryOne = ArrayList<BarEntry>()
        var entryTwo = ArrayList<BarEntry>()

        entryOne.add(BarEntry(1f, 3f))
        entryOne.add(BarEntry(2f, 4f))

        entryTwo.add(BarEntry(3f, thisWeekMyStepPoint))
        entryTwo.add(BarEntry(4f, thisWeekMyDiaryPoint))

        var bds1 = BarDataSet(entryOne, "목표")
        bds1.setColor(ContextCompat.getColor(requireActivity(), R.color.light_gray));
        bds1.valueTextSize = dpTextSize(13f)
        bds1.valueTextColor = ContextCompat.getColor(requireActivity(), R.color.custom_gray)
        bds1.valueFormatter = ThisMyValueFormatter("goal", thisWeekMyStepCountAvg)

        var bds2 = BarDataSet(entryTwo, "나")
        bds2.setColor(ContextCompat.getColor(requireActivity(), R.color.main_color));
        bds2.valueTextSize = dpTextSize(13f)
        bds2.valueTextColor = Color.BLACK
        bds2.valueFormatter = ThisMyValueFormatter("me", thisWeekMyStepCountAvg)

        var graphArr = ArrayList<IBarDataSet>()
        graphArr.add(bds1)
        graphArr.add(bds2)

        var data = BarData(graphArr)

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

        var barChartRender = CustomBarChartRender(
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
            formatThisWeekStart.substring(0, 4).toInt(),
            formatThisWeekStart.substring(5, 7).toInt(),
            formatThisWeekStart.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatThisWeekEnd.substring(0, 4).toInt(),
            formatThisWeekEnd.substring(5, 7).toInt(),
            formatThisWeekEnd.substring(8, 10).toInt()
        )

        for (stepDate in startDate..endDate) {
            var userSteps = db.collection("user_step_count")
                .document("$userId")
                .get()
                .await()

            userSteps.data?.forEach { (period, dateStepCount) ->
                if (period == stepDate.toString()) {
                    thisWeekMyStepCount += (dateStepCount as Long).toInt()
                }
            }
        }
    }

    suspend fun graphStepCalculateFun() {
        var daysDiffTime = thisNow.time - thisWeekStart.time
        var daysDiffDate = TimeUnit.DAYS.convert(daysDiffTime, TimeUnit.MILLISECONDS)
        thisWeekMyStepCountAvg = ((thisWeekMyStepCount / (daysDiffDate + 1).toDouble())).toInt()
        thisWeekMyStepPoint = (Math.round(thisWeekMyStepCountAvg / 1000.0)).toFloat()
    }

    suspend fun graphDiaryFun() {
        var thisWeekMyDiaryDB = diaryDB
            .whereGreaterThanOrEqualTo("timestamp", thisWeekStart)
            .whereLessThanOrEqualTo("timestamp", thisWeekEnd)
            .whereEqualTo("userId", userId)
            .count()

        var diaryTask = thisWeekMyDiaryDB.get(AggregateSource.SERVER).await()
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
                launch { likeToArrayFun() }
            ).joinAll()

        } catch (e: Throwable) {
            false
        }
    }

    suspend fun userIdToArrayFun() {
        var userdocuments = userDB.get().await()

        for (document in userdocuments) {
            var userType = document.data.getValue("userType").toString()

            if(userType == "사용자") {
                var userId = document.data?.getValue("userId").toString()
                var username = document.data.getValue("name").toString()
                var userAvatar = document.data.getValue("avatar").toString()

                var userEntry = RankingLine(
                    0,
                    userId,
                    username,
                    userAvatar,
                    0,
                )

                userPointArray.add(userEntry)
                userStepCountHashMap.put(userId, 0)
            }
        }
    }


    suspend fun stepCountToArrayFun() {

        val startDate = LocalDate.of(
            formatThisWeekStart.substring(0, 4).toInt(),
            formatThisWeekStart.substring(5, 7).toInt(),
            formatThisWeekStart.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatThisWeekEnd.substring(0, 4).toInt(),
            formatThisWeekEnd.substring(5, 7).toInt(),
            formatThisWeekEnd.substring(8, 10).toInt()
        )

        for (stepDate in startDate..endDate) {
            var dataSteps = db.collection("period_step_count")
                .document("$stepDate")
                .get()
                .await()


            dataSteps.data?.forEach { (stepUserId, dateStepCount) ->
                userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
                    if (keyUserId == stepUserId) {
                        userStepCountHashMap[keyUserId] =
                            valueStepCount + (dateStepCount as Long).toInt()
                    }
                }
            }
        }

        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
            userStepCountHashMap[keyUserId] = ((Math.floor(valueStepCount / 1000.0)) * 10).toInt()
        }

        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->

            userPointArray.forEach {
                if (it.userId == keyUserId) {
                    var currentpoint = it.point as Int
                    it.point = currentpoint + valueStepCount
                }

            }
        }
    }


    suspend fun diaryToArrayFun() {
        var diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", thisWeekStart)
            .whereLessThanOrEqualTo("timestamp", thisWeekEnd)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            var userId = diaryDocument.data.getValue("userId").toString()

            userPointArray.forEach {
                if (it.userId == userId) {
                    var currentpoint = it.point as Int
                    it.point = currentpoint + 100
                }
            }
        }
    }

    suspend fun commentToArrayFun() {
        var commentDocuments = db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", thisWeekStart)
            .whereLessThanOrEqualTo("timestamp", thisWeekEnd)
            .get()
            .await()

        for (commentDocument in commentDocuments) {
            var userId = commentDocument.data.getValue("userId").toString()
            userPointArray.forEach {
                if (it.userId == userId) {
                    var currentpoint = it.point as Int
                    it.point = currentpoint + 20
                }
            }
        }
    }

    suspend fun likeToArrayFun() {
        var likeDocuments = db.collectionGroup("likes")
            .whereGreaterThanOrEqualTo("timestamp", thisWeekStart)
            .whereLessThanOrEqualTo("timestamp", thisWeekEnd)
            .get()
            .await()

        for (likeDocument in likeDocuments) {
            var userId = likeDocument.data.getValue("id").toString()
            userPointArray.forEach {
                if (it.userId == userId) {
                    var currentpoint = it.point as Int
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

        rankingAdapter = RankingRecyclerAdapter(requireActivity(), userPointArray)
        binding.rankingRecyclerView.adapter = rankingAdapter
        binding.rankingRecyclerView.layoutManager =
            LinearLayoutManagerWrapper(
                requireActivity(),
                RecyclerView.VERTICAL,
                false
            )
    }
}

class ThisMyValueFormatter(var position: String, var thisWeekMyStepCount: Int) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {

        if (position == "goal" && thisStepCheck) {
            thisStepCheck = !thisStepCheck
            return "3,000 보"
        } else if (position == "goal" && !thisStepCheck) {
            thisStepCheck = !thisStepCheck
            return "4 회"
        } else if (position != "goal" && thisStepCheck) {
            var decimal = DecimalFormat("#,###")
            var stepLabel = decimal.format(thisWeekMyStepCount)
            thisStepCheck = !thisStepCheck
            return "${stepLabel} 보"
        } else {
            thisStepCheck = !thisStepCheck
            return "${value.toInt()} 회"
        }
    }
}

