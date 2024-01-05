package com.example.thermalmonitor.thermal


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.thermalmonitor.dataRepository.ThermalDataManager

class ThermalViewModel(application: Application) : AndroidViewModel(application) {

    val thermalList: LiveData<List<ThermalData>> = ThermalDataManager.thermalList

}

