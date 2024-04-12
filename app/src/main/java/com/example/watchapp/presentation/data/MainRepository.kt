package com.example.watchapp.presentation.data

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.watchapp.presentation.service.StressService
import com.example.watchapp.presentation.utils.Actions
import com.example.watchapp.presentation.utils.ActivityTransitionUtil
import com.google.android.gms.location.ActivityRecognition

/**
 * MainRepository class responsible for managing services and interacting with the Android system.
 *
 * @property applicationContext The [Context] used to access application-specific resources and components.
 */
class MainRepository(
    private val applicationContext: Context,
) {

    private val client = ActivityRecognition.getClient(applicationContext)

    //private var isRunning = false


    /**
     * Checks if a service with the specified class name is currently running.
     *
     * @param serviceName The fully qualified class name of the service to check.
     * @return `true` if the service is running, `false` otherwise.
     */
    fun isServiceRunning(serviceName: String): Boolean {
        val activityManager = applicationContext.getSystemService(ComponentActivity.ACTIVITY_SERVICE) as ActivityManager
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
     * Checks if a service with the specified class name is currently running based on sharedprefrences.
     *
     * @return `true` if the service is running, `false` otherwise.
     */
    fun isServiceRunning(): Boolean {
        val sharedPreferences = applicationContext.getSharedPreferences("stressMap", 0)
        Log.d("REPO", "isServiceRunning: sharedPreferences.getBoolean(\"isRunning\", false)")
        return sharedPreferences.getBoolean("isRunning", false)
    }

    /**
     * Starts the HeartRateService.
     * This method sends a start command to the HeartRateService using an Intent.
     */
    @SuppressLint("MissingPermission")
    fun startService() {
        if (isServiceRunning()) return

        Intent(applicationContext, StressService::class.java).also {
            it.action = Actions.START.toString()
            applicationContext.startService(it)
            applicationContext.getSharedPreferences("stressMap", 0).edit().putBoolean("isRunning", true).apply()
        }
        client
            .requestActivityTransitionUpdates(
                ActivityTransitionUtil.getActivityTransitionRequest(),
                ActivityTransitionUtil.createPendingIntent(applicationContext)
            )
            .addOnSuccessListener {
                Log.d("MainRepo", "requestActivityUpdates: Success - Request Updated")
            }
            .addOnFailureListener {
                Log.d("MainRepo", "requestActivityUpdates: Failure - Request Updated")
            }

    }

    /**
     * Stops the HeartRateService.
     * This method sends a stop command to the HeartRateService using an Intent.
     */
    @SuppressLint("MissingPermission")
    fun stopService() {
        Log.d("HeartRateService", "stopService1: ")
        if (!isServiceRunning()) return
        Log.d("HeartRateService", "stopService2: ")

        Intent(applicationContext, StressService::class.java).also {
            it.action = Actions.STOP.toString()
            applicationContext.startService(it)
            applicationContext.getSharedPreferences("stressMap", 0).edit().putBoolean("isRunning", false).apply()
        }

        client.removeActivityTransitionUpdates(ActivityTransitionUtil.createPendingIntent(applicationContext))
            .addOnSuccessListener {
                Log.d("MainRepo", "removeActivityUpdates: Successful unregistered")
            }
            .addOnFailureListener {
                Log.d("MainRepo", "removeActivityUpdates: Unsuccessful unregistered")
            }

    }
}