package com.example.thermalmonitor.overview

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel

class ViewModelFactory(
    private val batteryViewModel: BatteryViewModel,
    private val thermalViewModel: ThermalViewModel,
    private val socViewModel: SocViewModel,
    private val dataProcessor: DataProcessToSave,
    private val context: Context,
    private val   openFolderListener: OpenFolderListener
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataCaptureViewModel::class.java)) {
            return DataCaptureViewModel(batteryViewModel, thermalViewModel, socViewModel, dataProcessor, context,openFolderListener) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
