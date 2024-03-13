package com.example.watchapp.presentation.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.data.DataPoint
import com.example.watchapp.presentation.data.SendDataWorker
import com.example.watchapp.presentation.location.DefaultLocationClient
import com.example.watchapp.presentation.location.LocationClient
import com.example.watchapp.presentation.utils.getDecibel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class StressStreamManager(val context: Context, val scope: CoroutineScope) {

    private val TAG = "StressStreamManager"

    private val WINDOW_MILLI = 25_000 // 9 sec only for easy debug
    private var windowStart: Long = 0

    private val LOCATIONINTERVAL_MILLI = 10_000L // 10 sec because
    private var lastLocation: Pair<String, String>? = null

    private var heartrateData = ArrayList<DataPoint>()
    private var decibelData = ArrayList<Double>()

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationClient: LocationClient

    private lateinit var sessionId: UUID

    init {
        locationClient = DefaultLocationClient(
            context,
            fusedLocationProviderClient
        )

        locationClient
            .getLocationUpdates(LOCATIONINTERVAL_MILLI)
            .onEach { location ->
                val lat = location.latitude.toString()
                val lon = location.longitude.toString()
                lastLocation = Pair(lat, lon)
                Log.d(TAG, "locationUpdate: ${lastLocation.toString()}")

            }
            .launchIn(scope)
    }


    //val c = context

    /*init {
        Log.d(TAG, "context: ${c.toString()} isnull: ${c.equals(null)}")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(c)
    }*/



    /**
     * Adds a new heart rate data point to the window
     *
     * @param hr The heart rate value.
     */
    fun addHrDatapoint(hr: Float) {
        if (hr == 0.0f) return // Throws 0 HR

        val timestamp = System.currentTimeMillis()
        val dataPoint = DataPoint(hr, timestamp)

        if (windowStart.equals(0.toLong())) windowStart = timestamp
        else if (timestamp - windowStart > WINDOW_MILLI) { // Outside of window
            doAnalysis() // Check if stressed

            // Reset window
            windowStart = timestamp
            heartrateData = ArrayList()
            decibelData = ArrayList()
        }

        heartrateData.add(dataPoint)


    }

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
            var sum: Float = 0.0F
            for (e in heartRateDataCopy) {
                sum += e.hr
                //Log.d(TAG, e.hr.toString())
            }

            var sumDB = 0.0
            for( e in decibelDataCopy) {
                sumDB += e
            }

            val avg = sum / heartRateDataCopy.size
            val severity = ((avg%10)+1).toString()
            val timestamp = System.currentTimeMillis()

            val loc = lastLocation ?: return@launch

            Log.d(TAG, "doAnalysis: ${(avg%10)+1}, lat: ${loc.first}, lon: ${loc.second}, timestamp: ${timestamp.toString()}, dB: ${sumDB}")

            startWorker(severity, loc.first, loc.second, timestamp.toString())
            }

    }


    /**
     * Starts a worker to do a network request, sends lat, lon, and avg.
     * Network must be available for worker to start
     */
    private fun startWorker(dataPoint: String, lat: String, lon: String, timestamp: String) {
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", lat)
        data.putString("lon", lon)
        data.putString("dataPoint", dataPoint)
        data.putString("timestamp", timestamp)
        data.putString("sessionId", sessionId.toString())

        // Creating Worker
        val builder: Constraints.Builder = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)

        val oneTimeWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
            .addTag("SendData")
            .setInputData(data.build())
            .setConstraints(builder.build())
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWork)

    }

    fun setSessionId(sessionId: UUID) {
        Log.d(TAG, "setSessionId: $sessionId")
        this.sessionId = sessionId
    }

    fun close() {
        scope.cancel()
    }

}