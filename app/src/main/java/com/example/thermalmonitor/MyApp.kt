package com.example.thermalmonitor

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.overview.DataCaptureViewModel
import com.example.thermalmonitor.overview.DataProcessToSave
import com.example.thermalmonitor.overview.ViewModelFactory
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel

class MyApp : Application() {


    // 声明一个late init变量，用于保存ThermalViewModel的实例
    private lateinit var thermalViewModel: ThermalViewModel
    // 声明一个late init变量，用于保存BatteryViewModel的实例
    private lateinit var batteryViewModel: BatteryViewModel
    // 声明一个late init变量，用于保存SocViewModel的实例
    private lateinit var socViewModel: SocViewModel

    // DataCaptureViewModel的单例
    lateinit var dataCaptureViewModel: DataCaptureViewModel

    override fun onCreate() {
        super.onCreate()

        // 创建ThermalViewModel的实例
        thermalViewModel = ThermalViewModel(this)
        // 创建BatteryViewModel的实例
        batteryViewModel = BatteryViewModel(this)
        // 创建SocViewModel的实例
        socViewModel = SocViewModel(this)


        // 创建DataCaptureViewModel的单例，然后用于OverViewFragment以及FloatWindow中共享
        val dataProcessor = DataProcessToSave(thermalViewModel, socViewModel)
        val factory = ViewModelFactory(batteryViewModel, thermalViewModel, socViewModel, dataProcessor, this  )
        // 使用ViewModelProvider来创建DataCaptureViewModel实例
        dataCaptureViewModel = ViewModelProvider(
            ViewModelStore(),
            factory
        )[DataCaptureViewModel::class.java]
    }


    // 为了在FloatWindow中获取ThermalViewModel的实例，需要添加一个方法
    fun getThermalViewModel(): ThermalViewModel {
        return thermalViewModel
    }

    // 为了在FloatWindow中获取BatteryViewModel的实例，需要添加一个方法
    fun getBatteryViewModel(): BatteryViewModel {
        return batteryViewModel
    }

    // 为了在FloatWindow中获取SocViewModel的实例，需要添加一个方法
    fun getSocViewModel(): SocViewModel {
        return socViewModel
    }


}