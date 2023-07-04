package com.example.thermalmonitor.battery


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BatteryViewModel : ViewModel() {

    // live data to hold the battery data and expose it to the UI layer
    private val _batteryData = MutableLiveData<BatteryData>()
    val batteryData: LiveData<BatteryData> get() = _batteryData

    // a handler object to run the periodic update of current data on the main thread
    private val handler = Handler(Looper.getMainLooper())

    // a flag to indicate whether the periodic update is running or not
    private var isUpdating = false

    // a constant to define the interval time for the periodic update in milliseconds
    private val intervalTime = 1000L

    // broadcast receiver to receive the battery changes from the system and update the live data accordingly
    val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                // get the battery information from the intent extras and create a BatteryData object from them
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) // battery level percentage (-1 if unknown)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1) // charging status (constant defined in BatteryManager class)
                val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000f // battery voltage in millivolts (-1 if unknown)
                val source = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) // power source (constant defined in BatteryManager class)

                // convert the status and source constants to human-readable strings using when expressions
                val statusString = when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "充电中"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电中"
                    BatteryManager.BATTERY_STATUS_FULL -> "已充满"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "未充电"
                    BatteryManager.BATTERY_STATUS_UNKNOWN -> "未知"
                    else -> "未知"
                }

                val sourceString = when (source) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "交流电源"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB电源"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线电源"
                    0 -> "未插入电源"
                    else -> "未知"
                }

                // get the battery temperature from the intent extras in tenths of a degree Centigrade and convert it to degrees Celsius
                val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f // battery temperature in °C

                // update the live data with the new BatteryData object, keeping the current value unchanged
                _batteryData.value = _batteryData.value?.copy(level = level, status = statusString, temperature = temperature, voltage = voltage, source = sourceString)
                    ?: BatteryData(level, statusString, 0, temperature, voltage, sourceString)

                // start the periodic update of current data if not already started
                if (!isUpdating) {
                    startPeriodicUpdate(context)
                }
            }
        }
    }

    // a function to start the periodic update of current data using handler
    private fun startPeriodicUpdate(context: Context?) {
        isUpdating = true // set the flag to true
        handler.postDelayed(object : Runnable { // post a runnable object with a delay of interval time
            override fun run() {
                // get the battery manager service from the context and use it to get the battery current in mA
                val batteryManager = context?.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000// battery current in mA

                // update the live data with the new current value, keeping the other values unchanged
                _batteryData.value = _batteryData.value?.copy(current = current)
                    ?: BatteryData(0, "未知", current, 0f, 0f, "未知")

                handler.postDelayed(this, intervalTime) // post this runnable object again with a delay of interval time
            }
        }, intervalTime)
    }

    // a function to stop the periodic update of current data
    private fun stopPeriodicUpdate() {
        isUpdating = false // set the flag to false
        handler.removeCallbacksAndMessages(null) // remove all callbacks and messages from the handler
    }

    // override the onCleared() function of the view model to stop the periodic update when the view model is destroyed
    override fun onCleared() {
        super.onCleared()
        stopPeriodicUpdate()
    }
}
