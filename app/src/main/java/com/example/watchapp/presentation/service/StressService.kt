package com.example.watchapp.presentation.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.watchapp.R
import com.example.watchapp.presentation.location.DefaultLocationClient
import com.example.watchapp.presentation.utils.Actions
import com.example.watchapp.presentation.utils.ActivityTransitionUtil
import com.example.watchapp.presentation.utils.audioRecordingLoop
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.util.UUID

class StressService() : Service(), SensorEventListener {
    val TAG = "HeartRateService"
    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var isRunning = false
    private var isPaused = false

    private lateinit var scope: CoroutineScope

    private var stressStreamManager: StressStreamManager? = null

    private val LOCATIONINTERVAL_MILLI = 10_000L // 10 sec because


    //lateinit var notificationManager: NotificationManager

    // a client is binding to the service with bindService()
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "Service started, onStartCommand was triggered.")

        //repo = HealthServicesRepository(this)
        handleIntentAction(intent)

        return START_STICKY
    }

    private fun handleIntentAction(intent: Intent?) {
        when (intent?.action) {
            Actions.START.toString() -> {
                if (isRunning) {
                    Log.d(TAG, "START action received but service was already running.")
                    return
                }

                isRunning = true
                setup()
                start()
                Log.d(
                    TAG,
                    "START action received and service was not running. Service started."
                )
            }

            Actions.STOP.toString() -> {
                if(isRunning) stop() // Error if stop() is called but no service is running
                Log.d(TAG, "STOP action received. Service stopped.")
            }

            Actions.TRANSITION.toString() -> {
                val transition = intent.getStringExtra("transitionType")

                Log.d(TAG, "handleIntentAction: STILL ${intent.getStringExtra("transitionType")}")

                if (transition == "EXIT") { // EXIT
                    if (isPaused) return // Already paused - should not happen
                    isPaused = true
                    pause()
                    return
                }

                // ENTER
                if(!isPaused) return // Not paused but entering still mode - return
                isPaused = false
                unpause()
            }
        }
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

        startForeground(1, notification.build(), FOREGROUND_SERVICE_TYPE_MICROPHONE)
    }

    private fun pause() {
        sensorManager.unregisterListener(this)
        scope.cancel()
    }

    private fun unpause() {
        setup()
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_DETACH)
        pause()
        stopSelf()
    }

    private fun setup() {
        scope = CoroutineScope(SupervisorJob())
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if(!checkHeartRateSensorFound() && !checkAccelerometerSensorFound()) return; // If no HR sensor, do not start service

        // Setup stream manager
        stressStreamManager = StressStreamManager(this, scope)
        stressStreamManager!!.setSessionId(getSessionId())

        // Starting sensors
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        registerHrSensorListener()

        // Setup audio
        audioRecordingLoop(
            this,
            500L,
            10_000L,
            scope,
            stressStreamManager!!::addDecibelDataPoint
        )

        setupLocation()
    }

    private fun setupLocation() {
        val locationClient = DefaultLocationClient(
            this,
            LocationServices.getFusedLocationProviderClient(this)
        )

        locationClient
            .getLocationUpdates(LOCATIONINTERVAL_MILLI)
            .onEach { location ->
                val lat = location.latitude.toString()
                val lon = location.longitude.toString()
                stressStreamManager!!.setLocation(Pair(lat, lon))
                Log.d(TAG, "locationUpdate: $lat, $lon")

            }
            .launchIn(scope)
    }

    private fun registerHrSensorListener() {
        hrSensor?.also { hr ->
            sensorManager.registerListener(
                this,
                hr,
                100_000_000
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

        val hr = event.values[0]
        Log.d(TAG, "onSensorChanged_TYPE_HEART_RATE: hr ${hr}")
        stressStreamManager!!.addHrDatapoint(hr)

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
