package com.example.thermalmonitor.thermal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemThermalBinding


class ThermalAdapter(private val thermalList: LiveData<List<ThermalData>>) :
    RecyclerView.Adapter<ThermalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemThermalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThermalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = thermalList.value?.size ?: 0

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 用thermalList.value来获取List<Thermal>
        val thermalData = thermalList.value?.get(position) ?: return
        // 使用viewBinding绑定数据到视图元素，只更新temp值，避免整体重绘
        holder.binding.apply {
            thermalZone.text = thermalData.zone
            thermalType.text = thermalData.type
            thermalTemp.text = "${thermalData.temp}℃"
        }
    }

}