package com.example.thermalmonitor.filesList

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thermalmonitor.databinding.FragmentFileslistBinding
import java.io.File



class FilesListFragment : Fragment() {

    // 创建视图绑定
    private lateinit var binding: FragmentFileslistBinding
    private lateinit var fileAdapter : FileAdapter

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


    // 在视图创建完成后，获取本地目录文件，并更新到视图UI中
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val fileList = getFileList()
        fileAdapter = FileAdapter(requireContext(),fileList)
        recycler.adapter = fileAdapter

        recycler.layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false)
        recycler.setHasFixedSize(true)


        // 手动调用适配器的 notifyDataSetChanged 方法来刷新视图
        fileAdapter.notifyDataSetChanged()
    }

    private fun getFileList(): List<ExcelFile> {
        val files = getRawFileList() ?: return emptyList()

        return files.map { file ->
            ExcelFile(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                fileMTTime = file.lastModified()
            )
        }



    }


    private fun getRawFileList(): Array<File>? {
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"ThermalMonitor")
        return folder.listFiles()
    }

}