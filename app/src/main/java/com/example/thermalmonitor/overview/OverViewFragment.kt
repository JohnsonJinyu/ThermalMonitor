package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.interfaces.FloatWindowCallback
import com.example.thermalmonitor.interfaces.OpenFolderListener


class OverViewFragment : Fragment(), OpenFolderListener {

    /**
     * 这个类的目的是将读取的数据保存到设备
     * */

    private lateinit var viewModel: DataCaptureViewModel

    // 调用接口
    private var callback: FloatWindowCallback? = null



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
        val batteryViewModel = (activity?.application as MyApp).getBatteryViewModel()


        // 将创建新的thermalViewModel实例改为使用getThermalViewModel方法获取已创建的实例
        val thermalViewModel = (activity?.application as MyApp).getThermalViewModel()


        val socViewModel = (activity?.application as MyApp).getSocViewModel()

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


        // 分别定义三个变量绑定三个checkbox
        val cbBattery = binding.cbBattery
        val cbThermal = binding.cbThermal
        val cbSoc = binding.cbSoc


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
            if (!cbBattery.isChecked && !cbThermal.isChecked && !cbSoc.isChecked) {
                Toast.makeText(requireContext(), "请至少选择一项数据", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.startDataCapture()
            }
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


        /**
         * 悬浮窗开启与关闭按钮点击事件
         * */

        // 开启悬浮窗
        binding.btnStartFloat.setOnClickListener {
            callback?.showFloatWindow()
        }

        // 关闭悬浮窗
        binding.btnEndFloat.setOnClickListener {
            callback?.hideFloatWindow()
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
        checkAndRequestPermissions()
    }









    /**
     * onAttach() 方法会在 Fragment 被添加到 FragmentManager 并附加到其宿主 Activity 时调用。
     * 这时，您可以获取 Activity 的引用，并将其转换为 FloatWindowCallback 类型，
     * 以便在 Fragment 中调用显示或隐藏悬浮窗口的方法。
     * */
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // 保持对 Activity 的引用
        callback = context as FloatWindowCallback
    }

    /**
     * onDetach() 方法会在 Fragment 从 FragmentManager 中移除并与其宿主 Activity 分离时调用。
     * 这时，您可以将 Activity 的引用置为 null，以避免内存泄漏或其他问题。
     * */
    override fun onDetach() {
        callback = null
        super.onDetach()
    }





    /**
     * 权限申请部分
     * */

    private fun checkAndRequestPermissions() {
        // 检查是否已经有所有需要的权限
        var hasAllPermissions = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasAllPermissions = hasAllPermissions and Settings.canDrawOverlays(requireContext())
            hasAllPermissions = hasAllPermissions and isIgnoringBatteryOptimizations()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllPermissions = hasAllPermissions and Environment.isExternalStorageManager()
        }

        // 如果没有所有需要的权限，显示 AlertDialog
        if (!hasAllPermissions) {
            AlertDialog.Builder(requireContext())
                .setTitle("权限请求")
                .setMessage("我们需要以下权限来正常使用该应用……")
                .setPositiveButton("确定") { _, _ ->
                    // 用户点击确定后，请求权限
                    requestOverlayPermission()
                    requestBatteryOptimizationPermission()
                    requestManageExternalStoragePermission()
                }
                .setNegativeButton("取消", null)
                .show()
        }
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
        } else {
            // 权限已经被授予，不需要再次请求
        }
    }

    private fun requestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION_PERMISSION)
        } else {
            // 权限已经被授予，不需要再次请求
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






