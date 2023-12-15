package com.example.thermalmonitor.battery

// BatteryData.kt
data class BatteryData(
    val level: Int, // 电量百分比
    val status: String, // 充电状态
    val current: Int, // 实时电流
    val temperature: Int, // 电池温度
    val voltage: Int, // 电压
    val source: String // 供电来源
)

