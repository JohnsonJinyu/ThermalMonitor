package com.example.thermalmonitor

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.thermalmonitor.overview.DataCaptureViewModel
import com.example.thermalmonitor.overview.OverViewFragment


/**
 * 在NotificationAndControl这个类中
  1、 创建通知：定义一个方法，创建一个通知，设置默认内容为"00:00:00",标题为"ThermalMonitor is running",一个按钮，状态为”START“ 和 ”STOP“
  切换；这个创建通知的方法在MainActivity的onCreate()中调用
  2、 更新通知的方法：创建一个更新通知的方法，这个方法在通知栏中更新时间显示，在OverViewFragment类中的
  viewModel.timer.observe(viewLifecycleOwner) { timeString ->
  binding.tvTimer.text = timeString
  }
  这个方法下调用赐个更新通知的方法同步更新时间；

  3、 通知按钮点击事件：
    点击“START"按钮后，触发OverViewFragment类中的公共的开始数据捕获方法startDataCapture(),且按钮切换为"STOP"按钮
    点击“STOP"按钮后，触发OverViewFragment类中的公共的停止数据捕获方法stopDataCapture(),且按钮切换为"START"按钮
   点击通知，进入App页面
 * */

class NotificationAndControl (private val context: Context){

    val notificationID = 100
    private lateinit var notificationManager: NotificationManager
    lateinit var notificationBuilder: NotificationCompat.Builder


    private val viewModel: DataCaptureViewModel


    init {
        val myApp = context.applicationContext as MyApp
        viewModel = myApp.dataCaptureViewModel
    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() {
        val name = context.getString(R.string.channel_name) // 渠道名字
        val descriptionText = context.getString(R.string.channel_description) // 渠道描述
        val importance = NotificationManager.IMPORTANCE_MAX // 重要性级别
        val channel = NotificationChannel("channel_id", name, importance).apply {
            description = descriptionText
        }
        // 注册通道
        notificationManager.createNotificationChannel(channel)
    }


    //Method to create and show notification
    fun createNotification() {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 确保通知渠道已创建
        createNotificationChannel()

        //Create initial notification
        notificationBuilder = NotificationCompat.Builder(context, "channel_id")
            .setSmallIcon(R.drawable.tm_main_icon)
            .setContentTitle("ThermalMonitor is running")
            .setContentText("00:00:00")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setContentIntent(createPendingIntent())

        // setting ui action buttion to toggle between start and stop
        updateNotificationAction()

        notificationManager.notify(notificationID, notificationBuilder.build())
    }


    // 更新通知内容文本
    fun updateNotification(timeString: String) {

        ensureInitialized()
        // 使用现有实例更新内容文本
        notificationBuilder.setContentText(timeString)

        updateNotificationAction()  // 确保动作是最新的

        // 通知更新
        notificationManager.notify(notificationID, notificationBuilder.build())
    }



    //Method to create the pending intent for MainActivity
    private fun createPendingIntent(): PendingIntent {
        // Use MainActivity的Intent，然后使用FLAG_ACTIVITY_NO_ANIMATION标志避免动画效果
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            // 这里添加任何需要传递给MainActivity的额外数据
        }
        // 创建PendingIntent
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    //Method to update notification button action
    @SuppressLint("RestrictedApi")
    //Method to update notification button action
    // NotificationAndControl 中的 updateNotificationAction 方法
    fun updateNotificationAction() {
        // 清除之前设置的动作
        notificationBuilder.mActions.clear()

        // 创建一个指向 ThermalMonitorService 的 Intent
        val actionServiceIntent = Intent(context, ThermalMonitorService::class.java)
        actionServiceIntent.action = if (viewModel.isRecording) "stop" else "start"

        // 创建一个 PendingIntent，用于唤醒 ThermalMonitorService
        val actionPendingIntent = PendingIntent.getService(
            context,
            if (viewModel.isRecording) 1 else 0, // 使用不同的请求码区分动作
            actionServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 添加动作按钮到通知
        if (viewModel.isRecording) {
            notificationBuilder.addAction(
                R.drawable.noti_stop, "STOP", actionPendingIntent
            )
        } else {
            notificationBuilder.addAction(
                R.drawable.noti_start, "START", actionPendingIntent
            )
        }

        // 更新通知
        notificationManager.notify(notificationID, notificationBuilder.build())
    }







    private fun ensureInitialized() {
        if (!this::notificationBuilder.isInitialized) {
            createNotification() // 确保先调用这个方法
        }
    }

}