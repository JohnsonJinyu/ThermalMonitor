package com.example.thermalmonitor
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.thermalmonitor.overview.DataCaptureViewModel

class ThermalMonitorService : Service(), LifecycleOwner {

    companion object {
        const val ACTION_START_FOREGROUND = "com.example.thermalmonitor.START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.example.thermalmonitor.STOP_FOREGROUND"
    }

    private lateinit var notificationAndControl: NotificationAndControl
    private lateinit var notificationManager: NotificationManagerCompat

    // 定义DataCaptureViewModel的实例变量
    private lateinit var viewModel: DataCaptureViewModel


    inner class LocalBinder : Binder() {
        fun getService(): ThermalMonitorService = this@ThermalMonitorService
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        // 获取Application实例
        val myApp = application as MyApp
        // 从MyApp获取DataCaptureViewModel的实例
        viewModel = myApp.dataCaptureViewModel
        notificationManager = NotificationManagerCompat.from(this)
        // 创建前台服务的通知
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService() // 确保前台服务启动
        intent?.action?.let { action ->
            handleNotificationAction(action)
        }
        return START_STICKY
    }


    private fun handleNotificationAction(action: String) {
        when (action) {
            "start" -> startCapture()
            "stop" -> stopCapture()
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        notificationAndControl = NotificationAndControl(this)
        notificationAndControl.createNotification()
        startForeground(notificationAndControl.notificationID, notificationAndControl.notificationBuilder.build())

        viewModel.timer.observe(this, Observer { timeString ->
            // 直接在服务中更新通知
            //updateNotificationTime(timeString)
            // 当需要更新通知时
            notificationAndControl.updateNotification(timeString)
        })
    }

    private fun stopForegroundService() {
        notificationManager.cancel(notificationAndControl.notificationID)
    }


    fun startCapture() {
        viewModel.startDataCapture()
        notificationAndControl.updateNotificationAction()
    }

    fun stopCapture() {
        viewModel.stopDataCapture()
        notificationAndControl.updateNotificationAction()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止前台服务
        stopForegroundService()
    }

    // 待确认
    override val lifecycle: Lifecycle
        get() = TODO("Not yet implemented")
}

