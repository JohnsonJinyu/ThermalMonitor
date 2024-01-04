package com.example.thermalmonitor.floatWindow

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer


class FloatWindowService : LifecycleService() {

    private lateinit var floatWindowManager: FloatWindowManager
    private lateinit var floatViewModel: FloatViewModel

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)

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

    private fun observeFloatData() {
        floatViewModel.floatData.observe(this, Observer { data ->
            // 更新悬浮窗数据
            floatWindowManager.floatView.updateData(data)
        })
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