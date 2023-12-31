package com.example.thermalmonitor.floatWindow

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.battery.BatteryData
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalData
import com.example.thermalmonitor.thermal.ThermalViewModel

class FloatViewModel(application: Application) : AndroidViewModel(application) {

    // 定义liveData，用于内部修改和外部观察
    private val _floatData = MutableLiveData<List<FloatDataItem>>()
    val floatData: LiveData<List<FloatDataItem>>
        get() = _floatData





    // 使用ViewModelProvider来获取ViewModel实例
    private val batteryViewModel: BatteryViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(BatteryViewModel::class.java)
    }



    // 假设ThermalViewModel和SocViewModel也是AndroidViewModel的子类
    private val thermalViewModel: ThermalViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(ThermalViewModel::class.java)
    }
    private val socViewModel: SocViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(SocViewModel::class.java)
    }


    // 定义一个中间列表，分别用于存储battery、thermal、soc的数据
    private val batteryDataList = mutableListOf<FloatDataItem>()
    private val thermalDataList = mutableListOf<FloatDataItem>()



    fun startObserving() {
        // 启动观察其他ViewModel
        startObservingBatteryData()
        startObservingThermalData()
    }

    fun stopObserving() {
        // 停止观察
        stopObservingBatteryData()
        stopObservingThermalData()
    }






    /**
     * 对电池数据的观察与处理
     * */
    // 暂存观察者引用，以便可以移除
    private val batteryDataObserver = Observer<BatteryData> { batteryData ->
        handleBatteryData(batteryData)
    }

    // 启动电池数据的观察者
    private fun startObservingBatteryData() {
        batteryViewModel.batteryData.observeForever(batteryDataObserver)
    }
    // 停止电池数据的观察者
    private fun stopObservingBatteryData() {
        batteryViewModel.batteryData.removeObserver(batteryDataObserver)
    }

    // 一个方法，用于处理电池数据
    private fun handleBatteryData(batteryData: BatteryData) {
        // 这里处理电池数据，并更新_floatData
        val batteryDataItems = listOf(
            FloatDataItem("Battery Level", "${batteryData.level}%"),
            FloatDataItem("Battery Status", batteryData.status),
            FloatDataItem("Battery Current", "${batteryData.current}mA"),
            FloatDataItem("Battery Voltage", "${batteryData.voltage}V")
        )
        // 更新batteryDataList
        this.batteryDataList.clear()
        this.batteryDataList.addAll(batteryDataItems)
        // 更新_floatData
        _floatData.value = batteryDataList + this.thermalDataList

    }


    /**
     * Thermal部分数据的观察与处理
     * */

    private val thermalDataObserver = Observer<List<ThermalData>> { thermalDataList ->
        handleThermalData(thermalDataList)
    }

    private fun handleThermalData(thermalDataList: List<ThermalData>) {
        // 这里处理thermal数据，并更新_floatData
        val thermalDataItems = thermalDataList
            .filter { it.isChecked }
            .map {
                FloatDataItem(
                    itemName = it.type,
                    itemValue = it.temp

                )

            }


        this.thermalDataList.clear()
        this.thermalDataList.addAll(thermalDataItems)

        // 假设我们关心电池数据和thermal数据
        _floatData.value = batteryDataList + this.thermalDataList
        // Log打印一下thermalDataList
        Log.d("thermalDataList", "handleThermalData: $thermalDataList")

    }


    private fun startObservingThermalData() {
        thermalViewModel.thermalList.observeForever(thermalDataObserver)
    }

    private fun stopObservingThermalData() {
        thermalViewModel.thermalList.removeObserver(thermalDataObserver)
    }


}