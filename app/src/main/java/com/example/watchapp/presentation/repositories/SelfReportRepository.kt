package com.example.watchapp.presentation.repositories

import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.data.SendDataWorker
import com.example.watchapp.presentation.utils.getCurrentLocationNonBlocking
import com.google.android.gms.location.FusedLocationProviderClient
import java.util.UUID
import java.util.concurrent.TimeUnit

class SelfReportRepository() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var workManager: WorkManager
    private var dB: Double = 0.0
    private lateinit var uuid: UUID
    fun report(callback: (Pair<String, String>?) -> Unit) {
        getCurrentLocationNonBlocking(fusedLocationProviderClient, callback)
    }

    fun startWorker(stressValue: String, lat: String, lon: String) {
        val timestamp = System.currentTimeMillis().toString()
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", lat)
        data.putString("lon", lon)
        data.putString("stressValue", stressValue)
        data.putString("timestamp", timestamp.toString())
        data.putString("sessionId", uuid.toString())
        data.putString("decibel", dB.toString())

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

        workManager.enqueue(oneTimeWork)
    }

    fun setLocationProvider(locationProviderClient: FusedLocationProviderClient) {
        fusedLocationProviderClient = locationProviderClient
    }

    fun setWorkManager(workManager: WorkManager) {
        this.workManager = workManager
    }

    fun setDB(dB: Double) {
        this.dB = dB
    }

    fun setUUID(uuid: UUID) {
        Log.d("REPO", "setUUID: $uuid")
        this.uuid = uuid
    }
}