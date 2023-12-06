package com.example.watchapp.presentation.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HeartRateStreamManager {

    val TAG = "HeartRateStreamManager"
    val WINDOW_MILLI = 9_000 // 9 sec only for easy debug

    var heartrateData = ArrayList<DataPoint>()
    var windowStart: Long = 0

    private val scope = CoroutineScope(SupervisorJob())

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
            Log.d(TAG, "doAnalysis: ${sum / heartRateDataCopy.size}")
        }

    }
}