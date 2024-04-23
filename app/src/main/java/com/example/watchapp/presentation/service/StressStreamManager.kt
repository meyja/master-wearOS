package com.example.watchapp.presentation.service

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.data.SendDataWorker
import com.example.watchapp.presentation.models.Stressdata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class StressStreamManager(
    val context: Context,
    val scope: CoroutineScope,
    val countDownTimer: CountDownTimer,
    val pause: () -> Unit) {

    private val TAG = "StressStreamManager"

    private val WINDOW_MILLI = 120_000
    private var windowStart: Long = 0
    private var MIN_HR_FOR_STRESS = 90

    private var lastLocation: Pair<String, String>? = null

    private var heartrateData = ArrayList<Float>()
    private var decibelData = ArrayList<Double>()

    private lateinit var sessionId: UUID

    /**
     * Adds a new heart rate data point to the window
     *
     * @param hr The heart rate value.
     */
    fun addHrDatapoint(hr: Float) {
        if (hr == 0.0f) return // Throws 0 HR

        val timestamp = System.currentTimeMillis()

        if (windowStart.equals(0.toLong())) windowStart = timestamp
        else if (timestamp - windowStart > WINDOW_MILLI) { // Outside of window
            doAnalysis() // Check if stressed

            // Reset window
            windowStart = timestamp
            heartrateData = ArrayList()
            decibelData = ArrayList()
        }
        heartrateData.add(hr)


    }

    /**
     * Adds a new decibel data point to the window
     *
     * @param dB The decibel value.
     */
    fun addDecibelDataPoint(dB: Double) {
        Log.d(TAG, "addDecibelDataPoint: $dB")

        if (dB == 0.0) return

        decibelData.add(dB)
    }

    /**
     * Checks if data says someone is stressed, in a separate thread
     */
    private fun doAnalysis() {
        val heartRateDataCopy = heartrateData.toList() // Need to be separate copy if analysis takes awhile
        val decibelDataCopy = decibelData.toList()

        scope.launch {// Separate thread
            val sumHr: Float = heartRateDataCopy.sum()
            val avgHr = heartRateDataCopy.average()

            val sumDB = decibelDataCopy.sum()
            val avgDB = decibelDataCopy.average()

            val timestamp = System.currentTimeMillis()

            val loc = lastLocation ?: return@launch // No location
            if (avgHr < MIN_HR_FOR_STRESS || decibelDataCopy.isEmpty()) return@launch // Not high enough heart rate, no decibeldata

            Log.d(TAG, "doAnalysis: ${avgHr.toString()}, lat: ${loc.first}, lon: ${loc.second}, timestamp: ${timestamp.toString()}, dB: ${avgDB}")

            val stressData = Stressdata(
                loc.first,
                loc.second,
                avgHr.toString(),
                timestamp.toString(),
                sessionId.toString(),
                avgDB.toString())


            startWorker(stressData)
            countDownTimer.start()
            pause()
        }

    }


    /**
     * Starts a worker to do a network request, sends lat, lon, and avg.
     * Network must be available for worker to start
     */
    private fun startWorker(stressData: Stressdata) {
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", stressData.lat)
        data.putString("lon", stressData.lon)
        data.putString("stressValue", stressData.stressValue)
        data.putString("timestamp", stressData.timestamp)
        data.putString("sessionId", stressData.sessionId)
        data.putString("decibel", stressData.decibel)

        // Creating Worker
        val builder: Constraints.Builder = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)

        val oneTimeWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
            .addTag("SendData")
            .setInputData(data.build())
            .setConstraints(builder.build())
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                10,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWork)

    }

    /**
     * Sets sessionid for the sreammanager
     *
     * @param sessionId the ID
     */
    fun setSessionId(sessionId: UUID) {
        Log.d(TAG, "setSessionId: $sessionId")
        this.sessionId = sessionId
    }

    /**
     * Sets location for the streammanager
     *
     * @param loc the location
     */
    fun setLocation(loc: Pair<String, String>) {
        lastLocation = loc

    }
}