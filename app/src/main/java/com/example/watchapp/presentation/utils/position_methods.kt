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
fun getCurrentLocationBlocking(c: Context, fusedLocationProviderClient: FusedLocationProviderClient): Pair<String, String>? {

    if (!hasPermission(c)) return null

    val cancelToken = CancellationTokenSource()
    val loc = Tasks.await(
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.QUALITY_HIGH_ACCURACY,
            cancelToken.token
        )
    )

    //Log.d(TAG, "getCurrentLocation: ${loc.toString()}")
    return Pair(loc.latitude.toString(), loc.longitude.toString())
}

@SuppressLint("MissingPermission")
fun getCurrentLocationNonBlocking(
    c: Context,
    fusedLocationProviderClient: FusedLocationProviderClient,
    callback: (Pair<String, String>?) -> Unit) {

    if (!hasPermission(c)) {
        return callback(null)
    }

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

private fun hasPermission(c: Context): Boolean {
    return !(ActivityCompat.checkSelfPermission(
        c,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        c,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) != PackageManager.PERMISSION_GRANTED)
}