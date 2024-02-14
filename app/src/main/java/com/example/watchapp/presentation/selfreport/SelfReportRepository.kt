package com.example.watchapp.presentation.selfreport

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.data.SendDataWorker
import com.example.watchapp.presentation.utils.getCurrentLocationNonBlocking
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class SelfReportRepository() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var workManager: WorkManager
    fun report(callback: (Pair<String, String>?) -> Unit) {
        getCurrentLocationNonBlocking(fusedLocationProviderClient, callback)
    }

    fun startWorker(dataPoint: String, lat: String, lon: String) {
        val timestamp = System.currentTimeMillis().toString()
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", lat)
        data.putString("lon", lon)
        data.putString("dataPoint", dataPoint)
        data.putString("timestamp", timestamp)

        // Creating Worker
        val builder: Constraints.Builder = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)

        val oneTimeWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
            .addTag("SendData")
            .setInputData(data.build())
            .setConstraints(builder.build())
            .build()

        workManager.enqueue(oneTimeWork)
    }

    fun setLocationProvider(locationProviderClient: FusedLocationProviderClient) {
        fusedLocationProviderClient = locationProviderClient
    }

    fun setWorkManager(workManager: WorkManager) {
        this.workManager = workManager
    }
}