package com.example.thermalmonitor.floatWindow

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.WindowManager

class FloatWindowManager(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val params = WindowManager.LayoutParams()
    private val floatView = FloatWindowView(context).apply {
        setLayoutParams(params)
    }


    fun show() {

        Log.i("show is ok?","yes")

        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.format = PixelFormat.TRANSLUCENT  // 设置为半透明
        // 设置为半透明
        floatView.setOpacity(0.8f)
        windowManager.addView(floatView, params)
    }

    fun hide() {
        if (isShowing()) {
            windowManager.removeView(floatView)
        }
    }

    fun removeAllView() {
        if (isShowing()) {
            windowManager.removeView(floatView)
        }
    }

    fun isShowing() = floatView.parent != null




}
