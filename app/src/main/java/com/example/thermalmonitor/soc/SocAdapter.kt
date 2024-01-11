package com.example.thermalmonitor.soc

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.thermalmonitor.databinding.SocDynamicItemBinding


class SocAdapter(private val onCheckedChange: (coreNumber: Int, isChecked: Boolean) -> Unit) : ListAdapter<DynamicInfo, SocAdapter.ViewHolder>(DiffCallback()){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = SocDynamicItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dynamicInfo = getItem(position)
        holder.bind(dynamicInfo)
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

    inner class ViewHolder(private val binding: SocDynamicItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private lateinit var dynamicInfo: DynamicInfo

        init {
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                dynamicInfo.isChecked = isChecked
                onCheckedChange(dynamicInfo.coreNumber, isChecked)
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(info: DynamicInfo) {
            dynamicInfo = info // 保存DynamicInfo对象的引用，以便在复选框的监听器中使用
            binding.coreNumber.text = "核心${info.coreNumber}" // 设置核心编号
            binding.coreFrequency.text = "${info.coreFrequency}MHz" // 设置核心频率

            // 先移除OnCheckedChangeListener
            binding.checkBox.setOnCheckedChangeListener(null)
            // 更改CheckBox的选中状态
            binding.checkBox.isChecked = info.isChecked
            // 再为CheckBox添加OnCheckedChangeListener
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                dynamicInfo.isChecked = isChecked
                onCheckedChange(dynamicInfo.coreNumber, isChecked)
            }
        }
        /**
         *当你在bind方法中改变CheckBox的选中状态时，OnCheckedChangeListener就不会被触发，
         * 所以dynamicInfo.isChecked的值就不会被更改。
         *
         * 当用户在界面上点击CheckBox改变其选中状态时，OnCheckedChangeListener会被触发，
         * 所以dynamicInfo.isChecked的值会被更新为用户选择的状态
         *
         * */

        // add this method, only for update frequency data
        @SuppressLint("SetTextI18n")
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

