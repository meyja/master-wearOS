package com.example.watchapp.presentation.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.LocationRequest
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.utils.getCurrentLocationBlocking
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID


class HeartRateStreamManager(context: Context) {

    val TAG = "HeartRateStreamManager"
    val WINDOW_MILLI = 9_000 // 9 sec only for easy debug

    var heartrateData = ArrayList<DataPoint>()
    var windowStart: Long = 0

    private val scope = CoroutineScope(SupervisorJob())

    val c = context

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var sessionId: UUID


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
            heartrateData = ArrayList<DataPoint>()
        }

        heartrateData.add(dataPoint)


    }

    /**
     * Checks if data says someone is stressed, in a separate thread
     */
    private fun doAnalysis() {
        val heartRateDataCopy = heartrateData.toList() // Need to be separate copy if analysis takes awhile
        scope.launch {// Separate thread
            var sum: Float = 0.0F
            for (e in heartRateDataCopy) {
                sum += e.hr
                Log.d(TAG, e.hr.toString())
            }
            val loc = getCurrentLocationBlocking(fusedLocationProviderClient)
            Log.d(TAG, "doAnalysis: loc:${loc.toString()}")
            if (loc != null) {
                val avg = sum / heartRateDataCopy.size
                val severity = ((avg%10)+1).toString()
                val timestamp = System.currentTimeMillis()

                Log.d(TAG, "doAnalysis: ${(avg%10)+1}, lat: ${loc.first}, lon: ${loc.second}, timestamp: ${timestamp.toString()}")

                startWorker(severity, loc.first, loc.second, timestamp.toString())
            }
            else { // Only for debugging purposes
                startWorker("0", "0", "0", "00000")
            }
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

        WorkManager.getInstance(c).enqueue(oneTimeWork)

    }

    fun setSessionId(sessionId: UUID) {
        Log.d(TAG, "setSessionId: $sessionId")
        this.sessionId = sessionId
    }
}