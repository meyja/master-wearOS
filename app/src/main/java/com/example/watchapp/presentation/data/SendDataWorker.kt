package com.example.watchapp.presentation.data

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.FormBody
import okhttp3.OkHttpClient
import java.io.IOException


class SendDataWorker(context: Context, workerParams: WorkerParameters): Worker(context, workerParams) {
    val c = context
    override fun doWork(): Result {

        val avg = inputData.getFloat("avg", 0f)
        val lat = inputData.getString("lat")
        val lon = inputData.getString("lon")

        if (avg == 0f || lat.equals(null) || lon.equals(null)) return Result.failure()

        try {
            val client = OkHttpClient()

            val formBody = FormBody.Builder()
                .build()

            val request = okhttp3.Request.Builder()
                .url("https://httpbin.org/post")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Log.d("PostDataWorker", "Request successful" + response.toString())
                return Result.success()
            } else {
                Log.e("PostDataWorker", "Failed to post data: " + response.toString())
                return Result.failure()
            }
        } catch (e: IOException) {
            Log.e("PostDataWorker", "Network error posting data: ", e)
            return Result.failure()
        }
    }
}