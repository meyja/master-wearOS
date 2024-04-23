package com.example.watchapp.presentation.utils

import android.annotation.SuppressLint
import android.location.LocationRequest
import android.util.Log
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
        try {
            callback(Pair(it.latitude.toString(), it.longitude.toString()))
        } catch (e: Exception) {
            callback(null)
        }

    }

    loc.addOnFailureListener {
        callback(null)
    }

    loc.addOnCanceledListener {
        callback(null)
    }

}