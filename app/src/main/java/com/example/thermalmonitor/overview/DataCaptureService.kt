package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.thermalmonitor.MainActivity
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.R
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import timber.log.Timber
import java.util.Date
import java.util.Locale

class DataCaptureService(): Service() {


    companion object {
        const val CHANNEL_ID = "thermal_monitoring_channel"
        const val ACTION_START = "com.example.thermalmonitor.ACTION_START_CAPTURE"
        const val ACTION_STOP = "com.example.thermalmonitor.ACTION_STOP_CAPTURE"

    }



    private var isRecording = false
    private var job: Job? = null


    private lateinit var dataCaptureViewModel: DataCaptureViewModel

    private lateinit var batteryViewModel: BatteryViewModel
    private lateinit var thermalViewModel: ThermalViewModel
    private lateinit var socViewModel: SocViewModel

    // 使用一个可变的变量来存储计时器的值
    private var currentTime = 0


    // 定义一个时间戳
    private var timestamp = "00:00:00"




    // a two-dimensional array to store the data, default is empty
    private var timeDataArray = arrayOf<Array<String>>()
    // 分别给battery，thermal，soc定义二维数组去存储数据
    private var batteryDataStoreArray = arrayOf<Array<String>>()
    private var thermalDataStoreArray = arrayOf<Array<String>>()
    private var socDataStoreArray = arrayOf<Array<String>>()


    private lateinit var pendingIntent: PendingIntent
    private lateinit var startPendingIntent: PendingIntent
    private lateinit var stopPendingIntent: PendingIntent


    // 定义 Binder
    inner class LocalBinder : Binder() {
        // 返回当前服务的实例
        fun getService(): DataCaptureService = this@DataCaptureService
    }

    // 定义一个变量来持有 Binder 的实例
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }





    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action){
            ACTION_START -> {
                Log.i("Button State","START BUTTON PRESSED")
                startDataCapture()

            }
            ACTION_STOP -> {
                Log.i("Button State","STOP BUTTON PRESSED")
                stopDataCapture()
            }
        }
        return START_STICKY
    }



    override fun onCreate() {
        super.onCreate()

        // 获取application 实例
        val myAPP = applicationContext as MyApp
        // 获取DataCaptureViewModel实例
        dataCaptureViewModel = myAPP.dataCaptureViewModel


        // 获取BatteryViewModel实例
        batteryViewModel = myAPP.batteryViewModel
        thermalViewModel = myAPP.thermalViewModel
        socViewModel = myAPP.socViewModel






        // 观察DataCaptureViewModel中timer的值，并更新通知
        dataCaptureViewModel.timer.observeForever { newTimestamp ->
            updateTimestamp(newTimestamp)

        }


        // 创建各个PendingIntent
        pendingIntent = createPendingIntent()
        startPendingIntent = createStartPendingIntent()
        stopPendingIntent = createStopPendingIntent()

        // 创建通知渠道
        createNotificationChannel()
        startForegroundService()
    }


    /**
     * 启动一个协程，调用方法抓取数据
     * */
    fun startDataCapture() {

        // 检查是否至少选择了一个数据项
        if(dataCaptureViewModel.cbBatteryState.value == false &&
            dataCaptureViewModel.cbSocState.value == false &&
            dataCaptureViewModel.cbThermalState.value == false){

                Toast.makeText(this, "请至少选择一个数据项", Toast.LENGTH_SHORT).show()
                return
        }
        else if (!isRecording)
        {
            // 创建一个协程，调用方法抓取数据

                isRecording = true

                job = CoroutineScope(Dispatchers.IO).launch {

                    //showToast("已开始记录！")
                    //Toast.makeText(this@DataCaptureService, "已开始记录！", Toast.LENGTH_SHORT).show()

                    currentTime = 0 //重置计时器的值

                    // 使用 withContext 切换到主线程显示 Toast
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DataCaptureService, "已开始记录！", Toast.LENGTH_SHORT).show()
                    }

                    while (isActive && isRecording) {
                        // 这里放置数据抓取和更新 UI 的逻辑
                        val allDataList = getDataFromLiveData()
                        // 同时需要将返回的list类型的数据转为array 方便后续的处理
                        val batteryRow = allDataList[0].toTypedArray()
                        val thermalRow = allDataList[1].toTypedArray()
                        val socRow = allDataList[2].toTypedArray()
                        val timeRow = allDataList[3].toTypedArray()

                        // 保存数据到数组中
                        // 这里可以添加保存数据的逻辑
                        batteryDataStoreArray += batteryRow
                        thermalDataStoreArray += thermalRow
                        socDataStoreArray += socRow
                        timeDataArray += timeRow // 专门用于时间戳的数据记录 ，用于文件名的处理


                        timestamp = formatTime(++currentTime)
                        // 更新 LiveData
                        withContext(Dispatchers.Main) {
                            dataCaptureViewModel._timer.value = timestamp
                        }
                        updateTimestamp(timestamp)
                        delay(1000)
                    }
                }

        }
        else{
            // 使用 withContext 切换到主线程显示 Toast
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(this@DataCaptureService, "正在记录中，请勿重复点击", Toast.LENGTH_SHORT).show()
            }
        }

    }


    /**
     * 停止抓取数据的方法
     * */
    fun stopDataCapture() {
        if (isRecording) {
            isRecording = false
            job?.cancel()
            currentTime = 0

            Toast.makeText(this, "已停止记录", Toast.LENGTH_SHORT).show()
            /**
             * 需要将时间戳的字符串转换为 Date 对象，然后再进行格式化。
             * 可以使用 SimpleDateFormat 将时间戳的字符串解析为 Date 对象，然后再格式化为你想要的格式。
             * */
            val startTime = android.icu.text.SimpleDateFormat(
                "yyyyMMdd-HHmmss",
                Locale.getDefault()
            ).format(
                android.icu.text.SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(timeDataArray[0][0])
            )
            val endTime = android.icu.text.SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
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
                //showToast("数据保存成功，保存路径：${Environment.DIRECTORY_DOWNLOADS}/ThermalMonitor/$fileName")
                Toast.makeText(this@DataCaptureService,"数据保存成功，保存路径：${Environment.DIRECTORY_DOWNLOADS}/ThermalMonitor/$fileName",Toast.LENGTH_SHORT).show()

            }
        }
        else
        {
            //showToast("未开始记录，请点击开始按钮")
            Toast.makeText(this@DataCaptureService,"未开始记录，请点击开始按钮",Toast.LENGTH_SHORT).show()

        }
    }


    /**
     * 创建通知渠道的方法
     * */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = DataCaptureService.CHANNEL_ID
            val channelName = "Thermal Monitoring Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT // 设置为默认重要性
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channel for thermal monitoring service"
                setSound(null, null) // 禁用声音
                enableVibration(false) // 禁用震动
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC // 确保在锁屏时显示通知
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }




    /**
     * 创建通知的方法
     * */
    private fun createNotification(){

        val notification: Notification = NotificationCompat.Builder(this,
            CHANNEL_ID
        )
            .setContentTitle("ThermalMonitor Service Is Running")
            .setContentText("数据已经抓取 $timestamp")
            .setSmallIcon(R.drawable.tm_main_icon)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .setVibrate(null)
            .addAction(R.drawable.noti_start, "START", startPendingIntent)
            .addAction(R.drawable.noti_stop,"STOP",stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 确保在锁屏时显示通知
            .build()



        startForeground(1,notification)
    }

    /**
     * 定义一个方法，更新通知中的时间,用于在OverViewFragment的timer的观察者中调用
     * 然后更新通知中的timestamp
     * 只更新通知的内容，而不是创建整个通知
     * */
    private fun updateTimestamp(newTimestamp:String){

        // 获取当前的通知管理器
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 创建或者重用 NotificationCompat.Builder 对象
        val notificationBuilder = NotificationCompat.Builder(this, DataCaptureService.CHANNEL_ID)
            .setContentTitle("ThermalMonitor Service Is Running")
            .setContentText("数据已经抓取 $newTimestamp") // 只更新这部分内容
            .setSmallIcon(R.drawable.tm_main_icon)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .setVibrate(null)
            .addAction(R.drawable.noti_start, "START", startPendingIntent)
            .addAction(R.drawable.noti_stop, "STOP", stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 确保在锁屏时显示通知

        // 使用相同的通知ID更新通知
        notificationManager.notify(1, notificationBuilder.build())

    }



    private fun startForegroundService(){

        createNotification()

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
        val timeString = android.icu.text.SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())

        // create a SimpleDateFormat object for the target format,这个格式是用于excel中的时间戳
        val timeStamp = android.icu.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // add the time string to the singleTimeMutableList
        singleTimeMutableList.add(timeString)

        /**
         * 根据checkBox的状态，来保存数据到可变列表
         * */

        if (dataCaptureViewModel.cbBatteryState.value == true) { // if battery is checked, get data from batteryData and add to the result list
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

        if (dataCaptureViewModel.cbThermalState.value == true) { // if thermal is checked, get data from thermalList and add to the result list
            val thermal = thermalViewModel.thermalList.value
            if (thermal != null) { // check if thermal is not null
                thermalDataMutableList.add(timeStamp) // 首个添加时间戳
                for (t in thermal) {
                    thermalDataMutableList.add(t.temp)
                }
            }
        }

        if (dataCaptureViewModel.cbSocState.value == true) { // if soc is checked, get data from dynamicInfo and add to the result list
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
     *
     * 各个pendingIntent的action
     * */
    private fun createPendingIntent(): PendingIntent {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createStartPendingIntent(): PendingIntent {
        val startIntent = Intent(this, DataCaptureService::class.java).apply {
            action = ACTION_START
        }
        return PendingIntent.getService(this, 1, startIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, DataCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        return PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)
    }


    /**
     *  格式化时间为字符串
     * */
    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }



    /**
     * A function to save data to excel file and return a boolean result.
     * @param fileName the file name to save the data
     */
    private fun saveDataToExcel(fileName: String): Boolean {
        return try {
            val workbook = XSSFWorkbook() // create a workbook object

            // 如果电池的checkBox是Checked的状态，则对数据进行处理
            if (dataCaptureViewModel.cbBatteryState.value == true)
            {
                dataCaptureViewModel.dataProcessor.processBatteryData(workbook, batteryDataStoreArray)
            }

            // 如果温度的checkBox是checked的状态，则对数据进行处理
            if (dataCaptureViewModel.cbThermalState.value == true)
            {
                dataCaptureViewModel.dataProcessor.processThermalData(workbook,thermalDataStoreArray)
            }

            // 如果芯片的checkBox是checked的状态，则对数据进行处理
            if (dataCaptureViewModel.cbSocState.value == true)
            {
                dataCaptureViewModel.dataProcessor.processSocData(workbook,socDataStoreArray)
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
            val uri = this@DataCaptureService.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            )

            // 通过 contentResolver 打开一个输出流，写入文件内容
            if (uri != null) {
                this@DataCaptureService.contentResolver.openOutputStream(uri).use { os ->
                    workbook.write(os) // 将 workbook 写入输出流
                }
            }

            // 更新 ContentValues 对象，将文件状态设置为可用
            values.clear()

            values.put(MediaStore.Downloads.IS_PENDING, 0)
            // 通过 contentResolver 更新 MediaStore 中的记录
            if (uri != null) {
                this@DataCaptureService.contentResolver.update(uri, values, null, null)
            }
            // 关闭 workbook
            workbook.close()

            true  // 文件保存成功后返回 true

        } catch (e: Exception) {
            e.printStackTrace()
            Timber.tag("SAVE_DATA").e("保存数据失败，原因：%s", e.message) // print the exception message to the logcat
            Toast.makeText(this@DataCaptureService,"数据保存失败：${e.message}",Toast.LENGTH_SHORT).show() // 显示Toast消息
            false // return false if any exception occurs
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        /*isRecording = false
        job?.cancel()
        wakeLock.release()
        stopForeground(true)*/

    }


}