package com.example.thermalmonitor

import android.app.Application
import com.example.thermalmonitor.thermal.ThermalViewModel

class MyApp : Application() {


    // 声明一个late init变量，用于保存ThermalViewModel的实例
    private lateinit var thermalViewModel: ThermalViewModel

    override fun onCreate() {
        super.onCreate()

        // 创建ThermalViewModel的实例
        thermalViewModel = ThermalViewModel(this)
    }


    // 为了在FloatWindow中获取ThermalViewModel的实例，需要添加一个方法
    fun getThermalViewModel(): ThermalViewModel {
        return thermalViewModel
    }


}