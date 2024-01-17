package com.example.thermalmonitor.floatWindow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.battery.BatteryData
import com.example.thermalmonitor.soc.DynamicInfo
import com.example.thermalmonitor.thermal.ThermalData

class FloatViewModel(application: Application) : AndroidViewModel(application) {

    // 定义liveData，用于内部修改和外部观察
    private val _floatData = MutableLiveData<List<FloatDataItem>>()
    val floatData: LiveData<List<FloatDataItem>>
        get() = _floatData


    // 获取其他ViewModel
    private val batteryViewModel = (application as MyApp).getBatteryViewModel()
    private val thermalViewModel = (application as MyApp).getThermalViewModel()
    private val socViewModel = (application as MyApp).getSocViewModel()



    /**
     * 控制启动和停止观察外部liveData
     * */

    fun startObserving() {
        // 启动观察其他ViewModel
        startObservingBatteryData()
        startObservingThermalData()
        startObservingSocDynamicInfo()
    }

    fun stopObserving() {
        // 停止观察
        stopObservingBatteryData()
        stopObservingThermalData()
        stopObservingSocDynamicInfo()
    }


    /**
     * 对电池数据的观察
     * */


    // 启动电池数据的观察者
    private fun startObservingBatteryData() {
        batteryViewModel.batteryData.observeForever(batteryDataObserver)
    }

    // 停止电池数据的观察者
    private fun stopObservingBatteryData() {
        batteryViewModel.batteryData.removeObserver(batteryDataObserver)
    }

    // 暂存观察者引用，以便可以移除
    private val batteryDataObserver = Observer<BatteryData> { _ ->
        updateFloatData()
    }


    /**
     * 对thermal数据的观察
     * */

    // 暂存观察者引用，以便可以移除
    private val thermalDataObserver = Observer<List<ThermalData>> { _ ->
        updateFloatData()
    }


    // 启动thermal数据的观察者
    private fun startObservingThermalData() {
        thermalViewModel.thermalList.observeForever(thermalDataObserver)
    }

    // 停止thermal数据的观察者
    private fun stopObservingThermalData() {
        thermalViewModel.thermalList.removeObserver(thermalDataObserver)
    }


    /**
     *对socDynamicInfo的观察
     * */
    private val socDynamicInfoObserver = Observer<List<DynamicInfo>> { _ ->
        updateFloatData()
    }

    // 启动socDynamicInfo的观察者
    private fun startObservingSocDynamicInfo() {
        socViewModel.dynamicInfo.observeForever(socDynamicInfoObserver)
    }

    // 停止socDynamicInfo的观察者
    private fun stopObservingSocDynamicInfo() {
        socViewModel.dynamicInfo.removeObserver(socDynamicInfoObserver)
    }


    /**
     * 处理及更新数据
     * */

    private fun updateFloatData() {
        // 获取电池数据
        val batteryData = batteryViewModel.batteryData.value
        val batteryDataItems = if (batteryData != null) {
            listOf(
                FloatDataItem("Battery Level", "${batteryData.level}%"),
                FloatDataItem("Battery Status", batteryData.status),
                FloatDataItem("Battery Current", "${batteryData.current}mA"),
                FloatDataItem("Battery Voltage", "${batteryData.voltage}V")
            )
        } else {
            emptyList()
        }

        // 获取被选中的 thermal 数据
        val checkedThermalData = thermalViewModel.thermalList.value?.filter { it.isChecked }
        val thermalDataItems = checkedThermalData?.map {
            FloatDataItem(it.type, "${it.temp}℃")
        } ?: emptyList()

        // 更新 _floatData
        //_floatData.value = batteryDataItems + thermalDataItems


        // 虎丘被选中的soc frequency数据
        val checkedSocDynamicInfo = socViewModel.dynamicInfo.value?.filter { it.isChecked }
        val socDynamicInfoItems = checkedSocDynamicInfo?.map {
            FloatDataItem("Core ${it.coreNumber}", "${it.coreFrequency}MHz")
        } ?: emptyList()

        // 更新 _floatData

        _floatData.value = batteryDataItems + thermalDataItems + socDynamicInfoItems


    }


}