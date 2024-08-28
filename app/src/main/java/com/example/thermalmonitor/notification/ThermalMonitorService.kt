package com.example.thermalmonitor.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.thermalmonitor.MainActivity
import com.example.thermalmonitor.R

class ThermalMonitorService : Service() {


    companion object {
        const val CHANNEL_ID = "my_channel_01" // 通知渠道ID
    }

    private lateinit var notificationManager: NotificationManager


    // onCreate() 在服务首次创建时调用
    override fun onCreate() {
        super.onCreate()
        Log.i("MyForegroundService", "Service created")
        // 初始化服务所需的资源
        notificationManager = getSystemService(NotificationManager::class.java)
    }



    // onBind() 用于返回服务的通信组件，对于前台服务通常返回null
    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    // onStartCommand() 在服务启动时调用，用于处理启动请求
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("MyForegroundService", "Service received start id $startId: $intent")

        // 创建通知渠道，适用于 Android O（API 级别 26）及以上版本
        createNotificationChannel()

        // 创建通知
        val notification = createNotification()

        // 将服务设置为前台服务
        startForeground(startId, notification)



        // 返回 START_STICKY，表示服务被系统杀死后会尝试重新创建
        return START_STICKY
    }

    // 创建通知渠道
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name) // 渠道名称
        var description = getString(R.string.channel_description) // 渠道描述
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = description
        }
        notificationManager.createNotificationChannel(channel)
    }

    // 创建通知
    private fun createNotification(): Notification {
        // 创建一个PendingIntent，点击通知时将启动MainActivity
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 创建一个PendingIntent,用于通知中的开始按钮
        val intentStart = Intent(this,MainActivity::class.java).apply {
            action = "com.example.thermalmonitor.ACTION_START"
        }
        val pendingIntentStart = PendingIntent.getBroadcast(
            this, 0, intentStart,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建一个PendingIntent ，用于通知中的停止按钮
        val intentStop = Intent(this,MainActivity::class.java).apply {
            action = "com.example.thermalmonitor.ACTION_STOP"
        }
        val pendingIntentStop = PendingIntent.getBroadcast(
            this,1,intentStop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )




        // 构建通知
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service") // 通知标题
            .setContentText("This is a foreground service.") // 通知内容
            .setSmallIcon(R.drawable.noti_start) // 通知小图标
            .setContentIntent(pendingIntent) // 通知内容Intent
            .addAction(R.drawable.noti_start,"START",pendingIntentStart)  // add start button
            .addAction(R.drawable.noti_stop,"STOP",pendingIntentStop)  // add stop button
            .setOngoing(true) // 设置为持续通知
            .build()
    }




    // onDestroy() 在服务被销毁时调用
    override fun onDestroy() {
        super.onDestroy()
        Log.i("MyForegroundService", "Service destroyed")
        // 清理服务资源
    }

}

