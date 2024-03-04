package com.example.watchapp.presentation.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.watchapp.R
import com.example.watchapp.presentation.data.HealthServicesRepository
import com.example.watchapp.presentation.data.HeartRateStreamManager
import com.example.watchapp.presentation.data.MeasureMessage
import com.example.watchapp.presentation.utils.Actions
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class HeartRateService() : Service(), SensorEventListener {
    val TAG = "HeartRateService"
    private lateinit var repo: HealthServicesRepository
    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var isRunning = false

    private val scope = CoroutineScope(SupervisorJob())

    private lateinit var hrManager: HeartRateStreamManager

    //lateinit var notificationManager: NotificationManager

    // a client is binding to the service with bindService()
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service started, onStartCommand was triggered.")

        //repo = HealthServicesRepository(this)
        handleIntentAction(intent?.action)

        return START_STICKY
    }

    private fun start() {
        Log.d(TAG, "start function called.")

        val notification = NotificationCompat.Builder(this, "heartRate_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Monitoring heart rate")
            .setContentText("HR: 00")
            .setOnlyAlertOnce(true)
            .setPriority(-2)
        Log.d(TAG, "Notification built.")

        //notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        hrManager = HeartRateStreamManager(this)
        hrManager.setSessionId(getSessionId())


        startForeground(1, notification.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        /*
        scope.launch {
            Log.d(TAG, "Starting coroutine for heartRateMeasureFlow.")
            repo.heartRateMeasureFlow()
                .collect {
                    when (it) {
                        is MeasureMessage.MeasureData -> {
                            val hr = it.data.last().value
                            Log.d(TAG, "Collected new heart rate measurement: $hr.")
                            val updatedNotification = notification.setContentText("HR: $hr")
                            notificationManager.notify(1, updatedNotification.build())
                            Log.d(TAG, "Notification updated with new heart rate.")
                        }

                        is MeasureMessage.MeasureAvailability -> {
                            Log.d(TAG, "MeasureAvailability state change.")
                        }

                    }
                }
        }

        scope.launch {
            Log.d(TAG, "Starting heartbeat logger coroutine.")
            (0..1000).forEach {
                Log.d(TAG, "Heartbeat log: $it.")
                delay(5000)
            }
        }*/
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_DETACH)
        //scope.cancel()
        //notificationManager.cancel(1) // removes notification when service is destroyed
        sensorManager.unregisterListener(this)
        stopSelf()
    }

    private fun handleIntentAction(action: String?) {
        when (action) {
            Actions.START.toString() -> {
                if (!isRunning) {
                    isRunning = true

                    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

                    if(!checkHeartRateSensorFound() && !checkAccelerometerSensorFound()) return; // If no HR sensor, do not start service

                    hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                    accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

                    registerHrSensorListener()
                    //registerAccelerometerSensorListener()
                    start()
                    Log.d(
                        TAG,
                        "START action received and service was not running. Service started."
                    )
                } else {
                    Log.d(TAG, "START action received but service was already running.")
                }
            }

            Actions.STOP.toString() -> {
                if(isRunning) stop() // Error if stop() is called but no service is running
                Log.d(TAG, "STOP action received. Service stopped.")
            }
            
            Actions.TRANSITION.toString() -> {
                Log.d(TAG, "handleIntentAction: ")
            }
        }
    }


    private fun registerHrSensorListener() {
        hrSensor?.also { hr ->
            sensorManager.registerListener(
                this,
                hr,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Log.d(TAG, "Sensor Manager registered HR sensor listener.")
        } ?: run {
            Log.d(TAG, "hrSensor is null, sensor listener couldn't be registered.")
        }
    }

    private fun registerAccelerometerSensorListener() {
        accelerometerSensor?.also { acc ->
            sensorManager.registerListener(
                this,
                acc,
                100_000_000_0// microseconds, there is an upperlimit
            )
            Log.d(TAG, "Sensor Manager registered Accelerometer sensor listener.")
        } ?: run {
            Log.d(TAG, "accelerometerSensor is null, sensor listener couldn't be registered.")
        }
    }

    /**
     * https://developer.android.com/develop/sensors-and-location/location/transitions
     */

    private fun checkHeartRateSensorFound(): Boolean {
        return if (sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
            Log.d(TAG, "onCreate: Success! Heart rate sensor found")
            true
        } else {
            // Failure! No magnetometer.
            Log.d(TAG, "onCreate: Error, no heart rate sensor found.")
            false
        }
    }

    private fun checkAccelerometerSensorFound(): Boolean {
        return if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Log.d(TAG, "onCreate: Success! Accelerometer sensor found")
            true
        } else {
            Log.d(TAG, "onCreate: Error, no Accelerometer sensor found.")
            false
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        //Log.d(TAG, "onSensorChanged: ${event.toString()}")
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {

            /**
             * All values are in SI units (m/s^2)
             * values[0]: Acceleration minus Gx on the x-axis
             * values[1]: Acceleration minus Gy on the y-axis
             * values[2]: Acceleration minus Gz on the z-axis
             *
             * https://developer.android.com/reference/android/hardware/SensorEvent
             */
            Log.d(TAG, "onSensorChanged_TYPE_ACCELEROMETER: ${event.values[0]} ${event.values[1]} ${event.values[2]}")
        }
        else {
            val hr = event.values[0]
            Log.d(TAG, "onSensorChanged_TYPE_HEART_RATE: hr ${hr}")
            hrManager.addHrDatapoint(hr)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onSensorChanged: sensor ${sensor.toString()}")
    }

    private fun getSessionId(): UUID {
        // Get existing sessionId and timestamp
        val sharedPreferences = getSharedPreferences("stressMap", 0)

        val id = sharedPreferences.getString("sessionId", null) ?: return createAndSaveUUID(sharedPreferences)
        val timestamp = sharedPreferences.getString("idTimestamp", null) ?: return createAndSaveUUID(sharedPreferences)

        // There was an existing id and timestamp in sharedPreferences
        val today = LocalDate.now()
        val todayString = "${today.dayOfYear}${today.month.value}${today.dayOfMonth}"

        if(timestamp == todayString) return UUID.fromString(id) // Same date - return existing UUID

        return createAndSaveUUID(sharedPreferences)
    }

    private fun createAndSaveUUID(sharedPreferences: SharedPreferences): UUID {

        // There was an existing id and timestamp in sharedPreferences
        val today = LocalDate.now()
        val todayString = "${today.dayOfYear}${today.month.value}${today.dayOfMonth}"

        // Create new UUID
        val newId = UUID.randomUUID()

        // Save to sharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("sessionId", newId.toString())
        editor.putString("idTimestamp", todayString)
        editor.apply()

        return newId
    }
}
