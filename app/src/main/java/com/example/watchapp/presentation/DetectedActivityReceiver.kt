package com.example.watchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.watchapp.BuildConfig
import com.example.watchapp.presentation.service.HeartRateService
import com.example.watchapp.presentation.utils.Actions
import com.example.watchapp.presentation.utils.ActivityTransitionUtil
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class DetectedActivityReceiver() : BroadcastReceiver() {
    val TAG = "DetectedActivityReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: got one")

        Log.d(TAG, "    onReceive: Action: ${intent.action}");
        Log.d(TAG, "    onReceive: Extras: ${intent.extras}");

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.let {
                result.transitionEvents.forEach { event ->
                    // Do something useful here...

                    val info = "Transition: ${ActivityTransitionUtil.toActivityString(event.activityType)} - ${ActivityTransitionUtil.toTransitionType(event.transitionType)}"
                    if(ActivityTransitionUtil.toActivityString(event.activityType) == "STILL") {
                        Log.d(TAG, "onReceive: STILL")
                        val transitionType = ActivityTransitionUtil.toTransitionType(event.transitionType)
                        val newIntent = Intent(context, HeartRateService::class.java)
                        newIntent.action = Actions.TRANSITION.toString()
                        newIntent.putExtra("transitionType", transitionType)
                        context.applicationContext.startService(newIntent)

                    }
                    Log.d(TAG, "onReceive: $info")
                }
            }
        }
    }
}