package com.example.watchapp.presentation.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.watchapp.presentation.service.HeartRateService
import com.example.watchapp.presentation.utils.Actions

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
}