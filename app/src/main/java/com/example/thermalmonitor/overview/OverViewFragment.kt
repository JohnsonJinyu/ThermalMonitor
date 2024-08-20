package com.example.thermalmonitor.overview

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.thermalmonitor.MyApp
import com.example.thermalmonitor.databinding.FragmentOverviewBinding
import com.example.thermalmonitor.interfaces.FloatWindowCallback


class OverViewFragment : Fragment() {

    /**
     * 这个类的目的是将读取的数据保存到设备
     * */


    private lateinit var viewModel: DataCaptureViewModel


    // 调用接口
    private var callback: FloatWindowCallback? = null

    // 使用 lateinit 定义 binding 成员变量
    private lateinit var binding: FragmentOverviewBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        // Inflate the layout for this fragment
        binding = FragmentOverviewBinding.inflate(inflater)
        binding.lifecycleOwner = this


        // 获取Application实例
        val myApp = requireActivity().application as MyApp
        viewModel = myApp.dataCaptureViewModel // 获取DataCaptureViewModel实例



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
            // 当需要更新通知时
            //notificationControl.updateNotification(timeString)
        }

        // 观察toastMessage用于弹窗提醒用户
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        //观察 showAbortDialog,
        viewModel.showAbortDialog.observe(viewLifecycleOwner) {
            showAbortConfirmationDialog()
        }


        // 观察 通知栏按钮的开始或停止动作

        viewModel.action.observe(viewLifecycleOwner) { action ->
            when (action) {
                "start" -> startDataCapture()
                "stop" -> stopDataCapture()
            }
        }




        /**
         * 设置开始按钮的点击事件
         * */
        binding.btnStart.setOnClickListener {
            if (!cbBattery.isChecked && !cbThermal.isChecked && !cbSoc.isChecked) {
                Toast.makeText(requireContext(), "请至少选择一项数据", Toast.LENGTH_SHORT).show()
            } else {
                //viewModel.startDataCapture()
                startDataCapture() // 调用公共的开始数据捕获方法
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
            //viewModel.stopDataCapture()
            stopDataCapture() // 调用公共的停止数据捕获方法
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



        // 注册接收器
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(dataCaptureReceiver, IntentFilter().apply {
            addAction("start")
            addAction("stop")
        })

        return binding.root

    }



    private val dataCaptureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "start" -> startDataCapture()
                "stop" -> stopDataCapture()
            }
        }
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





    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    override fun onResume() {
        super.onResume()
        checkAndRequestPermissions()
    }


    override fun onDestroyView() {
        super.onDestroyView()

    }


    override fun onDestroy() {
        super.onDestroy()
        // 注销接收器
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(dataCaptureReceiver)
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



    /**
     * 申请权限的launcher
     * */
    // 使用新的ActivityResultContracts来请求权限

    private val requestOverlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (Settings.canDrawOverlays(context)) {
            // 悬浮窗权限已经被授予
        } else {
            // 用户未授予悬浮窗权限
            showOverLayRationaleDialog()
        }
    }


    private val requestBatteryOptimizationPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isIgnoringBatteryOptimizations()) {
            // 用户已经忽略电池优化
        } else {
            // 用户未忽略电池优化
            showBatteryRationaleDialog()
        }
    }

    private val requestManageExternalStoragePermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (Environment.isExternalStorageManager()) {
            // 外部存储管理权限已经被授予
        } else {
            // 用户未授予外部存储管理权限
            showFilesRationaleDialog()
        }
    }


    // 使用新的ActivityResultContracts请求通知权限
    private val requestNotificationSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // 这里无法确切知道用户是否更改了通知权限设置
        // 但你可以在这里执行一些如刷新UI的操作
        Toast.makeText(context, "请确保已开启通知权限", Toast.LENGTH_LONG).show()
    }





    /**
     * 检查和申请权限
     * */
    private fun checkAndRequestPermissions() {
        // 检查是否已经有所有需要的权限
        var hasAllPermissions = true
        hasAllPermissions = hasAllPermissions and Settings.canDrawOverlays(requireContext())
        hasAllPermissions = hasAllPermissions and isIgnoringBatteryOptimizations()
        hasAllPermissions = hasAllPermissions and Environment.isExternalStorageManager()
        // 通知权限
        hasAllPermissions = hasAllPermissions and Settings.canDrawOverlays(requireContext())

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
                    requestNotificationPermission()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }




    /**
     *
     * 申请权限以及被拒绝后弹窗重复申请
     * */
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(requireContext())) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
            requestOverlayPermissionLauncher.launch(intent)
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationPermission() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${requireContext().packageName}"))
            requestBatteryOptimizationPermissionLauncher.launch(intent)
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            requestManageExternalStoragePermissionLauncher.launch(intent)
        }
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, requireContext().applicationInfo.uid)
            //putExtra(Settings.EXTRA_APP_NAME, requireContext().getString(R.string.app_name))
        }
        requestNotificationSettingsLauncher.launch(intent)
    }



    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)
    }

    private fun showOverLayRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要悬浮窗权限")
            .setMessage("此应用需要悬浮窗权限来提供悬浮功能。请在设置中授予悬浮窗权限。")
            .setPositiveButton("设置") { _, _ ->
                // 引导用户再次打开设置页面
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
                requestOverlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    @SuppressLint("BatteryLife")
    private fun showBatteryRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要忽略电池优化")
            .setMessage("此应用需要需要忽略电池优化，方式后台被杀。")
            .setPositiveButton("设置") { _, _ ->
                // 引导用户再次打开设置页面
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${requireContext().packageName}"))
                requestOverlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFilesRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要文件管理权限")
            .setMessage("此应用需要文件管理权限读取数据以及保存文件。")
            .setPositiveButton("设置") { _, _ ->
                // 引导用户再次打开设置页面
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${requireContext().packageName}"))
                requestOverlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showNotificationRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("需要通知权限")
            .setMessage("此应用需要通知权限便于控制数据抓取")
            .setPositiveButton("设置") { _, _ ->
                // 引导用户再次打开设置页面
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS, Uri.parse("package:${requireContext().packageName}"))
                requestOverlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 定义公共的开始数据捕获方法
    fun startDataCapture() {
        viewModel.startDataCapture()
        //notificationControl.updateNotificationAction()
    }

    // 定义公共的停止数据捕获方法
    fun stopDataCapture() {
        viewModel.stopDataCapture()
        //notificationControl.updateNotificationAction()
    }

}






