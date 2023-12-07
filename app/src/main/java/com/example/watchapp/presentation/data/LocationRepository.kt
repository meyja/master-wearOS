package com.example.watchapp.presentation.data

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient

class LocationRepository(context: Context) {
    val c = context

    private lateinit var fusedLocationClient: FusedLocationProviderClient

}