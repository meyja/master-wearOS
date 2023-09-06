package com.example.watchapp.presentation.repositories

import android.content.ContentValues.TAG
import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.data.PassiveListenerConfig
import com.example.watchapp.presentation.services.PassiveDataService

class HealthServiceRepository(context: Context) {
    private val healthServiceClient = HealthServices.getClient(context)
    private val passiveMonitoringClient = healthServiceClient.passiveMonitoringClient
    private val dataTypes = setOf(DataType.HEART_RATE_BPM)

    // determines which data type to receive
    private val passiveListenerConfig = PassiveListenerConfig(
        dataTypes = dataTypes,
        shouldUserActivityInfoBeRequested = false,
        dailyGoals = setOf(),
        healthEventTypes = setOf()
    )

    suspend fun hasHeartRateCapability(): Boolean {
        val capabilities = passiveMonitoringClient.getCapabilitiesAsync().await()
        return DataType.HEART_RATE_BPM in capabilities.supportedDataTypesPassiveMonitoring
    }

    suspend fun registerForHeartRateData(){
        Log.i(TAG, "Registering listener")
        passiveMonitoringClient.setPassiveListenerServiceAsync(
            PassiveDataService::class.java,
            passiveListenerConfig
        ).await()
    }
}