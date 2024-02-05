package com.example.watchapp.presentation

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.example.watchapp.BuildConfig
import com.example.watchapp.presentation.data.MainRepository
import com.example.watchapp.presentation.theme.WatchAppTheme


class MainActivity : ComponentActivity() {
    private lateinit var receiver: DetectedActivityReceiver
    private lateinit var stressfactorLauncher: ActivityResultLauncher<Unit>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiver = DetectedActivityReceiver()
        // Create the intent filter corresponding to the action string.
        val intentFilter = IntentFilter(BuildConfig.APPLICATION_ID + ".DetectedActivityReceiver")

        // Register the receiver to listen for the broadcast.
        registerReceiver(receiver, intentFilter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BODY_SENSORS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ),
                0
            )
        }

        askPermissionForBackgroundUsage()

        // set wake lock to keep CPU awake
        //acquireWakeLock()



        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        val repo = MainRepository(activityManager, this)

        stressfactorLauncher = registerForActivityResult(StressfactorContract()) {
            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
        }

        setContent {
            val viewModel = viewModel<MainViewModel>(factory = MainViewModel.MainViewModelFactory(repo))
            val runningState by viewModel.isRunningState.collectAsStateWithLifecycle()

            WatchAppTheme {
                MonitoringApp(
                    running = runningState,
                    onStart = viewModel::startService,
                    onStop = viewModel::stopService)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver to prevent memory leaks
        unregisterReceiver(receiver)
    }

    /**
     * [MonitoringAppPreview] is used to preview [MonitoringApp] with mutable states.
     * It shows the state changes on the UI caused by "Start" and "Stop" buttons.
     */
    @Preview(showBackground = true)
    @Composable
    fun MonitoringAppPreview() {
        // Initialize running state to false
        val running = remember { mutableStateOf(false) }

        MonitoringApp(true, {}, {})
    }

    /**
     * Composable function [MonitoringApp] creates UI consisting of two buttons and a TextField.
     * It uses the passed [running] mutable state to start and stop services in actual application,
     * and to reflect the state change in TextView in the preview.
     *
     * @param running mutable state to control the start and stop of services
     *
     */
    @Composable
    fun MonitoringApp(running: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextFieldWithConditionalText(enabled = running)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                onStart()

            }) {
                Text("Start", modifier = Modifier.padding(5.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(modifier = Modifier.padding(horizontal = 16.dp), onClick = {
                onStop()
            }) {
                Text("Stop", modifier = Modifier.padding(5.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(modifier = Modifier.padding(horizontal = 16.dp).width(100.dp), onClick = {
                Log.d("MainActivity", "MonitoringApp: Self Report clicked")
                // startActivityForResult(Intent("test"), MainActivity)
                stressfactorLauncher.launch()

            }) {
                Text("Self Report", modifier = Modifier.padding(5.dp))
            }
        }
    }

    @Composable
    fun TextFieldWithConditionalText(enabled: Boolean) {

        val textB = when (enabled) {
            true -> "Enabled"
            false -> "Disabled"
        }

        Text(text = textB)
    }


    private fun acquireWakeLock() {
        //This code holds the CPU
        val gfgPowerDraw = getSystemService(POWER_SERVICE) as PowerManager
        val gfgPowerLatch = gfgPowerDraw.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "HeartRateApp::AchieveWakeLock"
        )
        gfgPowerLatch.acquire(20 * 60 * 1000L) // 20 minutes
    }

    private fun askPermissionForBackgroundUsage() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permission Needed!")
                .setMessage("Background Location Permission Needed!, tap \"Allow all time in the next screen\"")
                .setPositiveButton(
                    "OK"
                ) { dialog, which ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf<String>(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        0
                    )
                }
                .setNegativeButton(
                    "CANCEL"
                ) { dialog, which ->
                    // User declined for Background Location Permission.
                }
                .create().show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                0
            )
        }
    }
}

