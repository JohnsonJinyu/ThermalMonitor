package com.example.thermalmonitor.thermal


import android.app.Application
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
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ThermalViewModel(application: Application) : ViewModel() {

    private val _thermalList = MutableLiveData<List<ThermalData>> () //使用LiveData存储和更新数据，方便通知与观察
    val thermalList  :LiveData<List<ThermalData>>
        get() = _thermalList

    private var timerJob: Job? = null //用于保存定时任务的协程

    init {
        //在ViewModel的作用域内启动一个协程，在后台线程执行读取和处理数据的任务，每秒执行一次，使用exec函数读取文件内容
        timerJob = viewModelScope.launch(Dispatchers.IO){
            while (isActive){ //检查协程是否还在活动状态
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
                delay(1000) //使用delay函数而不是Thread.sleep方法来暂停协程
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel() //在ViewModel销毁时取消定时任务的协程
    }

    //执行命令并返回输出结果的字符串，如果出现异常则返回空字符串
    private fun exec(cmd:String):String{
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

