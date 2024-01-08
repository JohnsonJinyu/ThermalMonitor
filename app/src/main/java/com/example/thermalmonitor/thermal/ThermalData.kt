package com.example.thermalmonitor.thermal

data class ThermalData(
    val zone: String,
    val type: String,
    val temp: String,
    var isChecked: Boolean = false  //添加一个布尔值来表示是否选中
)
