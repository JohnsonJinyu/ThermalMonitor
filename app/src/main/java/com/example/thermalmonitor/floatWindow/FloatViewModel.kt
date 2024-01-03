package com.example.thermalmonitor.floatWindow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel


class FloatViewModel(application: Application) :AndroidViewModel(application) {

    //定义新的LiveData，用于保存筛选后的数据
    private val _floatData = MutableLiveData<List<FloatDataItem>>()
    // 一个LiveData对象，用于暴露给UI层观察和订阅
    val floatData: MutableLiveData<List<FloatDataItem>>
        get() = _floatData

    // 存储筛选后的数据
    private val filteredData = mutableListOf<FloatDataItem>()

    // 获取其他的三个viewModel
    private val batteryViewModel = BatteryViewModel(application)
    private val thermalViewModel = ThermalViewModel(application)
    private val socViewModel = SocViewModel(application)

    // 分别定义三个FloatDataItem格式的可变列表变量，用于观察三个ViewModel中的LiveData，对数据做出筛选
    private var batteryDataList = mutableListOf<FloatDataItem>()
    private var thermalDataList = mutableListOf<FloatDataItem>()
    private var socDataList = mutableListOf<FloatDataItem>()

    // 分别观察上面三个viewModel中相应的liveData，对相应的liveData数据做出筛选处理后添加到上面三个新定义的可变列表中
    // 最后将三个新定义的可变列表中的数据添加到新定义的LiveData中
    init {

        // 观察电池数据并处理后，通过中间列表更新到新定义的LiveData中
        batteryViewModel.batteryData.observeForever {

            // 先清空中间列表，避免重复添加数据
            filteredData.clear()

            val batteryLevelValue = it.level
            val batteryVoltageValue = it.voltage
            val batteryCurrentValue = it.current
            val batteryStatusValue = it.status

            //再将中间列表添加到liveData中
            filteredData.add(FloatDataItem("BatteryLevel","$batteryLevelValue"))
            filteredData.add(FloatDataItem("BatteryVoltage","$batteryVoltageValue"))
            filteredData.add(FloatDataItem("BatteryCurrent","$batteryCurrentValue"))
            filteredData.add(FloatDataItem("BatteryStatus","$batteryStatusValue"))

            // 使用postValue方法更新LiveData的值
            _floatData.postValue(filteredData)

        }





//        thermalViewModel.thermalList.observeForever {
//            for (item in it) {
//                thermalDataList.add(FloatDataItem(item.zone, item.type, item.temp))
//                _floatData.value = FloatDataItem(item.zone, item.type, item.temp)
//            }
//        }
//        socViewModel.socData.observeForever {
//            socDataList.add(FloatDataItem(it.socLevel, it.socTemperature, it.socVoltage))
//            _floatData.value = FloatDataItem(it.socLevel, it.socTemperature, it.socVoltage)
//        }
    }



}