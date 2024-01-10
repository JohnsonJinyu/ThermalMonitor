package com.example.thermalmonitor.thermal


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ThermalViewModel : ViewModel() {

    private val _thermalList = MutableLiveData<List<ThermalData>>() //使用LiveData存储和更新数据，方便通知与观察
    val thermalList: LiveData<List<ThermalData>>
        get() = _thermalList

    private var timerJob: Job? = null //用于保存定时任务的协程


    // 创建一个新的变量来保存用户的选择
    private val userSelections = mutableMapOf<String, Boolean>()



    init {
        //在ViewModel的作用域内启动一个协程，在后台线程执行读取和处理数据的任务，每秒执行一次，使用exec函数读取文件内容
        timerJob = viewModelScope.launch(Dispatchers.IO) {

            Log.d("CoroutineStatus", "Coroutine started") // 添加这条日志打印 已确认协程会启动两次

            while (isActive) { //检查协程是否还在活动状态
                // 创建一个可变列表，用于存储有效的thermal_zone的type和temp值
                val list = mutableListOf<ThermalData>()
                //使用async函数并发读取每个thermal_zone的type以及temp值，返回Deferred对象
                val deferred = mutableListOf<Deferred<Pair<String, String>>>()

                for (i in 0..130) {
                    val zone = "thermal_zone$i"
                    deferred.add(async {

                        val type = readFile(File("/sys/class/thermal/$zone/type"))
                        val temp = readFile(File("/sys/class/thermal/$zone/temp"))
                        // 使用exec的方法实现 更复杂，效率更低
                        //val type = exec("cat /sys/class/thermal/$zone/type").trim() //使用exec函数执行cat命令读取文件内容
                        //val temp = exec("cat /sys/class/thermal/$zone/temp").trim()
                        type to temp //返回一个Pair对象，包含type和temp 值

                    })
                }

                //使用 awaitAll 函数等待所有的Deferred对象完整，并获取他们的结果
                val results = deferred.awaitAll()

                //遍历结果，过滤无效的thermal_zone,如果有效则转换为temp值为浮点数并保留两位小数，添加到列表
                for (i in 0..130) {
                    val zone = "thermal_zone$i"
                    val (type, temp) = results[i]  //解构Pair对象，获取type和temp值
                    if (type.isNotEmpty() && temp.isNotEmpty() && temp != "0" && temp.toInt() > -20000) {
                        list.add(
                            ThermalData(
                                zone,
                                type,
                                "%.2f".format(temp.toInt() / 1000.0)
                            )
                        ) //使用字符串模板和双精度浮点数来格式化温度值
                    }
                }

                updateThermalList(list)
                //Log.d("init", "updateThermalList called from init")


                delay(1000) //使用delay函数而不是Thread.sleep方法来暂停协程
                    Log.d("delayConfirm","delay 1s")
            }
        }
    }


    /**
     * 使用 Kotlin 的 File I/O API 来读取文件内容
     * 更高效
     * */
    private fun readFile(file: File): String {
        return try {
            file.readText().trim()
        } catch (e: Exception) {
            ""
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ThermalViewModel", "Instance cleared") // 在onCleared方法中添加日志打印
        timerJob?.cancel() //在ViewModel销毁时取消定时任务的协程
    }


    // 添加一个方法，主要是为了让checkbox的状态维持旧的状态
    private fun updateThermalList(newData: List<ThermalData>) {
        val updatedData = newData.map { data ->
            val isChecked = userSelections[data.zone] ?: data.isChecked
            data.copy(isChecked = isChecked)
            }
        _thermalList.postValue(updatedData)

        // Log打印updatedData
        Log.d("updatedData", "updatedData: $updatedData")

    }







    //LiveData 的 setValue() 和 postValue() 方法必须在主线程上调用。你可以通过 Dispatchers.Main 来在主线程上更新 LiveData。
    fun updateItem(zone: String, isChecked: Boolean) {
        // 更新用户的选择
        userSelections[zone] = isChecked

    }






    /**
     * exec的方法执行shell命令数据，效率低，资源占用高，已舍弃
     * */
    //执行命令并返回输出结果的字符串，如果出现异常则返回空字符串
    /*private fun exec(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.plus(line)
            }
            reader.close()
            process.waitFor()
            output?.toString() ?: ""
            //output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }*/


}

