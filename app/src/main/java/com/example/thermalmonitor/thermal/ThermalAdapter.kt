package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemThermalBinding


class ThermalAdapter(private var thermalList: List<ThermalData>) :
    RecyclerView.Adapter<ThermalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemThermalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = thermalList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 用thermalList.value来获取List<Thermal>
        val thermalData = thermalList[position]
        // 使用viewBinding绑定数据到视图元素，只更新temp值，避免整体重绘
        holder.binding.apply {
            thermalZone.text = thermalData.zone
            thermalType.text = thermalData.type
            thermalTemp.text = "${thermalData.temp}℃"
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ThermalData>) {
        thermalList = newList
        notifyDataSetChanged()
    }

}