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
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.Keep
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.chugnchunon.chungchunon_android.Adapter.RankingRecyclerAdapter
import com.chugnchunon.chungchunon_android.DataClass.RankingLine
import com.chugnchunon.chungchunon_android.Fragment.PeriodLastWeekRankingFragment.Companion.lastStepCheck
import com.chugnchunon.chungchunon_android.Layout.CustomBarChartRender
import com.chugnchunon.chungchunon_android.R
import com.chugnchunon.chungchunon_android.databinding.FragmentRankingWeekBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.tasks.await
import me.moallemi.tools.daterange.localdate.rangeTo
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class PeriodLastWeekRankingFragment : Fragment() {

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

    private var formatLastWeekStart: String = ""
    private var formatLastWeekEnd: String = ""
    lateinit var lastWeekStart: Date
    lateinit var lastWeekEnd: Date

    private var lastWeekMyStepCount: Int = 0
    private var lastWeekMyStepCountAvg: Int = 0

    private var lastWeekMyStepPoint: Float = 0f
    private var lastWeekMyDiaryPoint: Float = 0f

    private val mutex = Mutex()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private var initialIndex: Int = 1
    lateinit var metrics: DisplayMetrics

    companion object {
        var questionClick = false
        var lastStepCheck = true
    }

    @SuppressLint("SimpleDateFormat")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRankingWeekBinding.inflate(inflater, container, false)
        val view = binding.root

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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormat.timeZone = android.icu.util.TimeZone.getTimeZone("Asia/Seoul")
        val today = Calendar.getInstance()
        val timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val startOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 59)
        }.time

        val startOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
//            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR))
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.DAY_OF_MONTH, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val endOfLastWeek = Calendar.getInstance(timeZone).apply {
            firstDayOfWeek = Calendar.MONDAY
//            set(Calendar.WEEK_OF_YEAR, today.get(Calendar.WEEK_OF_YEAR))
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_MONTH, -7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 59)
        }.time

        formatLastWeekStart = dateFormat.format(startOfLastWeek)
        formatLastWeekEnd = dateFormat.format(endOfLastWeek)

        lastWeekStart = dateFormat.parse(formatLastWeekStart)
        lastWeekEnd = dateFormat.parse(formatLastWeekEnd)

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

        entryTwo.add(BarEntry(3f, lastWeekMyStepPoint))
        entryTwo.add(BarEntry(4f, lastWeekMyDiaryPoint))


        var bds1 = BarDataSet(entryOne, "목표")
        bds1.setColor(ContextCompat.getColor(requireActivity(), R.color.light_gray));
        bds1.valueTextSize = dpTextSize(13f)
        bds1.valueTextColor = ContextCompat.getColor(requireActivity(), R.color.custom_gray)
        bds1.valueFormatter = LastMyValueFormatter("goal", lastWeekMyStepCountAvg)

        var bds2 = BarDataSet(entryTwo, "나")
        bds2.setColor(ContextCompat.getColor(requireActivity(), R.color.main_color));
        bds2.valueTextSize = dpTextSize(13f)
        bds2.valueTextColor = Color.BLACK
        bds2.valueFormatter = LastMyValueFormatter("me", lastWeekMyStepCountAvg)

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
        binding.rankingBarChart.animateY(1000, Easing.Linear)

        binding.rankingBarChart.description.isEnabled = false

        var barChartRender = CustomBarChartRender(
            binding.rankingBarChart,
            binding.rankingBarChart.animator,
            binding.rankingBarChart.viewPortHandler,
        )
        barChartRender.setRadius(20f)
        barChartRender.initBuffers()
        binding.rankingBarChart.renderer = barChartRender


        binding.rankingBarChart.invalidate()
    }

    suspend fun graphStepFun() {
        val startDate = LocalDate.of(
            formatLastWeekStart.substring(0, 4).toInt(),
            formatLastWeekStart.substring(5, 7).toInt(),
            formatLastWeekStart.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatLastWeekEnd.substring(0, 4).toInt(),
            formatLastWeekEnd.substring(5, 7).toInt(),
            formatLastWeekEnd.substring(8, 10).toInt()
        )

        for (stepDate in startDate..endDate) {
            var userSteps = db.collection("user_step_count")
                .document("$userId")
                .get()
                .await()

            userSteps.data?.forEach { (period, dateStepCount) ->
                if (period == stepDate.toString()) {
                    lastWeekMyStepCount += (dateStepCount as Long).toInt()
                }
            }
        }
    }

    suspend fun graphStepCalculateFun() {
        var daysDiffTime = lastWeekEnd.time - lastWeekStart.time
        var daysDiffDate = TimeUnit.DAYS.convert(daysDiffTime, TimeUnit.MILLISECONDS)
        lastWeekMyStepCountAvg = ((lastWeekMyStepCount / (daysDiffDate + 1).toDouble())).toInt()
        lastWeekMyStepPoint = (Math.round(lastWeekMyStepCountAvg / 1000.0)).toFloat()

    }

    suspend fun graphDiaryFun() {
        var thisWeekMyDiaryDB = diaryDB
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .whereEqualTo("userId", userId)
            .count()

        var diaryTask = thisWeekMyDiaryDB.get(AggregateSource.SERVER).await()
        if (diaryTask != null) {
            lastWeekMyDiaryPoint = (diaryTask.count).toFloat()
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

    suspend fun userIdToArrayFun() {
        val userdocuments = userDB.get().await()

        for (document in userdocuments) {
            if(document.data.containsKey("userType")){
                val userType = document.data.getValue("userType").toString()

                if(userType == "사용자") {
                    try {
                        val userId = document.data?.getValue("userId").toString()
                        val username = document.data.getValue("name").toString()
                        val userAvatar = document.data.getValue("avatar").toString()

                        val userEntry = RankingLine(
                            0,
                            userId,
                            username,
                            userAvatar,
                            0,
                        )

                        userPointArray.add(userEntry)
                        userStepCountHashMap.put(userId, 0)
                    } catch (e:Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    suspend fun stepCountToArrayFun() {

        val startDate = LocalDate.of(
            formatLastWeekStart.substring(0, 4).toInt(),
            formatLastWeekStart.substring(5, 7).toInt(),
            formatLastWeekStart.substring(8, 10).toInt()
        )
        val endDate = LocalDate.of(
            formatLastWeekEnd.substring(0, 4).toInt(),
            formatLastWeekEnd.substring(5, 7).toInt(),
            formatLastWeekEnd.substring(8, 10).toInt()
        )

        for (stepDate in startDate..endDate) {
            val dataSteps = db.collection("period_step_count")
                .document("$stepDate")
                .get()
                .await()


            dataSteps.data?.forEach { (stepUserId, dateStepCount) ->
                userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
                    if (keyUserId == stepUserId) {
                        if(dateStepCount.toString().toInt() < 10000) {
                            userStepCountHashMap[keyUserId] =
                                valueStepCount + (dateStepCount as Long).toInt()
                        } else {
                            userStepCountHashMap[keyUserId] =
                                valueStepCount + 10000
                        }
                    }
                }
            }
        }
        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
            userStepCountHashMap[keyUserId] = (Math.floor(valueStepCount / 1000.0) * 10).toInt()
        }

        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->

            userPointArray.forEach {
                if (it.userId == keyUserId) {
                    val currentpoint = it.point as Int
                    it.point = currentpoint + valueStepCount
                }

            }
        }
    }


    suspend fun diaryToArrayFun() {
        val diaryDocuments = db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .get()
            .await()

        for (diaryDocument in diaryDocuments) {
            val userId = diaryDocument.data.getValue("userId").toString()

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
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
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
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .get()
            .await()

        for (likeDocument in likeDocuments) {
            val userId = likeDocument.data.getValue("id").toString()
            userPointArray.forEach {
                if (userId == it.userId) {
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
}

@Keep
class LastMyValueFormatter(var position: String, var lastWeekStepCount: Int) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {

        if (position == "goal" && lastStepCheck) {
            lastStepCheck = !lastStepCheck
            return "3,000 보"
        } else if (position == "goal" && !lastStepCheck) {
            lastStepCheck = !lastStepCheck
            return "4 회"
        } else if (position != "goal" && lastStepCheck) {
            val decimal = DecimalFormat("#,###")
            val stepLabel = decimal.format(lastWeekStepCount)
            lastStepCheck = !lastStepCheck
            return "${stepLabel} 보"
        } else {
            lastStepCheck = !lastStepCheck
            return "${value.toInt()} 회"
        }
    }
}






