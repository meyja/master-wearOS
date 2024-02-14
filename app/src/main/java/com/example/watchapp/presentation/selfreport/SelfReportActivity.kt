package com.example.watchapp.presentation.selfreport

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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.StateFlow


class SelfReportActivity: ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val TAG = "StressfactorActivity"
    var hasClicked: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val repo = SelfReportRepository(this)

        setContent {
            //var severity: MutableState<Int> = remember { mutableIntStateOf(5) }
            val viewModel = viewModel<SelfReportViewModel>(factory = SelfReportViewModel.SelfReportViewModelFactory(repo))
            val severity by viewModel.severity.collectAsStateWithLifecycle()

            viewModel.returnMessage.observe(this) {// Success, Aborted, or Failed
                val result = Intent().putExtra(SelfReportContract.STRESSFACTOR, it)
                setResult(Activity.RESULT_OK, result)
                finish()
            }

            WatchAppTheme {
                StressfactorApp(severity, viewModel::changeSeverity, viewModel::report)
            }
        }
    }

    @Preview(device = "id:wearos_large_round", showBackground = true)
    @Composable
    fun StressfactorAppPreview() {
        var severity: MutableState<Int> = remember { mutableIntStateOf(5) }

        StressfactorApp(5, {}, {})
    }

    @Composable
    fun StressfactorApp(severity: Int, onSeverityChange: (Int)->Unit, report: ()->Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(text = severity.toString(), fontSize = 50.sp)
            InlineSlider(
                value = severity,
                onValueChange = { onSeverityChange(it)},
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
                    report()
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text("Report", modifier = Modifier.padding(5.dp))
            }
        }
    }

}
