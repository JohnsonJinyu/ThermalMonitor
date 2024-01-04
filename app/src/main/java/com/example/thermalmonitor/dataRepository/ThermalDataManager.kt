package com.example.thermalmonitor.dataRepository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.thermalmonitor.thermal.ThermalData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * 全局单例模式意味着创建一个单一的、全局可访问的数据管理者，用以统一管理和分发数据。
 * 这个单例会负责从硬件读取数据，并提供数据给所有需要订阅数据的组件，比如不同的`Fragment`和悬浮窗
 * */


object ThermalDataManager {

    private val _thermalList = MutableLiveData<List<ThermalData>>()
    val thermalList: LiveData<List<ThermalData>>
        get() = _thermalList

    private var timerJob: Job? = null


    // 初始化代码块，启动定时任务
    init {
        startFetchingData()
    }


    private fun startFetchingData() {
        timerJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                // 这里是原 ThermalViewModel 中的数据读取逻辑...
                val list = mutableListOf<ThermalData>()
                //使用async函数并发读取每个thermal_zone的type以及temp值，返回Deferred对象
                val deferreds = mutableListOf<Deferred<Pair<String, String>>>()
                for(i in 0 .. 130){
                    val zone = "thermal_zone$i"
                    deferreds.add(async {
                        val type = exec("cat /sys/class/thermal/$zone/type").trim() //使用exec函数执行cat命令读取文件内容
                        val temp = exec("cat /sys/class/thermal/$zone/temp").trim()
                        type to temp //返回一个Pair对象，包含type和tmep 值

                    })
                }
                //使用 awaitAll 函数等待所有的Deferred对象完整，并获取他们的结果
                val results = deferreds.awaitAll()
                //遍历结果，过滤无效的thermal_zone,如果有效则转换为temp值为浮点数并保留两位小数，添加到列表
                for(i in 0 .. 130){
                    val zone = "thermal_zone$i"
                    val (type,temp) = results[i]  //解构Pair对象，获取type和temp值
                    if(type.isNotEmpty() && temp.isNotEmpty() && temp != "0" && temp.toInt() > -20000){
                        list.add(ThermalData(zone,type,"%.2f".format(temp.toInt()/1000.0))) //使用字符串模板和双精度浮点数来格式化温度值
                    }
                }
                //在主线程更新LiveData的值，通知观察者数据变化
                withContext(Dispatchers.Main){
                    _thermalList.value = list //使用value属性而不是postValue方法来更新LiveData的值
                }
                // 当数据读取完成后，使用 _thermalList.postValue() 更新数据
                delay(1000)
            }
        }
    }


    fun stopFetchingData() {
        timerJob?.cancel()
    }


    private fun exec(cmd: String): String {
        // 这里是原 ThermalViewModel 中的 exec 方法...
        return try{
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            var line : String?
            while (reader.readLine().also{line = it} != null){
                output.plus(line)
            }
            reader.close()
            process.waitFor()
            output?.toString() ?: ""
            //output.toString()
        }catch (e:Exception){
            e.printStackTrace()
            ""
        }
    }
}
