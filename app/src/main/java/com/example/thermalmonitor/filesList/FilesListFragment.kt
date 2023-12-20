package com.example.thermalmonitor.filesList

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.thermalmonitor.databinding.FragmentFileslistBinding
import java.io.File


class FilesListFragment : Fragment() {

    // 创建视图绑定
    private lateinit var binding: FragmentFileslistBinding

    // 获取recyclerView的引用
    private val recycler by lazy { binding.recyclerViewFiles }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentFileslistBinding.inflate(inflater,container,false)


        return binding.root
    }

    val folder = File(Environment.getExternalStorageDirectory(),"/Download/thermalMonitor")
    val files = folder.listFiles { file ->
        file.isFile && file.name.endsWith(".xlsx")
    }

}