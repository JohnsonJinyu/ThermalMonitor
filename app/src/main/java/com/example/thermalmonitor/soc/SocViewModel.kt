package com.example.thermalmonitor.soc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class SocViewModel(application: Application) : AndroidViewModel(application) {

    private val staticInfoFile = File("/proc/cpuinfo") // 静态信息文件的路径，可以根据实际情况修改
    private val dynamicInfoFile = File("/sys/devices/system/cpu") // 动态信息文件的路径，可以根据实际情况修改

    private val _staticInfo = MutableLiveData<StaticInfo>() // 静态信息的内部可变数据，只能在view model中修改
    val staticInfo: LiveData<StaticInfo> = _staticInfo // 静态信息的外部不可变数据，可以在fragment中观察

    private val _dynamicInfo = MutableLiveData<List<DynamicInfo>>() // 动态信息的内部可变数据，只能在view model中修改
    val dynamicInfo: LiveData<List<DynamicInfo>> = _dynamicInfo // 动态信息的外部不可变数据，可以在fragment中观察

    /**
     * 在ViewModel中创建了一个Adapter的实例，并提供了onCheckedChange函数的实现。
     * 在这个实现中，我更新了_dynamicInfo的值。我首先获取了旧的DynamicInfo列表，然后用map方法创建了一个新的列表
     * ，这个新的列表中，核心编号为coreNumber的对象的isChecked属性被更新为新的状态，
     * 其他的对象保持不变。然后，我用这个新的列表更新了_dynamicInfo的值。
     * */
    val adapter = SocAdapter { coreNumber, isChecked ->
        _dynamicInfo.value = _dynamicInfo.value?.map {
            if (it.coreNumber == coreNumber) it.copy(isChecked = isChecked) else it
        }
    }


    // 定义一个协程作用域，这个作用域将在 ViewModel 销毁时取消
    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    init {
        readStaticInfo() // 读取静态信息并更新数据

        // 在协程中读取并更新动态数据信息
        viewModelScope.launch {
            while (isActive) {
                //Log.d("SocViewModel", "协程执行了一次")
                readDynamicInfo() // 读取动态信息并更新数据，并定时重复执行该任务以实现实时刷新
                delay(1000) // 每隔1秒执行一次任务
            }
        }

    }


    private fun readStaticInfo() {
        try {
            // 在尝试读取文件前进行权限检查
            if (staticInfoFile.canRead()) {
                // 读取文件的逻辑
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
                    val minFile = File(
                        dynamicInfoFile,
                        "cpu$i/cpufreq/scaling_min_freq"
                    ) // 根据核心编号，拼接出对应的最小频率文件路径
                    val maxFile = File(
                        dynamicInfoFile,
                        "cpu$i/cpufreq/scaling_max_freq"
                    ) // 根据核心编号，拼接出对应的最大频率文件路径
                    val minFrequency =
                        minFile.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
                    val maxFrequency =
                        maxFile.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
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
            } else {
                // 记录日志，提示文件访问权限受限
                Timber.tag("SocViewModel").e("Permission denied: Cannot read static info file")
            }
        } catch (e: SecurityException) {
            // 捕获权限异常，记录日志
            Timber.tag("SocViewModel").e(e, "Security exception when reading static info file")
        } catch (e: Exception) {
            // 捕获其他异常，记录日志
            Timber.tag("SocViewModel").e(e, "Failed to read static info file")
        }
    }

    /**
     * 两种读取频点的方法，
     * 1、遍历每个频点 cat sys/devices/system/cpu/cpu* /cpufreq/scaling_cur_freq
     * 2、按照大小核心分组 遍历，因为每组核心频点实际一样 cat sys/devices/system/cpu
     *
     * */


    private fun readDynamicInfo() {
        viewModelScope.launch(Dispatchers.IO) { // 在IO线程中读取文件
            try {
                // 在尝试读取文件前进行权限检查
                if (dynamicInfoFile.canRead()) {
                    // 读取文件的逻辑
                    val oldList = _dynamicInfo.value ?: emptyList<DynamicInfo>() // 获取当前的动态信息列表
                    val newList = mutableListOf<DynamicInfo>() // 创建一个新的动态信息列表，用于存储读取到的数据

                    for (i in 0 until (staticInfo.value?.coreCount
                        ?: 0).toInt()) { // 遍历每个核心，根据核心数来确定循环次数
                        val file = File(
                            dynamicInfoFile,
                            "cpu$i/cpufreq/scaling_cur_freq"
                        ) // 根据核心编号，拼接出对应的文件路径
                        val frequency =
                            file.readText().trim().toInt() / 1000 // 读取文件内容，并转换为整数，并除以1000得到MHz单位
                        val isChecked = oldList.find { it.coreNumber == i + 1 }?.isChecked
                            ?: false // 查找当前核心的isChecked状态，如果找不到，就默认为false
                        val info = DynamicInfo(
                            i + 1,
                            frequency,
                            isChecked
                        ) // 创建一个新的动态信息对象，用于存储核心编号、频率和isChecked状态
                        newList.add(info) // 将新的动态信息对象添加到新的列表中
                    }

                    _dynamicInfo.postValue(newList) // 在子线程中更新动态信息的数据，使用postValue方法
                    //  Log.d("newList", "$newList")
                } else {
                    // 记录日志，提示文件访问权限受限
                    Timber.tag("SocViewModel").e("Permission denied: Cannot read dynamic info file")
                }
            } catch (e: SecurityException) {
                // 捕获权限异常，记录日志
                Timber.tag("SocViewModel").e(e, "Security exception when reading dynamic info file")
            } catch (e: Exception) {
                // 捕获其他异常，记录日志
                Timber.tag("SocViewModel").e(e, "Failed to read dynamic info file")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()  // 当 ViewModel 销毁时，取消所有的协程
    }



    fun updateCheckedState(coreNumber: Int, isChecked: Boolean) {
        _dynamicInfo.value = _dynamicInfo.value?.map {
            if (it.coreNumber == coreNumber) it.copy(isChecked = isChecked) else it
        }
    }
}
