package com.example.thermalmonitor.soc

data class StaticInfo( // 静态信息的数据类，包含硬件名称、核心数、最大最小频率和频率范围等属性，初始值可以根据实际情况修改
    var hardwareName: String = "",
    var coreCount: Int = 0,
    var maxFrequency: Int = 0,
    var minFrequency: Int = Int.MAX_VALUE,
    var frequencyRange: String = ""
)

data class DynamicInfo( // 动态信息的数据类，包含核心编号和核心频率两个属性
    val coreNumber: Int,
    val coreFrequency: Int
)
