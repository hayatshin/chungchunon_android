package com.chugnchunon.chungchunon_android.BroadcastReceiver

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.chugnchunon.chungchunon_android.Fragment.MyDiaryFragment
import com.chugnchunon.chungchunon_android.MyService.Companion.ACTION_STEP_COUNTER_NOTIFICATION
import com.google.android.material.internal.ContextUtils.getActivity

open class StepCountBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if(intent!!.action == ACTION_STEP_COUNTER_NOTIFICATION) {
            var todayTotalStepCount = intent.getIntExtra("todayTotalStepCount", 0)


            val intent = Intent(ACTION_STEP_COUNTER_NOTIFICATION).apply { putExtra("todayTotalStepCount", todayTotalStepCount) }
            LocalBroadcastManager.getInstance(context!!).sendBroadcast(intent)
        }
    }
}



