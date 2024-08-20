package com.example.thermalmonitor

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import com.example.thermalmonitor.overview.DataCaptureViewModel

class ThermalMonitorService : LifecycleService() {


    // 定义一个常量用来表示
    private var isRecording = false
    private lateinit var viewModel: DataCaptureViewModel


    /**
     * 这个服务类的onCreate方法
     * */
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 初始化 viewModel
        viewModel = (applicationContext as MyApp).dataCaptureViewModel
        // 开始观察 LiveData
        observeElapsedTime()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // 调用创建通知渠道的方法
        createNotificationChannel()

        /**
         * 定义并创建通知
         * */
        // 创建NotificationCompat.Builder对象，这里使用NotificationCompat.Builder 类创建一个通知构建器实例。
        // 第一个参数 this 是当前的上下文（服务），第二个参数 "music_channel_id" 是之前创建的通知渠道的ID。
        val builder = NotificationCompat.Builder(this, "channel_id_1")

        // 根据当前状态设置按钮图标和文本
        val action = if (isRecording) "STOP" else "START"
        builder.addAction(
            NotificationCompat.Action(
                if (isRecording) R.drawable.noti_stop else R.drawable.noti_start,
                action,
                createActionPendingIntent(action)
            )
        )


        // 设置通知的内容
        val notification = builder
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
            .build()                                // 调用 build 方法后，会根据之前的设置生成一个 Notification 对象

        //startForeground 方法用于将服务置于前台状态，并显示传入的通知。
        // 第一个参数 1 是通知的ID，第二个参数是之前构建的通知对象
        startForeground(1, notification)



        //示如果服务因为内存不足而被系统销毁，系统将尝试重新创建服务
        return START_STICKY

    }


    /**
     * START 和 STOP 共享的Intent
     * */
    private fun createActionPendingIntent(action: String): PendingIntent? {
        return PendingIntent.getBroadcast(
            this, // 当前的上下文
            action.hashCode(), // 使用动作的哈希值作为请求码
            Intent(this, ControlReceiver::class.java).apply {
                // 使用动作名称作为 Intent 的 action
                this.action = action
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }



    /**
     * 停止前台服务，当服务不再需要运行在前台的时候
     * */
    fun stopForeground() {
        stopForeground(true)
        stopSelf()
    }


    /**
     * 更新通知的方法
     * */

    // 根据当前状态更新通知
    fun updateNotification() {
        val action = if (isRecording) "STOP" else "START"
        val actionIcon = if (isRecording) R.drawable.noti_stop else R.drawable.noti_start
        val actionTitle = if (isRecording) "STOP" else "START"

        val builder = NotificationCompat.Builder(this, "channel_id_1")
            // ... 现有设置 ...
            .addAction(
                NotificationCompat.Action(
                    actionIcon,
                    actionTitle,
                    createActionPendingIntent(action)
                )
            )

        val notification = builder.build()
        startForeground(1, notification) // 更新前台服务的通知
    }


    // ControlReceiver 作为内部类
    inner class ControlReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                "START" -> {
                    isRecording = true // 标记为正在录制
                    // 触发开始录制的逻辑
                    // 例如，您可以在这里调用一个方法来启动数据捕获
                    startCapturingData()
                }

                "STOP" -> {
                    isRecording = false // 标记为停止录制
                    // 触发停止录制的逻辑
                    // 例如，您可以在这里调用一个方法来停止数据捕获
                    stopCapturingData()
                }
            }
            // 更新通知以反映新的状态
            updateNotification()
        }
    }


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
    }




    override fun onDestroy() {
        super.onDestroy()
        // 取消 LiveData 观察
        viewModel.timer.removeObservers(this)
    }


}

