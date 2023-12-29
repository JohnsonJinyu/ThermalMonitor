package com.example.thermalmonitor.floatWindow

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.R

class FloatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {




    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view)
    /**
     * 这个方法负责创建每个列表项的视图，并返回一个ViewHolder对象，
     * 它包含了视图的引用。您可以使用LayoutInflater从一个XML布局文件中创建视图，
     * 然后传递给ViewHolder的构造函数
     * */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_float,parent,false)
        return MyViewHolder(view)
    }

    /**
     * 返回数据集的大小，也就是item的数量
     * */
    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }


    /**
     *
     * 负责将数据绑定到视图
     * 也就是根据列表项的位置填充视图
     *
     * */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        
        // 在这里绑定数据到视图
    }

    private fun floatDataSet(){

    }

}