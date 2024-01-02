package com.example.thermalmonitor.floatWindow

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log


class FloatWindowService : Service() {

    private lateinit var floatWindowManager: FloatWindowManager

    override fun onBind(intent: Intent): IBinder {

        return FloatWindowBinder()
    }

    inner class FloatWindowBinder : Binder() {
        fun getService(): FloatWindowService = this@FloatWindowService
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化浮动窗口
        floatWindowManager = FloatWindowManager(this)

    }

    fun show() {
        Log.d("show方法是否被调用", "是的")
        if (::floatWindowManager.isInitialized && !floatWindowManager.isShowing()) {
            floatWindowManager.show()
        }


    }

    fun hide() {
        floatWindowManager.hide()
    }

    override fun onDestroy() {
        floatWindowManager.removeAllView()
        super.onDestroy()
    }
}