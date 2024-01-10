package com.example.thermalmonitor.battery


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.databinding.FragmentBatteryBinding



class BatteryFragment : Fragment() {

    // 一个ViewBinding对象，用于绑定布局文件中的视图元素
    private var _binding: FragmentBatteryBinding? = null
    private val binding get() = _binding!!

    // 一个ViewModel对象，用于获取和暴露电池信息的数据
    //private val viewModel: BatteryViewModel by viewModels()
    // 声明一个viewModel
    private lateinit var viewModel: BatteryViewModel

    // 在Fragment创建视图时，初始化ViewBinding对象，并返回根视图
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatteryBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 在Fragment视图创建完成后，通过ViewBinding和LiveData绑定电池信息的数据，并更新UI视图
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 使用getBatteryViewModel方法获取ThermalViewModel的实例
        viewModel =  (activity?.application as MyApp).getBatteryViewModel()

        viewModel.batteryData.observe(viewLifecycleOwner) { batteryData ->
            binding.apply {
                // 通过ViewBinding对象访问布局文件中的视图元素，并设置相应的数据
                levelText.text = "${batteryData.level}%"
                statusText.text = batteryData.status

                /**
                 * 这里还需要验证一下，是否不同的设备都需要或者不需要除以1000，目前荣耀设备，安卓14不需要除以1000就可以获得电流值
                 * 其他厂商的设备还需要验证，用设备管理器App同步对比，两者电流值一致，说明方向没问题
                 * */
                currentText.text = "${batteryData.current }mA" // 将微安培转换为毫安培
                temperatureText.text = "${batteryData.temperature}℃"
                voltageText.text = "${batteryData.voltage}mV"
                sourceText.text = batteryData.source
            }
        }
    }

    // 在Fragment销毁视图时，释放ViewBinding对象，避免内存泄漏
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

