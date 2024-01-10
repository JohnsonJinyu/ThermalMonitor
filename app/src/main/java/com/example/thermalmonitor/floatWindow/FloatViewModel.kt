package com.example.thermalmonitor.floatWindow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.battery.BatteryData
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalData

class FloatViewModel(application: Application) : AndroidViewModel(application) {

    // 定义liveData，用于内部修改和外部观察
    private val _floatData = MutableLiveData<List<FloatDataItem>>()
    val floatData: LiveData<List<FloatDataItem>>
        get() = _floatData


    // 使用ViewModelProvider来获取ViewModel实例
    /*private val batteryViewModel: BatteryViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(BatteryViewModel::class.java)
    }*/
    private val batteryViewModel =  (application as MyApp).getBatteryViewModel()


    // 使用MyApp中的getThermalViewModel方法来获取ThermalViewModel的实例
    // 假设ThermalViewModel和SocViewModel也是AndroidViewModel的子类
    /*private val thermalViewModel: ThermalViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(ThermalViewModel::class.java)
    }*/
    private val thermalViewModel = (application as MyApp).getThermalViewModel()


    private val socViewModel: SocViewModel by lazy {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(SocViewModel::class.java)
    }


    // 定义一个中间列表，分别用于存储battery、thermal、soc的数据
    private val batteryDataList = mutableListOf<FloatDataItem>()
    private val thermalDataList = mutableListOf<FloatDataItem>()



    /**
     * 控制启动和停止观察外部liveData
     * */

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
        _floatData.value = batteryDataItems + thermalDataItems
        // Log打印thermalDataItems
        //Log.d("thermalDataItems", "thermalDataItems: $thermalDataItems")
    }


}