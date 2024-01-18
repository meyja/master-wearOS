package com.example.watchapp.presentation.data

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.watchapp.BuildConfig
import com.example.watchapp.presentation.DetectedActivityReceiver
import com.example.watchapp.presentation.service.HeartRateService
import com.example.watchapp.presentation.utils.Actions
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * MainRepository class responsible for managing services and interacting with the Android system.
 *
 * @property activityManager The [ActivityManager] instance used for managing activities and services.
 * @property applicationContext The [Context] used to access application-specific resources and components.
 */
class MainRepository(val activityManager: ActivityManager, val applicationContext: Context) {


    /**
     * Checks if a service with the specified class name is currently running.
     *
     * @param serviceName The fully qualified class name of the service to check.
     * @return `true` if the service is running, `false` otherwise.
     */
    fun isServiceRunning(serviceName: String) : Boolean {
        val runningServices: List<ActivityManager.RunningServiceInfo> =
            activityManager.getRunningServices(Int.MAX_VALUE) // this is deprecated, might have to use sharedPreferences

        for (serviceInfo in runningServices) {
            Log.d("isServiceRunning", serviceInfo.service.className)
            if (serviceInfo.service.className == serviceName) {
                return true
            }
        }

        return false
    }

    /**
     * Starts the HeartRateService.
     * This method sends a start command to the HeartRateService using an Intent.
     */
    fun startService() {
        Intent(applicationContext, HeartRateService::class.java).also {
            it.action = Actions.START.toString()
            applicationContext.startService(it)
        }
        RegisterAtivityTransition()
    }

    /**
     * Stops the HeartRateService.
     * This method sends a stop command to the HeartRateService using an Intent.
     */
    fun stopService() {
        Intent(applicationContext, HeartRateService::class.java).also {
            it.action = Actions.STOP.toString()
            applicationContext.startService(it)
        }
    }

    /**
     * https://heartbeat.comet.ml/detect-users-activity-in-android-using-activity-transition-api-f718c844efb2
     */
    @SuppressLint("MissingPermission")
    private fun RegisterAtivityTransition() {
        val transitions = mutableListOf<ActivityTransition>()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_BICYCLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
            .build()

        transitions += ActivityTransition.Builder()
            .setActivityType(DetectedActivity.ON_BICYCLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
            .build()

        val request = ActivityTransitionRequest(transitions)

        val intent = Intent(BuildConfig.APPLICATION_ID + ".DetectedActivityReceiver")
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE);
        val receiver = DetectedActivityReceiver()

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(
            receiver, IntentFilter(BuildConfig.APPLICATION_ID + ".DetectedActivityReceiver")
        )

        val task = ActivityRecognition.getClient(applicationContext)
            .requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            Log.d("ActivityRecognition", "Transitions Api registered with success")
        }

        task.addOnFailureListener { e: Exception ->
            Log.d("ActivityRecognition", "Transitions Api could NOT be registered ${e.localizedMessage}")
        }


    }
}