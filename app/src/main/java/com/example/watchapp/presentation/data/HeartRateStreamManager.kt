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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


class HeartRateStreamManager(context: Context) {

    val TAG = "HeartRateStreamManager"
    val WINDOW_MILLI = 9_000 // 9 sec only for easy debug

    var heartrateData = ArrayList<DataPoint>()
    var windowStart: Long = 0

    private val scope = CoroutineScope(SupervisorJob())

    val c = context

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val LOCATION_PERMISSION_REQUEST_CODE = 1


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
            val loc = getCurrentLocation()
            Log.d(TAG, "doAnalysis: loc:${loc.toString()}")
            if (loc != null) {
                val avg = sum / heartRateDataCopy.size

                Log.d(TAG, "doAnalysis: ${avg}, lat: ${loc.first}, lon: ${loc.second}")

                startWorker(avg, loc.first, loc.second)
            }
            else { // Only for debugging purposes
                startWorker(0.1f, "0", "0")
            }
        }

    }

    /**
     * Retrieves the last known location of the device.
     *
     * @return A Pair of strings containing the latitude and longitude of the location, or null if the location could not be retrieved.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation(): Pair<String, String>? { // currently not used
        if (!hasPermission()) return null

        val lm = c.getSystemService(LOCATION_SERVICE) as LocationManager
        val location = lm.getLastKnownLocation(LocationManager.FUSED_PROVIDER)

        //lm.getCurrentLocation()

        if (location == null) {
            Log.d(TAG, "getLastLocation: null")
            return null
        }

        val lat = location.latitude.toString()
        val long = location.longitude.toString()

        return Pair(lat, long)

    }

    /**
     * Retrieves the current location of the device using the FusedLocationProviderClient.
     * Active retrieval.
     *
     * @return A Pair of strings containing the latitude and longitude of the current location, or null if the location could not be retrieved.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Pair<String, String>? {

        if (!hasPermission()) return null

        val cancelToken = CancellationTokenSource()
        val loc = await(fusedLocationProviderClient.getCurrentLocation(LocationRequest.QUALITY_HIGH_ACCURACY, cancelToken.token))

        Log.d(TAG, "getCurrentLocation: ${loc.toString()}")
        return Pair(loc.latitude.toString(), loc.longitude.toString())
    }


    /**
     * Starts a worker to do a network request, sends lat, lon, and avg.
     * Network must be available for worker to start
     */
    private fun startWorker(avg: Float, lat: String, lon: String) {
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", lat)
        data.putString("lon", lon)
        data.putFloat("avg", avg)

        // Creating Worker
        val builder: Constraints.Builder = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)

        val oneTimeWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
            .addTag("SendData")
            .setInputData(data.build())
            .setConstraints(builder.build())
            .build()

        WorkManager.getInstance(c).enqueue(oneTimeWork)

    }

    /**
     * Checking if the device has the correct permissions to do the location requests
     *
     * @return Boolean true if app has permission, false if not
     */
    private fun hasPermission(): Boolean {
        return !(ActivityCompat.checkSelfPermission(
            c,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            c,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }
}