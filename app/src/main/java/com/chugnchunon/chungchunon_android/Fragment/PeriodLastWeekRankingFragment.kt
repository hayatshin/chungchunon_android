package com.chugnchunon.chungchunon_android.Fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
                binding.pointIntroduction.alpha = 0f

                binding.pointIntroduction.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()

                ObjectAnimator.ofFloat(binding.pointIntroduction, "translationY", -20f, 0f)
                    .apply {
                        duration = 300
                        interpolator = LinearInterpolator()
                        start()
                    }


                questionClick = true
            } else {
                Handler().postDelayed({
                    binding.pointIntroduction.visibility = View.GONE
                }, 400)

                binding.pointIntroduction.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .start()

                ObjectAnimator.ofFloat(binding.pointIntroduction, "translationY", 0f, -40f)
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
        bds1.setColor(ContextCompat.getColor(requireActivity(), R.color.anxious_color));
        bds1.valueTextSize = 16f
        bds1.valueTextColor = Color.WHITE
        bds1.valueFormatter = LastMyValueFormatter("goal", lastWeekMyStepCountAvg)

        var bds2 = BarDataSet(entryTwo, "나")
        bds2.setColor(ContextCompat.getColor(requireActivity(), R.color.teal_200));
        bds2.valueTextSize = 16f
        bds2.valueTextColor = Color.WHITE
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
            db.collection("user_step_count")
                .document("$userId")
                .get()
                .addOnSuccessListener { userSteps ->
                    userSteps.data?.forEach { (period, dateStepCount) ->
                        if (period == stepDate.toString()) {
                            lastWeekMyStepCount += (dateStepCount as Long).toInt()
                        }
                    }
                }.await()
        }
    }

    suspend fun graphStepCalculateFun() {
        var daysDiffTime = lastWeekEnd.time - lastWeekStart.time
        var daysDiffDate = TimeUnit.DAYS.convert(daysDiffTime, TimeUnit.MILLISECONDS)

        lastWeekMyStepCountAvg = ((lastWeekMyStepCount / (daysDiffDate+1).toDouble())).toInt()
        lastWeekMyStepPoint = (Math.round(lastWeekMyStepCountAvg / 1000.0)).toFloat()
    }

    suspend fun graphDiaryFun() {
        var thisWeekMyDiaryDB = diaryDB
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .whereEqualTo("userId", userId)
            .count()

        thisWeekMyDiaryDB.get(AggregateSource.SERVER).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                lastWeekMyDiaryPoint = (task.result.count).toFloat()
            }
        }.await()
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
        userDB.get().addOnSuccessListener { documents ->
            for (document in documents) {
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
        }.await()
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
            db.collection("period_step_count")
                .document("$stepDate")
                .get()
                .addOnSuccessListener { dateSteps ->
                    dateSteps.data?.forEach { (stepUserId, dateStepCount) ->

                        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
                            if (keyUserId == stepUserId) {
                                userStepCountHashMap[keyUserId] =
                                    valueStepCount + (dateStepCount as Long).toInt()
                            }
                        }
                    }
                }.await()
        }

        userStepCountHashMap.forEach { (keyUserId, valueStepCount) ->
            userStepCountHashMap[keyUserId] = (Math.floor(valueStepCount / 1000.0) * 10).toInt()
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
        db.collection("diary")
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .get()
            .addOnSuccessListener { diaryDocuments ->
                for (diaryDocument in diaryDocuments) {
                    var userId = diaryDocument.data.getValue("userId").toString()

                    userPointArray.forEach {
                        if (it.userId == userId) {
                            var currentpoint = it.point as Int
                            it.point = currentpoint + 100
                        }
                    }
                }
            }.await()
    }

    suspend fun commentToArrayFun() {
        db.collectionGroup("comments")
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .get()
            .addOnSuccessListener { commentDocuments ->
                for (commentDocument in commentDocuments) {
                    var userId = commentDocument.data.getValue("userId").toString()
                    userPointArray.forEach {
                        if (it.userId == userId) {
                            var currentpoint = it.point as Int
                            it.point = currentpoint + 20
                        }
                    }
                }
            }.await()
    }

    suspend fun likeToArrayFun() {
        db.collectionGroup("likes")
            .whereGreaterThanOrEqualTo("timestamp", lastWeekStart)
            .whereLessThanOrEqualTo("timestamp", lastWeekEnd)
            .get()
            .addOnSuccessListener { likeDocuments ->
                for (likeDocument in likeDocuments) {
                    var userId = likeDocument.data.getValue("id").toString()
                    userPointArray.forEach {
                        if (it.userId == userId) {
                            var currentpoint = it.point as Int
                            it.point = currentpoint + 10
                        }
                    }
                }
            }.await()
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
        rankingItems = ArrayList(userPointArray.filter { it.index!! <= 10 })

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

class LastMyValueFormatter(var position: String, var lastWeekStepCount: Int) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {

        if (position == "goal" && lastStepCheck) {
            lastStepCheck = !lastStepCheck
            return "3,000 걸음"
        } else if (position =="goal" && !lastStepCheck) {
            lastStepCheck = !lastStepCheck
            return "4일"
        } else if (position != "goal" && lastStepCheck) {
            var decimal = DecimalFormat("#,###")
            var stepLabel = decimal.format(lastWeekStepCount)
            lastStepCheck = !lastStepCheck
            return "${stepLabel} 걸음"
        } else {
            lastStepCheck = !lastStepCheck
            return "${value.toInt()} 일"
        }
    }
}






