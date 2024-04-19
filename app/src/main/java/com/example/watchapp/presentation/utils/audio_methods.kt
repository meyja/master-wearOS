package com.example.watchapp.presentation.utils

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.log10

// makes recording and extracts decibel
fun getDecibel(c: Context, length: Long): Double {
    // Initialize variables
    val TIME_AUDIO: Long = length// Length of recording in ms
    val recorder = MediaRecorder(c)
    val audioFile = createTempAudioFile(c) // Create a temporary file to store the recording

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

        // Wait for x.x second
        Thread.sleep(TIME_AUDIO)

        // Stop recording
        recorder.stop()

        val maxAmplitude = recorder.maxAmplitude
        //Log.d(TAG, "maxamp: $maxAmplitude")
        Log.d("getDecibel", maxAmplitude.toString())
        if (maxAmplitude <= 0) return -1.0

        // Turning amplitude to decibel, +90 constant to have positive numbers
        val dB = (20 * log10(maxAmplitude / 32767.0))+90

        return dB
    } catch (e: Exception) {
        Log.e("AudioRecording", "Error recording audio:", e)
        return -1.0 // Indicate error
    } finally {
        recorder.release()
        audioFile.delete() // Delete the temporary file
    }

}

// creates temporary file for the audio
private fun createTempAudioFile(c: Context): File {
    val tempDir = File(c.cacheDir, "audio_recordings")
    if (!tempDir.exists()) {
        tempDir.mkdirs()
    }
    return File(tempDir, "recording_${System.currentTimeMillis()}.aac")
}

// Loop for recording audio, with callback for when recorded and wait between each loop
fun audioRecordingLoop(c: Context, recordingLength: Long, recordingPauseLength: Long, scope: CoroutineScope, callback: (Double) -> Unit) {
    scope.launch {
        val recorder = MediaRecorder(c)
        val audioFile = createTempAudioFile(c) // Create a temporary file to store the recording

        while (this.isActive) {
            callback(getDecibel(c, recordingLength))
            delay(recordingPauseLength)
        }
    }
}

