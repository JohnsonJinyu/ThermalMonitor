package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.ItemThermalBinding


class ThermalAdapter(private var thermalList: List<ThermalData>,
                     private val onItemCheckChanged: (ThermalData, Boolean) -> Unit) :
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
            thermalCheckbox.setOnCheckedChangeListener(null)
            thermalCheckbox.isChecked = thermalData.isChecked
            thermalCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onItemCheckChanged(thermalData, isChecked)
            }
        }
    }




    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<ThermalData>) {
        thermalList = newList
        notifyDataSetChanged()
    }




    // 用于更新所有item中checkbox的值的方法
    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(isChecked: Boolean) {
        thermalList.forEach { it.isChecked = isChecked }
        notifyDataSetChanged()
    }

    // 在ThermalAdapter中添加一个getSelectedItems的方法，用来找出所有被选中的Item：
    fun getSelectedItems(): List<ThermalData> {
        return thermalList.filter { it.isChecked }
    }

}