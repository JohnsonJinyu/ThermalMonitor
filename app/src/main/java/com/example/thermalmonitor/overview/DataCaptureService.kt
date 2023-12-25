package com.example.thermalmonitor.overview

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.thermalmonitor.MainActivity

class DataCaptureService : Service()  {
    //在这里实现数据抓取和其他必要的功能

    private lateinit var notificationBuilder: NotificationCompat.Builder

    // 定义一个变量去判定是否在抓取状态，默认false
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = createNotificationBuilder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        while (!isRecording)
        {
            // 在这里启动你的数据抓取逻辑,将服务设置为一个前台服务，并显示一个通知
            startDataCapture()
            showNitification()

            isRecording = true
        }
        return START_STICKY
    }

    private fun showNitification() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID,notification)
    }


    override fun onDestroy() {
        // 在服务销毁时停止数据抓取逻辑，并移除通知
        stopDataCapture()
        stopForeground(true)
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        // 创建通知并设置必要的信息，例如标题、内容、图标等
        // 可以使用 NotificationCompat.Builder 类来构建通知
        // 设置通知的点击行为，例如点击跳转到应用的某个界面

        return notificationBuilder.build()
    }

    private fun createNotificationBuilder(): NotificationCompat.Builder {
        // 创建通知构建器
        val channelId = "ForegroundServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("前台服务")
            .setContentText("正在运行")
            .setSmallIcon(androidx.core.R.drawable.notification_icon_background)
            .setContentIntent(getPendingIntent())
            .setTicker("前台服务正在运行")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

    private fun getPendingIntent(): PendingIntent {
        // 设置通知的点击行为，例如点击跳转到应用的某个界面
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }


    private fun startDataCapture() {
        // 开始数据抓取逻辑


    }
    private fun stopDataCapture() {
        // 停止数据抓取逻辑
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }




}