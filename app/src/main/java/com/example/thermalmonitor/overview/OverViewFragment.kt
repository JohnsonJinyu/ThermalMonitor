package com.example.thermalmonitor.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thermalmonitor.databinding.FragmentOverviewBinding

class OverViewFragment : Fragment() {

    private lateinit var binding : FragmentOverviewBinding  //使用viewBinding绑定视图要素

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }
}
