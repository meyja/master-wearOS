package com.example.watchapp.presentation

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class HeartRateApp: Application() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "heartRate_channel",
            "Heart Rate",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}