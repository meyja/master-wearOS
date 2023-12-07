package com.example.watchapp.presentation.data

import android.Manifest
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper.getMainLooper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
     * Checks if data says someone is stressed, in a seperate thread
     */
    private fun doAnalysis() {
        val heartRateDataCopy = heartrateData.toList() // Need to be seperate copy if analysis takes awhile
        scope.launch {// Seperate thread
            var sum: Float = 0.0F
            for (e in heartRateDataCopy) {
                sum += e.hr
                Log.d(TAG, e.hr.toString())
            }
            val loc = getLastLocation()
            Log.d(TAG, "doAnalysis: loc:${loc.toString()}")
            if (loc != null) {
                Log.d(TAG, "doAnalysis: ${sum / heartRateDataCopy.size}, lat: ${loc.first}, lon: ${loc.second}")
            }
        }

    }

    private fun getLastLocation(): Pair<String, String>? {
        if (ActivityCompat.checkSelfPermission(
                c,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                c,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) { // not permission
            return null
        }

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
}