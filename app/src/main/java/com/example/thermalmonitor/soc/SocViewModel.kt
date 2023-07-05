package com.example.thermalmonitor.soc

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import java.io.File

class SocViewModel(application: Application) : AndroidViewModel(application) {

    private val staticInfoFile = File("/proc/cpuinfo") // 静态信息文件的路径，可以根据实际情况修改
    private val dynamicInfoFile = File("/sys/devices/system/cpu") // 动态信息文件的路径，可以根据实际情况修改

    private val _staticInfo = MutableLiveData<StaticInfo>() // 静态信息的内部可变数据，只能在view model中修改
    val staticInfo: LiveData<StaticInfo> = _staticInfo // 静态信息的外部不可变数据，可以在fragment中观察

    private val _dynamicInfo = MutableLiveData<List<DynamicInfo>>() // 动态信息的内部可变数据，只能在view model中修改
    val dynamicInfo: LiveData<List<DynamicInfo>> = _dynamicInfo // 动态信息的外部不可变数据，可以在fragment中观察

    private val handler = Handler(Looper.getMainLooper()) // 用于在主线程上执行任务的handler

    init {
        readStaticInfo() // 读取静态信息并更新数据
        readDynamicInfo() // 读取动态信息并更新数据，并定时重复执行该任务以实现实时刷新
    }

    private fun readStaticInfo() {
        val info = StaticInfo() // 创建一个静态信息对象，用于存储读取到的数据

        staticInfoFile.forEachLine { line -> // 遍历文件的每一行，解析出需要的数据，并赋值给静态信息对象的属性
            when {
                line.startsWith("Hardware") -> { // 如果是硬件名称，就截取冒号后面的部分，并去掉空格和换行符
                    info.hardwareName = line.substringAfter(":").trim()
                }
                line.startsWith("processor") -> { // 如果是处理器编号，就说明有一个核心，就让核心数加一
                    info.coreCount++
                }
            }
        }

        // 根据核心数，遍历每个核心，读取对应的最大最小频率文件，并生成一个频率范围的字符串，格式参考题目要求
        // 使用一个map来存储每个频率范围对应的核心数
        val rangeMap = mutableMapOf<String, Int>()
        for (i in 0 until info.coreCount) {
            val minFile = File(dynamicInfoFile, "cpu$i/cpufreq/scaling_min_freq") // 根据核心编号，拼接出对应的最小频率文件路径
            val maxFile = File(dynamicInfoFile, "cpu$i/cpufreq/scaling_max_freq") // 根据核心编号，拼接出对应的最大频率文件路径
            val minFrequency = minFile.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
            val maxFrequency = maxFile.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
            val range = "$minFrequency MHz - $maxFrequency MHz" // 拼接成一个频率范围字符串
            rangeMap[range] = rangeMap.getOrDefault(range, 0) + 1 // 在map中增加该频率范围对应的核心数
            }

        // 使用一个字符串构建器来拼接最终的频率范围字符串
        val rangeBuilder = StringBuilder()
        for ((range, count) in rangeMap) { // 遍历map中的每个键值对
            rangeBuilder.append("$count × $range\n") // 拼接成一行字符串，并换行
            }
        info.frequencyRange = rangeBuilder.toString().trimEnd() // 去掉最后的换行符

        _staticInfo.postValue(info) // 在子线程中更新静态信息的数据，使用postValue方法
    }

    private fun readDynamicInfo() {
        val list = mutableListOf<DynamicInfo>() // 创建一个动态信息列表，用于存储读取到的数据
        Timber.tag("SocViewModel").d("coreCount = " + staticInfo.value?.coreCount)

        for (i in 0 until (staticInfo.value?.coreCount ?: 0).toInt()) { // 遍历每个核心，根据核心数来确定循环次数
            val file = File(dynamicInfoFile, "cpu$i/cpufreq/scaling_cur_freq") // 根据核心编号，拼接出对应的文件路径
            val frequency = file.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
            Timber.tag("SocViewModel").d("frequency = " + frequency)
            val info = DynamicInfo(i + 1, frequency) // 创建一个动态信息对象，用于存储核心编号和频率
            list.add(info) // 将对象添加到列表中
            //Log.d("SocViewModel", "frequencyList = $list")
        }

        _dynamicInfo.postValue(list) // 在子线程中更新动态信息的数据，使用postValue方法
        Timber.tag("SocViewModel").d("_dynamicInfo = " + _dynamicInfo.value?.joinToString())
        handler.postDelayed({ readDynamicInfo() }, 1000) // 使用handler延迟1秒后再次执行该任务，实现定时刷新
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null) // 当view model被清除时，取消所有的handler任务，避免内存泄漏
    }
}
