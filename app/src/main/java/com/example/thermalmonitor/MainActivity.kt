package com.example.thermalmonitor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.thermalmonitor.battery.BatteryFragment
import com.example.thermalmonitor.filesList.FilesListFragment
import com.example.thermalmonitor.floatWindow.FloatWindowService
import com.example.thermalmonitor.interfaces.FloatWindowCallback
import com.example.thermalmonitor.overview.OverViewFragment
import com.example.thermalmonitor.soc.SocFragment
import com.example.thermalmonitor.thermal.ThermalFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() , FloatWindowCallback {

    //定义一个SectionsPagerAdapter对象，用来提供Fragment给ViewPager2
    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    //定义一个ViewPager2对象，用来显示不同的页面
    private lateinit var viewPager: ViewPager2

    //定义一个TabLayout对象，用来显示不同的标签
    private lateinit var tabLayout: TabLayout

    private lateinit var notificationControl: NotificationAndControl

    private lateinit var notificationActionReceiver: NotificationActionReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var overviewFragment: OverViewFragment

    /**
     * 悬浮窗服务
     * */

    private lateinit var floatWindowService: FloatWindowService
    private var floatWindowServiceConnected = false

    private val floatWindowServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as FloatWindowService.FloatWindowBinder
            floatWindowService = binder.getService()
            floatWindowServiceConnected = true
            //Log.d("service is connected?", "yes")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            floatWindowServiceConnected = false
            //Log.d("service is connected?","No")

        }

    }




    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingInflatedId")
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
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            //直接使用getString()方法获取字符串资源的引用
            tab.text = getString(
                when (position) {
                    0 -> R.string.TabName_OverView
                    1 -> R.string.TabName_Battery
                    2 -> R.string.TabName_Thermal
                    3 -> R.string.TabName_Soc
                    4 -> R.string.TabName_LocalFiles

                    else -> throw IllegalStateException("Invalid position : $position")

                }
            )
        }.attach()



        // 绑定浮动窗口服务
        Intent(this, FloatWindowService::class.java).also { intent ->
            bindService(intent, floatWindowServiceConnection, BIND_AUTO_CREATE)
        }


        // Initialize NotificationAndControl
        notificationControl = NotificationAndControl(this)
        notificationControl.createNotification()

        // 初始化 LocalBroadcastManager
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        // Attach receiver for notification actions
        val receiver = NotificationActionReceiver()
        localBroadcastManager.registerReceiver(NotificationActionReceiver(), IntentFilter("start"))
        localBroadcastManager.registerReceiver(NotificationActionReceiver(), IntentFilter("stop"))




    }




    class SectionsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {



        //返回总的页面数
        override fun getItemCount(): Int {
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


    override fun onDestroy() {
        // 解绑浮动窗口服务
        // 如果解绑，屏幕旋转后，悬浮窗口会消失
        //unbindService(floatWindowServiceConnection)
        //floatWindowServiceConnected = false
        // 使用 LocalBroadcastManager 注销广播接收器
        localBroadcastManager.unregisterReceiver(notificationActionReceiver)

        super.onDestroy()
    }



    /**
     * 展开悬浮窗
     * */

    override fun showFloatWindow() {
        if (floatWindowServiceConnected) {
            floatWindowService.show()
        } else {
            // 如果服务未连接，可以在此处理未连接的情况
            Log.e("FloatWindowService", "Float window service is not connected")
        }
    }


    /**
     * 隐藏悬浮窗
     * */
    override fun hideFloatWindow() {
        if (floatWindowServiceConnected) {
            floatWindowService.hide()
        }
    }


    // In the NotificationActionReceiver create the logic to call start/stop in OverViewFragment
    // 声明为静态内部类
    class NotificationActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action != null) {
                val mainActivity = context as? MainActivity
                when (action) {
                    "start" -> mainActivity?.overviewFragment?.startDataCapture()
                    "stop" -> mainActivity?.overviewFragment?.stopDataCapture()
                }
                // 更新通知按钮状态
                mainActivity?.notificationControl?.toggleCapture()
            }
        }
    }

    // 调用此方法发送本地广播
    private fun sendLocalBroadcast(action: String) {
        val intent = Intent(action)
        localBroadcastManager.sendBroadcast(intent)
    }

    fun handleNotificationActionFromReceiver(action: String) {
        when (action) {
            "start" -> {
                // 确保 overviewFragment 已经初始化并且可以被调用
                if (::overviewFragment.isInitialized) {
                    overviewFragment.startDataCapture()
                }
            }
            "stop" -> {
                if (::overviewFragment.isInitialized) {
                    overviewFragment.stopDataCapture()
                }
            }
        }
        notificationControl.toggleCapture()
    }


}




