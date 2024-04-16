package com.example.watchapp.presentation.data

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.example.watchapp.presentation.views.SelfReportActivity

class SelfReportContract: ActivityResultContract<Unit, String>() {

    companion object {
        const val STRESSFACTOR = "STRESSFACTOR"
    }

    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(context, SelfReportActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String {
        val stressfactor = intent!!.getIntExtra(STRESSFACTOR, -1)

        return when (stressfactor) {
            -1 -> "Failed"
            0 -> "Aborted"
            2 -> "Missing Permissions"
            else -> {
                "Success"
            }
        }
    }
}