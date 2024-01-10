package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.databinding.FragmentThermalBinding

class ThermalFragment : Fragment() {

    private lateinit var binding : FragmentThermalBinding //使用viewBinding绑定视图要素
    private lateinit var viewModel : ThermalViewModel //使用ViewModel获取和更新数据

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentThermalBinding.inflate(inflater,container,false)

        //viewModel = ViewModelProvider(this)[ThermalViewModel::class.java]
        viewModel = (activity?.application as MyApp).getThermalViewModel()

        //设置RecyclerView的布局管理器和适配器，根据屏幕方向显示不同数量的列
        binding.recyclerViewThermal.layoutManager =
            GridLayoutManager(context,if(resources.configuration.orientation == 1) 2 else 4)
        // 创建ThermalAdapter的实例时，你可以传入一个OnItemCheckedChangedListener实例
        // 用于监听列表中的Item的选中状态发生变化的事件
        binding.recyclerViewThermal.adapter = ThermalAdapter(viewModel.thermalList.value ?: emptyList(),object :
            ThermalAdapter.OnItemCheckedChangedListener {
            override fun onItemCheckedChanged(zone: String, isChecked: Boolean) {
                // 当列表中的Item的选中状态发生变化时，更新ThermalViewModel中的数据
                viewModel.updateItem(zone, isChecked)
            }
        })


        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //// 观察 ThermalDataManager 中的数据变化，当数据更新时通知适配器刷新界面
        viewModel.thermalList.observe(viewLifecycleOwner) { thermalDataList ->
            // 当数据更新时，更新适配器的数据
            (binding.recyclerViewThermal.adapter as ThermalAdapter).submitList(thermalDataList)
        }

        //添加一个OnLayoutChangeListener，当视图的布局发生变化时，重新设置RecyclerView的列数
        binding.recyclerViewThermal.addOnLayoutChangeListener{
            _,_,_,_,_,_,_,_,_ ->
            //获取GridLayoutManager对象
            val layoutManager = binding.recyclerViewThermal.layoutManager as GridLayoutManager
            layoutManager.spanCount = if (resources.configuration.orientation == 1) 2 else 4
        }

        // 为全选按钮添加点击事件监听器，当用户点击全选按钮时，遍历列表，
        // 更新每个Item的`isChecked`的值，并通知适配器数据发生了变化：
        binding.chbThermalSelectAll.setOnClickListener {
            val isChecked = binding.chbThermalSelectAll.isChecked
            val adapter = binding.recyclerViewThermal.adapter as ThermalAdapter

            binding.recyclerViewThermal.post{
                adapter.selectAll(isChecked)

            }
        }

    }


}
