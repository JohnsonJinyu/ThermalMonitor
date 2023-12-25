package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel


class OverViewFragment : Fragment() ,OpenFolderListener {

    /**
     * 这个类的目的是将读取的数据保存到设备
     * */

    private lateinit var viewModel: DataCaptureViewModel


    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        // Inflate the layout for this fragment
        val binding = FragmentOverviewBinding.inflate(inflater)
        binding.lifecycleOwner = this


        /**
         *
         * 在 OverViewFragment 中实现 OpenFolderListener 接口，并提供 openFolder 方法的具体实现。
         * 在 onViewCreated 方法中，创建 ViewModelFactory 实例时，传递当前 Fragment 作为 OpenFolderListener 实例。
         * 使用 ViewModelProvider 初始化 DataCaptureViewModel 时，传递正确的 ViewModelFactory 实例。
         * */
        // 在 OverViewFragment 中使用 ViewModelProvider 和 ViewModelFactory 创建 DataCaptureViewModel 实例
        val batteryViewModel = ViewModelProvider(this)[BatteryViewModel::class.java]
        val thermalViewModel = ViewModelProvider(this)[ThermalViewModel::class.java]
        val socViewModel = ViewModelProvider(this)[SocViewModel::class.java]
        val dataProcessor = DataProcessToSave(thermalViewModel, socViewModel)


        // Create the ViewModelFactory and pass the dependencies
        val viewModelFactory = ViewModelFactory(batteryViewModel, thermalViewModel, socViewModel, dataProcessor, requireContext(),this)

        // Initialize the DataCaptureViewModel using the ViewModelFactory
        viewModel = ViewModelProvider(this, viewModelFactory)[DataCaptureViewModel::class.java]




        /**
         * 关于checkbox的相关定义
         * */
        // 观察 checkBoxState 的变化，并在状态改变时执行相应的逻辑 ，这段是代码影响UI
        //电池
        viewModel.cbBatteryState.observe(viewLifecycleOwner) { isChecked ->
            // 在这里处理 CheckBox battery 状态变化后的逻辑
            binding.cbBattery.isChecked = isChecked
        }
        // 当 CheckBox battery的状态发生变化时，更新 ViewModel 中的状态  ,这段是UI 影响代码
        binding.cbBattery.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCheckBoxBattery(isChecked)
        }

        //Thermal
        viewModel.cbThermalState.observe(viewLifecycleOwner) { isChecked ->
            binding.cbThermal.isChecked = isChecked
        }
        binding.cbThermal.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCheckBoxThermal(isChecked)
        }

        // Soc
        viewModel.cbSocState.observe(viewLifecycleOwner) { isChecked ->
            binding.cbSoc.isChecked = isChecked
        }
        binding.cbSoc.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateCheckBoxSoc(isChecked)
        }





        // 观察 timer 的变化，并更新界面
        viewModel.timer.observe(viewLifecycleOwner) { timeString ->
            binding.tvTimer.text = timeString
        }

        // 观察toastMessage用于弹窗提醒用户
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        //观察 showAbortDialog,
        viewModel.showAbortDialog.observe(viewLifecycleOwner) {
            showAbortConfirmationDialog()
        }




        /**
         * 设置开始按钮的点击事件
         * */
        binding.btnStart.setOnClickListener {
            viewModel.startDataCapture()
        }


        /**
         * 设置中止按钮的点击事件
         * */
        binding.btnAbort.setOnClickListener {
            viewModel.onAbortButtonClicked()
        }


        /**
         * 设置停止按钮的点击事件
         * */
        binding.btnStopAndSave.setOnClickListener {
            viewModel.stopDataCapture()
        }

        return binding.root

    }


    // 点击中止按钮后的Dialog
    private fun showAbortConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("中止记录")
            .setMessage("确定要中止记录吗？已经记录的数据将不会被保存。")
            .setPositiveButton("确定") { _, _ ->
                // 在这里调用 View Model 中的方法来处理中止记录的逻辑
                viewModel.abortDataCapture()
            }
            .setNegativeButton("取消", null)
            .show()
    }



    // 实现 OpenFolderListener 接口的方法，在这里启动新的活动
    override fun openFolder() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        val uri = Uri.parse(Environment.getExternalStorageDirectory().path + "/Download/ThermalMonitor/")
        intent.setDataAndType(uri, "*/*")
        startActivity(Intent.createChooser(intent, "Open folder"))
    }




    /**
     * A helper function to convert a boolean value to an int value, 0 for false and 1 for true.
     */
    private fun Boolean.toInt() = if (this) 1 else 0

}






