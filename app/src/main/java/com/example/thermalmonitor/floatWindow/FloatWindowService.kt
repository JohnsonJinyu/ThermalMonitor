package com.example.thermalmonitor.floatWindow

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer


class FloatWindowService : LifecycleService() {

    private lateinit var floatWindowManager: FloatWindowManager
    // 声明floatViewModel
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

        //获取floatViewModel实例
        floatViewModel = FloatViewModel(application)



    }



    fun show() {
        //Log.d("show方法是否被调用", "是的")
        if (::floatWindowManager.isInitialized && !floatWindowManager.isShowing()) {
            floatWindowManager.show()

            // 在这里开始观察数据
            floatViewModel.startObserving()
            floatViewModel.floatData.observe(this, Observer { floatDataItems ->
                // 引用Adapter中的的updateData来更新Adapter中的数据
                floatWindowManager.floatView.adapter.updateData(floatDataItems)
            })
        }
    }

    fun hide() {
        floatWindowManager.hide()
        // 在这里停止观察数据
        floatViewModel.stopObserving()
        floatViewModel.floatData.removeObservers(this)
    }

    override fun onDestroy() {
        floatWindowManager.removeAllView()
        super.onDestroy()
    }
}