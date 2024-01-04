package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.thermalmonitor.dataRepository.ThermalDataManager
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
        viewModel = ViewModelProvider(this)[ThermalViewModel::class.java]
        //设置RecyclerView的布局管理器和适配器，根据屏幕方向显示不同数量的列
        binding.recyclerViewThermal.layoutManager =
            GridLayoutManager(context,if(resources.configuration.orientation == 1) 2 else 4)
        //初始化适配器时可以传递一个空列表或者初始列表。
        binding.recyclerViewThermal.adapter = ThermalAdapter(emptyList())

        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //// 观察 ThermalDataManager 中的数据变化，当数据更新时通知适配器刷新界面
        ThermalDataManager.thermalList.observe(viewLifecycleOwner) { thermalDataList ->
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
    }

}
