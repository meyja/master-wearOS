package com.example.watchapp.presentation.utils

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import java.io.File

fun getDecibel(c: Context): Double {
    // Initialize variables
    val TIME_AUDIO: Long = 500 // Length of recording in ms
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

        // Wait for 1 second
        Thread.sleep(TIME_AUDIO)

        // Stop recording
        recorder.stop()

        val maxAmplitude = recorder.maxAmplitude
        //Log.d(TAG, "maxamp: $maxAmplitude")
        Log.d("getDecibel", maxAmplitude.toString())
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

private fun createTempAudioFile(c: Context): File {
    val tempDir = File(c.cacheDir, "audio_recordings")
    if (!tempDir.exists()) {
        tempDir.mkdirs()
    }
    return File(tempDir, "recording_${System.currentTimeMillis()}.aac")
}