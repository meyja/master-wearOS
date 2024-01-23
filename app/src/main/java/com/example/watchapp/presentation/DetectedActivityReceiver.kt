package com.example.watchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.watchapp.BuildConfig
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class DetectedActivityReceiver() : BroadcastReceiver() {

    val RECEIVER_ACTION = BuildConfig.APPLICATION_ID + ".DetectedActivityReceiver"



    override fun onReceive(context: Context, intent: Intent) {

        Log.d("DetectedActivityReceiver", "Received an action.")


        if (RECEIVER_ACTION == intent.action) {
            Log.d("DetectedActivityReceiver", "Received an unsupported action.")
            return
        }

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)!!
            for (event in result.transitionEvents) {
                val activity = activityType(event.activityType)
                val transition = transitionType(event.transitionType)
                val message = "Transition: $activity ($transition)"
                Log.d("DetectedActivityReceiver", "onReceive: Event -> $event")
                Log.d("DetectedActivityReceiver", message)
            }
        }
    }

    private fun transitionType(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }

    private fun activityType(activity: Int): String {
        return when (activity) {
            DetectedActivity.RUNNING -> "RUNNING"
            DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
            DetectedActivity.WALKING -> "WALKING"
            else -> "UNKNOWN"
        }
    }
}