package com.example.watchapp.presentation.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import androidx.compose.runtime.getValue
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
import androidx.work.WorkManager
import com.example.watchapp.presentation.repositories.SelfReportRepository
import com.example.watchapp.presentation.data.SelfReportContract
import com.example.watchapp.presentation.theme.WatchAppTheme
import com.example.watchapp.presentation.utils.getDecibel
import com.example.watchapp.presentation.viewModels.SelfReportViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.time.LocalDate
import java.util.UUID


class SelfReportActivity: ComponentActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var workManager: WorkManager
    val TAG = "StressfactorActivity"
    var hasClicked: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(!hasPermission()) finishReport(-2)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        workManager = WorkManager.getInstance(this)

        val repo = SelfReportRepository()

        //val dB = getDecibel(this, 500) // takes noise measurements when the activity is started
        // might need to do this on separate thread, now pauses ui thread for 500ms

        //Log.d(TAG, "dB: $dB")

        setContent {
            // Preparing ViewModel and repository
            val viewModel = viewModel<SelfReportViewModel>(factory = SelfReportViewModel.SelfReportViewModelFactory(repo))

            // Primitive Dependency Injection - to lazy to use Dagger
            viewModel.setLocationProvider(fusedLocationProviderClient)
            viewModel.setWorkManager(workManager)
            //viewModel.setDB(dB)
            //viewModel.setUUID(getSessionId())

            val severity by viewModel.severity.collectAsStateWithLifecycle()

            viewModel.returnMessage.observe(this) {
                // Success, Aborted, or Failed
                Log.d(TAG, "onReturnMsg: $it")
                finishReport(it)
            }

            WatchAppTheme {
                StressfactorApp(severity, viewModel::changeSeverity, viewModel::report, ::hasPermission, ::finishReport) {
                    val dB = getDecibel(
                        this,
                        500
                    ) // takes noise measurements when the activity is started
                    viewModel.setDB(dB)
                    viewModel.setUUID(getSessionId())

                }
            }
        }
    }

    /**
     * Finishes self report and sends return code back
     *
     * @param returnCode the code to identify if the report was successfull
     */
    private fun finishReport(returnCode: Int) {
        val result = Intent().putExtra(SelfReportContract.STRESSFACTOR, returnCode)
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    /**
     * Checks if app has permission to location and microphone
     *
     * @return boolean weather app has permission or not
     */
    private fun hasPermission(): Boolean { // Permissions should be checked in View
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * gets session id from shared prefrences, if not there it creates one
     *
     * @return UUID - sessionId
     */
    private fun getSessionId(): UUID {
        // Get existing sessionId and timestamp
        val sharedPreferences = getSharedPreferences("stressMap", 0)

        val id = sharedPreferences.getString("sessionId", null) ?: return createAndSaveUUID(sharedPreferences)
        val timestamp = sharedPreferences.getString("idTimestamp", null) ?: return createAndSaveUUID(sharedPreferences)

        // There was an existing id and timestamp in sharedPreferences
        val today = LocalDate.now()
        val todayString = "${today.dayOfYear}${today.month.value}${today.dayOfMonth}"

        if(timestamp == todayString) return UUID.fromString(id) // Same date - return existing UUID

        return createAndSaveUUID(sharedPreferences)
    }

    /**
     * Creates UUID and saves is to sharedprefrences
     *
     * @return UUID - sessionId
     */
    private fun createAndSaveUUID(sharedPreferences: SharedPreferences): UUID {
        // There was an existing id and timestamp in sharedPreferences
        val today = LocalDate.now()
        val todayString = "${today.dayOfYear}${today.month.value}${today.dayOfMonth}"

        // Create new UUID
        val newId = UUID.randomUUID()

        // Save to sharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("sessionId", newId.toString())
        editor.putString("idTimestamp", todayString)
        editor.apply()

        return newId
    }

}

@Preview(device = "id:wearos_large_round", showBackground = true, showSystemUi = true)
@Composable
fun StressfactorAppPreview() {
    fun example(): Boolean {
        return true
    }
    StressfactorApp(5, {}, {}, ::example, {}, {})
}

@Composable
fun StressfactorApp(severity: Int, onSeverityChange: (Int)->Unit, report: ()->Unit, hasPermission: ()->Boolean, finishReport: (Int) -> Unit, doNoiseReading: ()->Unit) {
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
                if(hasPermission()) {
                    if (severity != 0) doNoiseReading()
                    report()
                }
                else finishReport(-2) // No permission
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
        ) {
            Text("Report", modifier = Modifier.padding(5.dp))
        }
    }
}
