package com.example.watchapp.presentation.service

import android.app.Service
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.watchapp.R
import com.example.watchapp.presentation.data.HealthServicesRepository
import com.example.watchapp.presentation.data.MeasureMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeartRateService() : Service(), SensorEventListener {
    val TAG = "HeartRateService"
    private lateinit var repo: HealthServicesRepository
    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    private var isRunning = false

    private val scope = CoroutineScope(SupervisorJob())

    lateinit var notificationManager: NotificationManager

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

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager


        startForeground(1, notification.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        Log.d(TAG, "Started service in foreground.")

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
        notificationManager.cancel(1) // removes notification when service is destroyed
        sensorManager.unregisterListener(this)
        stopSelf()
    }

    private fun handleIntentAction(action: String?) {
        when (action) {
            Actions.START.toString() -> {
                if (!isRunning) {
                    isRunning = true

                    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

                    if(!checkHeartRateSensorFound()) return; // If no HR sensor, do not start service

                    hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                    registerHrSensorListener()
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
                stop()
                Log.d(TAG, "STOP action received. Service stopped.")
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
            Log.d(TAG, "Sensor Manager registered sensor listener.")
        } ?: run {
            Log.d(TAG, "hrSensor is null, sensor listener couldn't be registered.")
        }
    }

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

    override fun onSensorChanged(event: SensorEvent) {
        val hr = event.values[0]
        Log.d(TAG, "onSensorChanged: hr ${hr}")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d(TAG, "onSensorChanged: sensor ${sensor.toString()}")
    }

    enum class Actions {
        START, STOP
    }
}