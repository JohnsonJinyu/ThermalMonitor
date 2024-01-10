package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemThermalBinding


class ThermalAdapter(private var thermalList: List<ThermalData>, private val listener: OnItemCheckedChangedListener) :
    RecyclerView.Adapter<ThermalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemThermalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = thermalList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val thermalData = thermalList[position]
        holder.binding.apply {
            thermalZone.text = thermalData.zone
            thermalType.text = thermalData.type
            thermalTemp.text = "${thermalData.temp}℃"
            thermalCheckbox.setOnCheckedChangeListener(null) // 清除之前的监听器，防止无限循环
            thermalCheckbox.isChecked = thermalData.isChecked
            thermalCheckbox.setOnCheckedChangeListener { _, isChecked ->
                thermalData.isChecked = isChecked
                //listener?.onItemCheckedChanged(thermalData.zone, thermalData.isChecked)
                // 更新 _thermalList
                // 不直接修改thermalData的isChecked属性，而是通过调用updateItem方法来修改isCheckedMap中的值
                listener.onItemCheckedChanged(thermalData.zone, isChecked)
            }
        }
    }




    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ThermalData>) {
        thermalList = newList
        notifyDataSetChanged()
    }




    // 用于全选点击事件后更新isChecked属性
    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(isChecked: Boolean) {
        thermalList.forEach {
            it.isChecked = isChecked
            listener.onItemCheckedChanged(it.zone, isChecked)
        }
        notifyDataSetChanged()
    }



    // 定义一个接口
    interface OnItemCheckedChangedListener {
        fun onItemCheckedChanged(zone: String, isChecked: Boolean)
    }


}