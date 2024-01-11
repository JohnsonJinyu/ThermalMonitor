package com.example.thermalmonitor.floatWindow

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.R

/**
 * 悬浮窗视图管理类
 * */
@SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
class FloatWindowView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f

    // 定义一个变量表示悬浮窗的缩放状态
    private var isMinimized: Boolean = false

    // 定义两个变量表示悬浮窗缩小和方法的图标
    private val iconMinimize = R.drawable.ic_minimize
    private val iconMaximize = R.drawable.ic_maximize

    val adapter = FloatAdapter(emptyList())


    // 初始化
    init {
        // 获取视图中的控件，绑定
        val view = LayoutInflater.from(context).inflate(R.layout.float_window, this)
        val dragHandle = view.findViewById<ImageButton>(R.id.drag_handle)
        val minOrMaxButton = view.findViewById<ImageButton>(R.id.btn_floatWindow_Min)

        // 获取悬浮窗中的recyclerview，并设置布局管理器
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_float)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // 设置拖动按钮的点击事件
        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - lastX
                    val deltaY = event.rawY - lastY
                    layoutParams?.let { params ->
                        params.x += deltaX.toInt()
                        params.y += deltaY.toInt()
                        windowManager.updateViewLayout(this, params)
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            true
        }

        // 缩放图标的点击事件
        minOrMaxButton.setOnClickListener {
            isMinimized = !isMinimized
            if (isMinimized) {
                minOrMaxButton.setImageResource(iconMaximize)
            } else {
                minOrMaxButton.setImageResource(iconMinimize)
            }

            updateSize()
        }
    }


    // 设置视图的布局参数
    fun setLayoutParams(params: WindowManager.LayoutParams) {
        this.layoutParams = params
    }


    // 悬浮窗缩放大小调整
    private fun updateSize() {
        layoutParams?.let { params ->
            params.height = if (isMinimized) dpToPx(30) else dpToPx(320)
            windowManager.updateViewLayout(this, params)
        }
    }


    /**
     * 因为WindowManager.LayoutParams类的height属性是以像素为单位的。
     * 如果直接使用dp，那么悬浮窗的高度可能会在不同的屏幕上有差异，导致UI效果不一致。
     * 因此，需要先将dp转换为px，然后再设置悬浮窗的高度。
     * */
    // dp转像素
    private fun dpToPx(dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }


    // 设置透明度
    /**
     * 这个setOpacity方法接受一个0到1之间的浮点数，表示悬浮窗的透明度，0表示完全透明，1表示完全不透明。
     * */
    fun setOpacity(opacity: Float) {
        this.alpha = opacity
    }

}


