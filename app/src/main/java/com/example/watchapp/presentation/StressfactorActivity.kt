package com.example.watchapp.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationRequest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.Text
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.data.SendDataWorker
import com.example.watchapp.presentation.theme.WatchAppTheme
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task


class StressfactorActivity: ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val TAG = "StressfactorActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        setContent {
            var severity: MutableState<Int> = remember { mutableIntStateOf(5) }

            WatchAppTheme {
                StressfactorApp(severity)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocationReq(): Task<Location>? {

        if (!hasPermission()) return null

        val cancelToken = CancellationTokenSource()
        val loc =
            fusedLocationProviderClient.getCurrentLocation(
                LocationRequest.QUALITY_HIGH_ACCURACY,
                cancelToken.token

        )

        return loc
    }

    private fun startWorker(dataPoint: String, lat: String, lon: String, timestamp: String) {
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

        WorkManager.getInstance(applicationContext).enqueue(oneTimeWork)

    }

    private fun hasPermission(): Boolean {
        return !(ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }

    fun handleResult(severity: Int) {

        //Log.d("StressfactorActivity", "doAnalysis: ${severity}, lat: ${loc.first}, lon: ${loc.second}")

        //startWorker(severity.toFloat(), loc.first, loc.second)
        val task = getCurrentLocationReq()

        if (task == null) {
            val result = Intent().putExtra(StressfactorContract.STRESSFACTOR, -1)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        task!!.addOnSuccessListener {
            Log.d("StressfactorActivity", "getCurrentLocation: ${it.toString()}")

            startWorker(severity.toString(), it.latitude.toString(), it.longitude.toString(), System.currentTimeMillis().toString())
            val result = Intent().putExtra(StressfactorContract.STRESSFACTOR, severity)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        task.addOnFailureListener {
            val result = Intent().putExtra(StressfactorContract.STRESSFACTOR, -1)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

        task.addOnCanceledListener {
            val result = Intent().putExtra(StressfactorContract.STRESSFACTOR, -1)
            setResult(Activity.RESULT_OK, result)
            finish()
        }

    }

    @Preview(device = "id:wearos_large_round", showBackground = true)
    @Composable
    fun StressfactorAppPreview() {
        var severity: MutableState<Int> = remember { mutableIntStateOf(5) }

        StressfactorApp(severity)
    }

    @Composable
    fun StressfactorApp(severity: MutableState<Int>) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(text = severity.value.toString(), fontSize = 50.sp)
            InlineSlider(
                value = severity.value,
                onValueChange = { severity.value = it},
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                valueProgression = 0..10,
                segmented = false,
                colors = InlineSliderDefaults.colors(selectedBarColor = Color.Red)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .width(100.dp),
                onClick = {
                    Log.d("MainActivity", "MonitoringApp: Self Report clicked")
                    handleResult(severity.value)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text("Report", modifier = Modifier.padding(5.dp))
            }
        }
    }

}
