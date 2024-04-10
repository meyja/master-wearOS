package com.example.watchapp.presentation.selfreport

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.example.watchapp.presentation.MainViewModel
import com.example.watchapp.presentation.data.MainRepository
import com.example.watchapp.presentation.utils.getCurrentLocationNonBlocking
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SelfReportViewModel(private val repo: SelfReportRepository): ViewModel() {

    private val _severity = MutableStateFlow(5)
    val severity = _severity.asStateFlow()

    private val _isReporting = MutableStateFlow(false)
    val isReportingState = _isReporting.asStateFlow()

    private val _returnMessage = MutableStateFlow(-2)
    val returnMessage = MutableLiveData<Int>()

    fun changeSeverity(newSeverity: Int) {
        _severity.value = newSeverity
    }

    fun report() {
        if(_isReporting.value) return

        _isReporting.value = true

        if (_severity.value == 0) {
            _returnMessage.value = 0
        }

        repo.report(::onReceivedLocation)
    }
    
    private fun onReceivedLocation(loc: Pair<String, String>?) {
        if (loc == null) { // Location not working
            _returnMessage.value = -1
            return
        }



        repo.startWorker(_severity.value.toString(), loc.first, loc.second)

        returnMessage.value = 1
    }

    fun getDB(): Double {

        return 0.0
    }

    fun setLocationProvider(fusedLocationProviderClient: FusedLocationProviderClient) {
        repo.setLocationProvider(fusedLocationProviderClient)
    }

    fun setWorkManager(workManager: WorkManager) {
        repo.setWorkManager(workManager)
    }

    fun setDB(dB: Double) {
        repo.setDB(dB)
    }

    fun setUUID(uuid: UUID) {
        repo.setUUID(uuid)
    }


    class SelfReportViewModelFactory(private val selfReportRepository: SelfReportRepository) : ViewModelProvider.Factory {

        /**
         * Creates a new instance of the specified ViewModel class.
         *
         * @param modelClass The class of the ViewModel to create.
         * @return A new instance of the specified ViewModel.
         * @throws IllegalArgumentException if the ViewModel class is not recognized.
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(SelfReportViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                SelfReportViewModel(selfReportRepository) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }
}