package com.example.watchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.watchapp.presentation.service.StressService
import com.example.watchapp.presentation.utils.Actions
import com.example.watchapp.presentation.utils.ActivityTransitionUtil
import com.google.android.gms.location.ActivityTransitionResult

class DetectedActivityReceiver() : BroadcastReceiver() {
    val TAG = "DetectedActivityReceiver"

    override fun onReceive(context: Context, intent: Intent) {

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    val info =
                        "Transition: ${ActivityTransitionUtil.toActivityString(event.activityType)} - ${
                            ActivityTransitionUtil.toTransitionType(event.transitionType)
                        }"
                    Log.d(TAG, "onReceive: $info")
                    if (ActivityTransitionUtil.toActivityString(event.activityType) == "STILL") {
                        //Log.d(TAG, "onReceive: STILL")
                        //handleStillEvent(event, context)
                        val transitionType = ActivityTransitionUtil.toTransitionType(event.transitionType)
                        val newIntent = Intent(context, StressService::class.java)
                        newIntent.action = Actions.TRANSITION.toString()
                        newIntent.putExtra("transitionType", transitionType)
                        context.applicationContext.startService(newIntent)
                    }
                }
            }
        }
    }
}