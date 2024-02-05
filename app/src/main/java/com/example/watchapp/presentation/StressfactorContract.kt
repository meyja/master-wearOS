package com.example.watchapp.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract

class StressfactorContract: ActivityResultContract<Unit, String>() {

    companion object {
        const val STRESSFACTOR = "STRESSFACTOR"
    }

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, StressfactorActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String {
        val stressfactor = intent!!.getIntExtra(STRESSFACTOR, -1)

        if (stressfactor == -1) {
            return "Failed"
        }
        else {
            // Do THE DEW
            Log.d("Severity", "parseResult: $stressfactor")
            return "Success"
        }
    }
}