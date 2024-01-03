package com.example.thermalmonitor.floatWindow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.R

/**
 * 这个类是悬浮窗布局中recyclerview的适配器，用于显示FloatViewModel中的LiveData数据
 * */

// 定义悬浮窗的适配器类,继承自RecyclerView.Adapter
class FloatAdapter : RecyclerView.Adapter<FloatAdapter.ViewHolder>() {

    // 定义一个内部类ViewHolder，继承自RecyclerView.ViewHolder
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.tv_float_ItemName)
        val itemValue: TextView = view.findViewById(R.id.tv_float_ItemValue)
    }

    // 定义一个内部类FloatDataItem，用于存储FloatViewModel中的LiveData数据
    private var floatDataList = emptyList<FloatDataItem>()

    // 定义一个方法，用于更新FloatViewModel中的LiveData数据
    fun updateData(newData: List<FloatDataItem>) {
        floatDataList = newData
        notifyDataSetChanged()
    }

    // 定义一个方法，用于创建ViewHolder实例
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_float, parent, false)
        return ViewHolder(view)
    }

    // 定义一个方法，用于对RecyclerView子项的数据进行赋值
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val floatDataItem = floatDataList[position]
        holder.itemName.text = floatDataItem.itemName
        holder.itemValue.text = floatDataItem.itemValue
    }

    // 定义一个方法，用于返回RecyclerView子项的个数
    override fun getItemCount() = floatDataList.size
}
