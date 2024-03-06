package com.example.watchapp.presentation.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationRequest
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.watchapp.presentation.selfreport.SelfReportContract
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks

@SuppressLint("MissingPermission")
fun getCurrentLocationBlocking(fusedLocationProviderClient: FusedLocationProviderClient): Pair<String, String>? {


    val cancelToken = CancellationTokenSource()
    val loc = Tasks.await(
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.QUALITY_HIGH_ACCURACY,
            cancelToken.token
        )
    ) ?: return null

    Log.d("local", "getCurrentLocation: ${loc.toString()}")
    return Pair(loc.latitude.toString(), loc.longitude.toString())
}

@SuppressLint("MissingPermission")
fun getCurrentLocationNonBlocking(
    fusedLocationProviderClient: FusedLocationProviderClient,
    callback: (Pair<String, String>?) -> Unit) {


    val cancelToken = CancellationTokenSource()
    val loc =
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.QUALITY_HIGH_ACCURACY,
            cancelToken.token
        )

    loc.addOnSuccessListener {
        callback(Pair(it.latitude.toString(), it.longitude.toString()))
    }

    loc.addOnFailureListener {
        callback(null)
    }

    loc.addOnCanceledListener {
        callback(null)
    }

}