package com.example.thermalmonitor.soc

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.SocDynamicItemBinding


class SocAdapter : ListAdapter<DynamicInfo, SocAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SocDynamicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int , payloads : MutableList<Any>) {
        if(payloads.isEmpty()){//如果payloads为空，说明没有部分更新，就调用原来的方法
            super.onBindViewHolder(holder, position, payloads)
        }
        else{
            //否则，就根据payloads中的数据来更新频率
            val frequency = payloads[0] as Int
            holder.updateFrequency(frequency)
        }
    }

    class ViewHolder(private val binding: SocDynamicItemBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(info: DynamicInfo) {
            binding.coreNumber.text = "核心${info.coreNumber}" // 设置核心编号
            binding.coreFrequency.text = "${info.coreFrequency}MHz" // 设置核心频率
        }

        // add this method, only for update frequency data
        fun updateFrequency(frequency :Int){
            binding.coreFrequency.text = "${frequency}MHz"
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DynamicInfo>() {

        override fun areItemsTheSame(oldItem: DynamicInfo, newItem: DynamicInfo): Boolean {
            return oldItem.coreNumber == newItem.coreNumber // 判断两个对象是否是同一个核心的信息，根据核心编号来判断
        }

        override fun areContentsTheSame(oldItem: DynamicInfo, newItem: DynamicInfo): Boolean {
            return oldItem == newItem // 判断两个对象的内容是否相同，根据数据类的equals方法来判断
        }

        // rewrite this method to return partially updated data
        override fun getChangePayload(oldItem: DynamicInfo, newItem: DynamicInfo): Any? {
            return if(oldItem.coreFrequency != newItem.coreFrequency){
                //如果核心频率发生了改变，就返回新的频率作为payload
                newItem.coreFrequency
            }else{
                // 否则，就返回null，表示没有部分更新
                null
            }
        }
    }
}
