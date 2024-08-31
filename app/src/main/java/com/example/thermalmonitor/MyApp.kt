package com.example.thermalmonitor

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.overview.DataCaptureViewModel
import com.example.thermalmonitor.overview.DataProcessToSave
import com.example.thermalmonitor.overview.ViewModelFactory
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel

class MyApp : Application() {

    // 使用 lateinit 声明 ViewModel 实例
    lateinit var thermalViewModel: ThermalViewModel
    lateinit var batteryViewModel: BatteryViewModel
    lateinit var socViewModel: SocViewModel

    // DataCaptureViewModel的单例
    lateinit var dataCaptureViewModel: DataCaptureViewModel

    // 定义 MutableLiveData 实例
    private val timer2 = MutableLiveData<String>()

    override fun onCreate() {
        super.onCreate()

        // 初始化 ViewModel 实例
        thermalViewModel = ThermalViewModel(this)
        batteryViewModel = BatteryViewModel(this)
        socViewModel = SocViewModel(this)

        // 创建 DataCaptureViewModel 的单例
        val dataProcessor = DataProcessToSave(thermalViewModel, socViewModel)
        val factory = ViewModelFactory(batteryViewModel, thermalViewModel, socViewModel, dataProcessor, this)
        dataCaptureViewModel = ViewModelProvider(
            ViewModelStore(),
            factory
        )[DataCaptureViewModel::class.java]
    }


    fun getTimer2(): MutableLiveData<String> {
        return timer2
    }
}