package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thermalmonitor.R
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import timber.log.Timber
import java.util.Date
import java.util.Locale

// 定义一个接口


class DataCaptureViewModel(
    private val batteryViewModel: BatteryViewModel,
    private val thermalViewModel: ThermalViewModel,
    private val socViewModel: SocViewModel,
    private val dataProcessor: DataProcessToSave,
    @SuppressLint("StaticFieldLeak") private val context: Context
) : ViewModel() {

    // 定义一个变量去判定是否在抓取状态，默认false
    var isRecording = false

    // 定义一个变量去管理开始或停止抓取数据
    val action = MutableLiveData<String>()


    // 创建 LiveData 对象来存储时间的值
    private val _timer = MutableLiveData<String>()
    val timer: LiveData<String>
        get() = _timer
    // 使用一个可变的变量来存储计时器的值
    private var currentTime = 0

    // a job to run the coroutine for recording and updating UI
    private var job: Job? = null

    // a two-dimensional array to store the data, default is empty
    private var timeDataArray = arrayOf<Array<String>>()
    // 分别给battery，thermal，soc定义二维数组去存储数据
    private var batteryDataStoreArray = arrayOf<Array<String>>()
    private var thermalDataStoreArray = arrayOf<Array<String>>()
    private var socDataStoreArray = arrayOf<Array<String>>()


    // 创建 MutableLiveData 对象来存储三个 CheckBox 的状态
    private val _cbBatteryState = MutableLiveData<Boolean>()
    val cbBatteryState: LiveData<Boolean>
        get() = _cbBatteryState

    private val _cbThermalState = MutableLiveData<Boolean>()
    val cbThermalState: LiveData<Boolean>
        get() = _cbThermalState

    private val _cbSocState = MutableLiveData<Boolean>()
    val cbSocState: LiveData<Boolean>
        get() = _cbSocState



    // 用于MVVM架构中弹窗提醒
    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String>
        get() = _toastMessage

    private fun showToast(message: String) {
        _toastMessage.value = message
    }


    private val _showAbortDialog = MutableLiveData<Unit>()
    val showAbortDialog: LiveData<Unit>
        get() = _showAbortDialog



    /**
     * 开始数据抓取的相关逻辑部分
     * */
    fun startDataCapture() {
        if (!isRecording)
        {

            isRecording = true


            job = viewModelScope.launch {
                //showToast("已开始记录！")
                Toast.makeText(context,"已开始记录！",Toast.LENGTH_SHORT).show()



                currentTime = 0 //重置计时器的值
                _timer.value = "00:00:00"  //更新 LiveData 对象


                while (isRecording) {
                    // 这里放置数据抓取和更新 UI 的逻辑
                    val allDataList = getDataFromLiveData()
                    //同时需要将返回的list类型的数据转为array 方便后续的处理
                    val batteryRow = allDataList[0].toTypedArray()
                    val thermalRow = allDataList[1].toTypedArray()
                    val socRow = allDataList[2].toTypedArray()
                    val timeRow = allDataList[3].toTypedArray()

                    batteryDataStoreArray += batteryRow
                    thermalDataStoreArray += thermalRow
                    socDataStoreArray += socRow
                    timeDataArray += timeRow // 专门用于时间戳的数据记录 ，用于文件名的处理

                    currentTime++
                    val timeString = formatTime(currentTime)
                    _timer.value = timeString  // fragment中的观察者会收到通知并更新UI



                    //打印日志
                    Timber.tag("DATA").d(
                        """ 
                            batteryRow: ${batteryRow.contentDeepToString()}
                            thermalRow: ${thermalRow.contentDeepToString()} 
                            socRow: ${socRow.contentDeepToString()}
                            """.trimIndent()  //用 trimIndent() 去掉前置空格
                    )

                    delay(1000)
                }
            }
        }
        else
        {
            showToast("正在记录中，请勿重复点击")
        }
    }




    /**
     * 中止按钮的点击事件
     * */
    fun onAbortButtonClicked() {
        if (isRecording) {
            _showAbortDialog.value = Unit  // Unit表示信号
        } else {
            showToast("未开始记录，请先点击开始按钮")
        }
    }
    /**
     * 点击中止按钮后执行的操作
     * */
    fun abortDataCapture() {
        isRecording = false // stop recording state
        job?.cancel() // cancel the coroutine
        currentTime = 0 // reset the timer to zero
        // 重置其他数据
        batteryDataStoreArray = arrayOf()
        thermalDataStoreArray = arrayOf()
        socDataStoreArray = arrayOf()
        timeDataArray = arrayOf()
    }


    /**
     * 数据抓取停止的逻辑
     * */
    fun stopDataCapture() {
        if (isRecording) {
            isRecording = false
            job?.cancel()
            currentTime = 0

            /**
             * 需要将时间戳的字符串转换为 Date 对象，然后再进行格式化。
             * 可以使用 SimpleDateFormat 将时间戳的字符串解析为 Date 对象，然后再格式化为你想要的格式。
             * */
            val startTime = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(
                SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(timeDataArray[0][0])
            )
            val endTime = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            val fileName = "TMData-$startTime-$endTime.xlsx"

            // save data to excel file and get a boolean result, and pass the fileName as a parameter
            val result = saveDataToExcel(fileName)

            if (result) {

                //停止后清除各个数组的数据
                batteryDataStoreArray = arrayOf()
                thermalDataStoreArray = arrayOf()
                socDataStoreArray = arrayOf()
                timeDataArray = arrayOf()

                // 弹窗提示保存成功以及保存路径
                showToast("数据保存成功，保存路径：${Environment.DIRECTORY_DOWNLOADS}/ThermalMonitor/$fileName")

            }
        }
        else
        {
            showToast("未开始记录，请点击开始按钮")
        }
    }

    /**
     * A function to save data to excel file and return a boolean result.
     * @param fileName the file name to save the data
     */
    private fun saveDataToExcel(fileName: String): Boolean {
        return try {
            val workbook = XSSFWorkbook() // create a workbook object

            // 如果电池的checkBox是Checked的状态，则对数据进行处理
            if (cbBatteryState.value == true)
            {
                dataProcessor.processBatteryData(workbook, batteryDataStoreArray)
            }

            // 如果温度的checkBox是checked的状态，则对数据进行处理
            if (cbThermalState.value == true)
            {
                dataProcessor.processThermalData(workbook,thermalDataStoreArray)
            }

            // 如果芯片的checkBox是checked的状态，则对数据进行处理
            if (cbSocState.value == true)
            {
                dataProcessor.processSocData(workbook,socDataStoreArray)
            }


            // 指定文件的MIME类型
            val mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            // 获取文件的相对路径，这里指定为 files/ThermalMonitor 目录
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ThermalMonitor"

            // 创建一个 ContentValues 对象，用于设置文件的属性
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                put(MediaStore.Downloads.IS_PENDING, 1) // 设置为待处理状态，防止其他应用访问
            }

            // 通过调用 requireContext() 方法获取到对应的 Context，然后使用它的 contentResolver 属性
            // 通过 contentResolver 向 MediaStore 插入一条新记录，返回一个 Uri 对象
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

            // 通过 contentResolver 打开一个输出流，写入文件内容
            if (uri != null) {
                context.contentResolver.openOutputStream(uri).use { os ->
                    workbook.write(os) // 将 workbook 写入输出流
                }
            }

            // 更新 ContentValues 对象，将文件状态设置为可用
            values.clear()

            values.put(MediaStore.Downloads.IS_PENDING, 0)
            // 通过 contentResolver 更新 MediaStore 中的记录
            if (uri != null) {
                context.contentResolver.update(uri, values, null, null)
            }
            // 关闭 workbook
            workbook.close()

            true  // 文件保存成功后返回 true

        } catch (e: Exception) {
            e.printStackTrace()
            Timber.tag("SAVE_DATA").e("保存数据失败，原因：%s", e.message) // print the exception message to the logcat
            false // return false if any exception occurs
        }


    }









    /**
     * A function to get data from live data and return a one-dimensional array.
     * 获取listData的数据，并最终返回一个包含所有可变列表的大列表
     */
    private fun getDataFromLiveData(): List<List<String>> {

        // 创建单独的时间可变列表用于文件名时间
        val singleTimeMutableList =
            mutableListOf<String>() // create a mutable list to store the result
        // 分别创建三个数据的可变列表
        val batteryDataMutableList = mutableListOf<String>()
        val thermalDataMutableList = mutableListOf<String>()
        val socDataMutableList = mutableListOf<String>()

        // get the current time as a string in HHmmss format
        val timeString = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

        // create a SimpleDateFormat object for the target format,这个格式是用于excel中的时间戳
        val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // add the time string to the singleTimeMutableList
        singleTimeMutableList.add(timeString)

        /**
         * 根据checkBox的状态，来保存数据到可变列表
         * */

        if (_cbBatteryState.value == true) { // if battery is checked, get data from batteryData and add to the result list
            val battery = batteryViewModel.batteryData.value
            if (battery != null) { // check if battery is not null
                //batteryDataMutableList.add(timeString) // 首个添加时间戳
                batteryDataMutableList.add(timeStamp) // 首个添加时间戳
                batteryDataMutableList.add(battery.level.toString())
                batteryDataMutableList.add(battery.status)
                batteryDataMutableList.add(battery.current.toString())
                batteryDataMutableList.add(battery.temperature.toString())
                batteryDataMutableList.add(battery.voltage.toString())
                batteryDataMutableList.add(battery.source)
            }
        }

        if (_cbThermalState.value == true) { // if thermal is checked, get data from thermalList and add to the result list
            val thermal = thermalViewModel.thermalList.value
            if (thermal != null) { // check if thermal is not null
                thermalDataMutableList.add(timeStamp) // 首个添加时间戳
                for (t in thermal) {
                    thermalDataMutableList.add(t.temp)
                }
            }
        }

        if (_cbSocState.value == true) { // if soc is checked, get data from dynamicInfo and add to the result list
            val soc = socViewModel.dynamicInfo.value
            if (soc != null) { // check if soc is not null
                socDataMutableList.add(timeStamp) // 首个添加时间戳
                for (s in soc) {
                    socDataMutableList.add(s.coreFrequency.toString())
                }
            }
        }

        // 最终返回一个包含所有列表的大列表
        return listOf(
            batteryDataMutableList,
            thermalDataMutableList,
            socDataMutableList,
            singleTimeMutableList

        )
    }


    /**
     * 用于更新checkbox 的 livedata的状态
     * */
    fun updateCheckBoxBattery(newState: Boolean) {
        _cbBatteryState.value = newState
    }
    fun updateCheckBoxThermal(newState: Boolean) {
        _cbThermalState.value = newState
    }
    fun updateCheckBoxSoc(newState: Boolean) {
        _cbSocState.value = newState
    }


    // 格式化时间为字符串
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

}