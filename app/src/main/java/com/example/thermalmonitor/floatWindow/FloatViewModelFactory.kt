package com.example.thermalmonitor.floatWindow

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class FloatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FloatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FloatViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}