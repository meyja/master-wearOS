package com.example.watchapp.presentation.data

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody.Part.Companion.create
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class SendDataWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
    override fun doWork(): Result {

        val dataPoint = inputData.getString("dataPoint")
        val lat = inputData.getString("lat")
        val lon = inputData.getString("lon")
        val timestamp = inputData.getString("timestamp")
        val id = inputData.getString("sessionId")
        val dB = inputData.getString("dB")

        Log.d("SendDataWorker", "doWork: ${lat}, ${lon}, ${dataPoint}, ${timestamp}, ${dB}")

        if (dataPoint.equals(null) || lat.equals(null) || lon.equals(null) || timestamp.equals(null)) return Result.failure()

        try {

            // Create an OkHttpClient.Builder instance
            val builder = OkHttpClient.Builder()

            // Configure timeouts as needed (in milliseconds)
            builder.connectTimeout(100, TimeUnit.SECONDS)
            builder.readTimeout(100, TimeUnit.SECONDS)
            builder.writeTimeout(100, TimeUnit.SECONDS)

            // Build the OkHttpClient instance
            val client = builder.build()

            // create json here
            val jsonObject = JSONObject()
            try {
                jsonObject.put("lat", lat!!)
                jsonObject.put("lon", lon!!)
                jsonObject.put("dataPoint", dataPoint!!)
                jsonObject.put("timestamp", timestamp!!)
                jsonObject.put("sessionId", id!!)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonObject.toString().toRequestBody(mediaType)

            val request = okhttp3.Request.Builder()
                .url("https://mongoapi-lr9d.onrender.com/stressdata")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            return if (response.isSuccessful) {
                Log.d("PostDataWorker", "Request successful" + response.toString())

                response.close()
                Result.success()
            } else {
                Log.e("PostDataWorker", "Failed to post data: " + response.toString())

                response.close()
                Result.failure()
            }
        } catch (e: IOException) {
            Log.e("PostDataWorker", "Network error posting data: ", e)
            return Result.failure()
        }
    }
}