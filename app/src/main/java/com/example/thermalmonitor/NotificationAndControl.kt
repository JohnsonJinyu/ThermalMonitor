package com.example.thermalmonitor

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
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

    private val notificationID = 100
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var isCapturingData = false // track the capture state

    //Method to create and show notification
    fun createNotification() {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager




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
    private fun updateNotificationAction() {
        val startIntent = Intent(context, MainActivity.NotificationActionReceiver::class.java).setAction("start")
        val startPendingIntent = PendingIntent.getBroadcast(
            context, 0, startIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MainActivity.NotificationActionReceiver::class.java).setAction("stop")
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder.addAction(
            R.drawable.noti_start, "START", startPendingIntent
        )
        notificationBuilder.addAction(
            R.drawable.noti_stop, "STOP", stopPendingIntent
        )

        notificationManager.notify(notificationID, notificationBuilder.build())
    }


    //Public method to toggle capture state
    fun toggleCapture(){
        isCapturingData = !isCapturingData
        ensureInitialized()
        updateNotificationAction()
        notificationManager.notify(notificationID,notificationBuilder.build())
    }

    private fun ensureInitialized() {
        if (!this::notificationBuilder.isInitialized) {
            createNotification() // 确保先调用这个方法
        }
    }

}