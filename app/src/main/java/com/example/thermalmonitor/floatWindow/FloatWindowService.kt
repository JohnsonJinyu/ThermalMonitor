package com.example.thermalmonitor.floatWindow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import com.example.thermalmonitor.R


class FloatWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: View

    // 定义一个 Binder 对象
    private val binder = FloatWindowBinder()

    override fun onCreate() {
        super.onCreate()

        // 获取窗口服务
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // 加载悬浮窗布局
        floatView = LayoutInflater.from(this).inflate(R.layout.float_window, null)

        // 设置布局参数
        val params = WindowManager.LayoutParams(
            // 宽高
            WindowManager.LayoutParams.WRAP_CONTENT,
            // 类型
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 设置标志
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        // 显示悬浮窗视图
        windowManager.addView(floatView, params)

    }

    override fun onDestroy() {
        windowManager.removeView(floatView)
        super.onDestroy()
    }

    // 重写 onBind 方法，返回 Binder 对象
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    // 启动悬浮窗的方法
    fun startFloatWindowService() {
        startService(Intent(this, FloatWindowService::class.java))
    }

    // 停止悬浮窗的方法
    fun stopFloatWindowService() {
        stopService(Intent(this, FloatWindowService::class.java))
    }


    // 定义一个内部类，继承自 Binder
    inner class FloatWindowBinder : Binder() {
        // 定义一个方法，返回服务的引用
        fun getService(): FloatWindowBinder = this
    }

}