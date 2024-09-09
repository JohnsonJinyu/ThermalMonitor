package com.example.thermalmonitor.soc


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.databinding.FragmentSocBinding

class SocFragment : Fragment() {

    private lateinit var binding: FragmentSocBinding // 使用view binding来访问布局中的控件
    private lateinit var viewModel: SocViewModel // 使用view model来管理数据和逻辑
    private lateinit var adapter: SocAdapter // 使用recycler view adapter来展示动态信息

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSocBinding.inflate(inflater, container, false) // 初始化view binding

        viewModel = (activity?.application as MyApp).socViewModel // 使用MyApp中的方法获取view model的实例

        adapter = SocAdapter { coreNumber, isChecked ->
            viewModel.updateCheckedState(coreNumber, isChecked)
        }                                       // 初始化adapter
        binding.recyclerView.adapter = adapter // 绑定adapter到recycler view
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext()) // 设置recycler view的布局管理器为线性布局

        viewModel.staticInfo.observe(viewLifecycleOwner) { info -> // 观察静态信息的变化，如果有变化就更新UI
            binding.hardwareName.text = info.socManyFacture +"  " + info.hardwareName // 设置硬件名称
            binding.coreCount.text = info.coreCount.toString() // 设置核心数
            binding.frequencyRange.text = info.frequencyRange // 设置频率范围
        }


        viewModel.dynamicInfo.observe(viewLifecycleOwner) { list -> // 观察动态信息的变化，如果有变化就更新UI
            adapter.submitList(list) // 提交新的列表给adapter，让它刷新数据
        }


        return binding.root // 返回根视图
    }
}
