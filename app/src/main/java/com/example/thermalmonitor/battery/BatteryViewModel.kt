package com.example.thermalmonitor.battery

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    // 一个MutableLiveData对象，用于存储和更新电池信息
    private val _batteryData = MutableLiveData<BatteryData>()

    // 一个LiveData对象，用于暴露给UI层观察和订阅
    val batteryData: LiveData<BatteryData>
        get() = _batteryData


    // 定义一个协程作用域，这个作用域将在 ViewModel 销毁时取消
    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    init {
        viewModelScope.launch {
            while (isActive) { // 当 ViewModel 没有销毁时，循环执行任务
                val intent = getApplication<Application>().registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                updateBatteryData(intent)
                delay(1000) //每个一秒，执行一次任务
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()  // 当 ViewModel 销毁时，取消所有的协程
    }

    // 一个方法，用于获取电池信息，并更新LiveData对象的值
    private fun updateBatteryData(intent: Intent?) {
        // 获取BatteryManager对象，用于获取电池信息的属性值
        val batteryManager =
            getApplication<Application>().getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        // 获取电量百分比，范围是0-100
        var level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level == -1) { // 如果获取失败，就从Intent中获取
            level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        }

        // 获取充电状态，转换为字符串表示
        val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
            BatteryManager.BATTERY_STATUS_FULL -> "已充满"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "未知"
            else -> "未知"
        }

        // 获取实时电流，单位是微安培（uA）
        var current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)


        // 获取电池温度，单位是摄氏度（℃）
        val temperature =
            intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.div(10) ?: -1

        // 获取电压，单位是毫伏（mV）
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1

        // 获取供电来源，转换为字符串表示
        val source = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
            BatteryManager.BATTERY_PLUGGED_AC -> "交流电源"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB端口"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            0 -> "电池供电"
            else -> "未知"
        }

        // 创建一个BatteryData对象，用于封装电池信息的属性值
        val batteryData = BatteryData(level, status, current, temperature, voltage, source)

        // 更新MutableLiveData对象的值，通知UI层数据变化
        _batteryData.postValue(batteryData)
    }
}

