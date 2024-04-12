package com.example.watchapp.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.watchapp.presentation.data.MainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(private val repo: MainRepository) : ViewModel() {

    /**
     * MutableStateFlow representing the running state of the service.
     */
    private val _isServiceRunning = MutableStateFlow(false)

    /**
     * Immutable StateFlow exposing the running state of the service.
     */
    val isRunningState = _isServiceRunning.asStateFlow()

    /**
     * Initializes the ViewModel by checking if the service is running.
     */
    init {
        checkIfServiceIsRunning()
    }

    /**
     * Starts the HeartRateService and updates the running state.
     */
    fun startService() {
        repo.startService()
        checkIfServiceIsRunning()
    }

    /**
     * Stops the HeartRateService and updates the running state.
     */
    fun stopService() {
        repo.stopService()
        _isServiceRunning.value = repo.isServiceRunning()
    }

    /**
     * Checks if the HeartRateService is currently running and updates the running state accordingly.
     */
    fun checkIfServiceIsRunning() {
        _isServiceRunning.value =
            repo.isServiceRunning()
    }

    /**
     * Factory class for creating instances of [MainViewModel].
     *
     * @property mainRepository The [MainRepository] instance to be used by the ViewModel.
     */
    class MainViewModelFactory(private val mainRepository: MainRepository) : ViewModelProvider.Factory {

        /**
         * Creates a new instance of the specified ViewModel class.
         *
         * @param modelClass The class of the ViewModel to create.
         * @return A new instance of the specified ViewModel.
         * @throws IllegalArgumentException if the ViewModel class is not recognized.
         */
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                MainViewModel(mainRepository) as T
            } else {
                throw IllegalArgumentException("ViewModel Not Found")
            }
        }
    }


}