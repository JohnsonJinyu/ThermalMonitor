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
import androidx.core.app.NotificationCompat
import com.example.thermalmonitor.MainActivity
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.R
import com.example.thermalmonitor.overview.DataCaptureViewModel

class ThermalMonitorService : Service() {


    companion object {
        const val CHANNEL_ID = "my_channel_01" // 通知渠道ID
        const val ACTION_START = "com.example.thermalmonitor.ACTION_START"
        const val ACTION_STOP = "com.example.thermalmonitor.ACTION_STOP"
    }


    private lateinit var dataCaptureViewModel: DataCaptureViewModel


    // onCreate() 在服务首次创建时调用
    override fun onCreate() {
        super.onCreate()

        // 获取application 实例
        val myAPP = applicationContext as MyApp
        // 获取DataCaptureViewModel实例
        dataCaptureViewModel = myAPP.dataCaptureViewModel



        // 创建通知渠道
        createNotificationChannel()
        startForegroundService()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when(intent?.action){
            ACTION_START -> {
                Log.i("Button State","START BUTTON PRESSED")
                dataCaptureViewModel.startDataCapture()

            }
            ACTION_STOP -> {
                Log.i("Button State","STOP BUTTON PRESSED")
                dataCaptureViewModel.stopDataCapture()
            }
        }

        return START_STICKY
    }


    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }


    private fun createNotification(){

        // back to activity
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)



        // start button intent
        val startIntent = Intent(this,ThermalMonitorService::class.java).apply {
            action = ACTION_START
        }
        val startPendingIntent = PendingIntent.getService(this,1,startIntent,PendingIntent.FLAG_IMMUTABLE)


        // stop button intent
        val stopIntent = Intent(this,ThermalMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this,2,stopIntent,PendingIntent.FLAG_IMMUTABLE)



        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ThermalMonitor Service Is Running")
            .setContentText("00:00:00")
            .setSmallIcon(R.drawable.tm_main_icon)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.noti_start, "START", startPendingIntent)
            .addAction(R.drawable.noti_stop,"STOP",stopPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)

            .build()

        startForeground(1,notification)
    }

    private fun startForegroundService(){

        createNotification()

    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


}

