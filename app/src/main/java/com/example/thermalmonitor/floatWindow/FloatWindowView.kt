package com.example.thermalmonitor.floatWindow

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.thermalmonitor.R

class FloatWindowView(context: Context) : FrameLayout(context) {

    private var startX = 0
    private var startY = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.float_window, this)

        // 手指按下记录坐标
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x.toInt()
                    startY = event.y.toInt()
                }
            }
            false
        }

        // 手指移动,更新悬浮窗位置
        setOnDragListener { _, event ->
            val params = (layoutParams as WindowManager.LayoutParams)
            params.x += (event.x.toInt() - startX)
            params.y += (event.y.toInt() - startY)
            layoutParams = params
            startX = event.x.toInt()
            startY = event.y.toInt()
            false
        }
    }

}