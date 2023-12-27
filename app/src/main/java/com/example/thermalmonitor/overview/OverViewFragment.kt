package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.battery.BatteryViewModel
import com.example.thermalmonitor.databinding.FloatWindowBinding
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.floatWindow.FloatWindowService
import com.example.thermalmonitor.soc.SocViewModel
import com.example.thermalmonitor.thermal.ThermalViewModel


class OverViewFragment : Fragment(), OpenFolderListener {

    /**
     * 这个类的目的是将读取的数据保存到设备
     * */

    private lateinit var viewModel: DataCaptureViewModel

    // 定义一个服务的变量
    private var serviceConnection: ServiceConnection? = null
    private var floatWindowService: FloatWindowService? = null

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
        val viewModelFactory = ViewModelFactory(
            batteryViewModel,
            thermalViewModel,
            socViewModel,
            dataProcessor,
            requireContext(),
            this
        )

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


        /**
         * 定义viewModel中liveData的观察者
         * */

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


        binding.btnStartFloart.setOnClickListener{
            floatWindowService?.startFloatWindowService()
        }

        binding.btnEndFloat.setOnClickListener {
            floatWindowService?.stopFloatWindowService()
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
        val uri =
            Uri.parse(Environment.getExternalStorageDirectory().path + "/Download/ThermalMonitor/")
        intent.setDataAndType(uri, "*/*")
        startActivity(Intent.createChooser(intent, "Open folder"))
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    override fun onResume() {
        super.onResume()
        //checkAndRequestPermissions()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        // 解绑 FloatWindowService
        unbindFloatWindowService()
    }

    // 绑定 FloatWindowService 的方法
    private fun bindFloatWindowService() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // 获取 FloatWindowService 的引用
                floatWindowService = (service as FloatWindowService.FloatWindowBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // 释放 FloatWindowService 的引用
                floatWindowService = null
            }
        }
        // 创建一个 Intent
        val intent = Intent(requireContext(), FloatWindowService::class.java)
        // 绑定服务
        requireActivity().bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    // 解绑 FloatWindowService 的方法
    private fun unbindFloatWindowService() {
        // 解绑服务
        requireActivity().unbindService(serviceConnection!!)
        // 释放 ServiceConnection 的引用
        serviceConnection = null
    }










    /**
     * part of request permissions
     * */

    private fun checkAndRequestPermissions() {
        requestOverlayPermission()
        requestBatteryOptimizationPermission()
        requestManageExternalStoragePermission()
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 101
        private const val REQUEST_BATTERY_OPTIMIZATION_PERMISSION = 102
        private const val REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION = 103
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(
                requireContext()
            )
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().packageName)
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }

    private fun requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION_PERMISSION)
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivityForResult(intent, REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                    requireContext()
                )
            ) {
                // 悬浮窗权限已经被授予
            } else {
                // 用户未授予悬浮窗权限
            }
        } else if (requestCode == REQUEST_BATTERY_OPTIMIZATION_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
                // 用户未忽略电池优化
            } else {
                // 用户已经忽略电池优化
            }
        } else if (requestCode == REQUEST_MANAGE_EXTERNAL_STORAGE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                // 外部存储管理权限已经被授予
            } else {
                // 用户未授予外部存储管理权限
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

}






