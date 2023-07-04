package com.example.thermalmonitor.battery

//数据类，用于封装电池信息
data class BatteryData(
    val level: Int, //电量百分比
    val status: String, //充电状态
    val current: Int, //实时电流，单位mA
    val temperature: Float, //电池温度，单位°C
    val voltage: Float, //电压，单位V
    val source: String //供电来源
)