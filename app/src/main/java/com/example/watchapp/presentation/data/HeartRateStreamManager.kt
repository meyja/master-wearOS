package com.example.watchapp.presentation.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.LocationRequest
import android.media.MediaRecorder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationRequestCompat.Quality
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.watchapp.presentation.location.DefaultLocationClient
import com.example.watchapp.presentation.location.LocationClient
import com.example.watchapp.presentation.utils.getCurrentLocationBlocking
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID


class HeartRateStreamManager(context: Context) {

    val TAG = "HeartRateStreamManager"
    val WINDOW_MILLI = 9_000 // 9 sec only for easy debug

    val LOCATIONINTERVAL_MILLI = 10_000L // 10 sec because

    var heartrateData = ArrayList<DataPoint>()
    var windowStart: Long = 0

    var lastLocation: Pair<String, String>? = null


    private val scope = CoroutineScope(SupervisorJob())

    val c = context

    private var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationClient: LocationClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private lateinit var sessionId: UUID

    init {
        locationClient = DefaultLocationClient(
            context,
            fusedLocationProviderClient
        )

        locationClient
            .getLocationUpdates(LOCATIONINTERVAL_MILLI)
            .onEach { location ->
                val lat = location.latitude.toString()
                val lon = location.longitude.toString()
                lastLocation = Pair(lat, lon)
                Log.d(TAG, "locationUpdate: ${lastLocation.toString()}")

            }
            .launchIn(scope)
    }


    //val c = context

    /*init {
        Log.d(TAG, "context: ${c.toString()} isnull: ${c.equals(null)}")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(c)
    }*/



    /**
     * Adds a new heart rate data point to the window
     *
     * @param hr The heart rate value.
     */
    fun addHrDatapoint(hr: Float) {
        if (hr == 0.0f) return // Throws 0 HR

        val timestamp = System.currentTimeMillis()
        val dataPoint = DataPoint(hr, timestamp)

        if (windowStart.equals(0.toLong())) windowStart = timestamp
        else if (timestamp - windowStart > WINDOW_MILLI) { // Outside of window
            doAnalysis() // Check if stressed

            // Reset window
            windowStart = timestamp
            heartrateData = ArrayList<DataPoint>()
        }

        heartrateData.add(dataPoint)


    }

    /**
     * Checks if data says someone is stressed, in a separate thread
     */
    private fun doAnalysis() {
        val heartRateDataCopy = heartrateData.toList() // Need to be separate copy if analysis takes awhile
        scope.launch {// Separate thread
            var sum: Float = 0.0F
            for (e in heartRateDataCopy) {
                sum += e.hr
                //Log.d(TAG, e.hr.toString())
            }
            /*var loc = getCurrentLocationBlocking(fusedLocationProviderClient)
            Log.d(TAG, "doAnalysis: loc:${loc.toString()}")
            if (loc != null) {
                lastLocation = loc
            }else{
                loc = lastLocation
            }*/
            val avg = sum / heartRateDataCopy.size
            val severity = ((avg%10)+1).toString()
            val timestamp = System.currentTimeMillis()

            //if (loc == null) return@launch
            val loc = lastLocation ?: return@launch

            val dB = recordAndMeasure()

            Log.d(TAG, "doAnalysis: ${(avg%10)+1}, lat: ${loc.first}, lon: ${loc.second}, timestamp: ${timestamp.toString()}, dB: $dB")

            startWorker(severity, loc.first, loc.second, timestamp.toString())
            }

    }


    /**
     * Starts a worker to do a network request, sends lat, lon, and avg.
     * Network must be available for worker to start
     */
    private fun startWorker(dataPoint: String, lat: String, lon: String, timestamp: String) {
        // Putting data for worker to retrieve
        val data: Data.Builder = Data.Builder()
        data.putString("lat", lat)
        data.putString("lon", lon)
        data.putString("dataPoint", dataPoint)
        data.putString("timestamp", timestamp)
        data.putString("sessionId", sessionId.toString())

        // Creating Worker
        val builder: Constraints.Builder = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)

        val oneTimeWork: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SendDataWorker>()
            .addTag("SendData")
            .setInputData(data.build())
            .setConstraints(builder.build())
            .build()

        WorkManager.getInstance(c).enqueue(oneTimeWork)

    }

    fun setSessionId(sessionId: UUID) {
        Log.d(TAG, "setSessionId: $sessionId")
        this.sessionId = sessionId
    }

    private fun recordAndMeasure(): Double {
        // Initialize variables
        val TIME_AUDIO: Long = 500 // Length of recording in ms
        val recorder = MediaRecorder(c)
        val audioFile = createTempAudioFile() // Create a temporary file to store the recording

        try {
            // Configure recorder settings
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(audioFile.absolutePath)

            // Start recording
            recorder.prepare()
            recorder.start()
            recorder.maxAmplitude // need to do this once before actually receiving max amplitude for some reason

            // Wait for 1 second
            Thread.sleep(TIME_AUDIO)

            // Stop recording
            recorder.stop()

            val maxAmplitude = recorder.maxAmplitude
            //Log.d(TAG, "maxamp: $maxAmplitude")
            if (maxAmplitude <= 0) return 0.0

            // Turning amplitude to decibel, +90 constant to have positive numbers
            val dB = (20 * Math.log10(maxAmplitude / 32767.0))+90

            return dB
        } catch (e: Exception) {
            Log.e("AudioRecording", "Error recording audio:", e)
            return -1.0 // Indicate error
        } finally {
            recorder.release()
            audioFile.delete() // Delete the temporary file
        }

    }

    private fun createTempAudioFile(): File {
        val tempDir = File(c.cacheDir, "audio_recordings")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return File(tempDir, "recording_${System.currentTimeMillis()}.aac")
    }
}