package com.example.thermalmonitor.floatWindow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.R

class FloatAdapter(var dataList: List<FloatDataItem>) : RecyclerView.Adapter<FloatAdapter.MyViewHolder>() {


    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view){
        val itemName :TextView = view.findViewById(R.id.tv_float_ItemName)
        val itemValue:TextView = view.findViewById(R.id.tv_float_ItemValue)
    }

    /**
     * 这个方法负责创建每个列表项的视图，并返回一个ViewHolder对象，
     * 它包含了视图的引用。您可以使用LayoutInflater从一个XML布局文件中创建视图，
     * 然后传递给ViewHolder的构造函数
     * */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_float, parent, false)
        return MyViewHolder(view)
    }

    /**
     * 返回数据集的大小，也就是item的数量
     * */
    override fun getItemCount(): Int {
        return dataList.size
    }


    /**
     *
     * 负责将数据绑定到视图
     * 也就是根据列表项的位置填充视图
     *
     * */
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val data = dataList[position]
        holder.itemName.text = data.itemName
        holder.itemValue.text = data.itemValue
        // 在这里绑定数据到视图
    }


    fun updateData(newData: List<FloatDataItem>) {
        this.dataList = newData
        notifyDataSetChanged()
    }

}