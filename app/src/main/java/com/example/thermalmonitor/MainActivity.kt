package com.example.thermalmonitor

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.thermalmonitor.battery.BatteryFragment
import com.example.thermalmonitor.filesList.FilesListFragment
import com.example.thermalmonitor.overview.OverViewFragment
import com.example.thermalmonitor.soc.SocFragment
import com.example.thermalmonitor.thermal.ThermalFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity(){




    //定义一个SectionsPagerAdapter对象，用来提供Fragment给ViewPager2
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    //定义一个ViewPager2对象，用来显示不同的页面
    private lateinit var viewPager : ViewPager2
    //定义一个TabLayout对象，用来显示不同的标签
    private lateinit var tabLayout : TabLayout


    // 定义一些常量
    companion object {
        const val REQUEST_CODE_STORAGE = 100 // 存储权限请求码
        const val REQUEST_CODE_BATTERY = 200 // 电池优化请求码
        const val REQUEST_CODE_OVERLAY = 300 // 悬浮窗权限请求码
        const val REQUEST_CODE_MANAGE_STORAGE = 400 // 所有文件访问权限请求码
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //初始化SectionsPagerAdapter对象，只传入this作为参数
        sectionsPagerAdapter = SectionsPagerAdapter(this)
        //初始化ViewPager2对象，使用findViewById()方法
        viewPager = findViewById(R.id.viewPager2)
        //初始化tabLayout对象
        tabLayout = findViewById(R.id.tabLayout)

        //设置ViewPager2对象的adapter为sectionsAdapter对象
        viewPager.adapter = sectionsPagerAdapter

        //设置ViewPager2对象的offScreenPageLimit属性为Fragment的数量，这样就会预加载所有的页面
        viewPager.offscreenPageLimit = sectionsPagerAdapter.itemCount

        // 使用TabLayoutMediator类，将TabLayout和ViewPager2绑定在一起，并设置标签名称
        TabLayoutMediator(tabLayout , viewPager){tab , position ->
            //直接使用getString()方法获取字符串资源的引用
            tab.text = getString(
                when(position){
                    0 -> R.string.TabName_OverView
                    1 -> R.string.TabName_Battery
                    2 -> R.string.TabName_Thermal
                    3 -> R.string.TabName_Soc
                    4 -> R.string.TabName_LocalFiles

                    else -> throw IllegalStateException("Invalid position : $position")

                }
            )
        }.attach()



        // 检查并请求存储权限
        //checkAndRequestStoragePermission()

        // 检查并请求忽略电池优化
        checkAndRequestIgnoreBatteryOptimization()

        // 检查并请求悬浮窗权限
        checkAndRequestOverlayPermission()

        // 检查并请求所有文件访问权限
        checkAndRequestManageStoragePermission()
    }

    // 检查并请求所有文件访问权限
    private fun checkAndRequestManageStoragePermission() {
        // 如果系统版本低于Android 11，直接返回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }

        // 如果已经有所有文件访问权限，直接返回
        if (Environment.isExternalStorageManager()) {
            return
        }

        // 如果没有所有文件访问权限，创建一个意图，跳转到设置页面
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
    }

    private fun checkAndRequestOverlayPermission() {
        // 如果系统版本低于Android 6.0，直接返回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        // 如果已经有悬浮窗权限，直接返回
        if (Settings.canDrawOverlays(this)) {
            return
        }

        // 如果没有悬浮窗权限，创建一个意图，跳转到设置页面
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_OVERLAY)
    }

    private fun checkAndRequestIgnoreBatteryOptimization() {
        // 如果系统版本低于Android 6.0，直接返回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        // 获取电源管理器
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        // 如果已经忽略电池优化，直接返回
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            return
        }

        // 如果没有忽略电池优化，创建一个意图，跳转到设置页面
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_BATTERY)
    }

    private fun checkAndRequestStoragePermission() {
        // 如果系统版本低于Android 6.0，直接返回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        // 如果已经有悬浮窗权限，直接返回
        if (Settings.canDrawOverlays(this)) {
            return
        }

        // 如果没有悬浮窗权限，创建一个意图，跳转到设置页面
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_CODE_OVERLAY)
    }


    // 处理权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 根据请求码判断是哪个权限的结果
        when (requestCode) {
            REQUEST_CODE_STORAGE -> {
                // 如果存储权限请求成功，继续您的逻辑
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: 您的逻辑
                } else {
                    // 如果存储权限请求失败，提示用户
                    // TODO: 您的提示
                }
            }
        }
    }



    // 处理设置页面的结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 根据请求码判断是哪个设置的结果
        when (requestCode) {
            REQUEST_CODE_BATTERY -> {
                // 如果忽略电池优化设置成功，继续您的逻辑
                if (resultCode == RESULT_OK) {
                    // TODO: 您的逻辑
                } else {
                    // 如果忽略电池优化设置失败，提示用户
                    // TODO: 您的提示
                }
            }
            REQUEST_CODE_OVERLAY -> {
                // 如果悬浮窗权限设置成功，继续您的逻辑
                if (Settings.canDrawOverlays(this)) {
                    // TODO: 您的逻辑
                } else {
                    // 如果悬浮窗权限设置失败，提示用户
                    // TODO: 您的提示
                }
            }
        }
    }












    class SectionsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        //返回总的页面数
        override fun getItemCount():Int{
            return 5
        }
        //根据位置返回对应的Fragment实例
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OverViewFragment()
                1 -> BatteryFragment()
                2 -> ThermalFragment()
                3 -> SocFragment()
                4 -> FilesListFragment()

                else -> throw java.lang.IllegalStateException("Invalid position: $position")
            }

        }
    }






    



}


