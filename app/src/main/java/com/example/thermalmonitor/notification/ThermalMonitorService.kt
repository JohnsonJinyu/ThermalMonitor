package com.example.thermalmonitor.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.example.thermalmonitor.MainActivity
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.R
import com.example.thermalmonitor.overview.DataCaptureViewModel
import timber.log.Timber

class ThermalMonitorService : LifecycleService() {


    // 定义一个常量用来表示
    //private var isRecording = false
    private lateinit var viewModel: DataCaptureViewModel
    private val START_REQUEST_CODE = 2
    private val STOP_REQUEST_CODE = 3
    private lateinit var notification : Notification
    /**
     * 这个服务类的onCreate方法
     * */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Log.d("ThermalMonitorService", "Service is being created.") // 添加日志输出
        createNotificationChannel()


        // 初始化 viewModel
        viewModel = (applicationContext as MyApp).dataCaptureViewModel

        // 开始观察 LiveData
        observeElapsedTime()



        // 动态注册广播接收器
        val filterStart = IntentFilter("START")
        registerReceiver(ActionStartReceiver(),filterStart, RECEIVER_NOT_EXPORTED)
        val filterStop = IntentFilter("STOP")
        registerReceiver(ActionStopReceiver(),filterStop, RECEIVER_NOT_EXPORTED)




    }


    /**
     * 创建通知渠道的方法,在`Service`的`onCreate`方法中调用
     * */

    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            "channel_id_1",
            "normal notification",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description = "used for monitor and control capture data "

        val notificationManager = getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)
    }


    /**
     * 前台服务控制，在service的onStartCommand中设置前台服务
     * */

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // 调用创建通知渠道的方法
        Log.d("ThermalMonitorService", "Service received start command.") // 添加日志输出
        //createNotificationChannel()
        // 注册广播接收器
        registerReceivers();
        // 创建通知
        createNotification()

        // 将当前服务设置为前台服务
        startForeground(1,notification)

        //示如果服务因为内存不足而被系统销毁，系统将尝试重新创建服务
        return START_STICKY

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun  registerReceivers() {
        val filterStart = IntentFilter("START")
        registerReceiver(ActionStartReceiver(), filterStart, RECEIVER_NOT_EXPORTED)
        val filterStop = IntentFilter("STOP")
        registerReceiver(ActionStopReceiver(), filterStop, RECEIVER_NOT_EXPORTED)
    }



    private fun createNotification(){

        /**
         * 创建触发intentStart的PendingIntent
         * */
        val intentStart = Intent(this, ThermalMonitorService::class.java).apply {
            action = "START"
        }
        val pendingIntentStart = PendingIntent.getForegroundService(
            this,
            START_REQUEST_CODE,
            intentStart,
            PendingIntent.FLAG_IMMUTABLE
        )

        /**
         * 创建触发intentStop的PendingIntent
         * */
        val intentStop = Intent("STOP")
        val pendingIntentStop: PendingIntent = PendingIntent.getBroadcast(
            this,
            3, // 请求码
            intentStop,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        /**
         * 定义并创建通知
         * */
        // 创建NotificationCompat.Builder对象，这里使用NotificationCompat.Builder 类创建一个通知构建器实例。
        // 第一个参数 this 是当前的上下文（服务），第二个参数 "music_channel_id" 是之前创建的通知渠道的ID。
        val builder = NotificationCompat.Builder(this, "channel_id_1")


        // 设置通知的内容
         notification = builder
            .setContentTitle("ThermalMonitor is running")   // 设置通知标题
            .setContentText("00:00:00")             // 设置通知内容
            .setSmallIcon(R.drawable.tm_main_icon)  // 设置通知的小图标
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )      // 设置通知的意图，即点击通知后会执行的动作
            .setVibrate(null)                       // 设置禁用震动
            .setAutoCancel(false)                   // 设置用户点击后不会自动取消
            .addAction(R.drawable.noti_start,"START",pendingIntentStart)
            .addAction(R.drawable.noti_stop,"STOP",pendingIntentStop)

            .build()                                // 调用 build 方法后，会根据之前的设置生成一个 Notification 对象
        Log.d("create notification state","Create Success")


    }



    /**
     * 停止前台服务，当服务不再需要运行在前台的时候
     * */
    private fun stopForeground() {
        stopForeground(true)
        stopSelf()
    }


    /**
     * 更新通知的方法
     * */

    // 根据当前状态更新通知






    // 开始捕获数据的方法
    private fun startCapturingData() {
        // 启动数据捕获的逻辑
        viewModel.startDataCapture()
    }

    // 停止捕获数据的方法
    private fun stopCapturingData() {
        // 停止数据捕获的逻辑
        viewModel.stopDataCapture()
    }


    /**
     *  用于观察DataCaptureViewModel中的timer以更新时长
     * */
    private fun observeElapsedTime() {
        viewModel.timer.observe(this, Observer { elapsedTime ->
            // 更新通知内容
            updateNotificationContent(elapsedTime)
        })
    }



    // 更新通知内容的方法
    private fun updateNotificationContent(elapsedTime: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val builder = NotificationCompat.Builder(this, "channel_id_1")
            .setContentTitle("ThermalMonitor is running")
            .setContentText(elapsedTime)
            .setSmallIcon(R.drawable.tm_main_icon)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVibrate(null)
            .setAutoCancel(false)

        val notification = builder.build()
        startForeground(1, notification)

        notificationManager.notify(1, builder.build())
    }




    override fun onDestroy() {
        super.onDestroy()

        stopForeground()
        // 取消 LiveData 观察
        viewModel.timer.removeObservers(this)

        // 取消注册广播接收器
        unregisterReceiver(ActionStartReceiver())
        unregisterReceiver(ActionStopReceiver())
    }



    inner class ActionStartReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("START" == intent.action ){
                // 处理Start的逻辑
                viewModel.startDataCapture()
                Log.d("ActionStartReceiver","开始按钮已经按下")
            }
        }
    }

    inner class ActionStopReceiver :BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if ("STOP" ==intent.action ){
                // 处理Stop的逻辑
                viewModel.stopDataCapture()
                Timber.tag("ActionStopReceiver").d("停止按钮已经按下")

            }
        }

    }
}

