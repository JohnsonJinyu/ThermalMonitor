package com.example.thermalmonitor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.thermalmonitor.battery.BatteryFragment
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

                    else -> throw IllegalStateException("Invalid position : $position")

                }
            )
        }.attach()

    }

    class SectionsPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        //返回总的页面数
        override fun getItemCount():Int{
            return 4
        }
        //根据位置返回对应的Fragment实例
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OverViewFragment()
                1 -> BatteryFragment()
                2 -> ThermalFragment()
                3 -> SocFragment()

                else -> throw java.lang.IllegalStateException("Invalid position: $position")
            }

        }

    }

}


