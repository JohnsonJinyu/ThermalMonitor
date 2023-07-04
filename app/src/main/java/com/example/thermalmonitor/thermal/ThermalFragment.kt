package com.example.thermalmonitor.thermal

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
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
        binding.recyclerViewThermal.adapter = ThermalAdapter(viewModel.thermalList)
        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //观察ViewModel中的数据变化，当数据更新时通知适配器刷新界面
        viewModel.thermalList.observe(viewLifecycleOwner){
            (binding.recyclerViewThermal.adapter as ThermalAdapter).notifyDataSetChanged()
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
