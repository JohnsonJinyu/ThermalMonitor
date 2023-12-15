package com.example.thermalmonitor.battery

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Timer
import java.util.TimerTask


class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    // 一个MutableLiveData对象，用于存储和更新电池信息
    private val _batteryData = MutableLiveData<BatteryData>()
    // 一个LiveData对象，用于暴露给UI层观察和订阅
    val batteryData: LiveData<BatteryData>
        get() = _batteryData

    // 一个BroadcastReceiver对象，用于接收系统广播的电池信息变化
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 调用一个方法，用于获取电池信息并更新LiveData对象的值
            updateBatteryData(intent)
        }
    }

    // 一个Timer对象，用于定时获取电池信息
    private val timer = Timer()

    // 在ViewModel初始化时，注册BroadcastReceiver对象，监听电池信息变化的系统广播，并启动Timer对象，每隔一秒就主动获取一次电池信息
    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        getApplication<Application>().registerReceiver(batteryReceiver, filter)
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                // 获取当前的Intent对象，包含电池信息的额外数据
                val intent = getApplication<Application>().registerReceiver(null, filter)
                // 调用一个方法，用于获取电池信息并更新LiveData对象的值
                updateBatteryData(intent)
            }
        }, 0, 1000) // 每隔1000毫秒（即1秒）执行一次任务
    }

    // 在ViewModel销毁时，注销BroadcastReceiver对象，取消Timer对象，避免内存泄漏
    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(batteryReceiver)
        timer.cancel()
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
//        if (current == 0) { // 如果获取失败，就尝试另一种方式获取
//            current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
//        }

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

