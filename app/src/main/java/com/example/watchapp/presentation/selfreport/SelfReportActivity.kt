package com.example.watchapp.presentation.selfreport

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationRequest
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
import com.example.watchapp.presentation.utils.getDecibel
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.StateFlow


class SelfReportActivity: ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var workManager: WorkManager
    val TAG = "StressfactorActivity"
    var hasClicked: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        workManager = WorkManager.getInstance(this)

        val repo = SelfReportRepository()

        val dB = getDecibel(this, 500) // takes noise measurements when the activity is started
        // might need to do this on separate thread, now pauses ui thread for 500ms

        Log.d(TAG, "dB: $dB")

        setContent {
            // Preparing ViewModel and repository
            val viewModel = viewModel<SelfReportViewModel>(factory = SelfReportViewModel.SelfReportViewModelFactory(repo))

            // Primitive Dependency Injection - to lazy to use Dagger
            viewModel.setLocationProvider(fusedLocationProviderClient)
            viewModel.setWorkManager(workManager)
            viewModel.setDB(dB)

            val severity by viewModel.severity.collectAsStateWithLifecycle()

            viewModel.returnMessage.observe(this) {
                // Success, Aborted, or Failed
                finishReport(it)
            }

            WatchAppTheme {
                StressfactorApp(severity, viewModel::changeSeverity, viewModel::report, ::hasPermission, ::finishReport)
            }
        }
    }

    private fun finishReport(returnCode: Int) {
        val result = Intent().putExtra(SelfReportContract.STRESSFACTOR, returnCode)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun hasPermission(): Boolean { // Permissions should be checked in View
        return !(ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
    }

}

@Preview(device = "id:wearos_large_round", showBackground = true, showSystemUi = true)
@Composable
fun StressfactorAppPreview() {
    fun example(): Boolean {
        return true
    }
    StressfactorApp(5, {}, {}, ::example, {})
}

@Composable
fun StressfactorApp(severity: Int, onSeverityChange: (Int)->Unit, report: ()->Unit, hasPermission: ()->Boolean, finishReport: (Int) -> Unit) {
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
                if(hasPermission()) report()
                else finishReport(2) // No permission
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
        ) {
            Text("Report", modifier = Modifier.padding(5.dp))
        }
    }
}
