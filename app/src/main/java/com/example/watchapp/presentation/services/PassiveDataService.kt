package com.example.watchapp.presentation.services

import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import com.example.passivedatacompose.data.latestHeartRate
import com.example.watchapp.presentation.repositories.PassiveDataRepository
import kotlinx.coroutines.runBlocking

class PassiveDataService: PassiveListenerService() {
    private val repository = PassiveDataRepository(this)

    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
        //Do something with the databpoints
        runBlocking {
            dataPoints.getData(DataType.HEART_RATE_BPM).latestHeartRate()?.let {
                repository.storeLatestHeartRate(it)
            }
        }
    }
}