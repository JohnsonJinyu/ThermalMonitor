package com.example.thermalmonitor.floatWindow

import android.content.Context
import android.util.Log
import android.view.WindowManager

class FloatWindowManager(context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val floatView = FloatWindowView(context) // 自定义View

    fun show() {

        Log.i("show is ok?","yes")
        val params = WindowManager.LayoutParams()
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

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
